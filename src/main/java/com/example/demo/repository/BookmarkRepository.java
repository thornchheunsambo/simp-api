package com.example.demo.repository;

import com.example.demo.entity.Bookmark;
import com.example.demo.entity.BookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {
    List<Bookmark> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}