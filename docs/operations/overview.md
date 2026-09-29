# Operations Overview

여기는 여러 use case에 적용되는 기술 계약을 둔다. 업무상 허용 조건과 사용자에게 보이는 결과는 [업무 정책](../policies/overview.md)에 둔다.

| 변경 대상 | 읽을 문서 | 함께 확인할 곳 |
| --- | --- | --- |
| JWT, cookie, CSRF, refresh session, 인증 실패 | [Security](security.md) | [Authentication Policy](../policies/authentication.md), `module/auth/infra/security` |
| 날짜 경계, 저장 시각, API timestamp, 테스트 clock | [Time](time.md) | 관련 업무 정책, `global/time/TimeConfig.java` |

인증이나 시간 계약을 바꾸면 해당 정책, 설정, use case, HTTP 또는 DB 테스트까지 같은 변경으로 검토한다.
