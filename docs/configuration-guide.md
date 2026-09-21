# Configuration Guide

## 1. 문서의 역할

이 문서는 Spring Boot 설정 파일의 구조와 환경별 값 표현 기준을 정의한다. 설정을 여러 파일에서 합성해 해석하는 비용보다 각 profile에서 실제로 사용하는 전체 설정을 한 파일에서 검토할 수 있는 것을 우선한다.

## 2. Profile 설정 파일

`application-local.yml`, `application-test.yml`, `application-prod.yml`은 같은 key, 계층과 배치 순서를 유지한다. 환경에 따라 달라지는 것은 값뿐이다.

- 설정 key를 추가하거나 제거하면 모든 `application-*.yml`을 같은 변경 단위에서 수정한다.
- 특정 profile에서 사용하지 않거나 runtime에 덮어쓰는 값도 key를 생략하지 않는다.
- 공통 설정을 `application.yml`에 숨겨 profile 파일을 함께 읽어야만 전체 설정을 알 수 있게 만들지 않는다.
- 환경별로 구조가 달라져야 한다면 단순 편의를 위한 예외인지 먼저 확인하고, 실제로 다른 설정 계약이 필요할 때만 차이를 문서화한다.

## 3. 환경별 값

local에서는 개발자가 별도 준비 없이 실행할 수 있는 비민감 기본값을 둘 수 있다. production의 접속 정보, secret과 배포 환경에 종속된 값은 환경변수로 받고 안전하지 않은 기본값을 두지 않는다.

test에서는 Testcontainers나 `DynamicPropertySource`가 runtime에 값을 덮어쓰더라도 대응하는 key를 모두 작성한다. 덮어쓰기 전 값이 실수로 사용됐을 때 실제 외부 시스템에 연결되지 않도록 `invalid`처럼 의도가 분명한 더미 값을 사용한다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://invalid:5432/invalid
    username: invalid
    password: invalid
```

빈 값이 기능 비활성화를 의미하고 framework가 이를 지원한다면 key는 유지하고 빈 기본값을 사용할 수 있다.

```yaml
logging:
  structured:
    format:
      console: ${LOGGING_STRUCTURED_FORMAT:}
```

## 4. 검토 기준

- 모든 `application-*.yml`에서 key, 계층과 순서가 일치하는가?
- 한 profile의 설정만 읽어도 애플리케이션이 요구하는 전체 설정을 확인할 수 있는가?
- test의 더미 값이 실제 공유 infrastructure에 연결될 가능성이 없는가?
- production secret과 접속 정보에 안전하지 않은 기본값이 들어 있지 않은가?
- 환경변수 또는 test override가 적용되지 않았을 때 실패 방식이 명확한가?
- 설정 변경 후 관련 profile로 application context가 정상적으로 시작되는지 검증했는가?
