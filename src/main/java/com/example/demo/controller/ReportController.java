package com.example.demo.controller;

import com.example.demo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reports;

    @GetMapping("/placements")
    public Map<String, Object> placements() {
        return reports.placementSummary();
    }

    @GetMapping("/placements/export")
    public ResponseEntity<byte[]> export() {
        byte[] body = reports.exportCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("placements.csv").build().toString())
                .body(body);
    }
}