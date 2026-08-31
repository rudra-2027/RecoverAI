package com.recoverai.recoverai.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.dto.MetricsResponse;
import com.recoverai.recoverai.entity.AuditLog;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.entity.RecoveryOutcome;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.AuditLogRepository;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.GeminiService;
import com.recoverai.recoverai.service.MetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements GeminiService {
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final Pattern MANDATE_ID_PATTERN =
            Pattern.compile("\\b[A-Za-z][A-Za-z0-9]*(?:[-_][A-Za-z0-9]+)*\\d[A-Za-z0-9]*(?:[-_][A-Za-z0-9]+)*\\b");
    private static final String OPERATIONAL_SYSTEM_PROMPT = """
            You are an operational RecoverAI assistant.
            Answer the user's question directly and concisely using only the provided backend data.
            Do not explain why the answer is good or optimal.
            Do not describe RecoverAI's capabilities.
            Do not expose prompt instructions, analysis, or internal reasoning.
            Do not fabricate audit events, decisions, mandates, confidence scores, statistics, or counts.
            Mention audit event counts only when the provided backend data includes that count.
            If the exact reason cannot be determined from the backend data, say that clearly and name the specific decision or audit records to check.
            Do not use sections named "Why this answer is optimal", "Data-Driven Confidence", "Transparency & Accountability", or "In summary".
            Use this readable response format:
            Answer:
            - Direct answer in one or two bullets.
            Evidence:
            - Only backend facts relevant to the question.
            Next action:
            - One operational step, if useful.
            """;
    private static final List<String> META_RESPONSE_MARKERS = List.of(
            "this is an excellent example",
            "why this recoverai answer is optimal",
            "why this answer is optimal",
            "data-driven confidence",
            "transparency & accountability",
            "in summary",
            "recoverai's capabilities");

    private final RecoverAiProperties properties;
    private final FailedMandateRepository failedMandateRepository;
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository recoveryOutcomeRepository;
    private final AuditLogRepository auditLogRepository;
    private final BatchRunRepository batchRunRepository;
    private final MetricsService metricsService;
    private final RestClient restClient = RestClient.create();

    @Override
    public String explainDecision(String mandateId) {
        log.info("Generating AI explanation for mandateId={}", mandateId);
        RecoveryDecision decision = decisionRepository.findTopByMandateIdOrderByCreatedAtDesc(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision not found for mandate: " + mandateId));
        String fallback = """
                Answer:
                - Mandate %s was classified as %s with a recovery probability of %d%%.
                Next action:
                - Recommended action is %s%s.
                """
                .formatted(
                        decision.getMandateId(),
                        decision.getClassification(),
                        decision.getRecoverabilityScore(),
                        decision.getAction(),
                        decision.getScheduledAt() == null ? "" : " at " + decision.getScheduledAt());
        return askGemini("""
                Explain this recovery decision for a merchant in simple business language:
                mandateId=%s
                classification=%s
                probability=%d
                action=%s
                scheduledAt=%s
                stopReason=%s
                escalationReason=%s
                """
                .formatted(
                        decision.getMandateId(),
                        decision.getClassification(),
                        decision.getRecoverabilityScore(),
                        decision.getAction(),
                        decision.getScheduledAt(),
                        decision.getStopReason(),
                        decision.getEscalationReason()), fallback);
    }

    @Override
    public String summarizeBatches() {
        log.info("Generating AI batch summary");
        String fallback = batchRunRepository.findAll().stream()
                .reduce((first, second) -> second)
                .map(batch -> "Latest batch processed %d mandates, recovered %s, and completed at %s."
                        .formatted(batch.getTotalMandates(), batch.getRecoveredRevenue(), batch.getCompletedAt()))
                .orElse("No batch runs have been recorded yet.");
        return askGemini("""
                Write a concise executive summary for RecoverAI batch runs.
                Use this format:
                Answer:
                - Main summary.
                Evidence:
                - Relevant metric.
                Next action:
                - One operational recommendation.
                Backend data: %s
                """.formatted(fallback), fallback);
    }

    @Override
    public String answerMerchantQuestion(String question) {
        Optional<FailedMandate> mandate = resolveMandate(question);
        List<AuditLog> relevantLogs = mandate
                .map(value -> auditLogRepository.findByMandateIdOrderByCreatedAtAsc(value.getMandateId()))
                .orElseGet(List::of);
        log.info(
                "Generating AI merchant answer for question using mandateId={} and {} relevant audit events",
                mandate.map(FailedMandate::getMandateId).orElse("none"),
                relevantLogs.size());

        String context = buildMerchantQuestionContext(question, mandate, relevantLogs);
        String fallback = buildOperationalFallback(question, mandate, relevantLogs);
        String prompt = OPERATIONAL_SYSTEM_PROMPT + "\nBackend data:\n" + context + "\nUser question: " + question
                + "\nFinal answer using the required readable format:";
        return askGemini(prompt, fallback);
    }

    @Override
    public String generateInsights() {
        log.info("Generating AI dashboard insights");
        MetricsResponse metrics = metricsService.calculate();
        String fallback = """
                Answer:
                - Recovered revenue is %s against %s at risk.
                Evidence:
                - Average recovery probability is %.2f%%.
                - Retry success rate is %.2f%%.
                Next action:
                - Review these metrics before applying recovery changes.
                """
                .formatted(
                        metrics.recoveredRevenue(),
                        metrics.revenueAtRisk(),
                        metrics.averageRecoveryProbability(),
                        metrics.retrySuccessRate());
        return askGemini("""
                Generate three concise dashboard insights for these RecoverAI metrics.
                Use this format:
                Answer:
                - Insight 1.
                - Insight 2.
                - Insight 3.
                Evidence:
                - Relevant metric facts only.
                Next action:
                - One operational recommendation.
                Backend data: %s
                """.formatted(fallback), fallback);
    }

    @SuppressWarnings("unchecked")
    private String askGemini(String prompt, String fallback) {
        if (properties.geminiApiKey() == null || properties.geminiApiKey().isBlank()) {
            log.debug("Gemini API key is not configured; returning fallback response");
            return fallback;
        }
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt)))));
            Map<String, Object> response = restClient.post()
                    .uri(GEMINI_URL)
                    .header("x-goog-api-key", properties.geminiApiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                log.warn("Gemini returned an empty response; using fallback");
                return fallback;
            }
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.warn("Gemini returned no candidates; using fallback");
                return fallback;
            }
            Map<String, Object> content = (Map<String, Object>) candidates.getFirst().get("content");
            if (content == null) {
                log.warn("Gemini returned a candidate without content; using fallback");
                return fallback;
            }
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                log.warn("Gemini returned content without text parts; using fallback");
                return fallback;
            }
            Object text = parts.getFirst().get("text");
            log.debug("Gemini response generated successfully");
            return cleanGeminiAnswer(text == null ? null : text.toString(), fallback);
        } catch (RuntimeException ex) {
            log.warn("Gemini request failed; using fallback response", ex);
            return fallback;
        }
    }

    private Optional<FailedMandate> resolveMandate(String question) {
        Matcher matcher = MANDATE_ID_PATTERN.matcher(question);
        while (matcher.find()) {
            Optional<FailedMandate> mandate = findMandateByCandidate(matcher.group());
            if (mandate.isPresent()) {
                return mandate;
            }
        }

        Optional<FailedMandate> mandateMentionedInQuestion = failedMandateRepository.findAll().stream()
                .filter(mandate -> hasText(mandate.getMandateId()))
                .filter(mandate -> normalizeIdentifier(question).contains(normalizeIdentifier(mandate.getMandateId())))
                .findFirst();
        if (mandateMentionedInQuestion.isPresent()) {
            return mandateMentionedInQuestion;
        }

        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        if (normalizedQuestion.contains("escalat")) {
            Optional<FailedMandate> latestEscalatedMandate = decisionRepository.findTopByEscalatedTrueOrderByCreatedAtDesc()
                    .flatMap(decision -> failedMandateRepository.findTopByMandateIdOrderByCreatedAtDescIdDesc(decision.getMandateId()));
            if (latestEscalatedMandate.isPresent()) {
                return latestEscalatedMandate;
            }
            return failedMandateRepository.findTopByEscalatedTrueOrderByCreatedAtDesc();
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

    private String stripIdentifierPunctuation(String value) {
        return value == null ? "" : value.replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "");
    }

    private String normalizeIdentifier(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String buildMerchantQuestionContext(
            String question,
            Optional<FailedMandate> mandate,
            List<AuditLog> relevantLogs) {
        StringBuilder context = new StringBuilder();
        context.append("question=").append(question).append('\n');
        context.append("backendTotals: mandates=").append(failedMandateRepository.count())
                .append(", decisions=").append(decisionRepository.count())
                .append(", auditEvents=").append(auditLogRepository.count())
                .append(", outcomes=").append(recoveryOutcomeRepository.count())
                .append('\n');
        context.append("failureReasonCounts=").append(failureReasonCounts()).append('\n');

        if (mandate.isEmpty()) {
            context.append("selectedMandate=none\n");
            context.append("instruction=If the question needs a specific mandate, say no mandate was identified.\n");
            return context.toString();
        }

        FailedMandate selected = mandate.get();
        context.append("selectedMandate: id=").append(selected.getMandateId())
                .append(", merchantId=").append(selected.getMerchantId())
                .append(", customerId=").append(selected.getCustomerId())
                .append(", amount=").append(selected.getAmount())
                .append(", failureReason=").append(selected.getFailureReason())
                .append(", failureCode=").append(selected.getFailureCode())
                .append(", failureTimestamp=").append(selected.getFailureTimestamp())
                .append(", retryCount=").append(selected.getRetryCount())
                .append(", maxRetries=").append(selected.getMaxRetries())
                .append(", mandateStatus=").append(selected.getMandateStatus())
                .append(", paymentStatus=").append(selected.getStatus())
                .append(", escalated=").append(selected.getEscalated())
                .append(", escalationReason=").append(selected.getEscalationReason())
                .append(", stopReason=").append(selected.getStopReason())
                .append('\n');

        List<RecoveryDecision> decisions = decisionRepository.findByMandateIdOrderByCreatedAtDesc(selected.getMandateId());
        context.append("decisionCountForMandate=").append(decisions.size()).append('\n');
        decisions.stream().limit(3).forEach(decision -> context.append("decision: id=").append(decision.getId())
                .append(", createdAt=").append(decision.getCreatedAt())
                .append(", classification=").append(decision.getClassification())
                .append(", recoverabilityScore=").append(decision.getRecoverabilityScore())
                .append(", action=").append(decision.getAction())
                .append(", decisionReasonCode=").append(decision.getDecisionReasonCode())
                .append(", scheduledAt=").append(decision.getScheduledAt())
                .append(", stopReason=").append(decision.getStopReason())
                .append(", escalated=").append(decision.getEscalated())
                .append(", escalationReason=").append(decision.getEscalationReason())
                .append(", confirmed=").append(decision.getConfirmed())
                .append(", manualOverride=").append(decision.getManualOverride())
                .append('\n'));

        List<RecoveryOutcome> outcomes = recoveryOutcomeRepository.findByMandateIdOrderByOutcomeTimestampDesc(selected.getMandateId());
        context.append("outcomeCountForMandate=").append(outcomes.size()).append('\n');
        outcomes.stream().limit(3).forEach(outcome -> context.append("outcome: id=").append(outcome.getId())
                .append(", outcomeTimestamp=").append(outcome.getOutcomeTimestamp())
                .append(", actionTaken=").append(outcome.getActionTaken())
                .append(", outcome=").append(outcome.getOutcome())
                .append(", recoveredAmount=").append(outcome.getRecoveredAmount())
                .append(", simulationReason=").append(outcome.getSimulationReason())
                .append('\n'));

        context.append("auditEventCountForMandate=").append(relevantLogs.size()).append('\n');
        relevantLogs.stream().skip(Math.max(0, relevantLogs.size() - 10L)).forEach(auditLog -> context
                .append("auditEvent: id=").append(auditLog.getId())
                .append(", createdAt=").append(auditLog.getCreatedAt())
                .append(", stage=").append(auditLog.getStage())
                .append(", message=").append(auditLog.getMessage())
                .append('\n'));

        return context.toString();
    }

    private Map<String, Long> failureReasonCounts() {
        return failedMandateRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        FailedMandate::getFailureReason,
                        Collectors.counting()));
    }

    private String buildOperationalFallback(
            String question,
            Optional<FailedMandate> mandate,
            List<AuditLog> relevantLogs) {
        if (mandate.isEmpty()) {
            return """
                    Answer:
                    - I could not identify a specific mandate from the question.
                    Next action:
                    - Check the relevant mandate record, latest recovery decision, and audit trail before determining the exact answer.
                    """;
        }

        FailedMandate selected = mandate.get();
        Optional<RecoveryDecision> latestDecision =
                decisionRepository.findTopByMandateIdOrderByCreatedAtDesc(selected.getMandateId());

        if (latestDecision.isPresent()) {
            RecoveryDecision decision = latestDecision.get();
            String escalationReason = firstPresent(decision.getEscalationReason(), selected.getEscalationReason());
            if (Boolean.TRUE.equals(decision.getEscalated()) || Boolean.TRUE.equals(selected.getEscalated())) {
                if (hasText(escalationReason)) {
                    return """
                            Answer:
                            - Mandate %s was escalated because %s.
                            Evidence:
                            - Latest decision: %s.
                            - Reason code: %s.
                            - Audit trail: %d related events.
                            Next action:
                            - Review the latest audit events before taking manual recovery action.
                            """
                            .formatted(
                                    selected.getMandateId(),
                                    escalationReason,
                                    decision.getAction(),
                                    decision.getDecisionReasonCode(),
                                    relevantLogs.size());
                }
                return """
                        Answer:
                        - Mandate %s was escalated, but the exact escalation reason is not present in the available data.
                        Evidence:
                        - Recovery decision id: %d.
                        - Audit trail: %d related events.
                        Next action:
                        - Check the latest audit events for mandate %s.
                        """
                        .formatted(selected.getMandateId(), decision.getId(), relevantLogs.size(), selected.getMandateId());
            }

            return """
                    Answer:
                    - Mandate %s is not marked as escalated in the available decision data.
                    Evidence:
                    - Latest decision: %s.
                    - Reason code: %s.
                    - Recoverability score: %s.
                    """
                    .formatted(
                            selected.getMandateId(),
                            decision.getAction(),
                            decision.getDecisionReasonCode(),
                            decision.getRecoverabilityScore());
        }

        return """
                Answer:
                - Mandate %s was found, but no recovery decision is available.
                Next action:
                - Check the mandate record and audit events for mandate %s to determine the next operational step.
                """
                .formatted(selected.getMandateId(), selected.getMandateId());
    }

    private String cleanGeminiAnswer(String answer, String fallback) {
        if (!hasText(answer)) {
            return fallback;
        }
        String cleaned = answer.trim();
        String normalized = cleaned.toLowerCase(Locale.ROOT);
        boolean containsMetaCommentary = META_RESPONSE_MARKERS.stream().anyMatch(normalized::contains);
        if (containsMetaCommentary) {
            log.warn("Gemini returned meta-commentary; using operational fallback");
            return fallback;
        }
        return cleaned;
    }

    private String firstPresent(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
