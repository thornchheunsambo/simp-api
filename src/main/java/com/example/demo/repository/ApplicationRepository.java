package com.example.demo.repository;

import com.example.demo.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByStudentIdAndInternshipId(Long studentId, Long internshipId);
    List<Application> findByStudentIdOrderByAppliedAtDesc(Long studentId);
    List<Application> findByInternshipIdOrderByAppliedAtAsc(Long internshipId);
    boolean existsByCvDocumentIdAndInternshipCompanyUserId(Long cvDocumentId, Long companyUserId);

    @Query("select a.status, count(a) from Application a group by a.status")
    List<Object[]> countByStatus();

    @Query("""
           select sp.fullName, c.name, i.title, a.status, a.appliedAt
           from Application a
             join a.internship i
             join i.company c
             join StudentProfile sp on sp.userId = a.studentId
           order by a.appliedAt
           """)
    List<Object[]> exportRows();
}