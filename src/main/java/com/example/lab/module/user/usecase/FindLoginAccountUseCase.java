package com.example.lab.module.user.usecase;

import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindLoginAccountUseCase {

    private final UserAccountMapper userAccountMapper;
    private final Clock clock;

    public FindLoginAccountUseCase(UserAccountMapper userAccountMapper, Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.clock = clock;
    }

    @Transactional
    public Optional<UserAccount> executeByEmail(String email) {
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
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
