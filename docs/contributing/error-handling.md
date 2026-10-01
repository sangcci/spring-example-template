# 오류 처리

오류는 발생한 경계에서 원인을 분류하고, 사용자에게 공개할 결과로 변환한다. HTTP 응답에는 내부 예외 메시지, SQL, token과 외부 서비스의 원문 오류를 싣지 않는다. 업무 결과가 미확정이면 status와 오류 code를 추측하지 말고 사용자에게 묻는다.

## 오류가 이동하는 경로

| 발생 위치 | 현재 변환 경로 | 확인할 결과 |
| --- | --- | --- |
| 요청 형식과 Bean Validation | `GlobalExceptionHandler` | `COMMON_INVALID_REQUEST`, 필요한 필드 오류 |
| Use Case의 예상 가능한 업무 실패 | `ApplicationException` → `GlobalExceptionHandler` | 해당 context의 error code와 status |
| DB constraint 위반 | Use Case가 원인을 분류 → `ApplicationException` | 업무상 충돌 결과 |
| 인증 filter의 잘못된 JWT | `JwtAuthenticationFilter`가 응답 작성 | `AUTH_INVALID_ACCESS_TOKEN` |
| 인증 필요 또는 CSRF 실패 | Spring Security handler가 응답 작성 | `COMMON_UNAUTHORIZED` 또는 `SECURITY_INVALID_CSRF_TOKEN` |
| 예상하지 못한 예외 | `GlobalExceptionHandler`가 기록하고 응답 작성 | `COMMON_INTERNAL_SERVER_ERROR` |
| Redis 등 외부 시스템 실패 | 해당 use case의 transaction과 정책에 따름 | rollback, 남는 side effect와 사용자 결과 |

이 표는 현재 구현을 읽는 지도다. 새 오류 유형을 모든 context에 적용할 공통 계층으로 만들라는 뜻이 아니다.

## 오류 코드의 위치와 계약

context가 의미를 정하는 오류 코드는 `module/<context>/error`에 둔다. 현재 user 오류는 `user/error/UserErrorCode.java`, auth use case 오류는 `auth/error/AuthErrorCode.java`가 소유한다. 업무 데이터나 use case 입출력 type과 구분하고, 오류를 사용하는 domain policy, use case와 adapter가 같은 코드를 참조한다. 특정 security protocol에만 속한 오류는 현재 `auth/infra/security/SecurityErrorCode`처럼 해당 기술 경계에 유지한다. context와 무관한 공통 오류 계약은 `global/error`가 소유한다.

현재 `ErrorCode`는 HTTP status, 공개 code와 메시지를 함께 갖는 애플리케이션 오류 계약이다. `error` 패키지로 이동해도 순수한 DDD domain failure가 되지는 않는다. domain 업무 데이터가 usecase 타입에 의존하게 만들지 않는다.

## 1. 입력 오류와 업무 오류를 구분한다

요청 body의 유효성 오류는 HTTP 경계에서 처리한다. 업무 규칙으로 거절하는 결과는 owner의 error code를 사용한다.

```java
// CreateUserAccountUseCase의 업무 판단
if (!validEmail) {
    throw new ApplicationException(UserErrorCode.INVALID_EMAIL);
}
```

```java
// GlobalExceptionHandler의 변환
var errorCode = exception.errorCode();
return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
```

[`GlobalExceptionHandler`](../../src/main/java/com/example/lab/global/web/GlobalExceptionHandler.java)는 입력 오류, 업무 오류와 예상하지 못한 오류의 HTTP 변환을 소유한다. [`ApiErrorResponse`](../../src/main/java/com/example/lab/global/web/ApiErrorResponse.java)는 응답의 형태를 정한다.

## 2. DB 오류를 실제 원인에 맞춰 변환한다

동시 회원가입에서는 이메일 사전 조회를 두 요청이 모두 통과할 수 있다. [migration](../../src/main/resources/db/migration/V1__create_user_account.sql)의 활성 계정 이메일 `UNIQUE` index가 중복을 막고 [`CreateUserAccountUseCase`](../../src/main/java/com/example/lab/module/user/usecase/CreateUserAccountUseCase.java)는 현재 이 write의 `DataIntegrityViolationException`을 `EMAIL_ALREADY_REGISTERED`로 변환한다.

```java
try {
    long accountId = userAccountMapper.insert(normalizedEmail, passwordHash, now);
    return new CreateUserAccountResult(accountId, UserRole.USER);
} catch (DataIntegrityViolationException exception) {
    throw new ApplicationException(UserErrorCode.EMAIL_ALREADY_REGISTERED, exception);
}
```

