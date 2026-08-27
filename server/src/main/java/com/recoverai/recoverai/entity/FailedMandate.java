package com.recoverai.recoverai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_mandates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedMandate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(name = "batch_run_id")
    private Long batchRunId;

    @Column(name = "mandate_id", nullable = false, length = 100)
    private String mandateId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "failure_reason", nullable = false, length = 100)
    private String failureReason;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_timestamp", nullable = false)
    private LocalDateTime failureTimestamp;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_reason", length = 100)
    private StopReason stopReason;

    @Column(name = "escalated")
    private Boolean escalated = false;

    @Column(name = "escalation_reason", length = 255)
    private String escalationReason;

    @Column(name = "mandate_status", length = 50)
    private String mandateStatus;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
