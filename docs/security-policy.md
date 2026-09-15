# Security Policy

## 1. 소유권과 경계

인증·인가 정책은 `auth` context가 소유한다. 요청 처리의 앞단에서 모든 context에 적용된다는 이유로 `global`에 두지 않는다. JWT 검증, 인증 주체 구성, Spring Security 설정과 CSRF 처리는 `auth/infra/security`에 둔다.

`global`은 인증과 관련된 business rule을 소유하지 않는다. `auth`의 security 구현은 일관된 HTTP body를 만들기 위해 `global`의 공통 오류·응답 계약을 사용할 수 있지만, `global`은 `auth`에 의존하지 않는다.

## 2. 인증 방식

클라이언트는 browser를 기준으로 하며 access token은 JWT를 사용한다. OAuth2 Resource Server는 사용하지 않고 servlet filter에서 access JWT 인증 흐름과 실패 응답을 직접 표현한다.

JWT의 Base64URL 처리, parsing과 signature 검증은 검증된 JWT library에 맡긴다. HMAC, JSON parsing과 cryptography를 직접 구현하지 않는다.

## 3. Access JWT

access JWT는 `HttpOnly` cookie로 전달한다. token에는 다음 claim만 둔다.

- `sub`: 변경되지 않는 account ID
- `iss`: token 발급자
- `aud`: token을 사용할 API
- `iat`: 발급 시각
- `nbf`: 사용 시작 시각
- `exp`: 만료 시각
- `jti`: token 식별자
- `role`: 애플리케이션 권한
- `token_type`: `access`

JWT에는 이메일, 닉네임과 같은 개인정보를 넣지 않는다. filter는 signature, algorithm, 만료, 사용 시작 시각, issuer, audience, token type과 필수 claim을 검증한다.

JWT 원문과 cookie 값은 로그에 남기지 않는다. 외부 응답은 token 만료, signature 불일치와 claim 오류를 구분하지 않고 `AUTH_INVALID_ACCESS_TOKEN`으로 반환한다. 내부 로그와 metric에서는 원인을 구분할 수 있다.

## 4. Refresh session

refresh token은 JWT가 아닌 opaque random token을 사용하고 Redis에 session을 저장한다. Redis에는 token 원문을 저장하지 않고 session ID, token hash, account ID, family ID, 상태와 만료 시각을 TTL과 함께 저장한다.

refresh token은 사용할 때마다 회전한다. 기존 token 소비와 새 token 발급은 Redis에서 원자적으로 처리한다. 이미 사용된 token이 다시 들어오면 replay로 판단하고 같은 family의 refresh session을 폐기한다.

Redis 장애 시 로그인과 refresh는 fail closed한다. 유효한 access JWT를 사용하는 일반 요청은 access token 만료 전까지 Redis 장애와 독립적으로 처리한다.

## 5. Browser 보안

access token과 refresh token은 `HttpOnly` cookie로 전달하며 운영 환경에서는 `Secure`를 사용한다. cookie의 `SameSite`, `Path`, 만료와 domain은 auth API와 배포 topology를 확정할 때 명시한다. 상위 domain을 공유해야 한다는 근거가 없다면 host-only cookie를 사용한다.

cookie가 browser 요청에 자동으로 포함되므로 CSRF protection을 활성화한다. CSRF token을 cookie로 전달하고 변경 요청에서는 header로 다시 보내게 한다. CORS는 허용 origin을 설정으로 열거하며 credentials와 wildcard origin을 함께 사용하지 않는다.

## 6. 실패 처리

- access token이 없으면 anonymous 상태로 다음 filter를 진행한다.
- 인증이 필요한 endpoint에 anonymous 사용자가 접근하면 `401 COMMON_UNAUTHORIZED`를 반환한다.
- access token이 잘못되면 `401 AUTH_INVALID_ACCESS_TOKEN`을 반환한다.
- 인증됐지만 권한이 부족하면 `403 COMMON_FORBIDDEN`을 반환한다.
- CSRF token이 없거나 일치하지 않으면 `403 SECURITY_INVALID_CSRF_TOKEN`을 반환한다.
- Security filter에서 발생한 오류는 ControllerAdvice에 도달한다고 가정하지 않는다.

## 7. 기본 접근 정책

명시적으로 공개한 endpoint만 anonymous 접근을 허용하고 나머지는 인증을 요구한다. health endpoint는 공개하지만 Prometheus endpoint는 기본적으로 보호한다. 로그인과 refresh endpoint는 해당 use case를 구현할 때 공개 목록에 추가한다.
