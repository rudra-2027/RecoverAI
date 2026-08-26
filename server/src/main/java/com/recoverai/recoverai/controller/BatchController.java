package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.dto.BatchRunResult;
import com.recoverai.recoverai.dto.BatchUploadResult;
import com.recoverai.recoverai.entity.BatchRun;
import com.recoverai.recoverai.exception.ResourceNotFoundException;
import com.recoverai.recoverai.repository.BatchRunRepository;
import com.recoverai.recoverai.service.BatchFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/batches")
public class BatchController {
    private final BatchRunRepository batchRunRepository;
    private final BatchFileService batchFileService;

    @GetMapping
    public List<BatchRun> all() {
        return batchRunRepository.findAll();
    }

    @GetMapping("/{id}")
    public BatchRun byId(@PathVariable Long id) {
        return batchRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch run not found: " + id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchUploadResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "process", defaultValue = "false") boolean process) {
        return batchFileService.upload(file, process);
    }

    @PostMapping("/{id}/run")
    public BatchRunResult runBatch(@PathVariable Long id) {
        return batchFileService.runBatch(id);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> exportReport(@PathVariable Long id) {
        byte[] report = batchFileService.exportReport(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("recoverai-batch-" + id + "-report.csv")
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(report);
    }
}
