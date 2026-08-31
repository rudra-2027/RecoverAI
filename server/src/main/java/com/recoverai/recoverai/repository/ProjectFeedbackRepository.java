package com.recoverai.recoverai.repository;

import com.recoverai.recoverai.entity.ProjectFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFeedbackRepository extends JpaRepository<ProjectFeedback, Long> {
    List<ProjectFeedback> findAllByOrderByCreatedAtDesc();
}
