package com.recoverai.recoverai.service.impl;

public abstract class AuditService {
    abstract void log(
            String mandateId,
            String stage,
            String message) ;
}
