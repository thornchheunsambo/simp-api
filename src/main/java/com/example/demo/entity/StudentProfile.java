package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String major;

    private String phone;

    @Column(name = "cv_document_id")
    private Long cvDocumentId;

    @ManyToMany
    @JoinTable(name = "student_skills",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();
}