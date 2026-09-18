package com.example.lab.module.user.usecase;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserAccountUseCase {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public CreateUserAccountUseCase(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public CreatedUserAccount execute(String email, String password) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        boolean validEmail = normalizedEmail.length() <= 320
                && normalizedEmail.indexOf('@') > 0
                && normalizedEmail.indexOf('@') == normalizedEmail.lastIndexOf('@')
                && normalizedEmail.indexOf('@') < normalizedEmail.length() - 1;
        if (!validEmail) {
            throw new ApplicationException(UserErrorCode.INVALID_EMAIL);
        }

        boolean validPassword = password.length() >= 8
                && password.length() <= 16
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*[0-9].*")
                && password.matches(".*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?].*")
                && !password.matches(".*\\s.*");
        if (!validPassword) {
            throw new ApplicationException(UserErrorCode.INVALID_PASSWORD);
        }

        if (userAccountMapper.findByEmail(normalizedEmail).isPresent()) {
            throw new ApplicationException(UserErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        Instant now = clock.instant();
        Instant rejoinBoundary = now.minus(7, ChronoUnit.DAYS);
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
