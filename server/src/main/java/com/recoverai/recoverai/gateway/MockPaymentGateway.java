package com.recoverai.recoverai.gateway;

import com.recoverai.recoverai.entity.FailedMandate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

@Component
@Slf4j
public class MockPaymentGateway implements PaymentGateway {
    private final SecureRandom random = new SecureRandom();

    @Override
    public PaymentResult charge(FailedMandate mandate, double probability) {
        boolean success = random.nextDouble(100.0) < probability;
        String transactionId = success ? "TXN-" + HexFormat.of().formatHex(randomBytes()).toUpperCase() : null;
        String message = success ? "Mock payment recovered" : "Mock payment retry failed";
        log.info("Mock payment attempted for mandateId={}, probability={}, success={}",
                mandate.getMandateId(), probability, success);
        return new PaymentResult(success, transactionId, message);
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[3];
        random.nextBytes(bytes);
        return bytes;
    }
}
