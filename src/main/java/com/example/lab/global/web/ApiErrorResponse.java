package com.example.lab.global.web;

import com.example.lab.global.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"timestamp", "code", "message", "errors"})
public record ApiErrorResponse(Instant timestamp, String code, String message, List<FieldErrorResponse> errors) {

    public ApiErrorResponse {
        if (errors != null) {
            errors = List.copyOf(errors);
        }
        if (errors != null && errors.isEmpty()) {
            errors = null;
        }
    }

    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(Instant.now(), errorCode.code(), errorCode.message(), null);
    }

    public static ApiErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> errors) {
        return new ApiErrorResponse(Instant.now(), errorCode.code(), errorCode.message(), errors);
    }
}
