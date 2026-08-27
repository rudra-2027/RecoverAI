package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.dto.CreateFailedMandateRequest;
import com.recoverai.recoverai.dto.SimulatorRecoveryResponse;

public interface SimulationService {
    String simulate(
            AnalysisResult analysis);

    SimulatorRecoveryResponse recover(CreateFailedMandateRequest request);
}
