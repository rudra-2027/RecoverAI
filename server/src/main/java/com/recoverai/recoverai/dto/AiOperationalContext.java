package com.recoverai.recoverai.dto;

public record AiOperationalContext(
        String intent,
        String contextType,
        String backendContext,
        String fallbackAnswer
) {
}
