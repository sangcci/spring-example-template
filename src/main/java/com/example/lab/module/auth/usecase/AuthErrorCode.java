package com.example.lab.module.auth.usecase;

import com.example.lab.global.error.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    INVALID_CREDENTIALS(401, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다."),
    ACCOUNT_RESTRICTED(403, "AUTH_ACCOUNT_RESTRICTED", "서비스 이용이 제한된 계정입니다."),
    INVALID_REFRESH_TOKEN(401, "AUTH_INVALID_REFRESH_TOKEN", "인증 갱신 정보가 유효하지 않습니다."),
    REFRESH_TOKEN_REPLAY(401, "AUTH_REFRESH_TOKEN_REPLAY", "인증 갱신 정보가 재사용되었습니다.");

    private final int status;
    private final String code;
    private final String message;

    AuthErrorCode(int status, String code, String message) {
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
