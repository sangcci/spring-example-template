package com.example.lab.module.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import com.example.lab.support.IntegrationTestSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeleteWithdrawnAccountsUseCaseIntegrationTest extends IntegrationTestSupport {

    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");

    @Autowired
    UserAccountMapper userAccountMapper;

    @Test
    @DisplayName("탈퇴 후 30일이 지난 계정만 삭제하고 보관 기간이 남은 계정은 유지한다")
    void deletesAccountsAtRetentionBoundary() {
        // given
        Instant createdAt = NOW.minusSeconds(100 * 24 * 60 * 60L);
        long expiredAccountId = userAccountMapper.insert("expired@example.com", "hash", createdAt);
        long retainedAccountId = userAccountMapper.insert("retained@example.com", "hash", createdAt);
        Instant retentionBoundary = NOW.minusSeconds(30 * 24 * 60 * 60L);
        userAccountMapper.withdraw(expiredAccountId, retentionBoundary);
        userAccountMapper.withdraw(retainedAccountId, retentionBoundary.plusSeconds(1));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var useCase = new DeleteWithdrawnAccountsUseCase(userAccountMapper, clock);

        // when
        int deletedCount = useCase.execute();

        // then
        assertThat(deletedCount).isOne();
        assertThat(userAccountMapper.findById(expiredAccountId)).isEmpty();
        assertThat(userAccountMapper.findById(retainedAccountId)).isPresent();
    }
}
