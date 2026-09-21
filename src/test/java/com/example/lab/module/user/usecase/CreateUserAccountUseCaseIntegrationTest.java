package com.example.lab.module.user.usecase;

import static com.example.lab.generated.jooq.tables.UserAccount.USER_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.lab.global.error.ApplicationException;
import com.example.lab.module.user.infra.persistence.UserAccountMapper;
import com.example.lab.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreateUserAccountUseCaseIntegrationTest extends IntegrationTestSupport {

    @Autowired
    CreateUserAccountUseCase createUserAccountUseCase;

    @Autowired
    UserAccountMapper userAccountMapper;

    @Test
    @DisplayName("계정을 생성할 때 정규화한 이메일을 저장한다")
    void normalizesEmailBeforeSaving() {
        // given
        String email = " USER@EXAMPLE.COM ";

        // when
        createUserAccountUseCase.execute(email, "Password1!");

        // then
        String storedEmail = dsl.select(USER_ACCOUNT.EMAIL).from(USER_ACCOUNT).fetchOne(USER_ACCOUNT.EMAIL);
        assertThat(storedEmail).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("이미 활성 계정이 사용하는 이메일로는 가입할 수 없다")
    void rejectsAlreadyRegisteredEmail() {
        // given
        createUserAccountUseCase.execute("user@example.com", "Password1!");

        // when
        Throwable thrown = catchThrowable(() -> createUserAccountUseCase.execute("user@example.com", "Password1!"));

        // then
        assertThat(thrown)
                .isInstanceOfSatisfying(ApplicationException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(UserErrorCode.EMAIL_ALREADY_REGISTERED));
    }

    @Test
    @DisplayName("탈퇴 후 7일이 지나기 전에는 같은 이메일로 재가입할 수 없다")
    void rejectsRejoinDuringRestrictionPeriod() {
        // given
        Instant withdrawnAt = Instant.now().minus(7, ChronoUnit.DAYS).plusSeconds(1);
        long accountId = userAccountMapper.insert("user@example.com", "hash", withdrawnAt);
        userAccountMapper.withdraw(accountId, withdrawnAt);

        // when
        Throwable thrown = catchThrowable(() -> createUserAccountUseCase.execute("user@example.com", "Password1!"));

        // then
        assertThat(thrown)
                .isInstanceOfSatisfying(ApplicationException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(UserErrorCode.EMAIL_REJOIN_RESTRICTED));
    }

    @Test
    @DisplayName("탈퇴 후 7일이 지나면 같은 이메일로 새 계정을 만들 수 있다")
    void allowsRejoinAtRestrictionBoundary() {
        // given
        Instant withdrawnAt = Instant.now().minus(7, ChronoUnit.DAYS).minusSeconds(1);
        long withdrawnAccountId = userAccountMapper.insert("user@example.com", "hash", withdrawnAt);
        userAccountMapper.withdraw(withdrawnAccountId, withdrawnAt);

        // when
        CreateUserAccountUseCase.CreatedUserAccount createdAccount =
                createUserAccountUseCase.execute("user@example.com", "Password1!");

        // then
        assertThat(createdAccount.accountId()).isNotEqualTo(withdrawnAccountId);
        assertThat(dsl.fetchCount(USER_ACCOUNT)).isEqualTo(2);
    }
}
