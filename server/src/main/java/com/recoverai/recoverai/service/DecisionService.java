package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.dto.DecisionResult;
import com.recoverai.recoverai.entity.FailedMandate;

public interface DecisionService {

    DecisionResult decide(
            FailedMandate mandate,
            AnalysisResult analysis);

}