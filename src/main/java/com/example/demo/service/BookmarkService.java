package com.example.demo.service;

import com.example.demo.dto.Dtos.InternshipResponse;
import com.example.demo.entity.Bookmark;
import com.example.demo.entity.BookmarkId;
import com.example.demo.entity.Internship;
import com.example.demo.enums.InternshipStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.BookmarkRepository;
import com.example.demo.repository.InternshipRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {
    private final BookmarkRepository bookmarks;
    private final InternshipRepository internships;
    private final InternshipService internshipService;

    public void add(AuthUser user, Long internshipId) {
        Internship i = internships.findById(internshipId)
                .orElseThrow(() -> ApiException.notFound("Internship not found"));
        if (i.getStatus() != InternshipStatus.APPROVED) throw ApiException.notFound("Internship not found");
        if (!bookmarks.existsById(new BookmarkId(user.id(), internshipId))) {
            bookmarks.save(Bookmark.builder().studentId(user.id()).internshipId(internshipId).build());
        }
    }

    public void remove(AuthUser user, Long internshipId) {
        BookmarkId id = new BookmarkId(user.id(), internshipId);
        if (bookmarks.existsById(id)) bookmarks.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<InternshipResponse> list(AuthUser user) {
        return bookmarks.findByStudentIdOrderByCreatedAtDesc(user.id()).stream()
                .map(b -> internships.findById(b.getInternshipId()).orElse(null))
                .filter(i -> i != null)
                .map(internshipService::toResponse)
                .toList();
    }
}
