package com.example.lab.module.auth.infra.persistence;

import java.time.Instant;

public record RefreshSession(
        String sessionId, long accountId, String familyId, String role, Status status, Instant expiresAt) {

    public enum Status {
        ACTIVE,
        CONSUMED
    }
}
