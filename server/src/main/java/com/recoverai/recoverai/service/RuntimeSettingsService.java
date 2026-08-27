package com.recoverai.recoverai.service;

import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.dto.RecoverySettingsRequest;
import com.recoverai.recoverai.dto.RecoverySettingsResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class RuntimeSettingsService {
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile int maxRetries;
    private volatile int escalateBelowProbability;
    private volatile int peakStartHour;
    private volatile int peakEndHour;
    private volatile String apiKey;

    public RuntimeSettingsService(RecoverAiProperties properties) {
        this.maxRetries = properties.maxRetries();
        this.escalateBelowProbability = properties.escalateBelowProbability();
        this.peakStartHour = properties.peakStartHour();
        this.peakEndHour = properties.peakEndHour();
        this.apiKey = properties.apiKey();
    }

    public RecoverySettingsResponse recoverySettings() {
        return new RecoverySettingsResponse(maxRetries, escalateBelowProbability, peakStartHour, peakEndHour);
    }

    public RecoverySettingsResponse updateRecoverySettings(RecoverySettingsRequest request) {
        if (request.maxRetries() != null) {
            this.maxRetries = request.maxRetries();
        }
        if (request.escalateBelowProbability() != null) {
            this.escalateBelowProbability = request.escalateBelowProbability();
        }
        if (request.peakStartHour() != null) {
            this.peakStartHour = request.peakStartHour();
        }
        if (request.peakEndHour() != null) {
            this.peakEndHour = request.peakEndHour();
        }
        return recoverySettings();
    }

    public int maxRetries() {
        return maxRetries;
    }

    public int escalateBelowProbability() {
        return escalateBelowProbability;
    }

    public int peakStartHour() {
        return peakStartHour;
    }

    public int peakEndHour() {
        return peakEndHour;
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean apiKeyEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String regenerateApiKey() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        this.apiKey = "rk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return this.apiKey;
    }
}
