package com.recoverai.recoverai.entity;

public enum StopReason {
    MAX_RETRIES_REACHED,
    MANDATE_REVOKED,
    MANDATE_EXPIRED,
    CUSTOMER_OPT_OUT,
    CUSTOMER_ALREADY_PAID,
    LOW_RECOVERY_PROBABILITY
}
