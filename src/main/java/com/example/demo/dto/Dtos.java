package com.example.demo.dto;

import com.example.demo.enums.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/** All request/response records in one place to keep the project small. */
public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Role role,
            @NotBlank String fullName,          // student name, or company contact name
            String major,                       // required for STUDENT
            String phone,
            String studentCode,
            String companyName,                 // required for COMPANY
            String industry,
            String address,
            String website) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(String token, Long userId, String email, Role role) {}

    public record AdminCreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Role role) {}

    public record UserResponse(Long id, String email, Role role, boolean enabled, Instant createdAt) {}

    // ---------- student ----------
    public record StudentProfileRequest(
            @NotBlank String fullName,
            @NotBlank String major,
            String phone,
            String studentCode,
            Set<String> skills) {}

    public record StudentProfileResponse(
            Long userId, String fullName, String major, String phone,
            String studentCode, Long cvDocumentId, Set<String> skills) {}

    // ---------- company ----------
    public record CompanyResponse(
            Long id, Long userId, String name, String industry, String address,
            String website, VerificationStatus verificationStatus) {}

    // ---------- internship ----------
    public record InternshipRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotBlank String field,
            String location,
            @Min(1) Integer durationWeeks,
            @Min(1) Integer slots,
            @NotNull @FutureOrPresent LocalDate deadline,
            Set<String> skills) {}

    public record InternshipResponse(
            Long id, Long companyId, String companyName, String title, String description,
            String field, String location, Integer durationWeeks, Integer slots,
            LocalDate deadline, InternshipStatus status, Set<String> skills) {}

    // ---------- application ----------
    public record ApplyRequest(@NotNull Long internshipId, String coverLetter) {}

    public record StatusUpdateRequest(@NotNull ApplicationStatus status, String note) {}

    public record ApplicationResponse(
            Long id, Long studentId, String studentName, Long internshipId, String internshipTitle,
            String companyName, ApplicationStatus status, String coverLetter,
            Long cvDocumentId, Instant appliedAt) {}

    public record HistoryResponse(
            ApplicationStatus fromStatus, ApplicationStatus toStatus,
            Long changedBy, String note, Instant changedAt) {}

    // ---------- interview ----------
    public record InterviewRequest(
            @NotNull Instant scheduledAt,
            @NotNull InterviewMode mode,
            String locationOrLink) {}

    public record InterviewResponse(
            Long id, Long applicationId, Instant scheduledAt, InterviewMode mode,
            String locationOrLink, InterviewStatus status) {}

    // ---------- misc ----------
    public record NotificationResponse(Long id, String type, String message, boolean read, Instant createdAt) {}

    public record DocumentResponse(
            Long id, DocType docType, String originalName, String contentType,
            Long sizeBytes, Long applicationId, Instant uploadedAt) {}
}