package com.example.lab.module.user.infra.schedule;

import com.example.lab.module.user.usecase.DeleteWithdrawnAccountsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WithdrawnAccountCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(WithdrawnAccountCleanupJob.class);

    private final DeleteWithdrawnAccountsUseCase deleteWithdrawnAccountsUseCase;

    public WithdrawnAccountCleanupJob(DeleteWithdrawnAccountsUseCase deleteWithdrawnAccountsUseCase) {
        this.deleteWithdrawnAccountsUseCase = deleteWithdrawnAccountsUseCase;
    }

    @Scheduled(cron = "${user.withdrawn-account-cleanup-cron}", zone = "Asia/Seoul")
    public void deleteExpiredAccounts() {
        int deletedAccountCount = deleteWithdrawnAccountsUseCase.execute();
        if (deletedAccountCount > 0) {
            log.info("withdrawn_accounts_deleted count={}", deletedAccountCount);
        }
    }
}
