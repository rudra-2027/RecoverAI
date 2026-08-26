package com.recoverai.recoverai.gateway;

public record PaymentResult(
        boolean success,
        String transactionId,
        String message
) {
}
