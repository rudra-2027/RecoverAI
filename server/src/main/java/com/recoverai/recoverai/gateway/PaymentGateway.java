package com.recoverai.recoverai.gateway;

import com.recoverai.recoverai.entity.FailedMandate;

public interface PaymentGateway {
    PaymentResult charge(FailedMandate mandate, double probability);
}
