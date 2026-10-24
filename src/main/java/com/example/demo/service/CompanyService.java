package com.example.demo.service;

import com.example.demo.dto.Dtos.CompanyResponse;
import com.example.demo.entity.Company;
import com.example.demo.enums.VerificationStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {
    private final CompanyRepository companies;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public CompanyResponse me(AuthUser user) {
        return toResponse(companies.findByUserId(user.id())
                .orElseThrow(() -> ApiException.notFound("Company profile not found")));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> pending() {
        return companies.findByVerificationStatusOrderByIdAsc(VerificationStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    public CompanyResponse review(AuthUser staff, Long companyId, boolean approved) {
        Company c = companies.findById(companyId).orElseThrow(() -> ApiException.notFound("Company not found"));
        c.setVerificationStatus(approved ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED);
        c.setVerifiedBy(staff.id());
        c.setVerifiedAt(Instant.now());
        notifications.send(c.getUserId(), "COMPANY_REVIEW",
                approved ? "Your company profile has been verified. You can now post internships."
                        : "Your company profile was rejected. Please contact the internship office.");
        return toResponse(c);
    }

    private CompanyResponse toResponse(Company c) {
        return new CompanyResponse(c.getId(), c.getUserId(), c.getName(), c.getIndustry(),
                c.getAddress(), c.getWebsite(), c.getVerificationStatus());
    }
}