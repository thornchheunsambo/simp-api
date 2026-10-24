package com.example.demo.service;

import com.example.demo.dto.Dtos.StudentProfileRequest;
import com.example.demo.dto.Dtos.StudentProfileResponse;
import com.example.demo.entity.Skill;
import com.example.demo.entity.StudentProfile;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {
    private final StudentProfileRepository students;
    private final SkillService skillService;

    @Transactional(readOnly = true)
    public StudentProfileResponse me(AuthUser user) {
        return toResponse(load(user.id()));
    }

    public StudentProfileResponse update(AuthUser user, StudentProfileRequest r) {
        StudentProfile sp = load(user.id());
        sp.setFullName(r.fullName());
        sp.setMajor(r.major());
        sp.setPhone(r.phone());
        sp.setStudentCode(r.studentCode());
        if (r.skills() != null) sp.setSkills(skillService.resolve(r.skills()));
        return toResponse(sp);
    }

    private StudentProfile load(Long id) {
        return students.findById(id).orElseThrow(() -> ApiException.notFound("Student profile not found"));
    }

    private StudentProfileResponse toResponse(StudentProfile sp) {
        Set<String> skills = new TreeSet<>();
        for (Skill s : sp.getSkills()) skills.add(s.getName());
        return new StudentProfileResponse(sp.getUserId(), sp.getFullName(), sp.getMajor(), sp.getPhone(),
                sp.getStudentCode(), sp.getCvDocumentId(), skills);
    }
}