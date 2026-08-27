package com.recoverai.recoverai.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.dto.MetricsResponse;
import com.recoverai.recoverai.entity.AuditLog;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.AuditLogRepository;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
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

    private final RecoverAiProperties properties;
    private final RecoveryDecisionRepository decisionRepository;
    private final AuditLogRepository auditLogRepository;
    private final BatchRunRepository batchRunRepository;
    private final MetricsService metricsService;
    private final RestClient restClient = RestClient.create();

    @Override
    public String explainDecision(String mandateId) {
        log.info("Generating AI explanation for mandateId={}", mandateId);
        RecoveryDecision decision = decisionRepository.findTopByMandateIdOrderByCreatedAtDesc(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision not found for mandate: " + mandateId));
        String fallback = "Mandate %s was classified as %s with a recovery probability of %d%%. Recommended action is %s%s."
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
        return askGemini("Write a concise executive summary for RecoverAI batch runs. " + fallback, fallback);
    }

    @Override
    public String answerMerchantQuestion(String question) {
        List<AuditLog> logs = auditLogRepository.findAll();
        log.info("Generating AI merchant answer using {} audit events", logs.size());
        String fallback = "Based on %d audit events, the current answer to '%s' is best found by checking the latest decisions and audit trail for the mandate in question."
                .formatted(logs.size(), question);
        return askGemini("Answer this merchant question using the RecoverAI audit/decision context: " + fallback, fallback);
    }

    @Override
    public String generateInsights() {
        log.info("Generating AI dashboard insights");
        MetricsResponse metrics = metricsService.calculate();
        String fallback = "Recovered revenue is %s against %s at risk. Average recovery probability is %.2f%% and retry success rate is %.2f%%."
                .formatted(
                        metrics.recoveredRevenue(),
                        metrics.revenueAtRisk(),
                        metrics.averageRecoveryProbability(),
                        metrics.retrySuccessRate());
        return askGemini("Generate three concise dashboard insights for these RecoverAI metrics: " + fallback, fallback);
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
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            Object text = parts.getFirst().get("text");
            log.debug("Gemini response generated successfully");
            return text == null ? fallback : text.toString();
        } catch (RuntimeException ex) {
            log.warn("Gemini request failed; using fallback response", ex);
            return "Gemini Error";
        }
    }
}
