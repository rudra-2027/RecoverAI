package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.config.RecoverAiProperties;
import com.recoverai.recoverai.entity.Merchant;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.MerchantRepository;
import com.recoverai.recoverai.repository.PaymentHistoryRepository;
import com.recoverai.recoverai.service.MerchantRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MerchantRegistrationServiceImpl implements MerchantRegistrationService {
    private final MerchantRepository merchantRepository;
    private final FailedMandateRepository failedMandateRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final RecoverAiProperties properties;

    @Override
    public void ensureMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return;
        }

        merchantRepository.findByMerchantId(merchantId).orElseGet(() -> merchantRepository.save(Merchant.builder()
                .merchantId(merchantId)
                .merchantName(merchantId)
                .maxRetries(properties.maxRetries())
                .peakStartHour(properties.peakStartHour())
                .peakEndHour(properties.peakEndHour())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build()));
    }

    @Override
    public void syncKnownMerchants() {
        failedMandateRepository.findDistinctMerchantIds().forEach(this::ensureMerchant);
        paymentHistoryRepository.findDistinctMerchantIds().forEach(this::ensureMerchant);
    }
}
