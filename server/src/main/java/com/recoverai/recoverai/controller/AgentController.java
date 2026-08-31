package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.agent.RevenueRecoveryAgent;
import com.recoverai.recoverai.dto.BatchRunResult;
import com.recoverai.recoverai.dto.RecoveryResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.service.BatchProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/agent")
public class AgentController {

    private final RevenueRecoveryAgent revenueRecoveryAgent;
    private final FailedMandateRepository failedMandateRepository;
    private final BatchProcessingService batchProcessingService;

    @PostMapping("/run/{mandateId}")
    public RecoveryResult run(
            @PathVariable String mandateId) {
        log.info("Agent run requested for mandateId={}", mandateId);

        FailedMandate mandate =
                failedMandateRepository
                        .findTopByMandateIdOrderByCreatedAtDescIdDesc(mandateId)
                        .orElseThrow(() -> new ResourceNotFoundException("Mandate not found: " + mandateId));

        return revenueRecoveryAgent.run(mandate);
    }

    @PostMapping("/run-all")
    public BatchRunResult runAll() {
        log.info("Agent run-all requested");
        return batchProcessingService.runAllFailedMandates();
    }
}
