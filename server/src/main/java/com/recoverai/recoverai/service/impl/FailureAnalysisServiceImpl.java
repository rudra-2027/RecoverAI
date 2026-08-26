package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.dto.AnalysisResult;
import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.service.FailureAnalysisService;
import org.springframework.stereotype.Service;

@Service
public class FailureAnalysisServiceImpl
        implements FailureAnalysisService {

    @Override
    public AnalysisResult analyze(
            FailedMandate mandate) {

        String reason =
                mandate.getFailureReason();

        String classification;

        switch (reason) {

            case "INSUFFICIENT_BALANCE",
                 "LIMIT_EXCEEDED" ->
                    classification = "BD";

            case "BANK_SERVER_DOWN",
                 "NPCI_TIMEOUT" ->
                    classification = "TD";

            case "CARD_EXPIRED" ->
                    classification = "CUSTOMER_ACTION_REQUIRED";

            default ->
                    classification = "PERMANENT";
        }

        return new AnalysisResult(
                classification,
                0,
                0.0
        );
    }
}
