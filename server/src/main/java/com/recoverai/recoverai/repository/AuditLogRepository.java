package com.recoverai.recoverai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recoverai.recoverai.entity.AuditLog;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>{
    List<AuditLog> findByMandateIdOrderByCreatedAtAsc(String mandateId);
}
