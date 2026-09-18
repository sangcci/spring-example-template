package com.example.lab.module.auth.usecase;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.auth.infra.persistence.IssuedRefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore.RotatedRefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore.RotationStatus;
import com.example.lab.module.auth.infra.security.AccessTokenIssuer;
import com.example.lab.module.user.usecase.FindLoginAccountUseCase;
import com.example.lab.module.user.usecase.UserAccount;
import com.example.lab.module.user.usecase.UserAccountStatus;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RefreshAuthenticationUseCase {

    private final RefreshSessionStore refreshSessionStore;
    private final FindLoginAccountUseCase findLoginAccountUseCase;
    private final AccessTokenIssuer accessTokenIssuer;

    public RefreshAuthenticationUseCase(
            RefreshSessionStore refreshSessionStore,
            FindLoginAccountUseCase findLoginAccountUseCase,
            AccessTokenIssuer accessTokenIssuer) {
        this.refreshSessionStore = refreshSessionStore;
        this.findLoginAccountUseCase = findLoginAccountUseCase;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public AuthenticationResult execute(String rawRefreshToken) {
        Optional<RefreshSession> sessionResult = refreshSessionStore.find(rawRefreshToken);
        if (sessionResult.isEmpty()) {
            throw new ApplicationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshSession session = sessionResult.get();
        Optional<UserAccount> accountResult = findLoginAccountUseCase.executeById(session.accountId());
        if (accountResult.isEmpty() || accountResult.get().status() != UserAccountStatus.ACTIVE) {
            refreshSessionStore.revokeFamily(session);
            throw new ApplicationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserAccount account = accountResult.get();
        RotatedRefreshSession rotated = refreshSessionStore.rotate(rawRefreshToken, session);
        if (rotated.status() == RotationStatus.REPLAY) {
            throw new ApplicationException(AuthErrorCode.REFRESH_TOKEN_REPLAY);
        }
        if (rotated.status() != RotationStatus.SUCCESS) {
            throw new ApplicationException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        IssuedRefreshSession refreshSession = rotated.issued();
        String role = account.role().name();
        AccessTokenIssuer.IssuedAccessToken accessToken = accessTokenIssuer.issue(account.id(), role);
        return new AuthenticationResult(
                account.id(),
                accessToken.value(),
                accessToken.expiresAt(),
                refreshSession.token(),
                refreshSession.expiresAt());
    }
}
