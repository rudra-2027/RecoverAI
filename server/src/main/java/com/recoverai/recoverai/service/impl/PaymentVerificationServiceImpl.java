package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.repository.PaymentHistoryRepository;
import com.recoverai.recoverai.service.PaymentVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentVerificationServiceImpl implements PaymentVerificationService {
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Override
    public boolean alreadyPaid(FailedMandate mandate) {
        return paymentHistoryRepository.existsByMandateIdAndStatusAndTransactionTimeAfter(
                mandate.getMandateId(),
                PaymentStatus.SUCCESS,
                mandate.getFailureTimestamp());
    }
}
