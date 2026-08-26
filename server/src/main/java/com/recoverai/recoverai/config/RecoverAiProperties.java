package com.recoverai.recoverai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recoverai")
public record RecoverAiProperties(
        int maxRetries,
        int escalateBelowProbability,
        int peakStartHour,
        int peakEndHour,
        String apiKey,
        String geminiApiKey
) {
}
