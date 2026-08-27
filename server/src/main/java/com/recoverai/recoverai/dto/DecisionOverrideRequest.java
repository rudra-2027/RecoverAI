package com.recoverai.recoverai.dto;

import com.recoverai.recoverai.entity.RecoveryAction;
import com.recoverai.recoverai.entity.StopReason;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DecisionOverrideRequest(
        @NotNull RecoveryAction action,
        Integer recoverabilityScore,
        String classification,
        LocalDateTime scheduledAt,
        StopReason stopReason,
        Boolean escalated,
        String escalationReason,
        String reason
) {
}
