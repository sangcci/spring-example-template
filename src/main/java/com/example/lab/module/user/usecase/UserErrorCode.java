package com.example.lab.module.user.usecase;

import com.example.lab.global.error.ErrorCode;

public enum UserErrorCode implements ErrorCode {
    EMAIL_ALREADY_REGISTERED(409, "USER_EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다."),
    EMAIL_REJOIN_RESTRICTED(409, "USER_EMAIL_REJOIN_RESTRICTED", "탈퇴 후 7일 동안 다시 가입할 수 없습니다."),
    INVALID_EMAIL(400, "USER_INVALID_EMAIL", "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD(400, "USER_INVALID_PASSWORD", "비밀번호 정책을 만족하지 않습니다."),
    PASSWORD_MISMATCH(400, "USER_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다."),
    ACCOUNT_NOT_FOUND(404, "USER_ACCOUNT_NOT_FOUND", "사용자를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;

    UserErrorCode(int status, String code, String message) {
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
