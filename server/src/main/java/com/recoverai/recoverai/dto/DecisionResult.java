package com.recoverai.recoverai.dto;

import com.recoverai.recoverai.entity.StopReason;

import java.time.LocalDateTime;

public record DecisionResult(
        String action,
        LocalDateTime scheduledAt,
        StopReason stopReason,
        boolean escalated,
        String escalationReason,
        String reasonCode
) {
}
