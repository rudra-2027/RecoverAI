package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.dto.CreateFeedbackRequest;
import com.recoverai.recoverai.entity.ProjectFeedback;
import com.recoverai.recoverai.repository.ProjectFeedbackRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final ProjectFeedbackRepository feedbackRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectFeedback createFeedback(@Valid @RequestBody CreateFeedbackRequest request) {
        log.info("Creating project feedback rating={}, category={}, severity={}",
                request.rating(), request.issueCategory(), request.severity());
        ProjectFeedback feedback = ProjectFeedback.builder()
                .reviewerName(request.reviewerName())
                .reviewerEmail(request.reviewerEmail())
                .reviewerRole(request.reviewerRole())
                .rating(request.rating())
                .issueCategory(request.issueCategory())
                .severity(request.severity())
                .failedArea(request.failedArea())
                .reviewTitle(request.reviewTitle())
                .feedback(request.feedback())
                .stepsToReproduce(request.stepsToReproduce())
                .expectedBehavior(request.expectedBehavior())
                .suggestedImprovement(request.suggestedImprovement())
                .wouldRecommend(request.wouldRecommend())
                .createdAt(LocalDateTime.now())
                .build();
        return feedbackRepository.save(feedback);
    }

    @GetMapping
    public List<ProjectFeedback> feedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }
}
