package com.recoverai.recoverai.service;

public interface MerchantRegistrationService {
    void ensureMerchant(String merchantId);

    void syncKnownMerchants();
}
