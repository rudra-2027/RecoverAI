package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.dto.AiChatRequest;
import com.recoverai.recoverai.service.GeminiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class GeminiController {
    private final GeminiService geminiService;

    @PostMapping("/explain/{mandateId}")
    public Map<String, String> explain(@PathVariable String mandateId) {
        return Map.of("answer", geminiService.explainDecision(mandateId));
    }

    @PostMapping("/summary")
    public Map<String, String> summary() {
        return Map.of("answer", geminiService.summarizeBatches());
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@Valid @RequestBody AiChatRequest request) {
        return Map.of("answer", geminiService.answerMerchantQuestion(request.question()));
    }

    @PostMapping("/insights")
    public Map<String, String> insights() {
        return Map.of("answer", geminiService.generateInsights());
    }
}
