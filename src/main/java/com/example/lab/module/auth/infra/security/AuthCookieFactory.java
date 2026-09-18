package com.example.lab.module.auth.infra.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieFactory {

    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final Clock clock;

    public AuthCookieFactory(JwtProperties jwtProperties, AuthProperties authProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    public ResponseCookie accessToken(String value, Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);
        return ResponseCookie.from(jwtProperties.accessTokenCookieName(), value)
                .httpOnly(true)
                .secure(authProperties.secureCookies())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie refreshToken(String value, Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);
        return ResponseCookie.from(authProperties.refreshTokenCookieName(), value)
                .httpOnly(true)
                .secure(authProperties.secureCookies())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie expireAccessToken() {
        return ResponseCookie.from(jwtProperties.accessTokenCookieName(), "")
                .httpOnly(true)
                .secure(authProperties.secureCookies())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie expireRefreshToken() {
        return ResponseCookie.from(authProperties.refreshTokenCookieName(), "")
                .httpOnly(true)
                .secure(authProperties.secureCookies())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
