package com.example.lab.module.auth.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore.RotatedRefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore.RotationStatus;
import com.example.lab.module.auth.usecase.AuthErrorCode;
import com.example.lab.module.auth.usecase.RefreshAuthenticationUseCase;
import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import com.example.lab.support.IntegrationTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshSessionStoreIntegrationTest extends IntegrationTestSupport {

    @Autowired
    RefreshSessionStore refreshSessionStore;

    @Autowired
    RefreshAuthenticationUseCase refreshAuthenticationUseCase;

    @Autowired
    UserAccountMapper userAccountMapper;

    @Nested
    @DisplayName("refresh token 회전 정책")
    class RefreshTokenRotationPolicy {

        @Test
        @DisplayName("소비한 refresh token이 재사용되면 replay로 판단하고 같은 token family를 폐기한다")
        void invalidatesTokenFamilyWhenConsumedTokenIsReused() {
            // given
            IssuedRefreshSession issued = refreshSessionStore.issue(1L, "USER", Duration.ofDays(14));
            RefreshSession current = refreshSessionStore.find(issued.token()).orElseThrow();
            RotatedRefreshSession firstRotation = refreshSessionStore.rotate(issued.token(), current);

            // when
            RotatedRefreshSession replay = refreshSessionStore.rotate(issued.token(), current);

            // then
            assertThat(firstRotation.status()).isEqualTo(RotationStatus.SUCCESS);
            assertThat(firstRotation.issued().expiresAt()).isEqualTo(issued.expiresAt());
            assertThat(replay.status()).isEqualTo(RotationStatus.REPLAY);
            assertThat(refreshSessionStore.find(firstRotation.issued().token())).isEmpty();
        }

        @Test
        @DisplayName("같은 refresh token의 동시 회전 요청은 하나만 성공할 수 있다")
        void allowsOnlyOneConcurrentRotation() throws Exception {
            // given
            IssuedRefreshSession issued = refreshSessionStore.issue(1L, "USER", Duration.ofDays(14));
            RefreshSession current = refreshSessionStore.find(issued.token()).orElseThrow();
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Callable<RotationStatus> rotation = () -> {
                ready.countDown();
                start.await();
                RotatedRefreshSession result = refreshSessionStore.rotate(issued.token(), current);
                return result.status();
            };
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                Future<RotationStatus> first = executor.submit(rotation);
                Future<RotationStatus> second = executor.submit(rotation);
                ready.await();

                // when
                start.countDown();
                List<RotationStatus> results = List.of(first.get(), second.get());

                // then
                assertThat(results).containsExactlyInAnyOrder(RotationStatus.SUCCESS, RotationStatus.REPLAY);
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    @DisplayName("탈퇴한 계정의 refresh 요청을 거절하고 해당 token family를 폐기한다")
    void rejectsRefreshForWithdrawnAccount() {
        // given
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        long accountId = userAccountMapper.insert("withdrawn@example.com", "hash", now);
        IssuedRefreshSession issued = refreshSessionStore.issue(accountId, "USER", Duration.ofDays(14));
        userAccountMapper.withdraw(accountId, now);

        // when
        Throwable thrown = catchThrowable(() -> refreshAuthenticationUseCase.execute(issued.token()));

        // then
        assertThat(thrown)
                .isInstanceOfSatisfying(ApplicationException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
        assertThat(refreshSessionStore.find(issued.token())).isEmpty();
    }
}
