package com.recoverai.recoverai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="recovery_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandate_id", nullable = false, length = 100)
    private String mandateId;

    @Column(length = 50)
    private String classification;

    @Column(name = "recoverability_score")
    private Integer recoverabilityScore;

    @Column(length = 50)
    private String action;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "decision_timestamp")
    private LocalDateTime decisionTimestamp;
    
}
