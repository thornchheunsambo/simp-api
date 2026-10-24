package com.example.demo.controller;

import com.example.demo.dto.Dtos.DocumentResponse;
import com.example.demo.enums.DocType;
import com.example.demo.security.AuthUser;
import com.example.demo.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documents;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@AuthenticationPrincipal AuthUser user,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam DocType docType,
                                   @RequestParam(required = false) Long applicationId) throws IOException {
        return documents.upload(user, file, docType, applicationId);
    }

    @GetMapping("/mine")
    public List<DocumentResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return documents.mine(user);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        DocumentService.Download d = documents.download(user, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.meta().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.meta().getOriginalName()).build().toString())
                .body(d.resource());
    }
}