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
| 기능 추가 | [Adding Feature](docs/contributing/adding-feature.md), [Error Handling](docs/contributing/error-handling.md) |
| 버그 수정 | [Fixing Bugs](docs/contributing/fixing-bugs.md), [Error Handling](docs/contributing/error-handling.md) |
| 리팩터링 | [Refactoring](docs/contributing/refactoring.md), [Error Handling](docs/contributing/error-handling.md) |
| Java 코드 또는 설정 변경 | [Contributing Overview](docs/contributing/overview.md), [Code Style](docs/contributing/code-style.md), 필요하면 [Configuration](docs/contributing/configuration.md) |
| 테스트 변경 | [Testing](docs/contributing/testing.md), 관련 업무 정책 |
| 인증 또는 시간 처리 | [Security](docs/operations/security.md), [Time](docs/operations/time.md) 중 관련 문서 |
| 정책 문서 변경 | [Policy Writing](docs/contributing/policy-writing.md), 관련 업무 정책 |
| 커밋 | [Git Guide](docs/contributing/git.md) |

문서와 구현이 충돌하면 차이를 드러내고, 확정된 결정에 따라 관련 문서와 구현을 함께 갱신한다. 같은 원칙을 여러 문서에 복제하지 않는다.

설계와 개발 지침은 사람과 AI가 같은 근거를 찾을 수 있도록 짧은 기준, 피할 예시와 권장 예시, 실제 코드 또는 테스트 경로, 예외 조건 순서로 쓴다. 코드와 SQL이 판단을 분명히 보여줄 수 있으면 긴 배경 설명 대신 작은 예시를 사용한다. 업무 정책서는 구현 코드보다 조건과 결과가 드러나는 사례를 사용한다.

## 문서에 없는 정책을 다루는 방법

문서에는 인간이 알고 있는 모든 요구사항과 결정이 담겨 있지 않다. 문서에 없는 조건을 기존 코드의 동작이나 일반적인 관례로 확정하지 않는다. 업무상 선택에 따라 사용자 결과, 권한, 데이터 보관, 동시성 또는 외부 side effect가 달라지면 사용자에게 구체적으로 질문한다.

- 확인한 사실, 문서에서 찾지 못한 조건, 가능한 선택지와 각 선택의 결과를 구분해 제시한다.
- 필요한 질문은 구현 전에 묶어서 묻되, 관련 코드 조사와 독립적인 작업은 계속 진행한다.
- 답을 기다리는 동안 임의의 정책을 확정하거나 테스트에 고정하지 않는다.
- 구현 선택만 남았고 기존 원칙으로 판단할 수 있다면 직접 결정하고 근거를 설명한다.
- 사용자의 답으로 정책이 확정되면 관련 정책서, 구현과 테스트를 같은 변경에서 갱신한다.

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

Gradle을 실행하기 전에 Java 21을 선택한다. SDKMAN을 사용할 수 있으면 저장소의 `.sdkmanrc`에 따라 `sdk env`를 실행한다. SDKMAN이 없으면 설치된 Java 21 JDK를 `JAVA_HOME`으로 선택한다. 적합한 JDK가 없으면 사용자에게 설치 또는 사용 경로를 확인한다. Gradle의 Java toolchain 설정은 빌드에 사용하는 JDK를 선택하지만, 플러그인을 로드하는 Gradle 실행 JVM까지 Java 21로 바꾸지는 않는다.

```bash
./gradlew bootRun          # local profile로 실행
./gradlew test             # test profile로 검증
./gradlew spotlessApply    # 커밋 전 항상 실행하고 적용된 변경을 검토
./gradlew spotlessCheck    # CI 또는 별도 형식 검사가 필요할 때 실행
```

커밋 전에는 변경 파일 종류와 관계없이 `spotlessApply`를 실행하고 결과를 검토한다. Git Guide의 메시지 형식과 Commitlint도 확인한다.

제한된 실행 환경에서 Gradle이 `~/.gradle`의 wrapper 또는 cache lock 파일에 접근하지 못하면 캐시를 삭제하지 않는다. 해당 경로의 실행 권한을 요청한 뒤 같은 명령을 다시 실행한다. 승인된 쓰기 가능한 Gradle cache 위치를 사용해야 하는 환경에서는 `GRADLE_USER_HOME`으로 경로를 지정한다.

## Review Output

코드와 설계를 검토할 때 시작점부터 결과까지의 data flow, side effect의 순서, 읽고 변경하는 table과 owner, business invariant의 최종 방어선, 실제 또는 예상 SQL, concurrency risk, external failure와 retry 또는 idempotency, 숨겨진 framework behavior와 검증 방법을 구체적으로 설명한다. 중요한 결정에는 비용, 대안과 변경 조건도 함께 제시한다.
