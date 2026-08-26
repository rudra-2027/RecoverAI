package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.dto.DecisionResult;
import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.RecoveryAction;
import com.recoverai.recoverai.entity.StopReason;
import com.recoverai.recoverai.service.DecisionService;
import com.recoverai.recoverai.service.RetryStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DecisionServiceImpl
        implements DecisionService {
    private final RecoverAiProperties properties;
    private final RetryStrategyService retryStrategyService;

    @Override
    public DecisionResult decide(
            FailedMandate mandate,
            AnalysisResult analysis) {

        if ("MANDATE_REVOKED".equals(mandate.getFailureReason())) {
            return stopped(StopReason.MANDATE_REVOKED, "MANDATE_REVOKED");
        }
        if ("MANDATE_EXPIRED".equals(mandate.getFailureReason())) {
            return stopped(StopReason.MANDATE_EXPIRED, "MANDATE_EXPIRED");
        }
        if ("CARD_EXPIRED".equals(mandate.getFailureReason())) {
            return new DecisionResult(
                    RecoveryAction.NOTIFY_CUSTOMER.name(),
                    null,
                    null,
                    false,
                    "Customer must update expired card before payment can be recovered",
                    "CARD_EXPIRED_NOTIFY_CUSTOMER");
        }
        if (safeRetryCount(mandate) >= maxRetries(mandate)) {
            return stopped(StopReason.MAX_RETRIES_REACHED, "MAX_RETRIES_REACHED");
        }
        if (analysis.recoverabilityScore() < properties.escalateBelowProbability()) {
            return new DecisionResult(
                    RecoveryAction.ESCALATE.name(),
                    null,
                    StopReason.LOW_RECOVERY_PROBABILITY,
                    true,
                    "Recovery probability below configured threshold",
                    "LOW_RECOVERY_PROBABILITY");
        }

        return new DecisionResult(
                RecoveryAction.RETRY.name(),
                retryStrategyService.generate(mandate).stream().findFirst().orElse(null),
                null,
                false,
                null,
                "RETRY_RECOMMENDED");
    }

    private DecisionResult stopped(StopReason stopReason, String reasonCode) {
        return new DecisionResult(
                RecoveryAction.CANCEL_MANDATE.name(),
                null,
                stopReason,
                false,
                null,
                reasonCode);
    }

    private int safeRetryCount(FailedMandate mandate) {
        return mandate.getRetryCount() == null ? 0 : mandate.getRetryCount();
    }

    private int maxRetries(FailedMandate mandate) {
        return mandate.getMaxRetries() == null ? properties.maxRetries() : mandate.getMaxRetries();
    }
}
