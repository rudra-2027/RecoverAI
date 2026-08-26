package com.recoverai.recoverai.service;

public interface  AuditService {
     void log(
            String mandateId,
            String stage,
            String message) ;
}
