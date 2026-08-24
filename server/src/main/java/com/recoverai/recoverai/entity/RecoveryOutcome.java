package com.recoverai.recoverai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_outcomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandate_id", nullable = false, length = 100)
    private String mandateId;

    @Column(name = "recovered_amount", precision = 12, scale = 2)
    private BigDecimal recoveredAmount;

    @Column(name = "outcome_timestamp")
    private LocalDateTime outcomeTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", length = 50)
    private RecoveryAction actionTaken;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RecoveryOutcomeStatus outcome;
}