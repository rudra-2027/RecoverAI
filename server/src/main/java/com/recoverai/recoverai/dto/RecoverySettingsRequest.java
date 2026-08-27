package com.recoverai.recoverai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RecoverySettingsRequest(
        @Min(0) Integer maxRetries,
        @Min(0) @Max(100) Integer escalateBelowProbability,
        @Min(0) @Max(23) Integer peakStartHour,
        @Min(0) @Max(23) Integer peakEndHour
) {
}
