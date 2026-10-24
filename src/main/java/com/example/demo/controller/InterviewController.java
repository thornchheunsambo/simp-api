package com.example.demo.controller;

import com.example.demo.dto.Dtos.InterviewResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {
    private final ApplicationService applications;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<InterviewResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return applications.myInterviews(user);
    }
}