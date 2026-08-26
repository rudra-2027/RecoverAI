package com.recoverai.recoverai.dto;

public record AnalysisResult(
        String classification,
        Integer recoverabilityScore,
        Double recoveryProbability
) {
}