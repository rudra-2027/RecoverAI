package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.dto.DecisionResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.service.impl.DecisionService;
import org.springframework.stereotype.Service;

@Service
public class DecisionServiceImpl
        implements DecisionService {

    @Override
    public DecisionResult decide(
            FailedMandate mandate,
            AnalysisResult analysis) {

        String action;

        if ("PERMANENT".equals(
                analysis.classification())) {

            action = "RE_REGISTER";

        } else if (
                mandate.getRetryCount() >= 3) {

            action = "STOP";

        } else {

            action = "RETRY";
        }

        return new DecisionResult(
                action,
                null
        );
    }
}