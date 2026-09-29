# Agent Guide

## Repository Purpose

이 저장소는 AI-native Spring Boot architecture를 실제 구현으로 검증하는 laboratory다. 사람이 data flow, transaction, SQL, invariant와 side effect를 빠르게 이해하고 검증할 수 있는 구조를 찾는다.

## Repository Structure

```text
spring-example-template/
├── src/
│   ├── main/
│   │   ├── java/com/example/lab/
│   │   │   ├── module/
│   │   │   │   ├── user/          # 사용자 계정의 업무 규칙과 데이터 변경
│   │   │   │   └── auth/          # 인증 use case, refresh session, HTTP 보안
│   │   │   └── global/            # 공통 HTTP 응답, 오류, 시간, 로깅
│   │   └── resources/db/migration/ # Flyway migration, DB schema의 source of truth
│   ├── test/                      # 정책, use case, HTTP, 실제 DB 연동 검증
│   └── docs/asciidoc/             # Spring REST Docs API 문서 구성
├── docs/                          # 문서별 경로는 아래 Documentation Map 참고
└── AGENTS.md
```

## Documentation Map

먼저 [Architecture Overview](docs/architecture/overview.md)에서 기본 구조를 확인한다. 이후 작업에 관련된 문서를 읽는다.

| 작업 | 읽을 문서 |
| --- | --- |
| 기능 설계, 구조 변경, 코드 리뷰 | [Decision Guide](docs/architecture/decision-guide.md), 관련 [업무 정책](docs/policies/overview.md) |
| Java 코드 또는 설정 변경 | [Contributing Overview](docs/contributing/overview.md), [Code Style](docs/contributing/code-style.md), 필요하면 [Configuration](docs/contributing/configuration.md) |
| 테스트 변경 | [Testing](docs/contributing/testing.md), 관련 업무 정책 |
| 인증 또는 시간 처리 | [Security](docs/operations/security.md), [Time](docs/operations/time.md) 중 관련 문서 |
| 정책 문서 변경 | [Policy Writing](docs/contributing/policy-writing.md), 관련 업무 정책 |
| 커밋 | [Git Guide](docs/contributing/git.md) |

문서와 구현이 충돌하면 차이를 드러내고, 확정된 결정에 따라 관련 문서와 구현을 함께 갱신한다. 같은 원칙을 여러 문서에 복제하지 않는다.

## Workflow

1. 대상 bounded context와 데이터 owner를 식별하고 관련 업무 정책을 확인한다.
2. use case의 입력, 출력, read, write, transaction과 external side effect를 정리한다.
3. Decision Guide를 사용해 Mapper, DB constraint, domain model과 external port의 필요성을 판단한다.
4. 주변 코드의 실제 패턴을 확인하고, 복잡성이 없는 class나 interface는 추가하지 않는다.
5. 변경 후 주요 data flow, transaction boundary와 실패 경로를 설명한다.
6. 가장 위험한 가정을 중심으로 테스트한다. SQL과 DB semantics는 가능한 한 실제 대상 DB에서 검증한다.
7. 기존 원칙의 예외나 장기적인 trade-off가 생기면 근거와 대안을 제시하고, 결정된 내용을 관련 문서에 반영한다.

## Change Boundaries

- 요구하지 않은 business example이나 framework dependency를 추가하지 않는다.
- JPA, 새로운 persistence 방식, cross-domain write와 distributed consistency 방식은 명시적인 결정 없이 도입하지 않는다.
- generated code를 직접 수정하지 않는다.
- external provider SDK type, presentation DTO와 jOOQ generated type을 다른 boundary로 불필요하게 전파하지 않는다.
- 테스트만을 위한 interface, generic repository, generic service와 의미 없는 `global` abstraction을 만들지 않는다.
- 검증된 library가 제공하는 security 또는 protocol을 직접 구현하지 않는다.
- package 구조를 확장하거나 새로운 domain abstraction의 이름을 정할 때는 근거, owner와 대안을 먼저 제시하고 사용자와 합의한다.
- domain policy와 SQL predicate에 같은 규칙이 필요하면 일치성을 검증하는 테스트를 함께 둔다.

## Commands

```bash
./gradlew bootRun          # local profile로 실행
./gradlew test             # test profile로 검증
./gradlew spotlessApply    # 커밋 전 항상 실행하고 적용된 변경을 검토
./gradlew spotlessCheck    # CI 또는 별도 형식 검사가 필요할 때 실행
```

커밋 전에는 변경 파일 종류와 관계없이 `spotlessApply`를 실행하고 결과를 검토한다. Git Guide의 메시지 형식과 Commitlint도 확인한다.

## Review Output

코드와 설계를 검토할 때 시작점부터 결과까지의 data flow, side effect의 순서, 읽고 변경하는 table과 owner, business invariant의 최종 방어선, 실제 또는 예상 SQL, concurrency risk, external failure와 retry 또는 idempotency, 숨겨진 framework behavior와 검증 방법을 구체적으로 설명한다. 중요한 결정에는 비용, 대안과 변경 조건도 함께 제시한다.
