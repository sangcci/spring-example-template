package com.example.lab.module.auth.usecase;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import com.example.lab.module.user.usecase.FindLoginAccountUseCase;
import com.example.lab.module.user.usecase.UserAccount;
import com.example.lab.module.user.usecase.UserErrorCode;
import com.example.lab.module.user.usecase.WithdrawUserAccountUseCase;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawAccountUseCase {

    private final FindLoginAccountUseCase findLoginAccountUseCase;
    private final WithdrawUserAccountUseCase withdrawUserAccountUseCase;
    private final RefreshSessionStore refreshSessionStore;
    private final PasswordEncoder passwordEncoder;

    public WithdrawAccountUseCase(
            FindLoginAccountUseCase findLoginAccountUseCase,
            WithdrawUserAccountUseCase withdrawUserAccountUseCase,
            RefreshSessionStore refreshSessionStore,
            PasswordEncoder passwordEncoder) {
        this.findLoginAccountUseCase = findLoginAccountUseCase;
        this.withdrawUserAccountUseCase = withdrawUserAccountUseCase;
        this.refreshSessionStore = refreshSessionStore;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void execute(long accountId, String currentPassword) {
        Optional<UserAccount> accountResult = findLoginAccountUseCase.executeById(accountId);
        if (accountResult.isEmpty()) {
            throw new ApplicationException(UserErrorCode.ACCOUNT_NOT_FOUND);
        }
        UserAccount account = accountResult.get();
        if (!passwordEncoder.matches(currentPassword, account.passwordHash())) {
            throw new ApplicationException(UserErrorCode.PASSWORD_MISMATCH);
        }

        withdrawUserAccountUseCase.execute(accountId);
        refreshSessionStore.revokeAll(accountId);
    }
}
