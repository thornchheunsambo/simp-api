package com.example.demo.controller;

import com.example.demo.dto.Dtos.InternshipResponse;
import com.example.demo.security.AuthUser;
import com.example.demo.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarks;

    @GetMapping
    public List<InternshipResponse> list(@AuthenticationPrincipal AuthUser user) {
        return bookmarks.list(user);
    }

    @PostMapping("/{internshipId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@AuthenticationPrincipal AuthUser user, @PathVariable Long internshipId) {
        bookmarks.add(user, internshipId);
    }

    @DeleteMapping("/{internshipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthUser user, @PathVariable Long internshipId) {
        bookmarks.remove(user, internshipId);
    }
}