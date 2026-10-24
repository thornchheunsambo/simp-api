package com.example.demo.controller;

import com.example.demo.dto.Dtos.*;
import com.example.demo.security.AuthUser;
import com.example.demo.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applications;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ApplyRequest request) {
        return applications.apply(user, request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ApplicationResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return applications.mine(user);
    }

    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ApplicationResponse withdraw(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return applications.withdraw(user, id);
    }

    @GetMapping("/internship/{internshipId}")
    @PreAuthorize("hasAnyRole('COMPANY','STAFF','ADMIN')")
    public List<ApplicationResponse> forInternship(@AuthenticationPrincipal AuthUser user,
                                                   @PathVariable Long internshipId) {
        return applications.forInternship(user, internshipId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COMPANY','STAFF','ADMIN')")
    public ApplicationResponse updateStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                            @Valid @RequestBody StatusUpdateRequest request) {
        return applications.updateStatus(user, id, request);
    }

    @PostMapping("/{id}/interview")
    @PreAuthorize("hasAnyRole('COMPANY','STAFF','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse scheduleInterview(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                               @Valid @RequestBody InterviewRequest request) {
        return applications.scheduleInterview(user, id, request);
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return applications.history(user, id);
    }
}