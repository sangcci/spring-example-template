package com.example.lab.module.auth.usecase;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.user.usecase.FindLoginAccountUseCase;
import com.example.lab.module.user.usecase.UserAccount;
import com.example.lab.module.user.usecase.UserAccountStatus;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private final FindLoginAccountUseCase findLoginAccountUseCase;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationIssuer authenticationIssuer;

    public LoginUseCase(
            FindLoginAccountUseCase findLoginAccountUseCase,
            PasswordEncoder passwordEncoder,
            AuthenticationIssuer authenticationIssuer) {
        this.findLoginAccountUseCase = findLoginAccountUseCase;
        this.passwordEncoder = passwordEncoder;
        this.authenticationIssuer = authenticationIssuer;
    }

    public AuthenticationResult execute(String email, String password, boolean rememberMe) {
        Optional<UserAccount> accountResult = findLoginAccountUseCase.executeByEmail(email);
        if (accountResult.isEmpty()) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        UserAccount account = accountResult.get();
        boolean passwordMatches = passwordEncoder.matches(password, account.passwordHash());
        if (!passwordMatches) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (account.status() == UserAccountStatus.RESTRICTED) {
            throw new ApplicationException(AuthErrorCode.ACCOUNT_RESTRICTED);
        }
        if (account.status() != UserAccountStatus.ACTIVE) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        return authenticationIssuer.issue(account.id(), account.role(), rememberMe);
    }
}
