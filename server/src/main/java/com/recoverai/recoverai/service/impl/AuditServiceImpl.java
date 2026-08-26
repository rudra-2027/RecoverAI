package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.entity.AuditLog;
import com.recoverai.recoverai.entity.RecoveryStage;
import com.recoverai.recoverai.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import com.recoverai.recoverai.service.AuditService;

import java.time.LocalDateTime;

@Service
public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(
            String mandateId,
            String stage,
            String message) {

        AuditLog log = new AuditLog();

        log.setMandateId(mandateId);
        log.setStage(normalizeStage(stage));
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(log);
    }

    private RecoveryStage normalizeStage(String stage) {
        return switch (stage) {
            case "ANALYSIS" -> RecoveryStage.CLASSIFICATION;
            case "PROBABILITY", "RETRY_PLANNING", "PRE_RETRY_CHECK" -> RecoveryStage.SCORING;
            case "STOPPED", "ESCALATED" -> RecoveryStage.DECISION;
            default -> RecoveryStage.valueOf(stage);
        };
    }
}
