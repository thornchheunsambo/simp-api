package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN account on startup (change the password via env vars in production). */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository users, PasswordEncoder encoder,
                           @Value("${app.admin-email}") String adminEmail,
                           @Value("${app.admin-password}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByEmail(adminEmail.toLowerCase())) {
            users.save(User.builder()
                    .email(adminEmail.toLowerCase())
                    .passwordHash(encoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build());
            log.info("Created default admin account: {}", adminEmail);
        }
    }
}