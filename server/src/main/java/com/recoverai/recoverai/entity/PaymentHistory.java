package com.recoverai.recoverai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", length = 100)
    private String merchantId;

    @Column(name = "mandate_id", nullable = false, length = 100)
    private String mandateId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;


    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentStatus status;


    @Column(length = 100)
    private String reason;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

}
