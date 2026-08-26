package com.recoverai.recoverai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateMerchantRequest(
        @NotBlank String merchantId,
        @NotBlank String merchantName,
        @Positive Integer maxRetries,
        Integer peakStartHour,
        Integer peakEndHour,
        Boolean active
) {
}
