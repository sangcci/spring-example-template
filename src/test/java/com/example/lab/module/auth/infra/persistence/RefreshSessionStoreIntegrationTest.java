package com.example.lab.module.auth.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lab.module.auth.infra.persistence.RefreshSessionStore.RotatedRefreshSession;
import com.example.lab.module.auth.infra.persistence.RefreshSessionStore.RotationStatus;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class RefreshSessionStoreIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    RefreshSessionStore refreshSessionStore;

    @Test
    void refresh_token은_한_번만_회전하고_재사용하면_family를_폐기한다() {
        IssuedRefreshSession issued = refreshSessionStore.issue(1L, "USER", Duration.ofDays(14));
        RefreshSession current = refreshSessionStore.find(issued.token()).orElseThrow();

        RotatedRefreshSession firstRotation = refreshSessionStore.rotate(issued.token(), current);
        RotatedRefreshSession replay = refreshSessionStore.rotate(issued.token(), current);

        assertThat(firstRotation.status()).isEqualTo(RotationStatus.SUCCESS);
        assertThat(replay.status()).isEqualTo(RotationStatus.REPLAY);
        assertThat(refreshSessionStore.find(firstRotation.issued().token())).isEmpty();
    }
}
