package com.example.lab.module.auth.infra.security;

import com.example.lab.global.error.ErrorCode;

public enum SecurityErrorCode implements ErrorCode {
    INVALID_ACCESS_TOKEN(401, "AUTH_INVALID_ACCESS_TOKEN", "인증 정보가 유효하지 않습니다."),
    INVALID_CSRF_TOKEN(403, "SECURITY_INVALID_CSRF_TOKEN", "CSRF token이 유효하지 않습니다.");

    private final int status;
    private final String code;
    private final String message;

    SecurityErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
