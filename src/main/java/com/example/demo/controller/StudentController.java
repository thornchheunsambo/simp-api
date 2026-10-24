package com.example.demo.controller;

import com.example.demo.dto.Dtos.*;
import com.example.demo.security.AuthUser;
import com.example.demo.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService students;

    @GetMapping
    public StudentProfileResponse me(@AuthenticationPrincipal AuthUser user) {
        return students.me(user);
    }

    @PutMapping
    public StudentProfileResponse update(@AuthenticationPrincipal AuthUser user,
                                         @Valid @RequestBody StudentProfileRequest request) {
        return students.update(user, request);
    }
}