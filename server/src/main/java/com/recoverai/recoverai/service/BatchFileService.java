package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.BatchRunResult;
import com.recoverai.recoverai.dto.BatchUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface BatchFileService {
    BatchUploadResult upload(MultipartFile file, boolean process);

    BatchRunResult runBatch(Long batchRunId);

    byte[] exportReport(Long batchRunId);
}
