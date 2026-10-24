package com.example.demo.service;

import com.example.demo.dto.Dtos.*;
import com.example.demo.entity.Company;
import com.example.demo.entity.StudentProfile;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.StudentProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository users;
    private final StudentProfileRepository students;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Value("${app.student-email-domain:}")
    private String studentEmailDomain;

    public AuthResponse register(RegisterRequest r) {
        if (r.role() != Role.STUDENT && r.role() != Role.COMPANY) {
            throw ApiException.badRequest("Only STUDENT or COMPANY accounts can self-register");
        }
        String email = r.email().trim().toLowerCase();

        if (r.role() == Role.STUDENT) {
            if (!StringUtils.hasText(r.major())) throw ApiException.badRequest("major is required for students");
            if (StringUtils.hasText(studentEmailDomain) && !email.endsWith("@" + studentEmailDomain)) {
                throw ApiException.badRequest("Students must register with a @" + studentEmailDomain + " email");
            }
        } else if (!StringUtils.hasText(r.companyName())) {
            throw ApiException.badRequest("companyName is required for companies");
        }

        User user = createUser(email, r.password(), r.role());

        if (r.role() == Role.STUDENT) {
            students.save(StudentProfile.builder()
                    .userId(user.getId()).fullName(r.fullName()).major(r.major())
                    .phone(r.phone()).studentCode(r.studentCode()).build());
        } else {
            companies.save(Company.builder()
                    .userId(user.getId()).name(r.companyName()).industry(r.industry())
                    .address(r.address()).website(r.website()).build());
        }
        return toAuth(user);
    }

    public AuthResponse login(LoginRequest r) {
        User user = users.findByEmail(r.email().trim().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!user.isEnabled() || !encoder.matches(r.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return toAuth(user);
    }

    // ---------- admin ----------
    public UserResponse adminCreate(AdminCreateUserRequest r) {
        if (r.role() == Role.STUDENT || r.role() == Role.COMPANY) {
            throw ApiException.badRequest("Students and companies must self-register");
        }
        return toUserResponse(createUser(r.email().trim().toLowerCase(), r.password(), r.role()));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return users.findAll(pageable).map(this::toUserResponse);
    }

    public UserResponse setEnabled(Long id, boolean enabled) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setEnabled(enabled);
        return toUserResponse(u);
    }

    // ---------- helpers ----------
    private User createUser(String email, String password, Role role) {
        if (users.existsByEmail(email)) throw ApiException.conflict("Email already registered");
        return users.save(User.builder().email(email).passwordHash(encoder.encode(password)).role(role).build());
    }

    private AuthResponse toAuth(User u) {
        return new AuthResponse(jwt.generate(u), u.getId(), u.getEmail(), u.getRole());
    }

    private UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}