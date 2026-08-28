package com.recoverai.recoverai.agent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.dto.DecisionResult;
import com.recoverai.recoverai.dto.RecoveryResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.entity.RecoveryAction;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.entity.RecoveryOutcome;
import com.recoverai.recoverai.entity.RecoveryOutcomeStatus;
import com.recoverai.recoverai.gateway.PaymentGateway;
import com.recoverai.recoverai.gateway.PaymentResult;
import com.recoverai.recoverai.entity.StopReason;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.AuditService;
import com.recoverai.recoverai.service.DecisionService;
import com.recoverai.recoverai.service.FailureAnalysisService;
import com.recoverai.recoverai.service.PaymentVerificationService;
import com.recoverai.recoverai.service.ScoringService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueRecoveryAgent {

    private final FailureAnalysisService failureAnalysisService;
    private final ScoringService scoringService;
    private final DecisionService decisionService;
    private final PaymentVerificationService paymentVerificationService;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;

    private final FailedMandateRepository failedMandateRepository;
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository outcomeRepository;

    public RecoveryResult run(FailedMandate mandate) {
        log.info("Starting revenue recovery for mandateId={}, merchantId={}", mandate.getMandateId(), mandate.getMerchantId());
        auditService.log(mandate.getMandateId(), "INGESTION", "Failure received");

        AnalysisResult analysis =
                failureAnalysisService.analyze(mandate);
        log.debug("Failure analysis completed for mandateId={}, classification={}", mandate.getMandateId(), analysis.classification());
        auditService.log(mandate.getMandateId(), "ANALYSIS", analysis.toString());

        analysis =
                scoringService.score(mandate, analysis);
        log.debug("Recovery scoring completed for mandateId={}, score={}", mandate.getMandateId(), analysis.recoverabilityScore());
        auditService.log(mandate.getMandateId(), "PROBABILITY", analysis.toString());

        DecisionResult decision =
                decisionService.decide(mandate, analysis);
        log.info("Recovery decision created for mandateId={}, action={}, reasonCode={}",
                mandate.getMandateId(), decision.action(), decision.reasonCode());

        RecoveryDecision decisionEntity =
                new RecoveryDecision();

        decisionEntity.setMandateId(
                mandate.getMandateId());

        decisionEntity.setClassification(
                analysis.classification());

        decisionEntity.setRecoverabilityScore(
                analysis.recoverabilityScore());

        decisionEntity.setAction(
                decision.action());

        decisionEntity.setScheduledAt(
                decision.scheduledAt());
        decisionEntity.setDecisionReasonCode(decision.reasonCode());
        decisionEntity.setStopReason(decision.stopReason());
        decisionEntity.setEscalated(decision.escalated());
        decisionEntity.setEscalationReason(decision.escalationReason());
        decisionEntity.setDecisionTimestamp(LocalDateTime.now());
        decisionEntity.setCreatedAt(LocalDateTime.now());

        decisionRepository.save(decisionEntity);
        auditService.log(mandate.getMandateId(), "DECISION", decision.toString());

        if (decision.escalated()) {
            log.warn("Recovery escalated for mandateId={}, reason={}", mandate.getMandateId(), decision.escalationReason());
            mandate.setEscalated(true);
            mandate.setEscalationReason(decision.escalationReason());
            mandate.setStopReason(decision.stopReason());
            failedMandateRepository.save(mandate);
            auditService.log(mandate.getMandateId(), "ESCALATED", decision.escalationReason());
            recordOutcome(mandate, decision, RecoveryOutcomeStatus.PENDING, null, decision.escalationReason());
            auditService.log(mandate.getMandateId(), "OUTCOME", RecoveryOutcomeStatus.PENDING.name());
            return result(mandate, analysis, decision, RecoveryOutcomeStatus.PENDING.name());
        }

        if (decision.stopReason() != null) {
            log.info("Recovery stopped for mandateId={}, stopReason={}", mandate.getMandateId(), decision.stopReason());
            mandate.setStopReason(decision.stopReason());
            failedMandateRepository.save(mandate);
            auditService.log(mandate.getMandateId(), "STOPPED", decision.stopReason().name());
            recordOutcome(mandate, decision, RecoveryOutcomeStatus.CANCELLED, null, decision.stopReason().name());
            auditService.log(mandate.getMandateId(), "OUTCOME", RecoveryOutcomeStatus.CANCELLED.name());
            return result(mandate, analysis, decision, RecoveryOutcomeStatus.CANCELLED.name());
        }

        if (RecoveryAction.NOTIFY_CUSTOMER.name().equals(decision.action())) {
            log.info("Customer notification required for mandateId={}", mandate.getMandateId());
            auditService.log(mandate.getMandateId(), "DECISION", decision.escalationReason());
            recordOutcome(mandate, decision, RecoveryOutcomeStatus.PENDING, null, "Customer notification required");
            auditService.log(mandate.getMandateId(), "OUTCOME", RecoveryOutcomeStatus.PENDING.name());
            return result(mandate, analysis, decision, RecoveryOutcomeStatus.PENDING.name());
        }

        mandate.setNextRetryAt(decision.scheduledAt());
        failedMandateRepository.save(mandate);
        auditService.log(mandate.getMandateId(), "RETRY_PLANNING", "Retry scheduled at " + decision.scheduledAt());
        auditService.log(mandate.getMandateId(), "PRE_RETRY_CHECK", "Checking whether customer already paid");

        if (paymentVerificationService.alreadyPaid(mandate)) {
            log.info("Recovery cancelled because customer already paid for mandateId={}", mandate.getMandateId());
            mandate.setStatus(PaymentStatus.SUCCESS);
            mandate.setNextRetryAt(null);
            mandate.setStopReason(StopReason.CUSTOMER_ALREADY_PAID);
            failedMandateRepository.save(mandate);
            decisionEntity.setStopReason(StopReason.CUSTOMER_ALREADY_PAID);
            decisionEntity.setDecisionReasonCode(StopReason.CUSTOMER_ALREADY_PAID.name());
            decisionRepository.save(decisionEntity);
            auditService.log(mandate.getMandateId(), "STOPPED", StopReason.CUSTOMER_ALREADY_PAID.name());
            recordOutcome(mandate, decision, RecoveryOutcomeStatus.CANCELLED, null, "Customer already paid");
            auditService.log(mandate.getMandateId(), "OUTCOME", RecoveryOutcomeStatus.CANCELLED.name());
            return result(mandate, analysis, decision, RecoveryOutcomeStatus.CANCELLED.name());
        }

        auditService.log(mandate.getMandateId(), "EXECUTION", "Payment execution started");
        PaymentResult paymentResult = paymentGateway.charge(mandate, analysis.recoverabilityScore());
        RecoveryOutcomeStatus outcomeStatus = paymentResult.success()
                ? RecoveryOutcomeStatus.SUCCESS
                : RecoveryOutcomeStatus.FAILED;
        if (outcomeStatus == RecoveryOutcomeStatus.SUCCESS) {
            mandate.setStatus(PaymentStatus.RETRY_SUCCESS);
            mandate.setNextRetryAt(null);
            failedMandateRepository.save(mandate);
        }
        recordOutcome(mandate, decision, outcomeStatus, paymentResult.transactionId(), paymentResult.message());
        auditService.log(mandate.getMandateId(), "OUTCOME", outcomeStatus.name());
        auditService.log(mandate.getMandateId(), "EXECUTION", "Payment execution completed: " + paymentResult.message());
        log.info("Recovery completed for mandateId={}, outcome={}, transactionId={}",
                mandate.getMandateId(), outcomeStatus, paymentResult.transactionId());

        return result(mandate, analysis, decision, outcomeStatus.name());
    }

    private void recordOutcome(
            FailedMandate mandate,
            DecisionResult decision,
            RecoveryOutcomeStatus outcomeStatus,
            String transactionId,
            String simulationReason) {
        RecoveryOutcome outcomeEntity = new RecoveryOutcome();
        outcomeEntity.setMandateId(mandate.getMandateId());
        outcomeEntity.setActionTaken(RecoveryAction.valueOf(decision.action()));
        outcomeEntity.setOutcome(outcomeStatus);
        outcomeEntity.setRecoveredAmount(outcomeStatus == RecoveryOutcomeStatus.SUCCESS ? mandate.getAmount() : BigDecimal.ZERO);
        outcomeEntity.setTransactionId(transactionId);
        outcomeEntity.setSimulationReason(simulationReason);
        outcomeEntity.setOutcomeTimestamp(LocalDateTime.now());
        outcomeRepository.save(outcomeEntity);
        log.debug("Recovery outcome recorded for mandateId={}, outcome={}", mandate.getMandateId(), outcomeStatus);
    }

    private RecoveryResult result(
            FailedMandate mandate,
            AnalysisResult analysis,
            DecisionResult decision,
            String outcome) {
        return new RecoveryResult(
                mandate.getMandateId(),
                analysis.classification(),
                analysis.recoverabilityScore(),
                decision.action(),
                outcome
        );
    }
}
