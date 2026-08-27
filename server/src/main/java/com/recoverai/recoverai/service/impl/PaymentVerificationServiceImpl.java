package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;
import com.recoverai.recoverai.repository.PaymentHistoryRepository;
import com.recoverai.recoverai.service.PaymentVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentVerificationServiceImpl implements PaymentVerificationService {
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Override
    public boolean alreadyPaid(FailedMandate mandate) {
        boolean alreadyPaid = paymentHistoryRepository.existsByMandateIdAndStatusAndTransactionTimeAfter(
                mandate.getMandateId(),
                PaymentStatus.SUCCESS,
                mandate.getFailureTimestamp());
        log.debug("Payment verification completed for mandateId={}, alreadyPaid={}",
                mandate.getMandateId(), alreadyPaid);
        return alreadyPaid;
    }
}
