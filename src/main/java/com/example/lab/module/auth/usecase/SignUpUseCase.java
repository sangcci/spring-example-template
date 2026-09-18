package com.example.lab.module.auth.usecase;

import com.example.lab.module.auth.infra.persistence.IssuedRefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import com.example.lab.module.auth.infra.security.AccessTokenIssuer;
import com.example.lab.module.auth.infra.security.AuthProperties;
import com.example.lab.module.user.usecase.CreateUserAccountUseCase;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignUpUseCase {

    private final CreateUserAccountUseCase createUserAccountUseCase;
    private final RefreshSessionStore refreshSessionStore;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AuthProperties authProperties;
    private final RegistrationRollbackLogger rollbackLogger;

    public SignUpUseCase(
            CreateUserAccountUseCase createUserAccountUseCase,
            RefreshSessionStore refreshSessionStore,
            AccessTokenIssuer accessTokenIssuer,
            AuthProperties authProperties,
            RegistrationRollbackLogger rollbackLogger) {
        this.createUserAccountUseCase = createUserAccountUseCase;
        this.refreshSessionStore = refreshSessionStore;
        this.accessTokenIssuer = accessTokenIssuer;
        this.authProperties = authProperties;
        this.rollbackLogger = rollbackLogger;
    }

    @Transactional
    public AuthenticationResult execute(String email, String password, boolean rememberMe) {
        CreateUserAccountUseCase.CreatedUserAccount account = createUserAccountUseCase.execute(email, password);
        Duration refreshTtl = authProperties.standardRefreshTtl();
        if (rememberMe) {
            refreshTtl = authProperties.rememberMeRefreshTtl();
        }

        String role = account.role().name();
        IssuedRefreshSession refreshSession = refreshSessionStore.issue(account.accountId(), role, refreshTtl);
        rollbackLogger.register(account.accountId(), refreshSession.sessionId());
        AccessTokenIssuer.IssuedAccessToken accessToken = accessTokenIssuer.issue(account.accountId(), role);
        return new AuthenticationResult(
                account.accountId(),
                accessToken.value(),
                accessToken.expiresAt(),
                refreshSession.token(),
                refreshSession.expiresAt());
    }
}
