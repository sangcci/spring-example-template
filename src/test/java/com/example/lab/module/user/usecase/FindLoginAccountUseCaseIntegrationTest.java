package com.example.lab.module.user.usecase;

import static com.example.lab.generated.jooq.tables.UserAccount.USER_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import com.example.lab.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FindLoginAccountUseCaseIntegrationTest extends IntegrationTestSupport {

    @Autowired
    FindLoginAccountUseCase findLoginAccountUseCase;

    @Autowired
    UserAccountMapper userAccountMapper;

    @Test
    @DisplayName("이용 제한 기간이 끝난 계정은 로그인 조회 시 활성화하고 제한 정보를 제거한다")
    void reactivatesAccountAfterRestrictionExpires() {
        // given
        Instant now = Instant.now();
        long accountId = userAccountMapper.insert("restricted@example.com", "hash", now);
        OffsetDateTime restrictionEndsAt = now.minus(1, ChronoUnit.MINUTES).atOffset(ZoneOffset.UTC);
        dsl.update(USER_ACCOUNT)
                .set(USER_ACCOUNT.STATUS, UserAccountStatus.RESTRICTED.name())
                .set(USER_ACCOUNT.RESTRICTION_REASON, "temporary restriction")
                .set(USER_ACCOUNT.RESTRICTION_ENDS_AT, restrictionEndsAt)
                .where(USER_ACCOUNT.ID.eq(accountId))
                .execute();

        // when
        UserAccount account = findLoginAccountUseCase.executeById(accountId).orElseThrow();

        // then
        assertThat(account.status()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(account.restrictionEndsAt()).isNull();
        String restrictionReason = dsl.select(USER_ACCOUNT.RESTRICTION_REASON)
                .from(USER_ACCOUNT)
                .where(USER_ACCOUNT.ID.eq(accountId))
                .fetchOne(USER_ACCOUNT.RESTRICTION_REASON);
        assertThat(restrictionReason).isNull();
    }
}
