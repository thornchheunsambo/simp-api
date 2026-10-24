package com.example.demo.service;

import com.example.demo.dto.Dtos.InternshipRequest;
import com.example.demo.dto.Dtos.InternshipResponse;
import com.example.demo.entity.Company;
import com.example.demo.entity.Internship;
import com.example.demo.entity.Skill;
import com.example.demo.entity.StudentProfile;
import com.example.demo.enums.InternshipStatus;
import com.example.demo.enums.Role;
import com.example.demo.enums.VerificationStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.InternshipRepository;
import com.example.demo.repository.InternshipSearchRepository;
import com.example.demo.repository.InternshipSearchRepository.Filter;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Transactional
public class InternshipService {
    private final InternshipRepository internships;
    private final InternshipSearchRepository searchRepo;
    private final CompanyRepository companies;
    private final StudentProfileRepository students;
    private final SkillService skillService;
    private final NotificationService notifications;

    // ---------- company ----------
    public InternshipResponse create(AuthUser user, InternshipRequest r) {
        Company company = companies.findByUserId(user.id())
                .orElseThrow(() -> ApiException.notFound("Company profile not found"));
        if (company.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw ApiException.forbidden("Your company must be verified by university staff before posting internships");
        }
        Internship i = Internship.builder()
                .company(company).title(r.title()).description(r.description())
                .field(r.field()).location(r.location()).durationWeeks(r.durationWeeks())
                .slots(r.slots() == null ? 1 : r.slots()).deadline(r.deadline())
                .status(InternshipStatus.PENDING_APPROVAL)
                .skills(skillService.resolve(r.skills()))
                .build();
        return toResponse(internships.save(i));
    }

    @Transactional(readOnly = true)
    public List<InternshipResponse> mine(AuthUser user) {
        Company company = companies.findByUserId(user.id())
                .orElseThrow(() -> ApiException.notFound("Company profile not found"));
        return internships.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream().map(this::toResponse).toList();
    }

    public InternshipResponse close(AuthUser user, Long id) {
        Internship i = load(id);
        if (!i.getCompany().getUserId().equals(user.id())) throw ApiException.forbidden("Not your internship");
        i.setStatus(InternshipStatus.CLOSED);
        return toResponse(i);
    }

    // ---------- staff ----------
    @Transactional(readOnly = true)
    public List<InternshipResponse> pending() {
        return internships.findByStatusOrderByCreatedAtAsc(InternshipStatus.PENDING_APPROVAL)
                .stream().map(this::toResponse).toList();
    }

    public InternshipResponse review(AuthUser staff, Long id, boolean approved) {
        Internship i = load(id);
        if (i.getStatus() != InternshipStatus.PENDING_APPROVAL) {
            throw ApiException.conflict("Only listings in PENDING_APPROVAL can be reviewed");
        }
        i.setStatus(approved ? InternshipStatus.APPROVED : InternshipStatus.REJECTED);
        i.setApprovedBy(staff.id());
        i.setApprovedAt(Instant.now());
        notifications.send(i.getCompany().getUserId(), "LISTING_REVIEW",
                "Your listing \"" + i.getTitle() + "\" was " + (approved ? "approved" : "rejected") + ".");
        return toResponse(i);
    }

    // ---------- everyone ----------
    @Transactional(readOnly = true)
    public Page<InternshipResponse> search(String field, String location, Integer maxDurationWeeks,
                                           LocalDate deadlineBefore, String skill, Pageable pageable) {
        Filter f = new Filter(field, location, maxDurationWeeks, deadlineBefore, skill, null, null);
        return searchRepo.searchOpen(f, pageable).map(this::toResponse);
    }

    /** Listings matching the student's major or any of their skills. */
    @Transactional(readOnly = true)
    public Page<InternshipResponse> recommended(AuthUser user, Pageable pageable) {
        StudentProfile sp = students.findById(user.id())
                .orElseThrow(() -> ApiException.notFound("Student profile not found"));
        List<String> skillNames = sp.getSkills().stream().map(s -> s.getName().toLowerCase()).toList();
        Filter f = new Filter(null, null, null, null, null, sp.getMajor(), skillNames);
        return searchRepo.searchOpen(f, pageable).map(this::toResponse);
    }

    public InternshipResponse get(AuthUser user, Long id) {
        Internship i = load(id);
        boolean privileged = user.role() == Role.STAFF || user.role() == Role.ADMIN
                || (user.role() == Role.COMPANY && i.getCompany().getUserId().equals(user.id()));
        if (i.getStatus() != InternshipStatus.APPROVED && !privileged) {
            throw ApiException.notFound("Internship not found");
        }
        return toResponse(i);
    }

    // ---------- helpers ----------
    private Internship load(Long id) {
        return internships.findById(id).orElseThrow(() -> ApiException.notFound("Internship not found"));
    }

    public InternshipResponse toResponse(Internship i) {
        Set<String> skills = new TreeSet<>();
        for (Skill s : i.getSkills()) skills.add(s.getName());
        return new InternshipResponse(i.getId(), i.getCompany().getId(), i.getCompany().getName(),
                i.getTitle(), i.getDescription(), i.getField(), i.getLocation(), i.getDurationWeeks(),
                i.getSlots(), i.getDeadline(), i.getStatus(), skills);
    }
}