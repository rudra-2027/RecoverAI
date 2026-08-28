package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class SchedulerServiceImpl
        implements SchedulerService {

    @Override
    public LocalDateTime schedule(
            AnalysisResult analysis) {

        if ("TD".equals(
                analysis.classification())) {

            LocalDateTime scheduledAt = LocalDateTime.now()
                    .plusHours(4);
            log.info("Scheduled recovery retry for classification={} at={}", analysis.classification(), scheduledAt);
            return scheduledAt;
        }

        LocalDateTime scheduledAt = LocalDateTime.now()
                .plusHours(24);
        log.info("Scheduled recovery retry for classification={} at={}", analysis.classification(), scheduledAt);
        return scheduledAt;
    }
}
