package com.example.lab.module.auth.infra.persistence;

import java.time.Instant;

public record IssuedRefreshSession(String sessionId, String token, Instant expiresAt) {}
