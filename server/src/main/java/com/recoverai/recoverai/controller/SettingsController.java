package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.dto.ApiKeyRegenerateResponse;
import com.recoverai.recoverai.dto.MerchantSettingsRequest;
import com.recoverai.recoverai.dto.RecoverySettingsRequest;
import com.recoverai.recoverai.dto.RecoverySettingsResponse;
import com.recoverai.recoverai.dto.SystemStatusResponse;
import com.recoverai.recoverai.entity.Merchant;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.MerchantRepository;
import com.recoverai.recoverai.repository.RecoveryDecisionRepository;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import com.recoverai.recoverai.service.RuntimeSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/settings")
public class SettingsController {
    private final RuntimeSettingsService runtimeSettingsService;
    private final MerchantRepository merchantRepository;
    private final FailedMandateRepository failedMandateRepository;
    private final RecoveryDecisionRepository decisionRepository;
    private final RecoveryOutcomeRepository outcomeRepository;
    private final BatchRunRepository batchRunRepository;

    @GetMapping("/recovery")
    public RecoverySettingsResponse recoverySettings() {
        return runtimeSettingsService.recoverySettings();
    }

    @PutMapping("/recovery")
    public RecoverySettingsResponse updateRecoverySettings(@Valid @RequestBody RecoverySettingsRequest request) {
        log.info("Updating recovery settings");
        return runtimeSettingsService.updateRecoverySettings(request);
    }

    @PutMapping("/merchants/{merchantId}")
    public Merchant updateMerchant(
            @PathVariable String merchantId,
            @Valid @RequestBody MerchantSettingsRequest request) {
        log.info("Updating merchant settings merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + merchantId));
        if (request.active() != null) {
            merchant.setActive(request.active());
        }
        if (request.maxRetries() != null) {
            merchant.setMaxRetries(request.maxRetries());
        }
        if (request.peakStartHour() != null) {
            merchant.setPeakStartHour(request.peakStartHour());
        }
        if (request.peakEndHour() != null) {
            merchant.setPeakEndHour(request.peakEndHour());
        }
        return merchantRepository.save(merchant);
    }

    @GetMapping("/status")
    public SystemStatusResponse status() {
        return new SystemStatusResponse(
                "UP",
                LocalDateTime.now(),
                merchantRepository.count(),
                failedMandateRepository.countByStatus(PaymentStatus.FAILED),
                decisionRepository.count(),
                outcomeRepository.count(),
                batchRunRepository.count(),
                runtimeSettingsService.apiKeyEnabled());
    }

    @PostMapping("/api-key/regenerate")
    public ApiKeyRegenerateResponse regenerateApiKey() {
        log.warn("Runtime API key regeneration requested");
        String apiKey = runtimeSettingsService.regenerateApiKey();
        return new ApiKeyRegenerateResponse(apiKey, "Runtime API key regenerated. Use this value in X-API-Key for future requests.");
    }
}
