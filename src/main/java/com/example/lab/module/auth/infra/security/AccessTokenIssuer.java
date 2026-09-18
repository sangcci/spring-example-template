package com.example.lab.module.auth.infra.security;

import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenIssuer {

    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final SecretKey secretKey;
    private final Clock clock;

    public AccessTokenIssuer(
            JwtProperties jwtProperties, AuthProperties authProperties, SecretKey secretKey, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.secretKey = secretKey;
        this.clock = clock;
    }

    public IssuedAccessToken issue(long accountId, String role) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(authProperties.accessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(Long.toString(accountId))
                .issuer(jwtProperties.issuer())
                .audience()
                .add(jwtProperties.audience())
                .and()
                .issuedAt(Date.from(issuedAt))
                .notBefore(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(tokenId)
                .claim("role", role)
                .claim("token_type", "access")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedAccessToken(token, expiresAt);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {}
}
