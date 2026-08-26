package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.MetricsResponse;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.entity.RecoveryOutcome;
import com.recoverai.recoverai.entity.RecoveryOutcomeStatus;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {
    private final FailedMandateRepository failedMandateRepository;
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository outcomeRepository;

    @Override
    public MetricsResponse calculate() {
        List<FailedMandate> failedMandates = failedMandateRepository.findAll();
        List<RecoveryDecision> decisions = decisionRepository.findAll();
        List<RecoveryOutcome> outcomes = outcomeRepository.findAll();

        BigDecimal revenueAtRisk = failedMandates.stream()
                .map(FailedMandate::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recoveredRevenue = outcomes.stream()
                .filter(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.SUCCESS)
                .map(RecoveryOutcome::getRecoveredAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double averageProbability = decisions.stream()
                .filter(decision -> decision.getRecoverabilityScore() != null)
                .mapToInt(RecoveryDecision::getRecoverabilityScore)
                .average()
                .orElse(0.0);
        BigDecimal predictedRecoverable = revenueAtRisk
                .multiply(BigDecimal.valueOf(averageProbability))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        long recoveredCount = outcomes.stream()
                .filter(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.SUCCESS)
                .map(RecoveryOutcome::getMandateId)
                .distinct()
                .count();
        long retryAttempts = outcomes.stream()
                .filter(outcome -> outcome.getActionTaken() != null)
                .count();
        double recoveryRate = failedMandates.isEmpty() ? 0.0 : recoveredCount * 100.0 / failedMandates.size();
        double retrySuccessRate = retryAttempts == 0 ? 0.0 : recoveredCount * 100.0 / retryAttempts;
        long highRiskCustomers = decisions.stream()
                .filter(decision -> decision.getRecoverabilityScore() != null && decision.getRecoverabilityScore() < 40)
                .map(RecoveryDecision::getMandateId)
                .distinct()
                .count();
        long highValueCustomers = failedMandates.stream()
                .filter(mandate -> mandate.getAmount().compareTo(BigDecimal.valueOf(10000)) >= 0)
                .map(FailedMandate::getCustomerId)
                .distinct()
                .count();

        return new MetricsResponse(
                recoveredRevenue,
                revenueAtRisk,
                predictedRecoverable,
                recoveredRevenue,
                recoveryRate,
                averageProbability,
                recoveredRevenue,
                retrySuccessRate,
                highRiskCustomers,
                highValueCustomers,
                failedMandates.size(),
                recoveredCount);
    }
}
