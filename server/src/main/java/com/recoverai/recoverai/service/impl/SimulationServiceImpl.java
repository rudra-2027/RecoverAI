package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.entity.RecoveryOutcomeStatus;
import com.recoverai.recoverai.service.SimulationService;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class SimulationServiceImpl
        implements SimulationService {

    private final Random random =
            new Random();

    @Override
    public String simulate(
            AnalysisResult analysis) {

        double probability =
                analysis.recoveryProbability();

        return random.nextDouble() < probability
                ? RecoveryOutcomeStatus.SUCCESS.name()
                : RecoveryOutcomeStatus.FAILED.name();
    }
}
