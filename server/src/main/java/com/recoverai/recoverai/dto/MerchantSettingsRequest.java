package com.recoverai.recoverai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MerchantSettingsRequest(
        Boolean active,
        Integer maxRetries,
        @Min(0) @Max(23) Integer peakStartHour,
        @Min(0) @Max(23) Integer peakEndHour
) {
}
