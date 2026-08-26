package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.dto.MetricsResponse;
import com.recoverai.recoverai.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metrics")
public class MetricsController {
    private final MetricsService metricsService;

    @GetMapping
    public MetricsResponse metrics() {
        return metricsService.calculate();
    }
}
