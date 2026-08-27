package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.dto.DecisionResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.RecoveryAction;
import com.recoverai.recoverai.entity.StopReason;
import com.recoverai.recoverai.service.DecisionService;
import com.recoverai.recoverai.service.RetryStrategyService;
import com.recoverai.recoverai.service.RuntimeSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionServiceImpl
        implements DecisionService {
    private final RuntimeSettingsService runtimeSettingsService;
    private final RetryStrategyService retryStrategyService;

    @Override
    public DecisionResult decide(
            FailedMandate mandate,
            AnalysisResult analysis) {

        if ("MANDATE_REVOKED".equals(mandate.getFailureReason())) {
            log.info("Decision stopped for mandateId={} because mandate was revoked", mandate.getMandateId());
            return stopped(StopReason.MANDATE_REVOKED, "MANDATE_REVOKED");
        }
        if ("MANDATE_EXPIRED".equals(mandate.getFailureReason())) {
            log.info("Decision stopped for mandateId={} because mandate was expired", mandate.getMandateId());
            return stopped(StopReason.MANDATE_EXPIRED, "MANDATE_EXPIRED");
        }
        if ("CARD_EXPIRED".equals(mandate.getFailureReason())) {
            log.info("Decision requires customer notification for mandateId={} because card expired", mandate.getMandateId());
            return new DecisionResult(
                    RecoveryAction.NOTIFY_CUSTOMER.name(),
                    null,
                    null,
                    false,
                    "Customer must update expired card before payment can be recovered",
                    "CARD_EXPIRED_NOTIFY_CUSTOMER");
        }
        if (safeRetryCount(mandate) >= maxRetries(mandate)) {
            log.info("Decision stopped for mandateId={} because max retries reached", mandate.getMandateId());
            return stopped(StopReason.MAX_RETRIES_REACHED, "MAX_RETRIES_REACHED");
        }
        if (analysis.recoverabilityScore() < runtimeSettingsService.escalateBelowProbability()) {
            log.info("Decision escalated for mandateId={} score={} threshold={}",
                    mandate.getMandateId(), analysis.recoverabilityScore(), runtimeSettingsService.escalateBelowProbability());
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
        return mandate.getMaxRetries() == null ? runtimeSettingsService.maxRetries() : mandate.getMaxRetries();
    }
}
