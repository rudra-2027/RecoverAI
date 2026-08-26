package com.recoverai.recoverai.service;

import com.recoverai.recoverai.entity.FailedMandate;

public interface PaymentVerificationService {
    boolean alreadyPaid(FailedMandate mandate);
}
