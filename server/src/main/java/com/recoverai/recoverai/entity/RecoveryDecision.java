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

    @Column(name = "decision_reason_code", length = 100)
    private String decisionReasonCode;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_reason", length = 100)
    private StopReason stopReason;

    @Column(name = "escalated")
    private Boolean escalated = false;

    @Column(name = "escalation_reason", length = 255)
    private String escalationReason;

    @Column(name = "confirmed")
    private Boolean confirmed = false;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "manual_override")
    private Boolean manualOverride = false;

    @Column(name = "decision_timestamp")
    private LocalDateTime decisionTimestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
}
