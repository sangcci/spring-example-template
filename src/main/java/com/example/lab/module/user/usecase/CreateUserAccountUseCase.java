package com.example.lab.module.user.usecase;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.user.domain.EmailPolicy;
import com.example.lab.module.user.domain.PasswordPolicy;
import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserAccountUseCase {

    private static final int EMAIL_REJOIN_RESTRICTION_DAYS = 7;

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailPolicy emailPolicy;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public CreateUserAccountUseCase(
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            EmailPolicy emailPolicy,
            PasswordPolicy passwordPolicy,
            Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailPolicy = emailPolicy;
        this.passwordPolicy = passwordPolicy;
        this.clock = clock;
    }

    @Transactional
    public CreatedUserAccount execute(String email, String password) {
        String normalizedEmail = emailPolicy.normalize(email);
        boolean validEmail = emailPolicy.isValid(normalizedEmail);
        if (!validEmail) {
            throw new ApplicationException(UserErrorCode.INVALID_EMAIL);
        }

        boolean validPassword = passwordPolicy.isValid(password);
        if (!validPassword) {
            throw new ApplicationException(UserErrorCode.INVALID_PASSWORD);
        }

        if (userAccountMapper.findByEmail(normalizedEmail).isPresent()) {
            throw new ApplicationException(UserErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        Instant now = clock.instant();
        Instant rejoinBoundary = now.minus(EMAIL_REJOIN_RESTRICTION_DAYS, ChronoUnit.DAYS);
        if (userAccountMapper.existsRecentlyWithdrawn(normalizedEmail, rejoinBoundary)) {
            throw new ApplicationException(UserErrorCode.EMAIL_REJOIN_RESTRICTED);
        }

        String passwordHash = passwordEncoder.encode(password);
        try {
            long accountId = userAccountMapper.insert(normalizedEmail, passwordHash, now);
            return new CreatedUserAccount(accountId, UserRole.USER);
        } catch (DataIntegrityViolationException exception) {
            throw new ApplicationException(UserErrorCode.EMAIL_ALREADY_REGISTERED, exception);
        }
    }

    public record CreatedUserAccount(long accountId, UserRole role) {}
}
