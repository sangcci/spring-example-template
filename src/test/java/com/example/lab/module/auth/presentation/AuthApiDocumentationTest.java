package com.example.lab.module.auth.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.cookies.CookieDocumentation.responseCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.example.lab.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class AuthApiDocumentationTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("CSRF token 발급 API의 응답과 cookie 계약을 문서화한다")
    void documentsCsrfTokenContract() throws Exception {
        // given
        // when
        ResultActions result = mockMvc.perform(get("/api/auth/csrf"));

        // then
        result.andExpect(status().isOk())
                .andDo(document(
                        "auth-csrf",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Authentication")
                                .summary("CSRF token 발급")
                                .description("상태 변경 요청의 header에 사용할 CSRF token을 cookie로 발급한다.")
                                .responseFields(
                                        fieldWithPath("timestamp").type(STRING).description("응답 생성 시각"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"))
                                .build()),
                        responseCookies(
                                cookieWithName("XSRF-TOKEN").description("요청 header의 X-XSRF-TOKEN에 전달할 CSRF token"))));
    }

    @Test
    @DisplayName("회원가입 API의 요청, 응답과 인증 cookie 계약을 문서화한다")
    void documentsSignUpContract() throws Exception {
        // given
        String body = """
                {"email":"user@example.com","password":"Password1!","rememberMe":false}
                """;

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/sign-up")
                .with(csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isOk())
                .andDo(document(
                        "auth-sign-up",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Authentication")
                                .summary("회원가입")
                                .description("이메일과 비밀번호로 계정을 생성하고 인증 상태를 시작한다.")
                                .requestHeaders(
                                        com.epages.restdocs.apispec.ResourceDocumentation.headerWithName("X-XSRF-TOKEN")
                                                .description("CSRF token request header"))
                                .requestFields(
                                        fieldWithPath("email").type(STRING).description("로그인 이메일"),
                                        fieldWithPath("password").type(STRING).description("비밀번호"),
                                        fieldWithPath("rememberMe")
                                                .type(BOOLEAN)
                                                .description("자동 로그인 여부"))
                                .responseFields(
                                        fieldWithPath("timestamp").type(STRING).description("응답 생성 시각"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"),
                                        fieldWithPath("result.accountId")
                                                .type(NUMBER)
                                                .description("생성한 계정 ID"))
                                .build()),
                        requestHeaders(headerWithName("X-XSRF-TOKEN").description("CSRF token request header")),
                        requestCookies(cookieWithName("XSRF-TOKEN").description("CSRF token cookie")),
                        responseCookies(
                                cookieWithName("access-token").description("API 인증에 사용하는 access token"),
                                cookieWithName("refresh-token").description("인증 갱신에 사용하는 refresh token"))));
    }

    @Test
    @DisplayName("로그인 API의 요청, 응답과 인증 cookie 계약을 문서화한다")
    void documentsLoginContract() throws Exception {
        // given
        signUp("login@example.com");
        String body = """
                {"email":"login@example.com","password":"Password1!","rememberMe":false}
                """;

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .with(csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isOk())
                .andDo(document(
                        "auth-login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Authentication")
                                .summary("로그인")
                                .description("등록한 이메일과 비밀번호를 확인하고 인증 token을 발급한다.")
                                .requestHeaders(
                                        com.epages.restdocs.apispec.ResourceDocumentation.headerWithName("X-XSRF-TOKEN")
                                                .description("CSRF token request header"))
                                .requestFields(
                                        fieldWithPath("email").type(STRING).description("로그인 이메일"),
                                        fieldWithPath("password").type(STRING).description("비밀번호"),
                                        fieldWithPath("rememberMe")
                                                .type(BOOLEAN)
                                                .description("자동 로그인 여부"))
                                .responseFields(
                                        fieldWithPath("timestamp").type(STRING).description("응답 생성 시각"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"),
                                        fieldWithPath("result.accountId")
                                                .type(NUMBER)
                                                .description("인증한 계정 ID"))
                                .build()),
                        requestHeaders(headerWithName("X-XSRF-TOKEN").description("CSRF token request header")),
                        requestCookies(cookieWithName("XSRF-TOKEN").description("CSRF token cookie")),
                        responseCookies(
                                cookieWithName("access-token").description("API 인증에 사용하는 access token"),
                                cookieWithName("refresh-token").description("인증 갱신에 사용하는 refresh token"))));
    }

    @Test
    @DisplayName("인증 갱신 API의 cookie 요청과 새로운 인증 cookie 계약을 문서화한다")
    void documentsRefreshContract() throws Exception {
        // given
        MvcResult signUpResult = signUp("refresh@example.com");
        Cookie refreshCookie = signUpResult.getResponse().getCookie("refresh-token");

        // when
        ResultActions result =
                mockMvc.perform(post("/api/auth/refresh").with(csrfToken()).cookie(refreshCookie));

        // then
        result.andExpect(status().isOk())
                .andDo(document(
                        "auth-refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Authentication")
                                .summary("인증 갱신")
                                .description("refresh token을 회전하고 새로운 access token과 refresh token을 발급한다.")
                                .requestHeaders(
                                        com.epages.restdocs.apispec.ResourceDocumentation.headerWithName("X-XSRF-TOKEN")
                                                .description("CSRF token request header"))
                                .responseFields(
                                        fieldWithPath("timestamp").type(STRING).description("응답 생성 시각"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"),
                                        fieldWithPath("result.accountId")
                                                .type(NUMBER)
                                                .description("인증한 계정 ID"))
                                .build()),
                        requestHeaders(headerWithName("X-XSRF-TOKEN").description("CSRF token request header")),
                        requestCookies(
                                cookieWithName("XSRF-TOKEN").description("요청 header와 함께 제출하는 CSRF token"),
                                cookieWithName("refresh-token").description("회전할 refresh token")),
                        responseCookies(
                                cookieWithName("access-token").description("새로 발급한 access token"),
                                cookieWithName("refresh-token").description("회전해 새로 발급한 refresh token"))));
    }

    @Test
    @DisplayName("로그아웃 API의 refresh cookie 요청과 인증 cookie 만료 계약을 문서화한다")
    void documentsLogoutContract() throws Exception {
        // given
        MvcResult signUpResult = signUp("logout@example.com");
        Cookie accessCookie = signUpResult.getResponse().getCookie("access-token");
        Cookie refreshCookie = signUpResult.getResponse().getCookie("refresh-token");

        // when
        ResultActions result =
                mockMvc.perform(post("/api/auth/logout").with(csrfToken()).cookie(accessCookie, refreshCookie));

        // then
        result.andExpect(status().isOk())
                .andDo(document(
                        "auth-logout",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Authentication")
                                .summary("로그아웃")
                                .description("현재 refresh session을 폐기하고 browser의 인증 cookie를 만료시킨다.")
                                .requestHeaders(
                                        com.epages.restdocs.apispec.ResourceDocumentation.headerWithName("X-XSRF-TOKEN")
                                                .description("CSRF token request header"))
                                .responseFields(
                                        fieldWithPath("timestamp").type(STRING).description("응답 생성 시각"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"))
                                .build()),
                        requestHeaders(headerWithName("X-XSRF-TOKEN").description("CSRF token request header")),
                        requestCookies(
                                cookieWithName("XSRF-TOKEN").description("요청 header와 함께 제출하는 CSRF token"),
                                cookieWithName("access-token").description("현재 계정을 인증하는 access token"),
                                cookieWithName("refresh-token").description("폐기할 refresh token")),
                        responseCookies(
                                cookieWithName("XSRF-TOKEN").description("후속 요청에 사용할 CSRF token"),
                                cookieWithName("access-token").description("만료한 access token cookie"),
                                cookieWithName("refresh-token").description("만료한 refresh token cookie"))));
    }

    @Test
    @DisplayName("비밀번호 변경 API의 요청과 인증 종료 응답 계약을 문서화한다")
    void documentsPasswordChangeContract() throws Exception {
        // given
        MvcResult signUpResult = signUp("password-docs@example.com");
        Cookie accessCookie = signUpResult.getResponse().getCookie("access-token");
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
                .andDo(document(
                        "account-change-password",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Account Security")
                                .summary("비밀번호 변경")
                                .description("현재 비밀번호를 확인해 새 비밀번호로 변경하고 모든 인증 상태를 종료한다.")
                                .requestHeaders(
                                        com.epages.restdocs.apispec.ResourceDocumentation.headerWithName("X-XSRF-TOKEN")
                                                .description("CSRF token request header"))
                                .requestFields(
                                        fieldWithPath("currentPassword")
                                                .type(STRING)
                                                .description("현재 비밀번호"),
                                        fieldWithPath("newPassword")
                                                .type(STRING)
                                                .description("새 비밀번호"))
                                .responseFields(
                                        fieldWithPath("timestamp").type(STRING).description("응답 생성 시각"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"))
                                .build()),
                        requestHeaders(headerWithName("X-XSRF-TOKEN").description("CSRF token request header")),
                        requestCookies(
                                cookieWithName("XSRF-TOKEN").description("요청 header와 함께 제출하는 CSRF token"),
                                cookieWithName("access-token").description("현재 계정을 인증하는 access token")),
                        responseCookies(
                                cookieWithName("XSRF-TOKEN").description("후속 요청에 사용할 CSRF token"),
                                cookieWithName("access-token").description("만료한 access token cookie"),
                                cookieWithName("refresh-token").description("만료한 refresh token cookie"))));
    }

    @Test
    @DisplayName("회원 탈퇴 API의 요청과 인증 종료 응답 계약을 문서화한다")
    void documentsAccountWithdrawalContract() throws Exception {
        // given
        MvcResult signUpResult = signUp("withdraw-docs@example.com");
        Cookie accessCookie = signUpResult.getResponse().getCookie("access-token");
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
                .andDo(document(
                        "account-withdrawal",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Account Security")
                                .summary("회원 탈퇴")
                                .description("현재 비밀번호를 확인해 계정을 탈퇴 상태로 바꾸고 모든 인증 상태를 종료한다.")
                                .requestHeaders(
                                        com.epages.restdocs.apispec.ResourceDocumentation.headerWithName("X-XSRF-TOKEN")
                                                .description("CSRF token request header"))
                                .requestFields(fieldWithPath("currentPassword")
                                        .type(STRING)
                                        .description("현재 비밀번호"))
                                .responseFields(
                                        fieldWithPath("timestamp").type(STRING).description("응답 생성 시각"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"))
                                .build()),
                        requestHeaders(headerWithName("X-XSRF-TOKEN").description("CSRF token request header")),
                        requestCookies(
                                cookieWithName("XSRF-TOKEN").description("요청 header와 함께 제출하는 CSRF token"),
                                cookieWithName("access-token").description("현재 계정을 인증하는 access token")),
                        responseCookies(
                                cookieWithName("XSRF-TOKEN").description("후속 요청에 사용할 CSRF token"),
                                cookieWithName("access-token").description("만료한 access token cookie"),
                                cookieWithName("refresh-token").description("만료한 refresh token cookie"))));
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
