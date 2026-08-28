package com.recoverai.recoverai.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;

@Repository
public interface FailedMandateRepository extends JpaRepository<FailedMandate, Long>{

    Optional<FailedMandate> findByMandateId(String mandateId);

    Optional<FailedMandate> findTopByEscalatedTrueOrderByCreatedAtDesc();

    List<FailedMandate> findByBatchRunId(Long batchRunId);

    List<FailedMandate> findByStatus(PaymentStatus status);

    List<FailedMandate> findByBatchRunIdAndStatus(Long batchRunId, PaymentStatus status);

    long countByStatus(PaymentStatus status);
}
