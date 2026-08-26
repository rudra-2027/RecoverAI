package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.entity.FailedMandate;

public interface ScoringService {
    AnalysisResult score(
            FailedMandate mandate,
            AnalysisResult analysis);
}