이 코드를 다른 Mapper의 모든 무결성 오류에 복사하지 않는다. 여러 constraint가 같은 write에서 실패할 수 있다면 어떤 위반인지 구분하고, 구분할 수 없을 때는 임의의 업무 실패로 바꾸지 않는다. [`CreateUserAccountUseCaseIntegrationTest`](../../src/test/java/com/example/lab/module/user/usecase/CreateUserAccountUseCaseIntegrationTest.java)는 중복 가입의 결과를 검증한다.

## 3. Security filter의 실패는 별도 경로로 처리한다

`JwtAuthenticationFilter`는 만료되거나 잘못된 access token을 읽으면 `AUTH_INVALID_ACCESS_TOKEN` 응답을 직접 작성한다. access token이 없으면 anonymous로 다음 filter에 전달하고, 인증이 필요한 endpoint에서 [`ApiAuthenticationEntryPoint`](../../src/main/java/com/example/lab/module/auth/infra/security/ApiAuthenticationEntryPoint.java)가 `COMMON_UNAUTHORIZED`를 작성한다. CSRF 실패는 [`ApiAccessDeniedHandler`](../../src/main/java/com/example/lab/module/auth/infra/security/ApiAccessDeniedHandler.java)가 처리한다.

```text
잘못된 JWT -> JwtAuthenticationFilter -> 401 AUTH_INVALID_ACCESS_TOKEN
JWT 없음 + 보호된 endpoint -> ApiAuthenticationEntryPoint -> 401 COMMON_UNAUTHORIZED
CSRF token 없음 -> ApiAccessDeniedHandler -> 403 SECURITY_INVALID_CSRF_TOKEN
```

Security filter의 예외가 ControllerAdvice에 도달한다고 가정하지 않는다. [`AuthHttpIntegrationTest`](../../src/test/java/com/example/lab/module/auth/presentation/AuthHttpIntegrationTest.java)는 CSRF 실패의 HTTP 결과를 확인한다. token과 cookie의 상세 계약은 [Security](../operations/security.md)를 따른다.

## 4. 외부 실패 뒤 남는 상태를 설명한다

회원가입에서 Redis session 발급이 실패하면 PostgreSQL account INSERT는 rollback된다. Redis 저장은 성공했지만 PostgreSQL commit이 실패하면 사용되지 않은 session이 TTL 동안 남을 수 있다. 현재 선택의 이유와 변경 조건은 [Security](../operations/security.md)에 있다.

```text
Redis 실패 -> account rollback -> 회원가입 실패
Redis 성공 + DB commit 실패 -> account rollback, Redis session은 TTL까지 남을 수 있음
```

`catch`로 실패를 성공 응답으로 바꾸거나, idempotency와 중복 실행 결과를 모른 채 retry하지 않는다. [`SignUpUseCaseIntegrationTest`](../../src/test/java/com/example/lab/module/auth/usecase/SignUpUseCaseIntegrationTest.java)는 DB rollback을 관찰한다.

## 5. 예상하지 못한 오류는 내부에 기록한다

```java
log.error("unexpected_error", exception);
var errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
return ResponseEntity.status(errorCode.status()).body(ApiErrorResponse.of(errorCode));
```

위 코드는 [`GlobalExceptionHandler`](../../src/main/java/com/example/lab/global/web/GlobalExceptionHandler.java)의 마지막 경계다. 사용자 응답에는 예외 원문을 넣지 않는다. 예상 가능한 업무 실패까지 이 경계로 보내고 있다면 owner와 변환 위치를 다시 확인한다.

## 변경할 때 확인할 질문

- 이 실패는 입력 오류, 업무상 거절, 동시성 충돌, 외부 장애 중 어디에 속하는가?
- 사용자에게 보여줄 code와 status가 정책 또는 API 계약으로 확정되었는가?
- 실패 전후에 DB와 외부 시스템에 무엇이 남는가?
- retry가 같은 작업을 중복 실행할 수 있는가?
- 오류가 filter, controller, use case, Mapper 중 어디에서 처음 발생하고 어디에서 변환되는가?
- 실제 결과를 어느 테스트에서 관찰할 수 있는가?

정책이 없는 질문은 [AGENTS.md](../../AGENTS.md)의 "문서에 없는 정책을 다루는 방법"에 따라 사용자에게 제시한다. 단순 구현 선택은 [Decision Guide](../architecture/decision-guide.md)와 주변 코드를 근거로 판단한다.
