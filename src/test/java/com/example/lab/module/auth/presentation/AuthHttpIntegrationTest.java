package com.example.lab.module.auth.presentation;

import static com.example.lab.generated.jooq.tables.UserAccount.USER_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.lab.module.auth.infra.persistence.RefreshSessionStore;
import com.example.lab.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class AuthHttpIntegrationTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RefreshSessionStore refreshSessionStore;

    @Test
    @DisplayName("CSRF endpoint는 browser가 요청 header에 사용할 token cookie를 발급한다")
    void issuesCsrfTokenCookie() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(get("/api/auth/csrf"));

        // then
        result.andExpect(status().isOk()).andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("CSRF token이 없는 상태 변경 요청은 보안 오류로 거절한다")
    void rejectsStateChangingRequestWithoutCsrfToken() throws Exception {
        // given
        String body = """
                {"email":"user@example.com","password":"Password1!","rememberMe":false}
                """;

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("SECURITY_INVALID_CSRF_TOKEN"));
    }

    @Test
    @DisplayName("일반 회원가입은 이메일을 정규화하고 14일 refresh session으로 로그인을 시작한다")
    void signsUpAndStartsStandardAuthenticationSession() throws Exception {
        // given
        String body = """
                {"email":" USER@EXAMPLE.COM ","password":"Password1!","rememberMe":false}
                """;

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/sign-up")
                .with(csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        MvcResult mvcResult = result.andExpect(status().isOk())
                .andExpect(cookie().httpOnly("access-token", true))
                .andExpect(cookie().httpOnly("refresh-token", true))
                .andExpect(jsonPath("$.result.accountId").isNumber())
                .andReturn();
        String storedEmail = dsl.select(USER_ACCOUNT.EMAIL).from(USER_ACCOUNT).fetchOne(USER_ACCOUNT.EMAIL);
        Cookie accessCookie = mvcResult.getResponse().getCookie("access-token");
        Cookie refreshCookie = mvcResult.getResponse().getCookie("refresh-token");
        assertThat(storedEmail).isEqualTo("user@example.com");
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.getMaxAge()).isBetween(899, 900);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getMaxAge()).isBetween(14 * 24 * 60 * 60 - 1, 14 * 24 * 60 * 60);
        assertThat(refreshSessionStore.find(refreshCookie.getValue())).isPresent();
    }

    @Test
    @DisplayName("자동 로그인을 선택하면 refresh session을 30일 동안 유지한다")
    void startsExtendedSessionWhenRememberMeIsEnabled() throws Exception {
        // given
        String body = """
                {"email":"user@example.com","password":"Password1!","rememberMe":true}
                """;

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/sign-up")
                .with(csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        MvcResult mvcResult = result.andExpect(status().isOk()).andReturn();
        Cookie refreshCookie = mvcResult.getResponse().getCookie("refresh-token");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getMaxAge()).isBetween(30 * 24 * 60 * 60 - 1, 30 * 24 * 60 * 60);
    }

    @Test
    @DisplayName("비밀번호를 변경하면 모든 refresh session과 browser 인증 cookie를 폐기한다")
    void revokesAuthenticationAfterPasswordChange() throws Exception {
        // given
        MvcResult signUpResult = signUp("password@example.com");
        MvcResult loginResult = login("password@example.com");
        Cookie accessCookie = loginResult.getResponse().getCookie("access-token");
        Cookie signUpRefreshCookie = signUpResult.getResponse().getCookie("refresh-token");
        Cookie loginRefreshCookie = loginResult.getResponse().getCookie("refresh-token");
        String body = """
                {"currentPassword":"Password1!","newPassword":"NewPassword2@"}
                """;

        // when
        ResultActions result = mockMvc.perform(patch("/api/users/me/password")
                .with(csrfToken())
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isOk())
                .andExpect(cookie().maxAge("access-token", 0))
                .andExpect(cookie().maxAge("refresh-token", 0));
        assertThat(refreshSessionStore.find(signUpRefreshCookie.getValue())).isEmpty();
        assertThat(refreshSessionStore.find(loginRefreshCookie.getValue())).isEmpty();
    }

    @Test
    @DisplayName("회원 탈퇴가 완료되면 계정을 탈퇴 상태로 바꾸고 모든 인증 수단을 폐기한다")
    void withdrawsAccountAndRevokesAuthentication() throws Exception {
        // given
        MvcResult signUpResult = signUp("withdraw@example.com");
        MvcResult loginResult = login("withdraw@example.com");
        Cookie accessCookie = loginResult.getResponse().getCookie("access-token");
        Cookie signUpRefreshCookie = signUpResult.getResponse().getCookie("refresh-token");
        Cookie loginRefreshCookie = loginResult.getResponse().getCookie("refresh-token");
        String body = """
                {"currentPassword":"Password1!"}
                """;

        // when
        ResultActions result = mockMvc.perform(delete("/api/users/me")
                .with(csrfToken())
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isOk())
                .andExpect(cookie().maxAge("access-token", 0))
                .andExpect(cookie().maxAge("refresh-token", 0));
        String accountStatus =
                dsl.select(USER_ACCOUNT.STATUS).from(USER_ACCOUNT).fetchOne(USER_ACCOUNT.STATUS);
        assertThat(accountStatus).isEqualTo("WITHDRAWN");
        assertThat(refreshSessionStore.find(signUpRefreshCookie.getValue())).isEmpty();
        assertThat(refreshSessionStore.find(loginRefreshCookie.getValue())).isEmpty();
    }

    private MvcResult signUp(String email) throws Exception {
        String body = """
                {"email":"%s","password":"Password1!","rememberMe":false}
                """.formatted(email);
        return mockMvc.perform(post("/api/auth/sign-up")
                        .with(csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult login(String email) throws Exception {
        String body = """
                {"email":"%s","password":"Password1!","rememberMe":false}
                """.formatted(email);
        return mockMvc.perform(post("/api/auth/login")
                        .with(csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    private RequestPostProcessor csrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        return request -> {
            Cookie[] requestCookies = request.getCookies();
            if (requestCookies == null) {
                request.setCookies(csrfCookie);
            } else {
                Cookie[] cookiesWithCsrfToken = Arrays.copyOf(requestCookies, requestCookies.length + 1);
                cookiesWithCsrfToken[requestCookies.length] = csrfCookie;
                request.setCookies(cookiesWithCsrfToken);
            }
            request.addHeader("X-XSRF-TOKEN", csrfCookie.getValue());
            return request;
        };
    }
}
