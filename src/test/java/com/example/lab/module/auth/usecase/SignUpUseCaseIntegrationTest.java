package com.example.lab.module.auth.usecase;

import static com.example.lab.generated.jooq.tables.UserAccount.USER_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.example.lab.module.auth.infra.persistence.IssuedRefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import com.example.lab.support.IntegrationTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SignUpUseCaseIntegrationTest extends IntegrationTestSupport {

    @Autowired
    SignUpUseCase signUpUseCase;

    @MockitoBean
    RefreshSessionStore refreshSessionStore;

    @Test
    @DisplayName("refresh session을 저장하지 못하면 생성한 계정도 rollback한다")
    void rollsBackAccountWhenRefreshSessionIssueFails() {
        // given
        when(refreshSessionStore.issue(anyLong(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("redis unavailable"));

        // when
        Throwable thrown = catchThrowable(() -> signUpUseCase.execute("user@example.com", "Password1!", false));

        // then
        assertThat(thrown).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(dsl.fetchCount(USER_ACCOUNT)).isZero();
    }

    @Test
    @DisplayName("refresh session 저장 후 database commit에 실패하면 계정 생성을 rollback한다")
    void rollsBackAccountWhenDatabaseCommitFails() {
        // given
        Instant expiresAt = Instant.now().plusSeconds(14 * 24 * 60 * 60L);
        IssuedRefreshSession issuedSession = new IssuedRefreshSession("session-id", "raw-token", expiresAt);
        when(refreshSessionStore.issue(anyLong(), any(), any())).thenAnswer(invocation -> {
            TransactionSynchronization synchronization = new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("database commit failed");
                }
            };
            TransactionSynchronizationManager.registerSynchronization(synchronization);
            return issuedSession;
        });

        // when
        Throwable thrown = catchThrowable(() -> signUpUseCase.execute("user@example.com", "Password1!", false));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("database commit failed");
        assertThat(dsl.fetchCount(USER_ACCOUNT)).isZero();
    }
}
