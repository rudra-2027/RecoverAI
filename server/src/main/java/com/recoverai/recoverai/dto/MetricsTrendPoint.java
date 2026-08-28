package com.recoverai.recoverai.dto;

import java.math.BigDecimal;

public record MetricsTrendPoint(
        String name,
        BigDecimal totalFailed,
        BigDecimal recovered,
        BigDecimal aiRecovered
) {
}
