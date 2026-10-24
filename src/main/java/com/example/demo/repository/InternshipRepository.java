package com.example.demo.repository;

import com.example.demo.entity.Internship;
import com.example.demo.enums.InternshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship, Long> {
    List<Internship> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<Internship> findByStatusOrderByCreatedAtAsc(InternshipStatus status);

    @Query("select i.status, count(i) from Internship i group by i.status")
    List<Object[]> countByStatus();
}