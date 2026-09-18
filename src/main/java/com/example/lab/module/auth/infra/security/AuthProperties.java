package com.example.lab.module.auth.infra.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.auth")
public record AuthProperties(
        @NotNull Duration accessTokenTtl,
        @NotNull Duration standardRefreshTtl,
        @NotNull Duration rememberMeRefreshTtl,
        @NotBlank String refreshTokenCookieName,
        boolean secureCookies,
        String csrfCookieDomain,
        @NotEmpty List<@NotBlank String> allowedOrigins) {}
