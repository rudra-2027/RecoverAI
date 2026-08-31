package com.recoverai.recoverai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFeedbackRequest(
        String reviewerName,
        String reviewerEmail,
        String reviewerRole,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank String issueCategory,
        @NotBlank String severity,
        @NotBlank String failedArea,
        @NotBlank String reviewTitle,
        @NotBlank String feedback,
        String stepsToReproduce,
        String expectedBehavior,
        String suggestedImprovement,
        Boolean wouldRecommend
) {
}
