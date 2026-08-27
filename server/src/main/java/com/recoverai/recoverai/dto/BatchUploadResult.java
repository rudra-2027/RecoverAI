package com.recoverai.recoverai.dto;

public record BatchUploadResult(
        Long batchRunId,
        String fileName,
        String sourceType,
        int importedMandates,
        boolean processed,
        BatchRunResult processingResult
) {
}
