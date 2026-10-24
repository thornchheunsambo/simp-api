package com.example.demo.service;

import com.example.demo.dto.Dtos.DocumentResponse;
import com.example.demo.entity.Application;
import com.example.demo.entity.Document;
import com.example.demo.entity.StudentProfile;
import com.example.demo.enums.ApplicationStatus;
import com.example.demo.enums.DocType;
import com.example.demo.enums.Role;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.ApplicationRepository;
import com.example.demo.repository.DocumentRepository;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stores files on local disk. NOTE: Render's free web-service disk is ephemeral -
 * for production replace {@link #store}/{@link #download} with S3 / Cloudinary / Supabase Storage.
 */
@Service
@Transactional
public class DocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png", "image/jpeg");

    private final DocumentRepository documents;
    private final StudentProfileRepository students;
    private final ApplicationRepository applications;
    private final Path root;

    public DocumentService(DocumentRepository documents, StudentProfileRepository students,
                           ApplicationRepository applications, @Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.documents = documents;
        this.students = students;
        this.applications = applications;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public DocumentResponse upload(AuthUser user, MultipartFile file, DocType type, Long applicationId) throws IOException {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("File is empty");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Allowed file types: PDF, DOC, DOCX, PNG, JPG");
        }
        if (applicationId != null) {
            Application a = applications.findById(applicationId)
                    .orElseThrow(() -> ApiException.notFound("Application not found"));
            if (!a.getStudentId().equals(user.id())) throw ApiException.forbidden("Not your application");
            if (type == DocType.COMPLETION_REPORT && a.getStatus() != ApplicationStatus.ACCEPTED
                    && a.getStatus() != ApplicationStatus.COMPLETED) {
                throw ApiException.badRequest("Completion documents require an ACCEPTED application");
            }
        } else if (type == DocType.COMPLETION_REPORT) {
            throw ApiException.badRequest("applicationId is required for completion documents");
        }

        String key = store(file);
        Document d = documents.save(Document.builder()
                .ownerId(user.id()).docType(type)
                .originalName(cleanName(file.getOriginalFilename()))
                .contentType(contentType).sizeBytes(file.getSize())
                .storageKey(key).applicationId(applicationId).build());

        if (type == DocType.CV) {
            StudentProfile sp = students.findById(user.id())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found"));
            sp.setCvDocumentId(d.getId());
        }
        return toResponse(d);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> mine(AuthUser user) {
        return documents.findByOwnerIdOrderByUploadedAtDesc(user.id()).stream().map(this::toResponse).toList();
    }

    public record Download(Resource resource, Document meta) {}

    @Transactional(readOnly = true)
    public Download download(AuthUser user, Long id) {
        Document d = documents.findById(id).orElseThrow(() -> ApiException.notFound("File not found"));
        boolean allowed = d.getOwnerId().equals(user.id())
                || user.role() == Role.STAFF || user.role() == Role.ADMIN
                || (user.role() == Role.COMPANY
                && applications.existsByCvDocumentIdAndInternshipCompanyUserId(d.getId(), user.id()));
        if (!allowed) throw ApiException.forbidden("You are not allowed to access this file");

        Path p = root.resolve(d.getStorageKey()).normalize();
        if (!p.startsWith(root) || !Files.exists(p)) throw ApiException.notFound("File not found on storage");
        return new Download(new FileSystemResource(p), d);
    }

    // ---------- storage ----------
    private String store(MultipartFile file) throws IOException {
        String ext = "";
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            String e = name.substring(name.lastIndexOf('.') + 1).replaceAll("[^A-Za-z0-9]", "");
            if (!e.isEmpty() && e.length() <= 5) ext = "." + e.toLowerCase();
        }
        String key = UUID.randomUUID() + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        }
        return key;
    }

    private String cleanName(String n) {
        if (n == null || n.isBlank()) return "file";
        String base = Paths.get(n).getFileName().toString();
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(d.getId(), d.getDocType(), d.getOriginalName(), d.getContentType(),
                d.getSizeBytes(), d.getApplicationId(), d.getUploadedAt());
    }
}