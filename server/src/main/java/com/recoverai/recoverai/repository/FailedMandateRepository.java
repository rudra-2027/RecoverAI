package com.recoverai.recoverai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.entity.PaymentStatus;

@Repository
public interface FailedMandateRepository extends JpaRepository<FailedMandate, Long>{

    Optional<FailedMandate> findTopByMandateIdOrderByCreatedAtDescIdDesc(String mandateId);

    Optional<FailedMandate> findTopByEscalatedTrueOrderByCreatedAtDesc();

    List<FailedMandate> findByBatchRunId(Long batchRunId);

    List<FailedMandate> findByStatus(PaymentStatus status);

    List<FailedMandate> findByBatchRunIdAndStatus(Long batchRunId, PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("select distinct f.merchantId from FailedMandate f where f.merchantId is not null and f.merchantId <> ''")
    List<String> findDistinctMerchantIds();
}
