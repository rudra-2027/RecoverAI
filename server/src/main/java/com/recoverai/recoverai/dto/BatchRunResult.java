package com.recoverai.recoverai.dto;

import java.math.BigDecimal;

public record BatchRunResult(
        Long batchRunId,
        int totalProcessed,
        int successfulRecoveries,
        int failedRecoveries,
        BigDecimal recoveredRevenue,
        double recoveryRate,
        String status
) {
}
