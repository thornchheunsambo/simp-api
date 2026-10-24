package com.example.demo.controller;

import com.example.demo.dto.Dtos.CompanyResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companies;

    @GetMapping("/me")
    @PreAuthorize("hasRole('COMPANY')")
    public CompanyResponse me(@AuthenticationPrincipal AuthUser user) {
        return companies.me(user);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public List<CompanyResponse> pending() {
        return companies.pending();
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public CompanyResponse verify(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                  @RequestParam boolean approved) {
        return companies.review(user, id, approved);
    }
}