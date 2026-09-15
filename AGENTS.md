# Agent Guide

## Repository Purpose

이 저장소는 AI-native Spring Boot architecture를 실험하고 검증하는 laboratory다. 특정 pattern의 완성형 구현보다 실제 data flow, transaction, SQL, invariant와 side effect를 사람이 빠르게 이해하고 검증할 수 있는 구조를 찾는 일을 우선한다.

## Source of Truth

작업하기 전에 다음 문서를 순서대로 읽는다.

1. [`docs/architecture.md`](docs/architecture.md): 아키텍처의 문제의식, 원칙과 기본 구조를 정의한다.
2. [`docs/decision-guide.md`](docs/decision-guide.md): 기능 설계, 기술 선택, 코드 리뷰와 기존 시스템 분석의 판단 기준을 정의한다.
3. [`docs/code-style.md`](docs/code-style.md): 코드의 표현 방식과 불필요한 추상화를 피하는 기준을 정의한다.
4. 관련 ADR: 해당 결정의 맥락, trade-off와 예외를 설명한다.

이 파일에는 아키텍처 원칙을 복제하지 않는다. 문서와 구현이 충돌하거나 새로운 판단이 필요하면 임의로 한쪽을 따르지 말고, 차이를 드러낸 뒤 문서 또는 ADR과 구현을 함께 갱신한다.

## Agent Workflow

1. 요청의 대상 bounded context와 owning context를 식별한다.
2. 변경할 use case의 입력, 출력, read, write와 external side effect를 정리한다.
3. `docs/decision-guide.md`를 사용해 persistence Mapper, DB constraint, transaction, domain model과 external port/client의 필요성을 판단한다.
4. template이 제공하는 빈 package는 유지하되, 실제 복잡성이 없는 class나 interface를 채우지 않는다.
5. 변경 후 주요 data flow와 transaction boundary를 설명한다.
6. 가장 위험한 가정을 중심으로 테스트한다. SQL과 DB semantics는 가능한 한 실제 대상 DB에서 검증한다.
7. Java 또는 Gradle 파일을 변경하면 `spotlessCheck`를 실행하고, 필요할 때 `spotlessApply`로 수정한 뒤 다시 검증한다.
8. 기존 원칙의 예외나 장기적인 trade-off가 생기면 ADR을 작성하거나 사용자에게 제안한다.

## Change Boundaries

- 요구하지 않은 business example이나 framework dependency를 임의로 추가하지 않는다.
- JPA, 새로운 persistence 방식, cross-domain write, distributed consistency 방식은 명시적인 결정 없이 도입하지 않는다.
- generated code를 직접 수정하지 않는다.
- external provider의 SDK type, presentation DTO와 jOOQ generated type을 다른 boundary로 불필요하게 전파하지 않는다.
- 테스트만을 위한 interface, generic repository, generic service와 의미 없는 `global` abstraction을 만들지 않는다.
- 이미 표준과 검증된 library로 해결된 security 또는 protocol을 직접 구현하지 않는다.
- package 구조를 확장하거나 새로운 domain abstraction의 이름을 정해야 하면 근거, owner와 대안을 먼저 제시하고 사용자와 합의한다.
- domain policy와 SQL predicate에 같은 규칙이 필요하면 동시성 또는 consistency 관점의 이유와 두 구현의 일치성을 검증하는 테스트를 함께 둔다.

## Review Output

코드나 설계를 검토할 때 pattern 준수 여부만 보고하지 않는다. 다음 내용을 구체적으로 설명한다.

- 시작점부터 결과까지의 data flow
- transaction과 side effect의 실행 순서
- 읽고 변경하는 table과 data owner
- business invariant의 위치와 최종 방어선
- 실제 또는 예상 SQL과 concurrency risk
- external failure, retry와 idempotency
- 숨겨진 framework behavior와 검증 방법
- 선택한 구조의 비용, 대안과 변경 조건

AI가 생성하거나 검토한 결과는 최종 판단이 아니다. 중요한 결정은 반론과 실패 가능성을 함께 제시하고, 사람이 trade-off를 선택할 수 있게 한다.
