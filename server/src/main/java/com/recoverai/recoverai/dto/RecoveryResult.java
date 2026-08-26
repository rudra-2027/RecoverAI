package com.recoverai.recoverai.dto;

public record RecoveryResult(
        String mandateId,
        String classification,
        Integer recoverabilityScore,
        String action,
        String outcome
) {}