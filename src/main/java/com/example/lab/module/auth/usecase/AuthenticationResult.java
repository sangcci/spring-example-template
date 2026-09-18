package com.example.lab.module.auth.usecase;

import java.time.Instant;

public record AuthenticationResult(
        long accountId, String accessToken, Instant accessExpiresAt, String refreshToken, Instant refreshExpiresAt) {}
