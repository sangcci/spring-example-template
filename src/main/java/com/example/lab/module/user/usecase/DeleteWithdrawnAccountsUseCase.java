package com.example.lab.module.user.usecase;

import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteWithdrawnAccountsUseCase {

    private static final int WITHDRAWN_ACCOUNT_RETENTION_DAYS = 30;

    private final UserAccountMapper userAccountMapper;
    private final Clock clock;

    public DeleteWithdrawnAccountsUseCase(UserAccountMapper userAccountMapper, Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.clock = clock;
    }

    @Transactional
    public int execute() {
        Instant now = clock.instant();
        Instant retentionBoundary = now.minus(WITHDRAWN_ACCOUNT_RETENTION_DAYS, ChronoUnit.DAYS);
        return userAccountMapper.deleteWithdrawnAtOrBefore(retentionBoundary);
    }
}
