package com.recoverai.recoverai.dto;

import java.time.LocalDateTime;

public record SystemStatusResponse(
        String status,
        LocalDateTime timestamp,
        long merchants,
        long failedMandates,
        long decisions,
        long outcomes,
        long batchRuns,
        boolean apiKeyEnabled
) {
}
