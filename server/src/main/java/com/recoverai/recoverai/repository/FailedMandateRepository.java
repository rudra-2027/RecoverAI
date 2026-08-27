package com.recoverai.recoverai.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recoverai.recoverai.entity.FailedMandate;

@Repository
public interface FailedMandateRepository extends JpaRepository<FailedMandate, Long>{

    Optional<FailedMandate> findByMandateId(String mandateId);

    List<FailedMandate> findByBatchRunId(Long batchRunId);
}
