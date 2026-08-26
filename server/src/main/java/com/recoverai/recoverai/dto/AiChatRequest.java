package com.recoverai.recoverai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(@NotBlank String question) {
}
