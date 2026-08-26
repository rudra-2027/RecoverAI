package com.recoverai.recoverai.dto;

import com.recoverai.recoverai.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePaymentHistoryRequest(
        String merchantId,
        @NotBlank String customerId,
        @NotBlank String mandateId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull PaymentStatus status,
        String reason,
        LocalDateTime transactionTime,
        LocalDateTime paymentDate
) {
}
