package com.recoverai.recoverai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recoverai.recoverai.entity.RecoveryOutcome;
import com.recoverai.recoverai.entity.RecoveryOutcomeStatus;

import java.util.List;

@Repository
public interface  RecoveryOutcomeRepository extends JpaRepository<RecoveryOutcome, Long> {
    List<RecoveryOutcome> findByMandateIdOrderByOutcomeTimestampDesc(String mandateId);

    long countByOutcome(RecoveryOutcomeStatus outcome);
}
