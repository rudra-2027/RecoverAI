package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.agent.RevenueRecoveryAgent;
import com.recoverai.recoverai.dto.DecisionOverrideRequest;
import com.recoverai.recoverai.dto.ProcessBatchDecisionsRequest;
import com.recoverai.recoverai.dto.RecoveryResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.entity.RecoveryDecision;
import com.recoverai.recoverai.entity.RecoveryOutcomeStatus;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/decisions")
public class DecisionController {
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository outcomeRepository;
    private final FailedMandateRepository failedMandateRepository;
    private final RevenueRecoveryAgent revenueRecoveryAgent;
    private final AuditService auditService;

    @GetMapping
    public List<RecoveryDecision> all() {
        Set<String> recoveredMandateIds = outcomeRepository.findByOutcome(RecoveryOutcomeStatus.SUCCESS).stream()
                .map(outcome -> outcome.getMandateId())
                .collect(Collectors.toSet());

        Map<String, RecoveryDecision> latestOpenDecisionByMandate = decisionRepository.findAll().stream()
                .filter(decision -> decision.getMandateId() != null)
                .filter(decision -> !recoveredMandateIds.contains(decision.getMandateId()))
                .collect(Collectors.toMap(
                        RecoveryDecision::getMandateId,
                        Function.identity(),
                        (first, second) -> isAfter(second, first) ? second : first));

        return latestOpenDecisionByMandate.values().stream()
                .sorted(Comparator.comparing(RecoveryDecision::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @GetMapping("/{mandateId}")
    public List<RecoveryDecision> byMandate(@PathVariable String mandateId) {
        return decisionRepository.findByMandateIdOrderByCreatedAtDesc(mandateId);
    }

    @PutMapping("/{mandateId}/override")
    public RecoveryDecision override(
            @PathVariable String mandateId,
            @Valid @RequestBody DecisionOverrideRequest request) {
        log.info("Manual decision override requested for mandateId={}, action={}", mandateId, request.action());
        FailedMandate mandate = failedMandateRepository.findByMandateId(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException("Mandate not found: " + mandateId));

        RecoveryDecision decision = RecoveryDecision.builder()
                .mandateId(mandate.getMandateId())
                .classification(request.classification())
                .recoverabilityScore(request.recoverabilityScore())
                .action(request.action().name())
                .decisionReasonCode("MANUAL_OVERRIDE")
                .scheduledAt(request.scheduledAt())
                .stopReason(request.stopReason())
                .escalated(Boolean.TRUE.equals(request.escalated()))
                .escalationReason(request.reason() == null ? request.escalationReason() : request.reason())
                .manualOverride(true)
                .confirmed(true)
                .confirmedAt(LocalDateTime.now())
                .decisionTimestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        RecoveryDecision saved = decisionRepository.save(decision);
        log.info("Manual decision override saved for mandateId={}, decisionId={}", mandateId, saved.getId());
        auditService.log(mandateId, "DECISION", "Manual override applied: " + request.action().name());
        return saved;
    }

    @PostMapping("/{mandateId}/confirm")
    public RecoveryDecision confirm(@PathVariable String mandateId) {
        log.info("Decision confirmation requested for mandateId={}", mandateId);
        RecoveryDecision decision = decisionRepository.findTopByMandateIdOrderByCreatedAtDesc(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException("Decision not found for mandate: " + mandateId));
        decision.setConfirmed(true);
        decision.setConfirmedAt(LocalDateTime.now());
        RecoveryDecision saved = decisionRepository.save(decision);
        log.info("Decision confirmed for mandateId={}, decisionId={}", mandateId, saved.getId());
        auditService.log(mandateId, "DECISION", "AI plan confirmed");
        return saved;
    }

    @PostMapping("/process-batch")
    public List<RecoveryResult> processBatch(@Valid @RequestBody ProcessBatchDecisionsRequest request) {
        log.info("Decision batch processing requested for {} mandates", request.mandateIds().size());
        return request.mandateIds().stream()
                .map(mandateId -> failedMandateRepository.findByMandateId(mandateId)
                        .orElseThrow(() -> new ResourceNotFoundException("Mandate not found: " + mandateId)))
                .filter(mandate -> mandate.getStatus() == PaymentStatus.FAILED)
                .filter(mandate -> outcomeRepository.findByMandateIdOrderByOutcomeTimestampDesc(mandate.getMandateId()).stream()
                        .noneMatch(outcome -> outcome.getOutcome() == RecoveryOutcomeStatus.SUCCESS))
                .map(revenueRecoveryAgent::run)
                .toList();
    }

    private boolean isAfter(RecoveryDecision candidate, RecoveryDecision current) {
        if (candidate.getCreatedAt() == null) {
            return false;
        }
        if (current.getCreatedAt() == null) {
            return true;
        }
        return candidate.getCreatedAt().isAfter(current.getCreatedAt());
    }
}
