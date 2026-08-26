package com.recoverai.recoverai.entity;

public enum RecoveryStage {
    INGESTION,
    CLASSIFICATION,
    SCORING,
    ANALYSIS,
    PROBABILITY,
    RETRY_PLANNING,
    DECISION,
    PRE_RETRY_CHECK,
    EXECUTION,
    OUTCOME,
    STOPPED,
    ESCALATED
}
