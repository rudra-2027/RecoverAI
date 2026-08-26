package com.recoverai.recoverai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 100)
    private String merchantId;

    @Column(name = "merchant_name", nullable = false, length = 150)
    private String merchantName;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "peak_start_hour")
    private Integer peakStartHour;

    @Column(name = "peak_end_hour")
    private Integer peakEndHour;

    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
