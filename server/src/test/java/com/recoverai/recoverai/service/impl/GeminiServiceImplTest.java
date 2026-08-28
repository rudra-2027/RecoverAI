package com.recoverai.recoverai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.entity.AuditLog;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.entity.RecoveryStage;
import com.recoverai.recoverai.repository.AuditLogRepository;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.MetricsService;

class GeminiServiceImplTest {

    @Test
    void answerMerchantQuestionUsesBackendEscalationDataWithoutInventedCounts() {
        RecoverAiProperties properties = new RecoverAiProperties(3, 20, 10, 13, null, null);
        FailedMandateRepository failedMandateRepository = mock(FailedMandateRepository.class);
        RecoveryDecisionRepository decisionRepository = mock(RecoveryDecisionRepository.class);
        RecoveryOutcomeRepository recoveryOutcomeRepository = mock(RecoveryOutcomeRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        BatchRunRepository batchRunRepository = mock(BatchRunRepository.class);
        MetricsService metricsService = mock(MetricsService.class);

        FailedMandate mandate = FailedMandate.builder()
                .mandateId("MND-0007")
                .merchantId("MERCHANT-DEMO")
                .customerId("CUS-007")
                .amount(BigDecimal.valueOf(1999))
                .failureReason("MANDATE_REVOKED")
                .failureCode("ERR-MANDATE_REVOKED")
                .failureTimestamp(LocalDateTime.now())
                .retryCount(1)
                .maxRetries(3)
                .mandateStatus("ACTIVE")
                .status(PaymentStatus.FAILED)
                .escalated(true)
                .escalationReason("mandate was revoked")
                .createdAt(LocalDateTime.now())
                .build();
        RecoveryDecision decision = RecoveryDecision.builder()
                .id(42L)
                .mandateId("MND-0007")
                .classification("PERMANENT_FAILURE")
                .recoverabilityScore(0)
                .action("ESCALATE")
                .decisionReasonCode("MANDATE_REVOKED")
                .escalated(true)
                .escalationReason("mandate was revoked")
                .createdAt(LocalDateTime.now())
                .build();
        List<AuditLog> logs = List.of(
                auditLog(1L, "INGESTION", "Failure received"),
                auditLog(2L, "DECISION", "Escalation decision created"),
                auditLog(3L, "ESCALATED", "mandate was revoked"));

        when(decisionRepository.findTopByEscalatedTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(decision));
        when(decisionRepository.findTopByMandateIdOrderByCreatedAtDesc("MND-0007")).thenReturn(Optional.of(decision));
        when(failedMandateRepository.findByMandateId("MND-0007")).thenReturn(Optional.of(mandate));
        when(auditLogRepository.findByMandateIdOrderByCreatedAtAsc("MND-0007")).thenReturn(logs);
        when(failedMandateRepository.count()).thenReturn(1L);
        when(decisionRepository.count()).thenReturn(1L);
        when(auditLogRepository.count()).thenReturn(3L);
        when(recoveryOutcomeRepository.count()).thenReturn(0L);
        when(failedMandateRepository.findAll()).thenReturn(List.of(mandate));
        when(decisionRepository.findByMandateIdOrderByCreatedAtDesc("MND-0007")).thenReturn(List.of(decision));
        when(recoveryOutcomeRepository.findByMandateIdOrderByOutcomeTimestampDesc("MND-0007")).thenReturn(List.of());

        GeminiServiceImpl service = new GeminiServiceImpl(
                properties,
                failedMandateRepository,
                decisionRepository,
                recoveryOutcomeRepository,
                auditLogRepository,
                batchRunRepository,
                metricsService);

        String answer = service.answerMerchantQuestion("Why was the latest escalated mandate escalated?");

        assertThat(answer).contains("MND-0007", "mandate was revoked", "3 related events");
        assertThat(answer).doesNotContain("237", "This is an excellent example", "Why this answer is optimal", "In summary");
    }

    private AuditLog auditLog(Long id, String stage, String message) {
        return AuditLog.builder()
                .id(id)
                .mandateId("MND-0007")
                .stage(RecoveryStage.valueOf(stage))
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
