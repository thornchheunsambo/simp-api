package com.example.demo.security;

import com.example.demo.enums.Role;

/** The authenticated principal placed in the SecurityContext by JwtAuthFilter. */
public record AuthUser(Long id, String email, Role role) {}