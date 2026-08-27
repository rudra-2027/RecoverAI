package com.recoverai.recoverai.service;

import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.dto.RecoverySettingsRequest;
import com.recoverai.recoverai.dto.RecoverySettingsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
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
            log.info("Runtime maxRetries updated from {} to {}", this.maxRetries, request.maxRetries());
            this.maxRetries = request.maxRetries();
        }
        if (request.escalateBelowProbability() != null) {
            log.info("Runtime escalateBelowProbability updated from {} to {}",
                    this.escalateBelowProbability, request.escalateBelowProbability());
            this.escalateBelowProbability = request.escalateBelowProbability();
        }
        if (request.peakStartHour() != null) {
            log.info("Runtime peakStartHour updated from {} to {}", this.peakStartHour, request.peakStartHour());
            this.peakStartHour = request.peakStartHour();
        }
        if (request.peakEndHour() != null) {
            log.info("Runtime peakEndHour updated from {} to {}", this.peakEndHour, request.peakEndHour());
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
        log.warn("Runtime API key regenerated");
        return this.apiKey;
    }
}
