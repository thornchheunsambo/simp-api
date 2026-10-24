package com.example.demo.repository;

import com.example.demo.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    @Query("select i from Interview i where i.applicationId in " +
            "(select a.id from Application a where a.studentId = :studentId) order by i.scheduledAt")
    List<Interview> findForStudent(@Param("studentId") Long studentId);
}