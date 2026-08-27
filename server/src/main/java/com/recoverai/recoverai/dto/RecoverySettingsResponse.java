package com.recoverai.recoverai.dto;

public record RecoverySettingsResponse(
        int maxRetries,
        int escalateBelowProbability,
        int peakStartHour,
        int peakEndHour
) {
}
