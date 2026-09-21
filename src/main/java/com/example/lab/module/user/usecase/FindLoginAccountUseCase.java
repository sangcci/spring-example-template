package com.example.lab.module.user.usecase;

import com.example.lab.module.user.domain.EmailPolicy;
import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindLoginAccountUseCase {

    private final UserAccountMapper userAccountMapper;
    private final EmailPolicy emailPolicy;
    private final Clock clock;

    public FindLoginAccountUseCase(UserAccountMapper userAccountMapper, EmailPolicy emailPolicy, Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.emailPolicy = emailPolicy;
        this.clock = clock;
    }

    @Transactional
    public Optional<UserAccount> executeByEmail(String email) {
        String normalizedEmail = emailPolicy.normalize(email);
        Optional<UserAccount> accountResult = userAccountMapper.findByEmail(normalizedEmail);
        if (accountResult.isEmpty()) {
            return Optional.empty();
        }

        UserAccount account = accountResult.get();
        Instant now = clock.instant();
        if (account.status() == UserAccountStatus.RESTRICTED
                && account.restrictionEndsAt() != null
                && !account.restrictionEndsAt().isAfter(now)) {
            userAccountMapper.activateExpiredRestriction(account.id(), now);
            return userAccountMapper.findById(account.id());
        }
        return Optional.of(account);
    }

    @Transactional
    public Optional<UserAccount> executeById(long accountId) {
        Optional<UserAccount> accountResult = userAccountMapper.findById(accountId);
        if (accountResult.isEmpty()) {
            return Optional.empty();
        }

        UserAccount account = accountResult.get();
        Instant now = clock.instant();
        if (account.status() == UserAccountStatus.RESTRICTED
                && account.restrictionEndsAt() != null
                && !account.restrictionEndsAt().isAfter(now)) {
            userAccountMapper.activateExpiredRestriction(account.id(), now);
            return userAccountMapper.findById(account.id());
        }
        return Optional.of(account);
    }
}
