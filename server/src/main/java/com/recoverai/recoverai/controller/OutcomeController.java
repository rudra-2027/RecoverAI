package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.entity.RecoveryOutcome;
import com.recoverai.recoverai.repository.RecoveryOutcomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/outcomes")
public class OutcomeController {
    private final RecoveryOutcomeRepository outcomeRepository;

    @GetMapping
    public List<RecoveryOutcome> all() {
        return outcomeRepository.findAll();
    }

    @GetMapping("/{mandateId}")
    public List<RecoveryOutcome> byMandate(@PathVariable String mandateId) {
        return outcomeRepository.findByMandateIdOrderByOutcomeTimestampDesc(mandateId);
    }
}
