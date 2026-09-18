package com.example.lab.module.auth.usecase;

import static com.example.lab.generated.jooq.tables.UserAccount.USER_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class SignUpUseCaseIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    SignUpUseCase signUpUseCase;

    @Autowired
    DSLContext dsl;

    @MockitoBean
    RefreshSessionStore refreshSessionStore;

    @Test
    void redis에_refresh_session을_저장하지_못하면_account_insert를_rollback한다() {
        when(refreshSessionStore.issue(anyLong(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("redis unavailable"));

        assertThatThrownBy(() -> signUpUseCase.execute("user@example.com", "Password1!", false))
                .isInstanceOf(DataAccessResourceFailureException.class);

        int accountCount = dsl.fetchCount(USER_ACCOUNT);
        assertThat(accountCount).isZero();
    }
}
