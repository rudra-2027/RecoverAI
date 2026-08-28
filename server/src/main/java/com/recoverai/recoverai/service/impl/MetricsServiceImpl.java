package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.MetricsResponse;
import com.recoverai.recoverai.dto.MetricsTrendPoint;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {
    private final FailedMandateRepository failedMandateRepository;
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository outcomeRepository;

    @Override
    public MetricsResponse calculate() {
        List<FailedMandate> failedMandates = failedMandateRepository.findByStatus(PaymentStatus.FAILED);
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

    @Override
    public List<MetricsTrendPoint> trends() {
        List<FailedMandate> failedMandates = failedMandateRepository.findAll();
        List<RecoveryOutcome> outcomes = outcomeRepository.findAll();

        YearMonth latestFailureMonth = failedMandates.stream()
                .filter(mandate -> mandate.getFailureTimestamp() != null)
                .map(mandate -> YearMonth.from(mandate.getFailureTimestamp()))
                .max(Comparator.naturalOrder())
                .orElse(null);
        YearMonth latestOutcomeMonth = outcomes.stream()
                .filter(outcome -> outcome.getOutcomeTimestamp() != null)
                .map(outcome -> YearMonth.from(outcome.getOutcomeTimestamp()))
                .max(Comparator.naturalOrder())
                .orElse(null);
        YearMonth latestMonth = Stream.of(latestFailureMonth, latestOutcomeMonth, YearMonth.now())
                .filter(month -> month != null)
                .max(Comparator.naturalOrder())
                .orElse(YearMonth.now());
        YearMonth startMonth = latestMonth.minusMonths(5);

        Map<YearMonth, BigDecimal> failedByMonth = failedMandates.stream()
                .filter(mandate -> mandate.getFailureTimestamp() != null)
                .filter(mandate -> !YearMonth.from(mandate.getFailureTimestamp()).isBefore(startMonth))
                .collect(Collectors.groupingBy(
                        mandate -> YearMonth.from(mandate.getFailureTimestamp()),
                        TreeMap::new,
                        Collectors.mapping(FailedMandate::getAmount, Collectors.reducing(BigDecimal.ZERO, this::safeAmount, BigDecimal::add))));

        Map<YearMonth, BigDecimal> recoveredByMonth = outcomes.stream()
                .filter(outcome -> outcome.getOutcomeTimestamp() != null)
                .filter(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.SUCCESS)
                .filter(outcome -> !YearMonth.from(outcome.getOutcomeTimestamp()).isBefore(startMonth))
                .collect(Collectors.groupingBy(
                        outcome -> YearMonth.from(outcome.getOutcomeTimestamp()),
                        TreeMap::new,
                        Collectors.mapping(RecoveryOutcome::getRecoveredAmount, Collectors.reducing(BigDecimal.ZERO, this::safeAmount, BigDecimal::add))));

        Map<YearMonth, BigDecimal> aiRecoveredByMonth = outcomes.stream()
                .filter(outcome -> outcome.getOutcomeTimestamp() != null)
                .filter(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.SUCCESS)
                .filter(outcome -> outcome.getActionTaken() != null)
                .filter(outcome -> !YearMonth.from(outcome.getOutcomeTimestamp()).isBefore(startMonth))
                .collect(Collectors.groupingBy(
                        outcome -> YearMonth.from(outcome.getOutcomeTimestamp()),
                        TreeMap::new,
                        Collectors.mapping(RecoveryOutcome::getRecoveredAmount, Collectors.reducing(BigDecimal.ZERO, this::safeAmount, BigDecimal::add))));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yy");
        return Stream.iterate(startMonth, month -> month.plusMonths(1))
                .limit(6)
                .map(month -> new MetricsTrendPoint(
                        month.format(formatter),
                        failedByMonth.getOrDefault(month, BigDecimal.ZERO),
                        recoveredByMonth.getOrDefault(month, BigDecimal.ZERO),
                        aiRecoveredByMonth.getOrDefault(month, BigDecimal.ZERO)))
                .toList();
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
