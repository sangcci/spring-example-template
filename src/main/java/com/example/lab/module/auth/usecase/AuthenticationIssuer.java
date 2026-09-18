package com.example.lab.module.auth.usecase;

import com.example.lab.module.auth.infra.persistence.IssuedRefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import com.example.lab.module.auth.infra.security.AccessTokenIssuer;
import com.example.lab.module.auth.infra.security.AuthProperties;
import com.example.lab.module.user.usecase.UserRole;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationIssuer {

    private final RefreshSessionStore refreshSessionStore;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AuthProperties authProperties;

    public AuthenticationIssuer(
            RefreshSessionStore refreshSessionStore,
            AccessTokenIssuer accessTokenIssuer,
            AuthProperties authProperties) {
        this.refreshSessionStore = refreshSessionStore;
        this.accessTokenIssuer = accessTokenIssuer;
        this.authProperties = authProperties;
    }

    public AuthenticationResult issue(long accountId, UserRole role, boolean rememberMe) {
        Duration refreshTtl = authProperties.standardRefreshTtl();
        if (rememberMe) {
            refreshTtl = authProperties.rememberMeRefreshTtl();
        }
        String roleName = role.name();
        IssuedRefreshSession refreshSession = refreshSessionStore.issue(accountId, roleName, refreshTtl);
        AccessTokenIssuer.IssuedAccessToken accessToken = accessTokenIssuer.issue(accountId, roleName);
        return new AuthenticationResult(
                accountId,
                accessToken.value(),
                accessToken.expiresAt(),
                refreshSession.token(),
                refreshSession.expiresAt());
    }
}
