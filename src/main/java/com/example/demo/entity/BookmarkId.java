package com.example.demo.entity;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class BookmarkId implements Serializable {
    private Long studentId;
    private Long internshipId;
}