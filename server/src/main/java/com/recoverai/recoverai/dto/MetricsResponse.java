package com.recoverai.recoverai.dto;

import java.math.BigDecimal;

public record MetricsResponse(
        BigDecimal revenueThisMonth,
        BigDecimal revenueAtRisk,
        BigDecimal predictedRecoverableRevenue,
        BigDecimal recoveredRevenue,
        double recoveryRate,
        double averageRecoveryProbability,
        BigDecimal recoveryLift,
        double retrySuccessRate,
        long highRiskCustomers,
        long highValueCustomers,
        long failedMandatesCount,
        long recoveredMandatesCount
) {
}
