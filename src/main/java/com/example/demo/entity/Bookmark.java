package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "bookmarks")
@IdClass(BookmarkId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bookmark {
    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Id
    @Column(name = "internship_id")
    private Long internshipId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}