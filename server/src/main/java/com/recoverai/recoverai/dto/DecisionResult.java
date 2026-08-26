package com.recoverai.recoverai.dto;

import java.time.LocalDateTime;

public record DecisionResult(
        String action,
        LocalDateTime scheduledAt
) {
}