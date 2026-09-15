package com.example.lab.global.error;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST(400, "COMMON_INVALID_REQUEST", "요청이 올바르지 않습니다."),
    UNAUTHORIZED(401, "COMMON_UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(403, "COMMON_FORBIDDEN", "요청을 수행할 권한이 없습니다."),
    NOT_FOUND(404, "COMMON_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "COMMON_METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "COMMON_UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(500, "COMMON_INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    CommonErrorCode(int status, String code, String message) {
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
