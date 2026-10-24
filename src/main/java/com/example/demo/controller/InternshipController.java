package com.example.demo.controller;

import com.example.demo.dto.Dtos.*;
import com.example.demo.security.AuthUser;
import com.example.demo.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {
    private final InternshipService internships;

    /** Search open (approved, not expired) listings. Any logged-in user. */
    @GetMapping
    public Page<InternshipResponse> search(
            @RequestParam(required = false) String field,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer maxDurationWeeks,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineBefore,
            @RequestParam(required = false) String skill,
            @PageableDefault(size = 20, sort = "deadline") Pageable pageable) {
        return internships.search(field, location, maxDurationWeeks, deadlineBefore, skill, pageable);
    }

    @GetMapping("/recommended")
    @PreAuthorize("hasRole('STUDENT')")
    public Page<InternshipResponse> recommended(@AuthenticationPrincipal AuthUser user,
                                                @PageableDefault(size = 20, sort = "deadline") Pageable pageable) {
        return internships.recommended(user, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('COMPANY')")
    public List<InternshipResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return internships.mine(user);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<InternshipResponse> pending() {
        return internships.pending();
    }

    @GetMapping("/{id}")
    public InternshipResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return internships.get(user, id);
    }

    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    @ResponseStatus(HttpStatus.CREATED)
    public InternshipResponse create(@AuthenticationPrincipal AuthUser user,
                                     @Valid @RequestBody InternshipRequest request) {
        return internships.create(user, request);
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public InternshipResponse review(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                     @RequestParam boolean approved) {
        return internships.review(user, id, approved);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('COMPANY')")
    public InternshipResponse close(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return internships.close(user, id);
    }
}