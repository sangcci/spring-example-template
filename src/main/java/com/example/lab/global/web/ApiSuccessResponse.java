package com.example.lab.global.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"timestamp", "code", "message", "result"})
public record ApiSuccessResponse<T>(Instant timestamp, String code, String message, T result) {

    public static <T> ApiSuccessResponse<T> of(T result) {
        return new ApiSuccessResponse<>(Instant.now(), "SUCCESS", "요청에 성공했습니다.", result);
    }

    public static ApiSuccessResponse<Void> empty() {
        return new ApiSuccessResponse<>(Instant.now(), "SUCCESS", "요청에 성공했습니다.", null);
    }
}
