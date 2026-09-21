package com.example.lab.module.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Nested
    @DisplayName("비밀번호 길이 정책")
    class PasswordLengthPolicy {

        @Test
        @DisplayName("비밀번호는 최소 길이인 8자부터 허용한다")
        void acceptsMinimumPasswordLength() {
            // given
            String password = "Aa1!aaaa";

            // when
            boolean valid = passwordPolicy.isValid(password);

            // then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("비밀번호는 최대 길이인 16자까지 허용한다")
        void acceptsMaximumPasswordLength() {
            // given
            String password = "Aa1!aaaaaaaaaaaa";

            // when
            boolean valid = passwordPolicy.isValid(password);

            // then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("비밀번호가 8자보다 짧으면 거절한다")
        void rejectsPasswordShorterThanMinimum() {
            // given
            String password = "Aa1!aaa";

            // when
            boolean valid = passwordPolicy.isValid(password);

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("비밀번호가 16자를 넘으면 거절한다")
        void rejectsPasswordLongerThanMaximum() {
            // given
            String password = "Aa1!aaaaaaaaaaaaa";

            // when
            boolean valid = passwordPolicy.isValid(password);

            // then
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("비밀번호 구성 정책")
    class PasswordCompositionPolicy {

        @ParameterizedTest
        @ValueSource(strings = {"password1!", "PASSWORD1!", "Password!", "Password1"})
        @DisplayName("대문자, 소문자, 숫자 또는 특수문자가 빠진 비밀번호는 거절한다")
        void rejectsPasswordMissingRequiredCharacterType(String password) {
            // given

            // when
            boolean valid = passwordPolicy.isValid(password);

            // then
            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("공백이 포함된 비밀번호는 거절한다")
        void rejectsPasswordContainingWhitespace() {
            // given
            String password = "Password 1!";

            // when
            boolean valid = passwordPolicy.isValid(password);

            // then
            assertThat(valid).isFalse();
        }
    }
}
