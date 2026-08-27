package com.recoverai.recoverai.config;

import com.recoverai.recoverai.entity.*;
import com.recoverai.recoverai.repository.FailedMandateRepository;
import com.recoverai.recoverai.repository.MerchantRepository;
import com.recoverai.recoverai.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private static final List<String> FAILURE_REASONS = List.of(
            "INSUFFICIENT_BALANCE",
            "LIMIT_EXCEEDED",
            "BANK_SERVER_DOWN",
            "NPCI_TIMEOUT",
            "CARD_EXPIRED",
            "MANDATE_REVOKED",
            "MANDATE_EXPIRED");

    private final FailedMandateRepository failedMandateRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final MerchantRepository merchantRepository;
    private final RecoverAiProperties properties;
    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        log.info("Starting demo data seeding");
        seedMerchant();
        if (failedMandateRepository.count() == 0) {
            seedFailedMandates();
        }
        if (paymentHistoryRepository.count() == 0) {
            seedPaymentHistory();
        }
        log.info("Demo data seeding completed");
    }

    private void seedMerchant() {
        merchantRepository.findByMerchantId("MERCHANT-DEMO")
                .orElseGet(() -> merchantRepository.save(Merchant.builder()
                        .merchantId("MERCHANT-DEMO")
                        .merchantName("RecoverAI Demo Merchant")
                        .maxRetries(properties.maxRetries())
                        .peakStartHour(properties.peakStartHour())
                        .peakEndHour(properties.peakEndHour())
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    private void seedFailedMandates() {
        log.info("Seeding demo failed mandates");
        for (int i = 1; i <= 100; i++) {
            String reason = FAILURE_REASONS.get(random.nextInt(FAILURE_REASONS.size()));
            failedMandateRepository.save(FailedMandate.builder()
                    .merchantId("MERCHANT-DEMO")
                    .mandateId("MND-%04d".formatted(i))
                    .customerId("CUS-%03d".formatted((i % 35) + 1))
                    .amount(BigDecimal.valueOf(499 + random.nextInt(25000)))
                    .failureReason(reason)
                    .failureCode("ERR-" + reason)
                    .failureTimestamp(LocalDateTime.now().minusDays(random.nextInt(25)))
                    .paymentDate(LocalDateTime.now().minusDays(random.nextInt(25)))
                    .retryCount(random.nextInt(4))
                    .maxRetries(properties.maxRetries())
                    .mandateStatus("ACTIVE")
                    .status(PaymentStatus.FAILED)
                    .createdAt(LocalDateTime.now())
                    .escalated(false)
                    .build());
        }
    }

    private void seedPaymentHistory() {
        log.info("Seeding demo payment history");
        for (int i = 1; i <= 500; i++) {
            PaymentStatus status = random.nextInt(10) < 8 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            paymentHistoryRepository.save(PaymentHistory.builder()
                    .merchantId("MERCHANT-DEMO")
                    .mandateId("MND-%04d".formatted((i % 100) + 1))
                    .customerId("CUS-%03d".formatted((i % 35) + 1))
                    .amount(BigDecimal.valueOf(499 + random.nextInt(25000)))
                    .status(status)
                    .reason(status == PaymentStatus.SUCCESS ? "PAID" : "FAILED_RETRY")
                    .transactionTime(LocalDateTime.now().minusDays(random.nextInt(365)))
                    .paymentDate(LocalDateTime.now().minusDays(random.nextInt(365)))
                    .build());
        }
    }
}
