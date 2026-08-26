package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.entity.FailedMandate;

public interface FailureAnalysisService {
    AnalysisResult analyze(FailedMandate mandate);
}
