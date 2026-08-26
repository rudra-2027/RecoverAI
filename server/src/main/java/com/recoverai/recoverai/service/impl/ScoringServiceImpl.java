package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.service.ProbabilityEngineService;
import com.recoverai.recoverai.service.ScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoringServiceImpl
        implements ScoringService {
    private final ProbabilityEngineService probabilityEngineService;

    @Override
    public AnalysisResult score(
            FailedMandate mandate,
            AnalysisResult analysis) {

        int score = probabilityEngineService.calculate(mandate);

        return new AnalysisResult(
                analysis.classification(),
                score,
                score / 100.0
        );
    }
}
