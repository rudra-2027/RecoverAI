package com.recoverai.recoverai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.recoverai.recoverai.entity.RecoveryDecision;

import java.util.List;
import java.util.Optional;


@Repository
public interface RecoveryDecisionRepository extends JpaRepository<RecoveryDecision, Long > {
    Optional<RecoveryDecision> findTopByMandateIdOrderByCreatedAtDesc(String mandateId);

    Optional<RecoveryDecision> findTopByEscalatedTrueOrderByCreatedAtDesc();

    List<RecoveryDecision> findByMandateIdOrderByCreatedAtDesc(String mandateId);
}
