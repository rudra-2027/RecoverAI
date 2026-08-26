package com.recoverai.recoverai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateFailedMandateRequest(
        @NotBlank String merchantId,
        @NotBlank String customerId,
        @NotBlank String mandateId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String failureReason,
        String failureCode,
        Integer retryCount,
        Integer maxRetries,
        LocalDateTime failureTimestamp,
        LocalDateTime paymentDate,
        String mandateStatus
) {
}
