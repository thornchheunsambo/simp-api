package com.example.demo.service;

import com.example.demo.dto.Dtos.*;
import com.example.demo.entity.*;
import com.example.demo.enums.ApplicationStatus;
import com.example.demo.enums.InternshipStatus;
import com.example.demo.enums.Role;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.*;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.demo.enums.ApplicationStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    /** Allowed status transitions for company/staff. Students can only withdraw. */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> TRANSITIONS = Map.of(
            PENDING, Set.of(SHORTLISTED, REJECTED),
            SHORTLISTED, Set.of(INTERVIEWED, REJECTED),
            INTERVIEWED, Set.of(ACCEPTED, REJECTED),
            ACCEPTED, Set.of(COMPLETED));

    private final ApplicationRepository applications;
    private final InternshipRepository internships;
    private final StudentProfileRepository students;
    private final StatusHistoryRepository history;
    private final InterviewRepository interviews;
    private final NotificationService notifications;

    // ---------- student ----------
    public ApplicationResponse apply(AuthUser user, ApplyRequest req) {
        Internship i = internships.findById(req.internshipId())
                .orElseThrow(() -> ApiException.notFound("Internship not found"));
        if (i.getStatus() != InternshipStatus.APPROVED || i.getDeadline().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("This internship is not open for applications");
        }
        if (applications.existsByStudentIdAndInternshipId(user.id(), i.getId())) {
            throw ApiException.conflict("You have already applied to this internship");
        }
        StudentProfile sp = students.findById(user.id())
                .orElseThrow(() -> ApiException.notFound("Student profile not found"));

        Application a = applications.save(Application.builder()
                .studentId(user.id()).internship(i)
                .cvDocumentId(sp.getCvDocumentId()).coverLetter(req.coverLetter())
                .status(PENDING).build());
        addHistory(a.getId(), null, PENDING, user.id(), "Application submitted");
        notifications.send(i.getCompany().getUserId(), "NEW_APPLICATION",
                sp.getFullName() + " applied for \"" + i.getTitle() + "\".");
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> mine(AuthUser user) {
        return applications.findByStudentIdOrderByAppliedAtDesc(user.id()).stream().map(this::toResponse).toList();
    }

    public ApplicationResponse withdraw(AuthUser user, Long id) {
        Application a = load(id);
        if (!a.getStudentId().equals(user.id())) throw ApiException.forbidden("Not your application");
        if (a.getStatus() != PENDING && a.getStatus() != SHORTLISTED) {
            throw ApiException.badRequest("Only PENDING or SHORTLISTED applications can be withdrawn");
        }
        change(a, WITHDRAWN, user.id(), "Withdrawn by student");
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> myInterviews(AuthUser user) {
        return interviews.findForStudent(user.id()).stream().map(this::toInterviewResponse).toList();
    }

    // ---------- company / staff ----------
    @Transactional(readOnly = true)
    public List<ApplicationResponse> forInternship(AuthUser user, Long internshipId) {
        Internship i = internships.findById(internshipId)
                .orElseThrow(() -> ApiException.notFound("Internship not found"));
        if (user.role() == Role.COMPANY && !i.getCompany().getUserId().equals(user.id())) {
            throw ApiException.forbidden("Not your internship");
        }
        return applications.findByInternshipIdOrderByAppliedAtAsc(internshipId).stream().map(this::toResponse).toList();
    }

    public ApplicationResponse updateStatus(AuthUser user, Long id, StatusUpdateRequest req) {
        Application a = load(id);
        assertCompanyOwnsOrStaff(user, a);
        Set<ApplicationStatus> allowed = TRANSITIONS.getOrDefault(a.getStatus(), Set.of());
        if (!allowed.contains(req.status())) {
            throw ApiException.badRequest("Cannot move application from " + a.getStatus() + " to " + req.status()
                    + (allowed.isEmpty() ? "" : ". Allowed: " + allowed));
        }
        change(a, req.status(), user.id(), req.note());
        notifications.send(a.getStudentId(), "APPLICATION_STATUS",
                "Your application for \"" + a.getInternship().getTitle() + "\" is now " + req.status() + ".");
        return toResponse(a);
    }

    public InterviewResponse scheduleInterview(AuthUser user, Long applicationId, InterviewRequest req) {
        Application a = load(applicationId);
        assertCompanyOwnsOrStaff(user, a);
        if (a.getStatus() != SHORTLISTED) {
            throw ApiException.badRequest("Interviews can only be scheduled for SHORTLISTED applications");
        }
        Interview iv = interviews.save(Interview.builder()
                .applicationId(a.getId()).scheduledAt(req.scheduledAt())
                .mode(req.mode()).locationOrLink(req.locationOrLink()).build());
        notifications.send(a.getStudentId(), "INTERVIEW_SCHEDULED",
                "Interview scheduled for \"" + a.getInternship().getTitle() + "\" at " + req.scheduledAt() + ".");
        return toInterviewResponse(iv);
    }

    // ---------- shared ----------
    @Transactional(readOnly = true)
    public List<HistoryResponse> history(AuthUser user, Long id) {
        Application a = load(id);
        boolean isStudentOwner = user.role() == Role.STUDENT && a.getStudentId().equals(user.id());
        boolean isCompanyOwner = user.role() == Role.COMPANY && a.getInternship().getCompany().getUserId().equals(user.id());
        boolean staff = user.role() == Role.STAFF || user.role() == Role.ADMIN;
        if (!isStudentOwner && !isCompanyOwner && !staff) throw ApiException.forbidden("Not allowed");
        return history.findByApplicationIdOrderByChangedAtAsc(id).stream()
                .map(h -> new HistoryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedBy(), h.getNote(), h.getChangedAt()))
                .toList();
    }

    // ---------- helpers ----------
    private void assertCompanyOwnsOrStaff(AuthUser user, Application a) {
        if (user.role() == Role.COMPANY && !a.getInternship().getCompany().getUserId().equals(user.id())) {
            throw ApiException.forbidden("This application is not for one of your internships");
        }
    }

    private void change(Application a, ApplicationStatus to, Long by, String note) {
        ApplicationStatus from = a.getStatus();
        a.setStatus(to);
        addHistory(a.getId(), from, to, by, note);
    }

    private void addHistory(Long appId, ApplicationStatus from, ApplicationStatus to, Long by, String note) {
        history.save(ApplicationStatusHistory.builder()
                .applicationId(appId).fromStatus(from).toStatus(to).changedBy(by).note(note).build());
    }

    private Application load(Long id) {
        return applications.findById(id).orElseThrow(() -> ApiException.notFound("Application not found"));
    }

    private ApplicationResponse toResponse(Application a) {
        String studentName = students.findById(a.getStudentId()).map(StudentProfile::getFullName).orElse("Unknown");
        Internship i = a.getInternship();
        return new ApplicationResponse(a.getId(), a.getStudentId(), studentName, i.getId(), i.getTitle(),
                i.getCompany().getName(), a.getStatus(), a.getCoverLetter(), a.getCvDocumentId(), a.getAppliedAt());
    }

    private InterviewResponse toInterviewResponse(Interview iv) {
        return new InterviewResponse(iv.getId(), iv.getApplicationId(), iv.getScheduledAt(),
                iv.getMode(), iv.getLocationOrLink(), iv.getStatus());
    }
}