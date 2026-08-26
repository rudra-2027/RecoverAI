package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.service.SchedulerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SchedulerServiceImpl
        implements SchedulerService {

    @Override
    public LocalDateTime schedule(
            AnalysisResult analysis) {

        if ("TD".equals(
                analysis.classification())) {

            return LocalDateTime.now()
                    .plusHours(4);
        }

        return LocalDateTime.now()
                .plusHours(24);
    }
}
