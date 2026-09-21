package com.example.lab.global.web;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.lab.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
class CommonApiDocumentationTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("모든 성공 응답이 공유하는 필드를 문서화한다")
    void documentsCommonSuccessResponseFields() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(get("/api/auth/csrf"));

        // then
        result.andExpect(status().isOk())
                .andDo(document(
                        "common-success-response",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("timestamp").type(STRING).description("응답 생성 시각(ISO 8601 UTC)"),
                                fieldWithPath("code").type(STRING).description("성공 코드"),
                                fieldWithPath("message").type(STRING).description("성공 메시지"))));
    }

    @Test
    @DisplayName("공통 오류 응답 필드와 HTTP header를 문서화한다")
    void documentsCommonErrorResponseFieldsAndHttpHeaders() throws Exception {
        // given
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        String requestId = "docs-common-contract";
        String body = """
                {"email":"","password":"","rememberMe":false}
                """;

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/sign-up")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .header("X-Request-Id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", requestId))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andDo(document(
                        "common-error-response",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("X-Request-Id")
                                        .description("요청 추적 ID. 생략하거나 형식이 올바르지 않으면 server가 새 값을 생성한다."),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("JSON request body의 media type")),
                        responseHeaders(
                                headerWithName("X-Request-Id").description("요청 처리에 사용한 추적 ID"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("JSON response body의 media type")),
                        responseFields(
                                fieldWithPath("timestamp").type(STRING).description("응답 생성 시각(ISO 8601 UTC)"),
                                fieldWithPath("code").type(STRING).description("오류 코드"),
                                fieldWithPath("message").type(STRING).description("오류 메시지"),
                                fieldWithPath("errors")
                                        .type(ARRAY)
                                        .description("필드별 오류 목록")
                                        .optional(),
                                fieldWithPath("errors[].field")
                                        .type(STRING)
                                        .description("오류가 발생한 request field")
                                        .optional(),
                                fieldWithPath("errors[].message")
                                        .type(STRING)
                                        .description("해당 field의 오류 메시지")
                                        .optional())));
    }
}
