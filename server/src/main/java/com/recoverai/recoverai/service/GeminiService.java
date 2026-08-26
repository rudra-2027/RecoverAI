package com.recoverai.recoverai.service;

public interface GeminiService {
    String explainDecision(String mandateId);

    String summarizeBatches();

    String answerMerchantQuestion(String question);

    String generateInsights();
}
