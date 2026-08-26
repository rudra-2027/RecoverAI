package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.entity.AuditLog;
import com.recoverai.recoverai.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/{mandateId}")
    public List<AuditLog> byMandate(@PathVariable String mandateId) {
        return auditLogRepository.findByMandateIdOrderByCreatedAtAsc(mandateId);
    }

    @GetMapping("/{mandateId}/export")
    public ResponseEntity<byte[]> exportByMandate(@PathVariable String mandateId) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,mandateId,stage,message,createdAt\n");
        for (AuditLog log : auditLogRepository.findByMandateIdOrderByCreatedAtAsc(mandateId)) {
            csv.append(log.getId()).append(',')
                    .append(escape(log.getMandateId())).append(',')
                    .append(log.getStage()).append(',')
                    .append(escape(log.getMessage())).append(',')
                    .append(log.getCreatedAt())
                    .append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-" + mandateId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
