package com.example.lab.module.auth.infra.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RefreshSessionStore {

    private static final String SESSION_KEY_PREFIX = "auth:refresh:session:";
    private static final String FAMILY_KEY_PREFIX = "auth:refresh:family:";
    private static final String ACCOUNT_KEY_PREFIX = "auth:refresh:account:";

    private static final DefaultRedisScript<Long> ISSUE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1],
                'sessionId', ARGV[1],
                'accountId', ARGV[2],
                'familyId', ARGV[3],
                'role', ARGV[4],
                'status', 'ACTIVE',
                'expiresAt', ARGV[5])
            redis.call('PEXPIREAT', KEYS[1], ARGV[6])
            redis.call('SADD', KEYS[2], KEYS[1])
            redis.call('PEXPIREAT', KEYS[2], ARGV[6])
            redis.call('SADD', KEYS[3], KEYS[2])
            redis.call('PEXPIREAT', KEYS[3], ARGV[6])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            local status = redis.call('HGET', KEYS[1], 'status')
            local familyKey = KEYS[2]
            local accountKey = KEYS[3]
            if status == 'CONSUMED' then
                local tokenKeys = redis.call('SMEMBERS', familyKey)
                for _, tokenKey in ipairs(tokenKeys) do
                    redis.call('DEL', tokenKey)
                end
                redis.call('DEL', familyKey)
                redis.call('SREM', accountKey, familyKey)
                return 2
            end
            if status ~= 'ACTIVE' then
                return 0
            end
            redis.call('HSET', KEYS[1], 'status', 'CONSUMED')
            redis.call('HSET', KEYS[4],
                'sessionId', ARGV[1],
                'accountId', ARGV[2],
                'familyId', ARGV[3],
                'role', ARGV[4],
                'status', 'ACTIVE',
                'expiresAt', ARGV[5])
            redis.call('PEXPIREAT', KEYS[4], ARGV[6])
            redis.call('SADD', familyKey, KEYS[4])
            redis.call('PEXPIREAT', familyKey, ARGV[6])
            redis.call('PEXPIREAT', accountKey, ARGV[6])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> REVOKE_FAMILY_SCRIPT = new DefaultRedisScript<>("""
            local tokenKeys = redis.call('SMEMBERS', KEYS[1])
            for _, tokenKey in ipairs(tokenKeys) do
                redis.call('DEL', tokenKey)
            end
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], KEYS[1])
            return #tokenKeys
            """, Long.class);

    private static final DefaultRedisScript<Long> REVOKE_ACCOUNT_SCRIPT = new DefaultRedisScript<>("""
            local familyKeys = redis.call('SMEMBERS', KEYS[1])
            local removed = 0
            for _, familyKey in ipairs(familyKeys) do
                local tokenKeys = redis.call('SMEMBERS', familyKey)
                for _, tokenKey in ipairs(tokenKeys) do
                    redis.call('DEL', tokenKey)
                    removed = removed + 1
                end
                redis.call('DEL', familyKey)
            end
            redis.call('DEL', KEYS[1])
            return removed
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public RefreshSessionStore(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
        this.clock = clock;
    }

    public IssuedRefreshSession issue(long accountId, String role, Duration ttl) {
        Instant expiresAt = clock.instant().plus(ttl);
        String familyId = UUID.randomUUID().toString();
        return issue(accountId, role, familyId, expiresAt);
    }

    public Optional<RefreshSession> find(String rawToken) {
        String tokenHash = sha256(rawToken);
        String sessionKey = SESSION_KEY_PREFIX + tokenHash;
        Map<Object, Object> values = redisTemplate.opsForHash().entries(sessionKey);
        if (values.isEmpty()) {
            return Optional.empty();
        }

        RefreshSession session = new RefreshSession(
                (String) values.get("sessionId"),
                Long.parseLong((String) values.get("accountId")),
                (String) values.get("familyId"),
                (String) values.get("role"),
                RefreshSession.Status.valueOf((String) values.get("status")),
                Instant.parse((String) values.get("expiresAt")));
        return Optional.of(session);
    }

    public RotatedRefreshSession rotate(String currentRawToken, RefreshSession currentSession) {
        IssuedRefreshSession issued = issueMaterial(currentSession.expiresAt());
        String currentHash = sha256(currentRawToken);
        String newHash = sha256(issued.token());
        String currentKey = SESSION_KEY_PREFIX + currentHash;
        String newKey = SESSION_KEY_PREFIX + newHash;
        String familyKey = FAMILY_KEY_PREFIX + currentSession.familyId();
        String accountKey = ACCOUNT_KEY_PREFIX + currentSession.accountId();
        long expiresAtMillis = currentSession.expiresAt().toEpochMilli();

        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(currentKey, familyKey, accountKey, newKey),
                issued.sessionId(),
                Long.toString(currentSession.accountId()),
                currentSession.familyId(),
                currentSession.role(),
                currentSession.expiresAt().toString(),
                Long.toString(expiresAtMillis));
        if (Long.valueOf(1).equals(result)) {
            return new RotatedRefreshSession(RotationStatus.SUCCESS, issued);
        }
        if (Long.valueOf(2).equals(result)) {
            return new RotatedRefreshSession(RotationStatus.REPLAY, null);
        }
        return new RotatedRefreshSession(RotationStatus.INVALID, null);
    }

    public void revokeFamily(RefreshSession session) {
        String familyKey = FAMILY_KEY_PREFIX + session.familyId();
        String accountKey = ACCOUNT_KEY_PREFIX + session.accountId();
        redisTemplate.execute(REVOKE_FAMILY_SCRIPT, List.of(familyKey, accountKey));
    }

    public void revokeAll(long accountId) {
        String accountKey = ACCOUNT_KEY_PREFIX + accountId;
        redisTemplate.execute(REVOKE_ACCOUNT_SCRIPT, List.of(accountKey));
    }

    private IssuedRefreshSession issue(long accountId, String role, String familyId, Instant expiresAt) {
        IssuedRefreshSession issued = issueMaterial(expiresAt);
        String tokenHash = sha256(issued.token());
        String sessionKey = SESSION_KEY_PREFIX + tokenHash;
        String familyKey = FAMILY_KEY_PREFIX + familyId;
        String accountKey = ACCOUNT_KEY_PREFIX + accountId;
        long expiresAtMillis = expiresAt.toEpochMilli();
        redisTemplate.execute(
                ISSUE_SCRIPT,
                List.of(sessionKey, familyKey, accountKey),
                issued.sessionId(),
                Long.toString(accountId),
                familyId,
                role,
                expiresAt.toString(),
                Long.toString(expiresAtMillis));
        return issued;
    }

    private IssuedRefreshSession issueMaterial(Instant expiresAt) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String sessionId = UUID.randomUUID().toString();
        return new IssuedRefreshSession(sessionId, rawToken, expiresAt);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
            byte[] hash = digest.digest(valueBytes);
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    public enum RotationStatus {
        SUCCESS,
        REPLAY,
        INVALID
    }

    public record RotatedRefreshSession(RotationStatus status, IssuedRefreshSession issued) {}
}
