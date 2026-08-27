package com.recoverai.recoverai.dto;

import com.recoverai.recoverai.entity.FailedMandate;

public record SimulatorRecoveryResponse(
        FailedMandate mandate,
        RecoveryResult result
) {
}
