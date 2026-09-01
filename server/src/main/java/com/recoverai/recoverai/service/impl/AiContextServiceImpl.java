package com.recoverai.recoverai.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.recoverai.recoverai.dto.AiOperationalContext;
import com.recoverai.recoverai.dto.MetricsResponse;
import com.recoverai.recoverai.dto.MetricsTrendPoint;
import com.recoverai.recoverai.entity.AuditLog;
import com.recoverai.recoverai.entity.BatchRun;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.entity.RecoveryOutcome;
import com.recoverai.recoverai.entity.RecoveryOutcomeStatus;
import com.recoverai.recoverai.repository.AuditLogRepository;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.AiContextService;
import com.recoverai.recoverai.service.MetricsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiContextServiceImpl implements AiContextService {
    private static final Pattern MANDATE_ID_PATTERN =
            Pattern.compile("\\b[A-Za-z][A-Za-z0-9]*(?:[-_][A-Za-z0-9]+)*\\d[A-Za-z0-9]*(?:[-_][A-Za-z0-9]+)*\\b");

    private final FailedMandateRepository failedMandateRepository;
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository recoveryOutcomeRepository;
    private final AuditLogRepository auditLogRepository;
    private final BatchRunRepository batchRunRepository;
    private final MetricsService metricsService;

    @Override
    public AiOperationalContext buildContext(String question) {
        String safeQuestion = question == null ? "" : question.trim();
        Optional<String> mandateCandidate = extractMandateCandidate(safeQuestion);
        Optional<FailedMandate> mandate = mandateCandidate.flatMap(this::findMandateByCandidate);

        if (mandate.isPresent()) {
            return specificMandateContext(safeQuestion, mandate.get());
        }
        if (mandateCandidate.isPresent() && looksMandateSpecific(safeQuestion)) {
            return unknownMandateContext(safeQuestion, mandateCandidate.get());
        }

        Intent intent = classify(safeQuestion);
        return switch (intent) {
            case BATCH_ANALYSIS -> batchContext(safeQuestion);
            case RETRY_ANALYSIS -> retryContext(safeQuestion);
            case CUSTOMER -> customerRiskContext(safeQuestion);
            case MERCHANT -> merchantContext(safeQuestion);
            case AUDIT_DECISION -> auditDecisionContext(safeQuestion);
            case TRENDS_COMPARISONS -> trendContext(safeQuestion);
            case OPERATIONAL_RECOMMENDATIONS -> recommendationContext(safeQuestion);
            case RECOVERY_PERFORMANCE -> recoveryPerformanceContext(safeQuestion);
            case FAILURE_ANALYSIS -> failureAnalysisContext(safeQuestion);
            case SPECIFIC_MANDATE -> latestEscalatedMandateContext(safeQuestion);
            case GENERAL -> generalHelpContext(safeQuestion);
        };
    }

    private AiOperationalContext specificMandateContext(String question, FailedMandate mandate) {
        List<AuditLog> logs = auditLogRepository.findByMandateIdOrderByCreatedAtAsc(mandate.getMandateId());
        List<RecoveryDecision> decisions = decisionRepository.findByMandateIdOrderByCreatedAtDesc(mandate.getMandateId());
        List<RecoveryOutcome> outcomes = recoveryOutcomeRepository.findByMandateIdOrderByOutcomeTimestampDesc(mandate.getMandateId());
        Optional<RecoveryDecision> latestDecision = decisions.stream().findFirst();
        Optional<RecoveryOutcome> latestOutcome = outcomes.stream().findFirst();

        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "SPECIFIC_MANDATE", "MANDATE");
        context.append("selectedMandate: id=").append(mandate.getMandateId())
                .append(", merchantId=").append(mandate.getMerchantId())
                .append(", customerId=").append(mandate.getCustomerId())
                .append(", amount=").append(mandate.getAmount())
                .append(", failureReason=").append(mandate.getFailureReason())
                .append(", failureCode=").append(mandate.getFailureCode())
                .append(", failureTimestamp=").append(mandate.getFailureTimestamp())
                .append(", retryCount=").append(defaultInt(mandate.getRetryCount()))
                .append(", maxRetries=").append(mandate.getMaxRetries())
                .append(", nextRetryAt=").append(mandate.getNextRetryAt())
                .append(", mandateStatus=").append(mandate.getMandateStatus())
                .append(", paymentStatus=").append(mandate.getStatus())
                .append(", escalated=").append(mandate.getEscalated())
                .append(", escalationReason=").append(mandate.getEscalationReason())
                .append(", stopReason=").append(mandate.getStopReason())
                .append('\n');
        latestDecision.ifPresent(decision -> context.append("latestDecision: classification=").append(decision.getClassification())
                .append(", recoverabilityScore=").append(decision.getRecoverabilityScore())
                .append(", action=").append(decision.getAction())
                .append(", reasonCode=").append(decision.getDecisionReasonCode())
                .append(", scheduledAt=").append(decision.getScheduledAt())
                .append(", stopReason=").append(decision.getStopReason())
                .append(", escalated=").append(decision.getEscalated())
                .append(", escalationReason=").append(decision.getEscalationReason())
                .append(", confirmed=").append(decision.getConfirmed())
                .append(", manualOverride=").append(decision.getManualOverride())
                .append('\n'));
        latestOutcome.ifPresent(outcome -> context.append("latestOutcome: outcome=").append(outcome.getOutcome())
                .append(", actionTaken=").append(outcome.getActionTaken())
                .append(", recoveredAmount=").append(outcome.getRecoveredAmount())
                .append(", transactionId=").append(outcome.getTransactionId())
                .append(", outcomeTimestamp=").append(outcome.getOutcomeTimestamp())
                .append(", reason=").append(outcome.getSimulationReason())
                .append('\n'));
        context.append("decisionCountForMandate=").append(decisions.size()).append('\n');
        context.append("outcomeCountForMandate=").append(outcomes.size()).append('\n');
        context.append("auditEventCountForMandate=").append(logs.size()).append('\n');
        logs.stream().skip(Math.max(0, logs.size() - 10L)).forEach(log -> context
                .append("auditEvent: createdAt=").append(log.getCreatedAt())
                .append(", stage=").append(log.getStage())
                .append(", message=").append(log.getMessage())
                .append('\n'));

        return new AiOperationalContext(
                "SPECIFIC_MANDATE",
                "MANDATE",
                context.toString(),
                mandateFallback(mandate, latestDecision, logs.size()));
    }

    private AiOperationalContext latestEscalatedMandateContext(String question) {
        Optional<FailedMandate> latestEscalatedMandate = decisionRepository.findTopByEscalatedTrueOrderByCreatedAtDesc()
                .flatMap(decision -> failedMandateRepository.findTopByMandateIdOrderByCreatedAtDescIdDesc(decision.getMandateId()))
                .or(failedMandateRepository::findTopByEscalatedTrueOrderByCreatedAtDesc);
        return latestEscalatedMandate
                .map(mandate -> specificMandateContext(question, mandate))
                .orElseGet(() -> aggregateEmptyContext(question, "AUDIT_DECISION",
                        "No escalated mandate is available in RecoverAI data."));
    }

    private AiOperationalContext failureAnalysisContext(String question) {
        List<FailedMandate> mandates = failedMandateRepository.findAll();
        long totalFailures = mandates.stream().filter(this::isFailedMandate).count();
        Map<String, Long> failureCounts = topCounts(mandates.stream()
                .filter(this::isFailedMandate)
                .map(FailedMandate::getFailureReason), 8);
        Map<String, BigDecimal> revenueByFailure = topAmounts(mandates.stream()
                .filter(this::isFailedMandate)
                .collect(Collectors.groupingBy(
                        mandate -> valueOrUnknown(mandate.getFailureReason()),
                        Collectors.mapping(FailedMandate::getAmount, Collectors.reducing(BigDecimal.ZERO, this::safeAmount, BigDecimal::add)))));

        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "FAILURE_ANALYSIS", "AGGREGATE");
        context.append("totalFailedPayments=").append(totalFailures).append('\n');
        appendCountsWithPercentages(context, "failureReason", failureCounts, totalFailures);
        context.append("revenueAtRiskByFailureReason=").append(revenueByFailure).append('\n');
        context.append(metricsLine()).append('\n');

        return new AiOperationalContext(
                "FAILURE_ANALYSIS",
                "AGGREGATE",
                context.toString(),
                failureFallback(totalFailures, failureCounts));
    }

    private AiOperationalContext recoveryPerformanceContext(String question) {
        MetricsResponse metrics = metricsService.calculate();
        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "RECOVERY_PERFORMANCE", "AGGREGATE");
        context.append(metricsDetails(metrics));
        context.append("outcomeCounts=").append(topCounts(recoveryOutcomeRepository.findAll().stream()
                .map(outcome -> outcome.getOutcome() == null ? "UNKNOWN" : outcome.getOutcome().name()), 8)).append('\n');

        return new AiOperationalContext(
                "RECOVERY_PERFORMANCE",
                "AGGREGATE",
                context.toString(),
                """
                        KEY FINDING
                        Recovered revenue is %s against %s currently at risk.

                        EVIDENCE
                        - Recovery rate: %.2f%%
                        - Average recovery probability: %.2f%%
                        - Retry success rate: %.2f%%

                        RECOMMENDED ACTION
                        Focus review on high-risk failed mandates and low-probability recoveries before changing recovery strategy.
                        """.formatted(
                        metrics.recoveredRevenue(),
                        metrics.revenueAtRisk(),
                        metrics.recoveryRate(),
                        metrics.averageRecoveryProbability(),
                        metrics.retrySuccessRate()));
    }

    private AiOperationalContext retryContext(String question) {
        List<FailedMandate> mandates = failedMandateRepository.findAll();
        List<RecoveryOutcome> outcomes = recoveryOutcomeRepository.findAll();
        long attemptedMandates = mandates.stream().filter(mandate -> defaultInt(mandate.getRetryCount()) > 0).count();
        long retryAttempts = mandates.stream().mapToLong(mandate -> defaultInt(mandate.getRetryCount())).sum();
        long successfulRetries = outcomes.stream()
                .filter(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.SUCCESS)
                .filter(outcome -> outcome.getActionTaken() != null)
                .count();
        long failedRetries = outcomes.stream()
                .filter(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.FAILED)
                .filter(outcome -> outcome.getActionTaken() != null)
                .count();
        double successRate = retryAttempts == 0 ? 0.0 : successfulRetries * 100.0 / retryAttempts;

        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "RETRY_ANALYSIS", "AGGREGATE");
        context.append("mandatesWithRetries=").append(attemptedMandates).append('\n');
        context.append("retryAttemptsFromMandateRecords=").append(retryAttempts).append('\n');
        context.append("successfulRetryOutcomes=").append(successfulRetries).append('\n');
        context.append("failedRetryOutcomes=").append(failedRetries).append('\n');
        context.append("retrySuccessRate=").append(round(successRate)).append("%\n");
        context.append("topFailureReasonsAmongRetriedMandates=").append(topCounts(mandates.stream()
                .filter(mandate -> defaultInt(mandate.getRetryCount()) > 0)
                .map(FailedMandate::getFailureReason), 6)).append('\n');

        return new AiOperationalContext(
                "RETRY_ANALYSIS",
                "AGGREGATE",
                context.toString(),
                """
                        KEY FINDING
                        Retry success rate is %.2f%% based on available retry outcomes.

                        EVIDENCE
                        - Retry attempts recorded on mandates: %d
                        - Successful retry outcomes: %d
                        - Failed retry outcomes: %d

                        RECOMMENDED ACTION
                        Compare retry outcomes by failure reason before changing retry timing or limits.
                        """.formatted(successRate, retryAttempts, successfulRetries, failedRetries));
    }

    private AiOperationalContext batchContext(String question) {
        Optional<BatchRun> latestBatch = batchRunRepository.findAll().stream()
                .max(Comparator.comparing(this::batchSortTime));
        if (latestBatch.isEmpty()) {
            return aggregateEmptyContext(question, "BATCH_ANALYSIS", "No batch runs have been recorded yet.");
        }

        BatchRun batch = latestBatch.get();
        List<FailedMandate> mandates = failedMandateRepository.findByBatchRunId(batch.getId());
        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "BATCH_ANALYSIS", "BATCH");
        context.append("latestBatch: id=").append(batch.getId())
                .append(", status=").append(batch.getStatus())
                .append(", sourceFileName=").append(batch.getSourceFileName())
                .append(", sourceType=").append(batch.getSourceType())
                .append(", startedAt=").append(batch.getStartedAt())
                .append(", completedAt=").append(batch.getCompletedAt())
                .append(", totalMandates=").append(batch.getTotalMandates())
                .append(", successfulRecoveries=").append(batch.getSuccessfulRecoveries())
                .append(", failedRecoveries=").append(batch.getFailedRecoveries())
                .append(", recoveredRevenue=").append(batch.getRecoveredRevenue())
                .append(", errorMessage=").append(batch.getErrorMessage())
                .append('\n');
        context.append("batchFailureReasons=").append(topCounts(mandates.stream().map(FailedMandate::getFailureReason), 6)).append('\n');
        context.append("batchMerchantFailures=").append(topCounts(mandates.stream().map(FailedMandate::getMerchantId), 6)).append('\n');

        return new AiOperationalContext(
                "BATCH_ANALYSIS",
                "BATCH",
                context.toString(),
                """
                        LATEST BATCH
                        - Mandates processed: %s
                        - Recovered: %s
                        - Completion: %s

                        KEY FINDING
                        The latest batch recovered %s across %s mandates.

                        RECOMMENDED ACTION
                        Review the top failure reasons from this batch before the next run.
                        """.formatted(
                        batch.getTotalMandates(),
                        batch.getRecoveredRevenue(),
                        batch.getCompletedAt(),
                        batch.getRecoveredRevenue(),
                        batch.getTotalMandates()));
    }

    private AiOperationalContext customerRiskContext(String question) {
        List<FailedMandate> mandates = failedMandateRepository.findAll();
        Map<String, CustomerAggregate> byCustomer = new LinkedHashMap<>();
        mandates.forEach(mandate -> byCustomer
                .computeIfAbsent(valueOrUnknown(mandate.getCustomerId()), CustomerAggregate::new)
                .add(mandate));
        decisionRepository.findAll().forEach(decision -> findMandateByCandidate(decision.getMandateId())
                .ifPresent(mandate -> byCustomer
                        .computeIfAbsent(valueOrUnknown(mandate.getCustomerId()), CustomerAggregate::new)
                        .addDecision(decision)));

        List<CustomerAggregate> topCustomers = byCustomer.values().stream()
                .sorted(Comparator.comparing(CustomerAggregate::riskRank).reversed())
                .limit(8)
                .toList();
        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "CUSTOMER", "AGGREGATE");
        topCustomers.forEach(customer -> context.append("customerRisk: customerId=").append(customer.customerId)
                .append(", failedPayments=").append(customer.failedPayments)
                .append(", atRiskAmount=").append(customer.atRiskAmount)
                .append(", avgRecoverabilityScore=").append(round(customer.averageScore()))
                .append(", retryAttempts=").append(customer.retryAttempts)
                .append(", escalatedMandates=").append(customer.escalatedMandates)
                .append('\n'));

        return new AiOperationalContext(
                "CUSTOMER",
                "AGGREGATE",
                context.toString(),
                rankedFallback("CUSTOMER RISK", topCustomers.stream()
                        .map(customer -> "%s: %d failed payments, %s at risk"
                                .formatted(customer.customerId, customer.failedPayments, customer.atRiskAmount))
                        .toList()));
    }

    private AiOperationalContext merchantContext(String question) {
        List<FailedMandate> mandates = failedMandateRepository.findAll();
        Map<String, MerchantAggregate> byMerchant = mandates.stream()
                .collect(Collectors.groupingBy(
                        mandate -> valueOrUnknown(mandate.getMerchantId()),
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), MerchantAggregate::from)));
        List<MerchantAggregate> topMerchants = byMerchant.values().stream()
                .sorted(Comparator.comparing(MerchantAggregate::failureRateRank).reversed())
                .limit(8)
                .toList();

        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "MERCHANT", "AGGREGATE");
        topMerchants.forEach(merchant -> context.append("merchantPerformance: merchantId=").append(merchant.merchantId)
                .append(", failedPayments=").append(merchant.failedPayments)
                .append(", failedAmount=").append(merchant.failedAmount)
                .append(", recoveredAmount=").append(recoveredAmountForMandates(merchant.mandateIds))
                .append(", topFailureReasons=").append(merchant.topFailureReasons)
                .append('\n'));

        return new AiOperationalContext(
                "MERCHANT",
                "AGGREGATE",
                context.toString(),
                rankedFallback("MERCHANT PERFORMANCE", topMerchants.stream()
                        .map(merchant -> "%s: %d failed payments, %s failed amount"
                                .formatted(merchant.merchantId, merchant.failedPayments, merchant.failedAmount))
                        .toList()));
    }

    private AiOperationalContext auditDecisionContext(String question) {
        long customerActionRequired = decisionRepository.findAll().stream()
                .filter(decision -> containsAny(valueOrUnknown(decision.getAction()), "notify", "customer"))
                .count();
        long escalated = Stream.concat(
                        decisionRepository.findAll().stream().filter(decision -> Boolean.TRUE.equals(decision.getEscalated())).map(RecoveryDecision::getMandateId),
                        failedMandateRepository.findAll().stream().filter(mandate -> Boolean.TRUE.equals(mandate.getEscalated())).map(FailedMandate::getMandateId))
                .distinct()
                .count();
        Map<String, Long> actions = topCounts(decisionRepository.findAll().stream().map(RecoveryDecision::getAction), 8);
        Map<String, Long> classifications = topCounts(decisionRepository.findAll().stream().map(RecoveryDecision::getClassification), 8);

        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "AUDIT_DECISION", "AGGREGATE");
        context.append("customerActionRequiredCount=").append(customerActionRequired).append('\n');
        context.append("escalatedMandateCount=").append(escalated).append('\n');
        context.append("decisionActionCounts=").append(actions).append('\n');
        context.append("decisionClassificationCounts=").append(classifications).append('\n');
        context.append("latestAuditEvents=").append(auditLogRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(8)
                .map(log -> "%s:%s:%s".formatted(log.getMandateId(), log.getStage(), log.getMessage()))
                .toList()).append('\n');

        return new AiOperationalContext(
                "AUDIT_DECISION",
                "AGGREGATE",
                context.toString(),
                """
                        KEY FINDING
                        %d mandates are escalated and %d decisions require customer-oriented action.

                        EVIDENCE
                        - Decision actions: %s
                        - Classifications: %s

                        RECOMMENDED ACTION
                        Prioritize escalations and customer-action cases with permanent failure reasons.
                        """.formatted(escalated, customerActionRequired, actions, classifications));
    }

    private AiOperationalContext trendContext(String question) {
        List<MetricsTrendPoint> trends = metricsService.trends();
        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "TRENDS_COMPARISONS", "TREND");
        trends.forEach(point -> context.append("trendPoint: period=").append(point.name())
                .append(", totalFailed=").append(point.totalFailed())
                .append(", recovered=").append(point.recovered())
                .append(", aiRecovered=").append(point.aiRecovered())
                .append('\n'));
        return new AiOperationalContext(
                "TRENDS_COMPARISONS",
                "TREND",
                context.toString(),
                trends.isEmpty()
                        ? "I don't have enough RecoverAI trend data to answer that reliably."
                        : "KEY FINDING\nTrend data is available for %d periods.\n\nEVIDENCE\n- Latest period: %s\n\nRECOMMENDED ACTION\nCompare failed amount against recovered amount before adjusting operations."
                                .formatted(trends.size(), trends.getLast().name()));
    }

    private AiOperationalContext recommendationContext(String question) {
        AiOperationalContext failure = failureAnalysisContext(question);
        AiOperationalContext recovery = recoveryPerformanceContext(question);
        AiOperationalContext retry = retryContext(question);
        String context = failure.backendContext() + recovery.backendContext() + retry.backendContext();
        return new AiOperationalContext(
                "OPERATIONAL_RECOMMENDATIONS",
                "AGGREGATE",
                context,
                """
                        KEY FINDING
                        The biggest opportunity should be selected from failure concentration, revenue at risk, and retry effectiveness.

                        EVIDENCE
                        - %s
                        - %s

                        RECOMMENDED ACTION
                        Start with the highest-volume failure reason that also has meaningful revenue at risk.
                        """.formatted(topFailureLine(), metricsLine()));
    }

    private AiOperationalContext generalHelpContext(String question) {
        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "GENERAL", "HELP");
        context.append("supportedTopics=mandate recovery, payment failures, recovery performance, retries, batches, customer risk, merchant performance, audit decisions, revenue, trends, operational recommendations\n");
        return new AiOperationalContext(
                "GENERAL",
                "HELP",
                context.toString(),
                "I can help with RecoverAI operations such as mandate recovery, payment failures, recovery performance, retries, batches, customer risk, merchant performance, and audit decisions. Try asking about one of these areas.");
    }

    private AiOperationalContext aggregateEmptyContext(String question, String intent, String message) {
        StringBuilder context = new StringBuilder();
        appendHeader(context, question, intent, "AGGREGATE");
        context.append("dataAvailability=").append(message).append('\n');
        return new AiOperationalContext(intent, "AGGREGATE", context.toString(), message);
    }

    private AiOperationalContext unknownMandateContext(String question, String candidate) {
        StringBuilder context = new StringBuilder();
        appendHeader(context, question, "SPECIFIC_MANDATE", "MANDATE");
        context.append("selectedMandate=not_found\n");
        context.append("requestedMandateId=").append(stripIdentifierPunctuation(candidate)).append('\n');
        return new AiOperationalContext(
                "SPECIFIC_MANDATE",
                "MANDATE",
                context.toString(),
                """
                        KEY FINDING
                        I could not find mandate %s in the available RecoverAI data.

                        EVIDENCE
                        - No mandate record matched the requested identifier.

                        RECOMMENDED ACTION
                        Verify the mandate ID or ask an aggregate recovery question instead.
                        """.formatted(stripIdentifierPunctuation(candidate)));
    }

    private Intent classify(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        if (normalized.contains("mandate") && normalized.contains("escalat")) {
            return Intent.SPECIFIC_MANDATE;
        }
        if (containsAny(normalized, "batch", "run", "latest run", "last batch")) {
            return Intent.BATCH_ANALYSIS;
        }
        if (containsAny(normalized, "retry", "retries", "attempt")) {
            return Intent.RETRY_ANALYSIS;
        }
        if (containsAny(normalized, "customer", "customers", "portfolio")) {
            return Intent.CUSTOMER;
        }
        if (containsAny(normalized, "merchant", "merchants")) {
            return Intent.MERCHANT;
        }
        if (containsAny(normalized, "trend", "compare", "comparison", "increasing", "decreasing", "changed", "previous")) {
            return Intent.TRENDS_COMPARISONS;
        }
        if (containsAny(normalized, "recommend", "focus", "opportunity", "should we", "what should", "next")) {
            return Intent.OPERATIONAL_RECOMMENDATIONS;
        }
        if (containsAny(normalized, "audit", "decision", "decisions", "escalat", "customer action", "actions taken")) {
            return Intent.AUDIT_DECISION;
        }
        if (containsAny(normalized, "recovery", "recovering", "recovered", "revenue", "rate", "performance", "effective", "at risk")) {
            return Intent.RECOVERY_PERFORMANCE;
        }
        if (containsAny(normalized, "fail", "failure", "failures", "why are payments", "causing", "reason")) {
            return Intent.FAILURE_ANALYSIS;
        }
        if (containsAny(normalized, "mandate", "escalated")) {
            return Intent.SPECIFIC_MANDATE;
        }
        return Intent.GENERAL;
    }

    private Optional<String> extractMandateCandidate(String question) {
        Matcher matcher = MANDATE_ID_PATTERN.matcher(question);
        while (matcher.find()) {
            String candidate = stripIdentifierPunctuation(matcher.group());
            if (!isCommonNonIdentifier(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Optional<FailedMandate> findMandateByCandidate(String candidate) {
        String cleanedCandidate = stripIdentifierPunctuation(candidate);
        Optional<FailedMandate> exactMatch =
                failedMandateRepository.findTopByMandateIdOrderByCreatedAtDescIdDesc(cleanedCandidate);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        String uppercaseCandidate = cleanedCandidate.toUpperCase(Locale.ROOT);
        if (!uppercaseCandidate.equals(cleanedCandidate)) {
            Optional<FailedMandate> uppercaseMatch =
                    failedMandateRepository.findTopByMandateIdOrderByCreatedAtDescIdDesc(uppercaseCandidate);
            if (uppercaseMatch.isPresent()) {
                return uppercaseMatch;
            }
        }

        String normalizedCandidate = normalizeIdentifier(cleanedCandidate);
        return failedMandateRepository.findAll().stream()
                .filter(mandate -> hasText(mandate.getMandateId()))
                .filter(mandate -> normalizeIdentifier(mandate.getMandateId()).equals(normalizedCandidate))
                .findFirst();
    }

    private String mandateFallback(FailedMandate mandate, Optional<RecoveryDecision> latestDecision, int auditEventCount) {
        if (latestDecision.isPresent()) {
            RecoveryDecision decision = latestDecision.get();
            if (Boolean.TRUE.equals(decision.getEscalated()) || Boolean.TRUE.equals(mandate.getEscalated())) {
                String reason = firstPresent(decision.getEscalationReason(), mandate.getEscalationReason());
                return """
                        KEY FINDING
                        Mandate %s was escalated%s.

                        EVIDENCE
                        - Latest decision: %s
                        - Reason code: %s
                        - Recoverability score: %s
                        - Audit trail: %d related events

                        RECOMMENDED ACTION
                        Review the latest audit events before taking manual recovery action.
                        """.formatted(
                        mandate.getMandateId(),
                        hasText(reason) ? " because " + reason : ", but no escalation reason is present in the available data",
                        decision.getAction(),
                        decision.getDecisionReasonCode(),
                        decision.getRecoverabilityScore(),
                        auditEventCount);
            }
            return """
                    Mandate %s - Recovery Status

                    MANDATE OVERVIEW
                    - Amount: %s
                    - Mandate status: %s
                    - Payment status: %s
                    - Failure reason: %s

                    RECOVERY STATUS
                    - Recoverability: %s/100
                    - Retry attempts: %d of %s
                    - Latest action: %s

                    ANALYSIS
                    The latest decision is %s with reason code %s.

                    RECOMMENDED ACTION
                    Follow the latest recovery decision and verify the audit trail before manual intervention.
                    """.formatted(
                    mandate.getMandateId(),
                    mandate.getAmount(),
                    mandate.getMandateStatus(),
                    mandate.getStatus(),
                    mandate.getFailureReason(),
                    decision.getRecoverabilityScore(),
                    defaultInt(mandate.getRetryCount()),
                    mandate.getMaxRetries(),
                    decision.getAction(),
                    decision.getAction(),
                    decision.getDecisionReasonCode());
        }

        return """
                KEY FINDING
                Mandate %s exists, but no recovery decision is available.

                EVIDENCE
                - Failure reason: %s
                - Payment status: %s

                RECOMMENDED ACTION
                Run or review the recovery agent for this mandate before deciding the next step.
                """.formatted(mandate.getMandateId(), mandate.getFailureReason(), mandate.getStatus());
    }

    private String failureFallback(long totalFailures, Map<String, Long> failureCounts) {
        if (totalFailures == 0 || failureCounts.isEmpty()) {
            return "I don't have enough RecoverAI data to answer that reliably.";
        }
        String topReason = failureCounts.entrySet().iterator().next().getKey();
        long topCount = failureCounts.entrySet().iterator().next().getValue();
        return """
                KEY FINDING
                %s is currently the largest payment failure driver.

                EVIDENCE
                - %s: %.2f%% (%d of %d failed payments)

                ANALYSIS
                This failure reason is the largest visible operational concentration in the available RecoverAI data.

                RECOMMENDED ACTION
                Prioritize the recovery playbook for %s before the next retry cycle.
                """.formatted(topReason, topReason, topCount * 100.0 / totalFailures, topCount, totalFailures, topReason);
    }

    private String rankedFallback(String title, List<String> rows) {
        if (rows.isEmpty()) {
            return "I don't have enough RecoverAI data to answer that reliably.";
        }
        return """
                %s

                KEY FINDING
                The highest ranked record is %s.

                EVIDENCE
                - %s

                RECOMMENDED ACTION
                Review the top-ranked records first because they represent the largest visible operational exposure.
                """.formatted(title, rows.getFirst(), String.join("\n- ", rows));
    }

    private void appendHeader(StringBuilder context, String question, String intent, String contextType) {
        context.append("question=").append(question).append('\n');
        context.append("intent=").append(intent).append('\n');
        context.append("contextType=").append(contextType).append('\n');
        context.append("generatedOn=").append(LocalDate.now()).append('\n');
        context.append("backendTotals: mandates=").append(failedMandateRepository.count())
                .append(", decisions=").append(decisionRepository.count())
                .append(", auditEvents=").append(auditLogRepository.count())
                .append(", outcomes=").append(recoveryOutcomeRepository.count())
                .append(", batches=").append(batchRunRepository.count())
                .append('\n');
    }

    private void appendCountsWithPercentages(StringBuilder context, String label, Map<String, Long> counts, long total) {
        counts.forEach((key, count) -> context.append(label).append(": value=").append(key)
                .append(", count=").append(count)
                .append(", percentage=").append(total == 0 ? "0.00" : round(count * 100.0 / total))
                .append("%\n"));
    }

    private String metricsLine() {
        MetricsResponse metrics = metricsService.calculate();
        return "metrics: recoveredRevenue=%s, revenueAtRisk=%s, recoveryRate=%.2f%%, averageRecoveryProbability=%.2f%%, retrySuccessRate=%.2f%%"
                .formatted(
                        metrics.recoveredRevenue(),
                        metrics.revenueAtRisk(),
                        metrics.recoveryRate(),
                        metrics.averageRecoveryProbability(),
                        metrics.retrySuccessRate());
    }

    private String metricsDetails(MetricsResponse metrics) {
        return """
                recoveredRevenue=%s
                revenueAtRisk=%s
                predictedRecoverableRevenue=%s
                recoveryRate=%.2f%%
                averageRecoveryProbability=%.2f%%
                retrySuccessRate=%.2f%%
                highRiskCustomers=%d
                highValueCustomers=%d
                failedMandatesCount=%d
                recoveredMandatesCount=%d
                """.formatted(
                metrics.recoveredRevenue(),
                metrics.revenueAtRisk(),
                metrics.predictedRecoverableRevenue(),
                metrics.recoveryRate(),
                metrics.averageRecoveryProbability(),
                metrics.retrySuccessRate(),
                metrics.highRiskCustomers(),
                metrics.highValueCustomers(),
                metrics.failedMandatesCount(),
                metrics.recoveredMandatesCount());
    }

    private Map<String, Long> topCounts(Stream<String> values, int limit) {
        return values
                .map(this::valueOrUnknown)
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    private Map<String, BigDecimal> topAmounts(Map<String, BigDecimal> values) {
        return values.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    private BigDecimal recoveredAmountForMandates(List<String> mandateIds) {
        return recoveryOutcomeRepository.findAll().stream()
                .filter(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.SUCCESS)
                .filter(outcome -> mandateIds.contains(outcome.getMandateId()))
                .map(RecoveryOutcome::getRecoveredAmount)
                .reduce(BigDecimal.ZERO, this::addSafe);
    }

    private String topFailureLine() {
        List<FailedMandate> mandates = failedMandateRepository.findAll();
        Map<String, Long> counts = topCounts(mandates.stream().filter(this::isFailedMandate).map(FailedMandate::getFailureReason), 1);
        if (counts.isEmpty()) {
            return "No failed-payment driver is available";
        }
        Map.Entry<String, Long> entry = counts.entrySet().iterator().next();
        return "Top failure driver is %s with %d failed payments".formatted(entry.getKey(), entry.getValue());
    }

    private LocalDateTime batchSortTime(BatchRun batch) {
        return Stream.of(batch.getCompletedAt(), batch.getStartedAt())
                .filter(value -> value != null)
                .findFirst()
                .orElse(LocalDateTime.MIN);
    }

    private boolean looksMandateSpecific(String question) {
        return containsAny(question.toLowerCase(Locale.ROOT), "mandate", "check", "why did", "status", "fail", "escalat");
    }

    private boolean isCommonNonIdentifier(String candidate) {
        String normalized = candidate.toLowerCase(Locale.ROOT);
        return List.of("payment", "payments", "recoverai", "latest", "batch", "customer", "merchant").contains(normalized);
    }

    private boolean isFailedMandate(FailedMandate mandate) {
        return mandate.getStatus() == null || mandate.getStatus() == PaymentStatus.FAILED;
    }

    private String stripIdentifierPunctuation(String value) {
        return value == null ? "" : value.replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "");
    }

    private String normalizeIdentifier(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstPresent(String first, String second) {
        return hasText(first) ? first : second;
    }

    private String valueOrUnknown(String value) {
        return hasText(value) ? value : "UNKNOWN";
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal addSafe(BigDecimal first, BigDecimal second) {
        return safeAmount(first).add(safeAmount(second));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private enum Intent {
        SPECIFIC_MANDATE,
        CUSTOMER,
        MERCHANT,
        FAILURE_ANALYSIS,
        RECOVERY_PERFORMANCE,
        RETRY_ANALYSIS,
        BATCH_ANALYSIS,
        AUDIT_DECISION,
        TRENDS_COMPARISONS,
        OPERATIONAL_RECOMMENDATIONS,
        GENERAL
    }

    private static class CustomerAggregate {
        private final String customerId;
        private int failedPayments;
        private BigDecimal atRiskAmount = BigDecimal.ZERO;
        private int retryAttempts;
        private int scoreCount;
        private int scoreTotal;
        private int escalatedMandates;

        CustomerAggregate(String customerId) {
            this.customerId = customerId;
        }

        void add(FailedMandate mandate) {
            failedPayments++;
            atRiskAmount = atRiskAmount.add(mandate.getAmount() == null ? BigDecimal.ZERO : mandate.getAmount());
            retryAttempts += mandate.getRetryCount() == null ? 0 : mandate.getRetryCount();
            if (Boolean.TRUE.equals(mandate.getEscalated())) {
                escalatedMandates++;
            }
        }

        void addDecision(RecoveryDecision decision) {
            if (decision.getRecoverabilityScore() != null) {
                scoreTotal += decision.getRecoverabilityScore();
                scoreCount++;
            }
            if (Boolean.TRUE.equals(decision.getEscalated())) {
                escalatedMandates++;
            }
        }

        double averageScore() {
            return scoreCount == 0 ? 0.0 : scoreTotal * 1.0 / scoreCount;
        }

        BigDecimal riskRank() {
            return atRiskAmount
                    .add(BigDecimal.valueOf(failedPayments * 100L))
                    .add(BigDecimal.valueOf(escalatedMandates * 500L))
                    .add(BigDecimal.valueOf(Math.max(0.0, 100.0 - averageScore())));
        }
    }

    private static class MerchantAggregate {
        private final String merchantId;
        private final int failedPayments;
        private final BigDecimal failedAmount;
        private final Map<String, Long> topFailureReasons;
        private final List<String> mandateIds;

        private MerchantAggregate(String merchantId, int failedPayments, BigDecimal failedAmount, Map<String, Long> topFailureReasons, List<String> mandateIds) {
            this.merchantId = merchantId;
            this.failedPayments = failedPayments;
            this.failedAmount = failedAmount;
            this.topFailureReasons = topFailureReasons;
            this.mandateIds = mandateIds;
        }

        static MerchantAggregate from(List<FailedMandate> mandates) {
            String merchantId = mandates.stream().map(FailedMandate::getMerchantId).filter(value -> value != null && !value.isBlank()).findFirst().orElse("UNKNOWN");
            BigDecimal failedAmount = mandates.stream()
                    .map(FailedMandate::getAmount)
                    .reduce(BigDecimal.ZERO, (first, second) -> first.add(second == null ? BigDecimal.ZERO : second));
            Map<String, Long> topFailureReasons = mandates.stream()
                    .map(FailedMandate::getFailureReason)
                    .map(value -> value == null || value.isBlank() ? "UNKNOWN" : value)
                    .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
            List<String> mandateIds = mandates.stream().map(FailedMandate::getMandateId).toList();
            return new MerchantAggregate(merchantId, mandates.size(), failedAmount, topFailureReasons, mandateIds);
        }

        BigDecimal failureRateRank() {
            return failedAmount.add(BigDecimal.valueOf(failedPayments * 100L));
        }
    }
}
