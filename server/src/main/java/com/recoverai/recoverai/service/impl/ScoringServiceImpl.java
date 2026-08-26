package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.service.impl.ScoringService;
import org.springframework.stereotype.Service;

@Service
public class ScoringServiceImpl
        implements ScoringService {

    @Override
    public AnalysisResult score(
            FailedMandate mandate,
            AnalysisResult analysis) {

        int score;

        switch (mandate.getFailureReason()) {

            case "BANK_SERVER_DOWN" -> score = 95;

            case "NPCI_TIMEOUT" -> score = 90;

            case "INSUFFICIENT_BALANCE" -> score = 75;

            case "LIMIT_EXCEEDED" -> score = 60;

            default -> score = 5;
        }

        return new AnalysisResult(
                analysis.classification(),
                score,
                score / 100.0
        );
    }
}