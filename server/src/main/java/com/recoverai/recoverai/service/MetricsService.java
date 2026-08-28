package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.MetricsResponse;
import com.recoverai.recoverai.dto.MetricsTrendPoint;

import java.util.List;

public interface MetricsService {
    MetricsResponse calculate();

    List<MetricsTrendPoint> trends();
}
