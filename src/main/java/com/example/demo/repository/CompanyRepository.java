package com.example.demo.repository;

import com.example.demo.entity.Company;
import com.example.demo.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByUserId(Long userId);
    List<Company> findByVerificationStatusOrderByIdAsc(VerificationStatus status);
}