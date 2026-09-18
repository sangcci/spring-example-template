package com.example.lab.module.user.usecase;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawUserAccountUseCase {

    private final UserAccountMapper userAccountMapper;
    private final Clock clock;

    public WithdrawUserAccountUseCase(UserAccountMapper userAccountMapper, Clock clock) {
        this.userAccountMapper = userAccountMapper;
        this.clock = clock;
    }

    @Transactional
    public void execute(long accountId) {
        Instant now = clock.instant();
        boolean withdrawn = userAccountMapper.withdraw(accountId, now);
        if (!withdrawn) {
            throw new ApplicationException(UserErrorCode.ACCOUNT_NOT_FOUND);
        }
    }
}
