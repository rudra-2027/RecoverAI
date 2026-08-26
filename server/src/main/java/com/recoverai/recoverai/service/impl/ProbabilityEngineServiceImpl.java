package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.repository.PaymentHistoryRepository;
import com.recoverai.recoverai.service.ProbabilityEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProbabilityEngineServiceImpl implements ProbabilityEngineService {
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Override
    public int calculate(FailedMandate mandate) {
        int score = switch (mandate.getFailureReason()) {
            case "INSUFFICIENT_BALANCE" -> 80;
            case "LIMIT_EXCEEDED" -> 75;
            case "BANK_SERVER_DOWN" -> 90;
            case "NPCI_TIMEOUT" -> 85;
            case "CARD_EXPIRED" -> 10;
            case "MANDATE_REVOKED", "MANDATE_EXPIRED" -> 0;
            default -> 25;
        };

        if (isPermanent(mandate)) {
            return 0;
        }

        long successfulPayments = paymentHistoryRepository.countByCustomerIdAndStatus(
                mandate.getCustomerId(),
                PaymentStatus.SUCCESS);
        long failedPayments = paymentHistoryRepository.countByCustomerIdAndStatus(
                mandate.getCustomerId(),
                PaymentStatus.FAILED);

        if (successfulPayments >= 12) {
            score += 10;
        }
        if (failedPayments >= 3) {
            score -= 10;
        }
        if (safeRetryCount(mandate) >= 2) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private boolean isPermanent(FailedMandate mandate) {
        return "MANDATE_REVOKED".equals(mandate.getFailureReason())
                || "MANDATE_EXPIRED".equals(mandate.getFailureReason());
    }

    private int safeRetryCount(FailedMandate mandate) {
        return mandate.getRetryCount() == null ? 0 : mandate.getRetryCount();
    }
}
