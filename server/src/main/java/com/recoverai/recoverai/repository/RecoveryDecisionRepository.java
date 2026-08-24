package com.recoverai.recoverai.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.recoverai.recoverai.entity.RecoveryDecision;


@Repository
public interface RecoveryDecisionRepository extends JpaRepository<RecoveryDecision, UUID > {
    
}
