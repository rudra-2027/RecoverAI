package com.recoverai.recoverai.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProcessBatchDecisionsRequest(
        @NotEmpty List<String> mandateIds
) {
}
