package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.BatchRunResult;

public interface BatchProcessingService {
    BatchRunResult runAllFailedMandates();
}
