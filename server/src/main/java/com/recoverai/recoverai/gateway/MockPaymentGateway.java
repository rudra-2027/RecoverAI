package com.recoverai.recoverai.gateway;

import com.recoverai.recoverai.entity.FailedMandate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class MockPaymentGateway implements PaymentGateway {
    private final SecureRandom random = new SecureRandom();

    @Override
    public PaymentResult charge(FailedMandate mandate, double probability) {
        boolean success = random.nextDouble(100.0) < probability;
        String transactionId = success ? "TXN-" + HexFormat.of().formatHex(randomBytes()).toUpperCase() : null;
        String message = success ? "Mock payment recovered" : "Mock payment retry failed";
        return new PaymentResult(success, transactionId, message);
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[3];
        random.nextBytes(bytes);
        return bytes;
    }
}
