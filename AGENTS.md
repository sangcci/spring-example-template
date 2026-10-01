# Agent Guide

## Repository Purpose

Spring Boot 아키텍처 검증용 저장소다. 작업별 읽기 경로와 실행 절차를 안내한다.

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
| 기능 추가 | [Adding Feature](docs/contributing/adding-feature.md), [Error Handling](docs/contributing/error-handling.md) |
| 버그 수정 | [Fixing Bugs](docs/contributing/fixing-bugs.md), [Error Handling](docs/contributing/error-handling.md) |
| 리팩터링 | [Refactoring](docs/contributing/refactoring.md), [Error Handling](docs/contributing/error-handling.md) |
| Java 코드 또는 설정 변경 | [Contributing Overview](docs/contributing/overview.md), [Code Style](docs/contributing/code-style.md), 필요하면 [Configuration](docs/contributing/configuration.md) |
| 테스트 변경 | [Testing](docs/contributing/testing.md), 관련 업무 정책 |
| 인증 또는 시간 처리 | [Security](docs/operations/security.md), [Time](docs/operations/time.md) 중 관련 문서 |
| 문서 변경 | [Documentation](docs/contributing/documentation.md), 정책서는 [Policy Writing](docs/contributing/policy-writing.md) |
| 커밋 | [Git Guide](docs/contributing/git.md) |

문서와 구현의 차이는 보고하고 확정된 결정에 맞춰 함께 갱신한다. 문서 작성은 [Documentation](docs/contributing/documentation.md)을 따른다.

## 문서에 없는 정책

사용자 결과, 권한, 데이터 보관, 동시성과 외부 부수 효과가 달라지는 미확정 정책은 사용자에게 확인한다. 코드나 관례로 확정하지 않는다.

- 확인한 사실, 미확정 조건, 선택지와 결과를 정리한다.
- 구현 전에 질문을 묶어 묻고, 답과 무관한 조사는 계속한다.
- 답을 기다리는 동안 임의 정책을 코드나 테스트에 고정하지 않는다.
- 기존 원칙으로 판단할 수 있는 구현 선택은 직접 결정하고 근거를 적는다.
- 확정된 정책은 원본 정책서, 구현과 테스트에 함께 반영한다.

## 레거시 코드와 리팩터링

- 기존 레거시 기능을 수정하거나 추가할 때는 해당 영역의 명명, 구조와 호출 스타일을 유지한다. 부분적인 컨벤션 정리와 리팩터링을 섞지 않는다.
- 새 도메인과 새 Use Case는 프로젝트 가이드를 따른다. 기존 레거시 도메인을 호출하는 접점은 해당 코드의 방식을 유지한다.
- 리팩터링은 [Refactoring](docs/contributing/refactoring.md)의 E2E 회귀 테스트, 흐름 파악, 미확정 사항 합의, 메시지와 경계 정리, Use Case 전체 재작성 순서를 따른다.
- 운영 위험이 큰 경우에만 Feature Flag로 Legacy와 New 경로를 점진 전환한다.
- 기능 작업에서 발견한 리팩터링 후보는 별도 작업으로 제안한다.

## Workflow

1. 대상 bounded context와 데이터 owner를 식별하고 관련 업무 정책을 확인한다.
2. use case의 입력, 출력, read, write, transaction과 external side effect를 정리한다.
3. Decision Guide를 사용해 Mapper, DB constraint, domain model과 external port의 필요성을 판단한다.
4. [레거시와 리팩터링 기준](#레거시-코드와-리팩터링)에 따라 적용할 스타일을 정한다. 불필요한 class와 interface는 추가하지 않는다.
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

Gradle 실행 JVM은 Java 21을 사용한다. toolchain 설정과 별도로 실행 JVM을 확인한다.

| 환경 | JDK 선택 |
| --- | --- |
| SDKMAN | `.sdkmanrc`에 따라 `sdk env` |
| SDKMAN 미사용 | 설치된 Java 21의 `JAVA_HOME` 설정 |
| Java 21 미설치 | 사용자에게 설치 또는 경로 확인 |

```bash
./gradlew bootRun          # local profile로 실행
./gradlew test             # test profile로 검증
./gradlew spotlessApply    # 파일 종류와 관계없이 커밋 전 실행, 적용 결과 검토
./gradlew spotlessCheck    # CI 또는 별도 형식 검사가 필요할 때 실행
```

커밋 형식과 Commitlint는 [Git Guide](docs/contributing/git.md)를 따른다.

Gradle wrapper/cache 접근 실패 시 캐시를 삭제하지 않는다. 해당 경로의 권한을 요청해 재시도하거나 승인된 cache를 `GRADLE_USER_HOME`으로 지정한다.

## Review Output

검토 결과에는 data flow, read/write와 owner, SQL, 불변식의 최종 방어선, 트랜잭션, 부수 효과, 동시성, 외부 실패, retry/idempotency와 검증법을 적는다. 프레임워크의 암묵적 동작을 표시하고 중요한 결정에는 비용, 대안과 변경 조건을 제시한다.
