package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.agent.RevenueRecoveryAgent;
import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.dto.CreateFailedMandateRequest;
import com.recoverai.recoverai.dto.RecoveryResult;
import com.recoverai.recoverai.dto.SimulatorRecoveryResponse;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.entity.RecoveryOutcomeStatus;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.service.MerchantRegistrationService;
import com.recoverai.recoverai.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SimulationServiceImpl
        implements SimulationService {

    private final Random random =
            new Random();
    private final RecoverAiProperties properties;
    private final FailedMandateRepository failedMandateRepository;
    private final RevenueRecoveryAgent revenueRecoveryAgent;
    private final MerchantRegistrationService merchantRegistrationService;

    @Override
    public String simulate(
            AnalysisResult analysis) {

        double probability =
                analysis.recoveryProbability();

        return random.nextDouble() < probability
                ? RecoveryOutcomeStatus.SUCCESS.name()
                : RecoveryOutcomeStatus.FAILED.name();
    }

    @Override
    public SimulatorRecoveryResponse recover(CreateFailedMandateRequest request) {
        merchantRegistrationService.ensureMerchant(request.merchantId());
        FailedMandate mandate = FailedMandate.builder()
                .merchantId(request.merchantId())
                .customerId(request.customerId())
                .mandateId(request.mandateId())
                .amount(request.amount())
                .failureReason(request.failureReason())
                .failureCode(request.failureCode())
                .retryCount(request.retryCount() == null ? 0 : request.retryCount())
                .maxRetries(request.maxRetries() == null ? properties.maxRetries() : request.maxRetries())
                .failureTimestamp(request.failureTimestamp() == null ? LocalDateTime.now() : request.failureTimestamp())
                .paymentDate(request.paymentDate())
                .mandateStatus(request.mandateStatus() == null ? "ACTIVE" : request.mandateStatus())
                .status(PaymentStatus.FAILED)
                .escalated(false)
                .createdAt(LocalDateTime.now())
                .build();
        FailedMandate saved = failedMandateRepository.save(mandate);
        RecoveryResult result = revenueRecoveryAgent.run(saved);
        return new SimulatorRecoveryResponse(saved, result);
    }
}
