package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.agent.RevenueRecoveryAgent;
import com.recoverai.recoverai.dto.BatchRunResult;
import com.recoverai.recoverai.dto.RecoveryResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.service.BatchProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent")
public class AgentController {

    private final RevenueRecoveryAgent revenueRecoveryAgent;
    private final FailedMandateRepository failedMandateRepository;
    private final BatchProcessingService batchProcessingService;

    @PostMapping("/run/{mandateId}")
    public RecoveryResult run(
            @PathVariable String mandateId) {

        FailedMandate mandate =
                failedMandateRepository
                        .findByMandateId(mandateId)
                        .orElseThrow(() -> new ResourceNotFoundException("Mandate not found: " + mandateId));

        return revenueRecoveryAgent.run(mandate);
    }

    @PostMapping("/run-all")
    public BatchRunResult runAll() {
        return batchProcessingService.runAllFailedMandates();
    }
}
