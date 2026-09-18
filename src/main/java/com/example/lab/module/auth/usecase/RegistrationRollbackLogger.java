package com.example.lab.module.auth.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RegistrationRollbackLogger {

    private static final Logger log = LoggerFactory.getLogger(RegistrationRollbackLogger.class);

    public void register(long accountId, String refreshSessionId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.warn(
                            "registration_rolled_back_with_refresh_session accountId={} refreshSessionId={}",
                            accountId,
                            refreshSessionId);
                }
                if (status == STATUS_UNKNOWN) {
                    log.error(
                            "registration_completion_unknown_with_refresh_session accountId={} refreshSessionId={}",
                            accountId,
                            refreshSessionId);
                }
            }
        });
    }
}
