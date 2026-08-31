package com.recoverai.recoverai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_name", length = 120)
    private String reviewerName;

    @Column(name = "reviewer_email", length = 160)
    private String reviewerEmail;

    @Column(name = "reviewer_role", length = 80)
    private String reviewerRole;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "issue_category", nullable = false, length = 80)
    private String issueCategory;

    @Column(nullable = false, length = 80)
    private String severity;

    @Column(name = "failed_area", nullable = false, length = 160)
    private String failedArea;

    @Column(name = "review_title", nullable = false, length = 160)
    private String reviewTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String feedback;

    @Column(name = "steps_to_reproduce", columnDefinition = "TEXT")
    private String stepsToReproduce;

    @Column(name = "expected_behavior", columnDefinition = "TEXT")
    private String expectedBehavior;

    @Column(name = "suggested_improvement", columnDefinition = "TEXT")
    private String suggestedImprovement;

    @Column(name = "would_recommend")
    private Boolean wouldRecommend;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
