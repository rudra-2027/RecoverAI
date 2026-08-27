package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.dto.CreateFailedMandateRequest;
import com.recoverai.recoverai.dto.CreateMerchantRequest;
import com.recoverai.recoverai.dto.CreatePaymentHistoryRequest;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.Merchant;
import com.recoverai.recoverai.entity.PaymentHistory;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.MerchantRepository;
import com.recoverai.recoverai.repository.PaymentHistoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/ingest")
public class IngestionController {
    private final MerchantRepository merchantRepository;
    private final FailedMandateRepository failedMandateRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final RecoverAiProperties properties;

    @PostMapping("/merchants")
    @ResponseStatus(HttpStatus.CREATED)
    public Merchant createMerchant(@Valid @RequestBody CreateMerchantRequest request) {
        log.info("Creating merchant merchantId={}", request.merchantId());
        Merchant merchant = Merchant.builder()
                .merchantId(request.merchantId())
                .merchantName(request.merchantName())
                .maxRetries(request.maxRetries() == null ? properties.maxRetries() : request.maxRetries())
                .peakStartHour(request.peakStartHour() == null ? properties.peakStartHour() : request.peakStartHour())
                .peakEndHour(request.peakEndHour() == null ? properties.peakEndHour() : request.peakEndHour())
                .active(request.active() == null || request.active())
                .createdAt(LocalDateTime.now())
                .build();
        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant created merchantId={}, id={}", saved.getMerchantId(), saved.getId());
        return saved;
    }

    @GetMapping("/merchants")
    public List<Merchant> merchants() {
        return merchantRepository.findAll();
    }

    @PostMapping("/failed-mandates")
    @ResponseStatus(HttpStatus.CREATED)
    public FailedMandate createFailedMandate(@Valid @RequestBody CreateFailedMandateRequest request) {
        log.info("Creating failed mandate mandateId={}, merchantId={}", request.mandateId(), request.merchantId());
        FailedMandate mandate = FailedMandate.builder()
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .mandateId(request.mandateId())
                .amount(request.amount())
                .failureReason(request.failureReason())
                .failureCode(request.failureCode())
                .retryCount(request.retryCount() == null ? 0 : request.retryCount())
                .maxRetries(request.maxRetries() == null ? properties.maxRetries() : request.maxRetries())
                .failureTimestamp(request.failureTimestamp() == null ? LocalDateTime.now() : request.failureTimestamp())
                .paymentDate(request.paymentDate())
                .mandateStatus(request.mandateStatus() == null ? "ACTIVE" : request.mandateStatus())
                .status(PaymentStatus.FAILED)
                .escalated(false)
                .createdAt(LocalDateTime.now())
                .build();
        FailedMandate saved = failedMandateRepository.save(mandate);
        log.info("Failed mandate created mandateId={}, id={}", saved.getMandateId(), saved.getId());
        return saved;
    }

    @GetMapping("/failed-mandates")
    public List<FailedMandate> failedMandates() {
        return failedMandateRepository.findAll();
    }

    @GetMapping("/failed-mandates/export")
    public ResponseEntity<byte[]> exportFailedMandates() {
        log.info("Failed mandates export requested");
        StringBuilder csv = new StringBuilder();
        csv.append("id,merchantId,customerId,mandateId,amount,failureReason,failureCode,retryCount,maxRetries,status,mandateStatus,nextRetryAt,stopReason,escalated,createdAt\n");
        for (FailedMandate mandate : failedMandateRepository.findAll()) {
            csv.append(mandate.getId()).append(',')
                    .append(escape(mandate.getMerchantId())).append(',')
                    .append(escape(mandate.getCustomerId())).append(',')
                    .append(escape(mandate.getMandateId())).append(',')
                    .append(mandate.getAmount()).append(',')
                    .append(escape(mandate.getFailureReason())).append(',')
                    .append(escape(mandate.getFailureCode())).append(',')
                    .append(mandate.getRetryCount()).append(',')
                    .append(mandate.getMaxRetries()).append(',')
                    .append(mandate.getStatus()).append(',')
                    .append(escape(mandate.getMandateStatus())).append(',')
                    .append(mandate.getNextRetryAt()).append(',')
                    .append(mandate.getStopReason()).append(',')
                    .append(mandate.getEscalated()).append(',')
                    .append(mandate.getCreatedAt())
                    .append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"failed-mandates.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/payment-history")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentHistory createPaymentHistory(@Valid @RequestBody CreatePaymentHistoryRequest request) {
        log.info("Creating payment history mandateId={}, status={}", request.mandateId(), request.status());
        PaymentHistory history = PaymentHistory.builder()
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .mandateId(request.mandateId())
                .amount(request.amount())
                .status(request.status())
                .reason(request.reason())
                .transactionTime(request.transactionTime() == null ? LocalDateTime.now() : request.transactionTime())
                .paymentDate(request.paymentDate())
                .build();
        PaymentHistory saved = paymentHistoryRepository.save(history);
        log.info("Payment history created mandateId={}, id={}", saved.getMandateId(), saved.getId());
        return saved;
    }

    @GetMapping("/payment-history")
    public List<PaymentHistory> paymentHistory() {
        return paymentHistoryRepository.findAll();
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
