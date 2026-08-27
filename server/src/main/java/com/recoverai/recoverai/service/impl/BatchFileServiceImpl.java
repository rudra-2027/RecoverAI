package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.agent.RevenueRecoveryAgent;
import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.dto.BatchRunResult;
import com.recoverai.recoverai.dto.BatchUploadResult;
import com.recoverai.recoverai.dto.RecoveryResult;
import com.recoverai.recoverai.entity.BatchRun;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.entity.RecoveryOutcome;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.BatchFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchFileServiceImpl implements BatchFileService {
    private final BatchRunRepository batchRunRepository;
    private final FailedMandateRepository failedMandateRepository;
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository outcomeRepository;
    private final RevenueRecoveryAgent revenueRecoveryAgent;
    private final RecoverAiProperties properties;

    @Override
    public BatchUploadResult upload(MultipartFile file, boolean process) {
        if (file == null || file.isEmpty()) {
            log.warn("Rejected empty batch upload");
            throw new IllegalArgumentException("Upload file is required");
        }

        String fileName = Objects.requireNonNullElse(file.getOriginalFilename(), "uploaded-batch");
        String sourceType = sourceType(fileName);
        log.info("Uploading batch file name={}, sourceType={}, process={}", fileName, sourceType, process);
        BatchRun batchRun = batchRunRepository.save(BatchRun.builder()
                .startedAt(LocalDateTime.now())
                .sourceFileName(fileName)
                .sourceType(sourceType)
                .status(process ? "RUNNING" : "REGISTERED")
                .totalMandates(0)
                .successfulRecoveries(0)
                .failedRecoveries(0)
                .recoveredRevenue(BigDecimal.ZERO)
                .build());

        try {
            List<FailedMandate> mandates = parse(file, sourceType);
            log.info("Parsed {} mandates from batchRunId={}, fileName={}", mandates.size(), batchRun.getId(), fileName);
            mandates.forEach(mandate -> {
                mandate.setBatchRunId(batchRun.getId());
                mandate.setCreatedAt(LocalDateTime.now());
                if (mandate.getRetryCount() == null) {
                    mandate.setRetryCount(0);
                }
                if (mandate.getMaxRetries() == null) {
                    mandate.setMaxRetries(properties.maxRetries());
                }
                if (mandate.getStatus() == null) {
                    mandate.setStatus(PaymentStatus.FAILED);
                }
            });
            failedMandateRepository.saveAll(mandates);

            batchRun.setTotalMandates(mandates.size());
            batchRunRepository.save(batchRun);

            BatchRunResult processingResult = process ? runBatch(batchRun.getId()) : null;
            log.info("Batch upload completed for batchRunId={}, process={}, totalMandates={}",
                    batchRun.getId(), process, mandates.size());
            return new BatchUploadResult(
                    batchRun.getId(),
                    fileName,
                    sourceType,
                    mandates.size(),
                    process,
                    processingResult);
        } catch (RuntimeException ex) {
            log.error("Batch upload failed for batchRunId={}, fileName={}", batchRun.getId(), fileName, ex);
            batchRun.setStatus("FAILED");
            batchRun.setCompletedAt(LocalDateTime.now());
            batchRun.setErrorMessage(ex.getMessage());
            batchRunRepository.save(batchRun);
            throw ex;
        } catch (Exception ex) {
            log.error("Batch import failed for batchRunId={}, fileName={}", batchRun.getId(), fileName, ex);
            batchRun.setStatus("FAILED");
            batchRun.setCompletedAt(LocalDateTime.now());
            batchRun.setErrorMessage(ex.getMessage());
            batchRunRepository.save(batchRun);
            throw new IllegalArgumentException("Could not import batch file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public BatchRunResult runBatch(Long batchRunId) {
        log.info("Starting batch processing for batchRunId={}", batchRunId);
        BatchRun batchRun = batchRunRepository.findById(batchRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch run not found: " + batchRunId));
        List<FailedMandate> mandates = failedMandateRepository.findByBatchRunId(batchRunId);
        int successes = 0;
        int failures = 0;
        BigDecimal recoveredRevenue = BigDecimal.ZERO;

        batchRun.setStatus("RUNNING");
        batchRunRepository.save(batchRun);

        for (FailedMandate mandate : mandates) {
            RecoveryResult result = revenueRecoveryAgent.run(mandate);
            log.debug("Batch run id={} processed mandateId={} with outcome={}",
                    batchRunId, mandate.getMandateId(), result.outcome());
            if ("SUCCESS".equals(result.outcome())) {
                successes++;
                recoveredRevenue = recoveredRevenue.add(mandate.getAmount());
            } else {
                failures++;
            }
        }

        batchRun.setCompletedAt(LocalDateTime.now());
        batchRun.setTotalMandates(mandates.size());
        batchRun.setSuccessfulRecoveries(successes);
        batchRun.setFailedRecoveries(failures);
        batchRun.setRecoveredRevenue(recoveredRevenue);
        batchRun.setStatus("COMPLETED");
        batchRunRepository.save(batchRun);
        log.info("Completed batchRunId={} total={} successes={} failures={} recoveredRevenue={}",
                batchRun.getId(), mandates.size(), successes, failures, recoveredRevenue);

        double recoveryRate = mandates.isEmpty() ? 0.0 : successes * 100.0 / mandates.size();
        return new BatchRunResult(batchRun.getId(), mandates.size(), successes, failures, recoveredRevenue, recoveryRate, batchRun.getStatus());
    }

    @Override
    public byte[] exportReport(Long batchRunId) {
        log.info("Exporting batch report for batchRunId={}", batchRunId);
        batchRunRepository.findById(batchRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch run not found: " + batchRunId));
        List<FailedMandate> mandates = failedMandateRepository.findByBatchRunId(batchRunId);
        StringBuilder csv = new StringBuilder();
        csv.append("batchRunId,merchantId,customerId,mandateId,amount,failureReason,retryCount,action,score,outcome,recoveredAmount,transactionId\n");
        for (FailedMandate mandate : mandates) {
            RecoveryDecision decision = decisionRepository.findTopByMandateIdOrderByCreatedAtDesc(mandate.getMandateId()).orElse(null);
            RecoveryOutcome outcome = outcomeRepository.findByMandateIdOrderByOutcomeTimestampDesc(mandate.getMandateId())
                    .stream()
                    .findFirst()
                    .orElse(null);
            csv.append(batchRunId).append(',')
                    .append(escape(mandate.getMerchantId())).append(',')
                    .append(escape(mandate.getCustomerId())).append(',')
                    .append(escape(mandate.getMandateId())).append(',')
                    .append(mandate.getAmount()).append(',')
                    .append(escape(mandate.getFailureReason())).append(',')
                    .append(mandate.getRetryCount()).append(',')
                    .append(decision == null ? "" : escape(decision.getAction())).append(',')
                    .append(decision == null ? "" : decision.getRecoverabilityScore()).append(',')
                    .append(outcome == null ? "" : outcome.getOutcome()).append(',')
                    .append(outcome == null ? "" : outcome.getRecoveredAmount()).append(',')
                    .append(outcome == null ? "" : escape(outcome.getTransactionId()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<FailedMandate> parse(MultipartFile file, String sourceType) throws Exception {
        return switch (sourceType) {
            case "CSV" -> parseCsv(file);
            case "XLS", "XLSX" -> parseWorkbook(file);
            default -> throw new IllegalArgumentException("Only CSV, XLS, and XLSX files are supported");
        };
    }

    private List<FailedMandate> parseCsv(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return List.of();
            }
            List<String> headers = splitCsvLine(headerLine);
            List<FailedMandate> mandates = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    mandates.add(toMandate(headers, splitCsvLine(line)));
                }
            }
            return mandates;
        }
    }

    private List<FailedMandate> parseWorkbook(MultipartFile file) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.rowIterator();
            if (!rows.hasNext()) {
                return List.of();
            }
            DataFormatter formatter = new DataFormatter();
            List<String> headers = cells(rows.next(), formatter);
            List<FailedMandate> mandates = new ArrayList<>();
            while (rows.hasNext()) {
                Row row = rows.next();
                List<String> values = cells(row, formatter);
                if (values.stream().anyMatch(value -> !value.isBlank())) {
                    mandates.add(toMandate(headers, values));
                }
            }
            return mandates;
        }
    }

    private FailedMandate toMandate(List<String> headers, List<String> values) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(normalize(headers.get(i)), i < values.size() ? values.get(i).trim() : "");
        }

        return FailedMandate.builder()
                .merchantId(required(row, "merchantid"))
                .customerId(required(row, "customerid"))
                .mandateId(required(row, "mandateid"))
                .amount(new BigDecimal(required(row, "amount")))
                .failureReason(required(row, "failurereason"))
                .failureCode(row.get("failurecode"))
                .retryCount(parseInteger(row.get("retrycount")))
                .maxRetries(parseInteger(row.get("maxretries")))
                .failureTimestamp(parseDateTime(row.get("failuretimestamp"), LocalDateTime.now()))
                .paymentDate(parseDateTime(row.get("paymentdate"), null))
                .mandateStatus(row.getOrDefault("mandatestatus", "ACTIVE"))
                .status(PaymentStatus.FAILED)
                .escalated(false)
                .build();
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                quoted = !quoted;
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        values.add(value.toString());
        return values;
    }

    private List<String> cells(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            values.add(formatter.formatCellValue(row.getCell(i)));
        }
        return values;
    }

    private String sourceType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return "CSV";
        }
        if (lower.endsWith(".xls")) {
            return "XLS";
        }
        if (lower.endsWith(".xlsx")) {
            return "XLSX";
        }
        return "UNKNOWN";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").replace("_", "").replace("-", "").trim().toLowerCase(Locale.ROOT);
    }

    private String required(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required column: " + key);
        }
        return value;
    }

    private Integer parseInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime fallback) {
        return value == null || value.isBlank() ? fallback : LocalDateTime.parse(value);
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
