package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.AnalysisResult;

import java.time.LocalDateTime;

public interface SchedulerService {

    LocalDateTime schedule(
            AnalysisResult analysis);
}