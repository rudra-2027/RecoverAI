package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.agent.RevenueRecoveryAgent;
import com.recoverai.recoverai.dto.BatchRunResult;
import com.recoverai.recoverai.dto.RecoveryResult;
import com.recoverai.recoverai.entity.BatchRun;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchProcessingServiceImpl implements com.recoverai.recoverai.service.BatchProcessingService {
    private final FailedMandateRepository failedMandateRepository;
    private final BatchRunRepository batchRunRepository;
    private final RevenueRecoveryAgent revenueRecoveryAgent;

    @Override
    public BatchRunResult runAllFailedMandates() {
        BatchRun batchRun = BatchRun.builder()
                .startedAt(LocalDateTime.now())
                .recoveredRevenue(BigDecimal.ZERO)
                .successfulRecoveries(0)
                .failedRecoveries(0)
                .totalMandates(0)
                .status("RUNNING")
                .build();
        batchRunRepository.save(batchRun);

        List<FailedMandate> mandates = failedMandateRepository.findAll();
        int successes = 0;
        int failures = 0;
        BigDecimal recoveredRevenue = BigDecimal.ZERO;

        for (FailedMandate mandate : mandates) {
            RecoveryResult result = revenueRecoveryAgent.run(mandate);
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

        double recoveryRate = mandates.isEmpty() ? 0.0 : (successes * 100.0) / mandates.size();
        return new BatchRunResult(batchRun.getId(), mandates.size(), successes, failures, recoveredRevenue, recoveryRate, batchRun.getStatus());
    }
}
