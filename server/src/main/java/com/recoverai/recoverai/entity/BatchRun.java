package com.recoverai.recoverai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalMandates;
    private Integer successfulRecoveries;
    private Integer failedRecoveries;

    @Column(length = 255)
    private String sourceFileName;

    @Column(length = 30)
    private String sourceType;

    @Column(length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(precision = 14, scale = 2)
    private BigDecimal recoveredRevenue;
}
