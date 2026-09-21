package com.example.lab.module.user.usecase;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.user.domain.PasswordPolicy;
import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserPasswordUseCase {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public UpdateUserPasswordUseCase(
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    @Transactional
    public void execute(long accountId, String currentPasswordHash, String newPassword) {
        boolean validPassword = passwordPolicy.isValid(newPassword);
        if (!validPassword) {
            throw new ApplicationException(UserErrorCode.INVALID_PASSWORD);
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        Instant now = clock.instant();
        boolean updated = userAccountMapper.updatePassword(accountId, currentPasswordHash, newPasswordHash, now);
        if (!updated) {
            throw new ApplicationException(UserErrorCode.ACCOUNT_NOT_FOUND);
        }
    }
}
