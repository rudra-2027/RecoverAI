package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;

import java.time.LocalDateTime;

public interface SchedulerService {

    LocalDateTime schedule(
            AnalysisResult analysis);
}