package com.example.lab.module.user.domain;

import java.time.Instant;

public record UserAccount(
        long id,
        String email,
        String passwordHash,
        UserAccountStatus status,
        UserRole role,
        Instant restrictionEndsAt) {}
