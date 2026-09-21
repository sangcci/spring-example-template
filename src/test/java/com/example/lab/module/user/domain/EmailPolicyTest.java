package com.example.lab.module.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailPolicyTest {

    private final EmailPolicy emailPolicy = new EmailPolicy();

    @Nested
    @DisplayName("이메일 형식 정책")
    class EmailFormatPolicy {

        @ParameterizedTest
        @ValueSource(strings = {"user@example.com", "first.last+tag@sub.example.co.kr"})
        @DisplayName("서비스가 허용하는 이메일 형식을 유효하다고 판단한다")
        void acceptsValidEmailFormats(String email) {
            // given

            // when
            boolean valid = emailPolicy.isValid(email);

            // then
            assertThat(valid).isTrue();
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "user",
                    "user@localhost",
                    ".user@example.com",
                    "user.@example.com",
                    "user..name@example.com",
                    "user@-example.com",
                    "user@example-.com",
                    "user@example..com"
                })
        @DisplayName("사용자 식별에 사용할 수 없는 이메일 형식은 거절한다")
        void rejectsInvalidEmailFormats(String email) {
            // given

            // when
            boolean valid = emailPolicy.isValid(email);

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("이메일 local part는 64자까지 허용한다")
        void acceptsMaximumLocalPartLength() {
            // given
            String email = "a".repeat(64) + "@example.com";

            // when
            boolean valid = emailPolicy.isValid(email);

            // then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("이메일 local part가 64자를 넘으면 거절한다")
        void rejectsLocalPartLongerThanMaximum() {
            // given
            String email = "a".repeat(65) + "@example.com";

            // when
            boolean valid = emailPolicy.isValid(email);

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("전체 이메일이 320자를 넘으면 거절한다")
        void rejectsEmailLongerThanMaximum() {
            // given
            String domain = String.join(".", "a".repeat(63), "b".repeat(63), "c".repeat(63), "d".repeat(64));
            String email = "e".repeat(64) + "@" + domain;

            // when
            boolean valid = emailPolicy.isValid(email);

            // then
            assertThat(email).hasSize(321);
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("이메일 정규화 정책")
    class EmailNormalizationPolicy {

        @ParameterizedTest
        @ValueSource(strings = {" USER@EXAMPLE.COM ", "\tUser@Example.Com\n"})
        @DisplayName("같은 사용자를 식별할 수 있도록 앞뒤 공백을 제거하고 소문자로 변환한다")
        void normalizesEmailForIdentityComparison(String email) {
            // given

            // when
            String normalizedEmail = emailPolicy.normalize(email);

            // then
            assertThat(normalizedEmail).isEqualTo("user@example.com");
        }
    }
}
