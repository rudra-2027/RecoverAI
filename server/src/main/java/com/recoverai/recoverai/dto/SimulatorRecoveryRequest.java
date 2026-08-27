package com.recoverai.recoverai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SimulatorRecoveryRequest(
        @Valid @NotNull CreateFailedMandateRequest mandate
) {
}
