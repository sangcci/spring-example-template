# Test Guide

## 1. 문서의 역할

이 문서는 이 저장소에서 테스트를 선택하고 표현하는 기준을 정의한다. 테스트는 구현의 정답 여부만 확인하지 않는다. 사람과 AI agent가 테스트를 읽고 업무 정책, 실패 경계, 주요 data flow와 transaction의 결과를 빠르게 이해할 수 있는 실행 가능한 명세로 사용한다.

테스트가 ADR을 대신하지는 않는다. 테스트는 현재 보장하는 동작을 구체적인 예로 보여주고, ADR은 그 구조와 trade-off를 선택한 이유를 설명한다.

## 2. 테스트를 설계하는 세 가지 축

테스트를 작성하기 전에 다음 세 가지를 각각 결정한다.

### 무엇을 검증하는가

검증 대상은 policy, state transition, 계산 규칙, invariant, use case 흐름, orchestration, transaction, dependency, DB mapping, query, 외부 API, message broker, serialization, HTTP contract, validation, authentication, error response, 사용자 시나리오와 end-to-end 흐름 등이 될 수 있다.

### 어디까지 함께 실행하는가

- Unit test: 하나의 domain model, policy, 계산 또는 격리할 가치가 있는 use case 판단을 검증한다.
- Component test: DB adapter, 외부 client, message 처리기나 HTTP adapter처럼 하나의 기술 경계를 필요한 infrastructure와 함께 검증한다.
- Integration test: use case와 실제 DB, transaction, application wiring 등 여러 구성 요소의 협력을 검증한다.
- End-to-end test: 실제 진입점부터 주요 infrastructure까지 연결된 사용자 시나리오를 제한적으로 검증한다.

### 테스트의 목적은 무엇인가

- Verification: 현재 구현이 기대한 결과를 내는지 확인한다.
- Regression Safety: 이미 보장한 동작이 변경으로 깨지는 것을 감지한다.
- Specification: 허용되는 동작과 거절되는 경계를 실행 가능한 예로 표현한다.
- Design Feedback: 테스트하기 어려운 결합, 숨겨진 상태와 불명확한 책임을 드러낸다.
- Documentation: 업무 흐름과 시스템의 관찰 가능한 결과를 사람과 AI agent에게 설명한다.

세 축은 일대일로 대응하지 않는다. 정책을 unit test와 DB integration test에서 각각 검증할 수 있고, 하나의 테스트가 검증, 회귀 방지, 명세와 문서 역할을 함께 수행할 수도 있다. “정책 테스트는 unit test다”처럼 검증 대상만으로 실행 범위를 고정하지 않는다.

## 3. 테스트 범위를 선택하는 기준

검증하려는 위험을 실제로 관찰할 수 있는 가장 작은 범위를 선택한다. 빠르거나 격리되어 있다는 이유만으로 운영에서 중요한 동작을 mock의 호출 여부로 대체하지 않는다.

| 검증하려는 위험 | 우선 선택 |
|---|---|
| 순수한 business policy와 계산 | unit test |
| domain state transition과 invariant | domain unit test |
| domain model이 없는 use case의 판단과 분기 | use case unit test |
| use case의 주요 업무 흐름 | 필요한 협력 범위를 포함한 unit test 또는 integration test |
| transaction commit과 rollback | 실제 transaction을 사용하는 use case integration test |
| transaction 완료 전후의 side effect | 실제 transaction과 side effect 경계를 포함한 integration test |
| SQL, mapping, constraint와 query | 실제 대상 DB를 사용하는 component 또는 integration test |
| locking, concurrency와 isolation | 병렬 실행을 포함한 실제 DB integration test |
| 외부 API의 protocol mapping과 오류 처리 | client contract 또는 component test |
| message 발행·소비와 serialization | broker contract 또는 component test |
| HTTP contract, validation, authentication과 error response | HTTP component 또는 integration test |
| application wiring과 핵심 사용자 시나리오 | 제한된 end-to-end test |

운영 DB의 semantics가 검증 대상이면 Testcontainers로 운영과 같은 종류와 가능한 한 가까운 version의 DB를 사용한다. in-memory DB나 mock Mapper로 SQL correctness, constraint, isolation 또는 rollback을 증명하지 않는다.

## 4. Domain과 Use Case 테스트

### Domain이 있는 경우

policy, state transition, 계산 규칙과 invariant는 domain unit test에서 우선 검증한다. 허용되는 대표 사례만 확인하지 않고, 규칙이 바뀌는 경계값과 거절 조건을 negative test case로 명시한다.

예를 들어 비밀번호 길이가 8자 이상이라면 임의의 짧은 값 하나만 거절하는 것으로 끝내지 않는다. 최소 허용값과 그 바로 아래 값을 함께 두어 정책의 경계를 드러낸다. domain policy와 SQL predicate가 같은 규칙을 각각 담당한다면 동일한 경계값과 상태 조합을 domain unit test와 DB integration test에서 검증한다.

### Domain이 없는 경우

domain model이 없다는 이유로 policy 검증을 생략하지 않는다. use case가 판단을 소유한다면 그 판단과 분기를 use case test에서 나누어 검증한다. 테스트만을 위해 domain abstraction이나 interface를 만들지는 않는다.

### Use Case

domain test에서 이미 충분히 검증한 정책 조합을 use case test마다 반복하지 않는다. use case test는 다음 내용을 우선해서 보여준다.

- 주요 업무 성공 흐름
- data access와 domain logic의 호출 순서가 만드는 결과
- transaction 안에서 함께 commit되거나 rollback되는 변경
- transaction 완료 전후의 side effect
- 실패했을 때 실행되지 않아야 하는 후속 작업
- 외부 실패가 use case 결과와 local state에 미치는 영향

예를 들어 비밀번호 변경 use case는 다음과 같이 구성할 수 있다.

```java
@Test
@DisplayName("유효한 현재 비밀번호를 확인한 사용자는 비밀번호를 변경할 수 있다")
void changePasswordSuccessfully() {
    // given

    // when

    // then
}

@Test
@DisplayName("사용자가 존재하지 않으면 변경 사항을 모두 되돌린다")
void failsWhenUserDoesNotExist() {
    // given

    // when

    // then
}

@Test
@DisplayName("비밀번호 변경이 완료된 뒤 사용자에게 알림을 보낸다")
void sendsNotificationAfterPasswordChanged() {
    // given

    // when

    // then
}

@Test
@DisplayName("비밀번호 변경에 실패하면 알림을 보내지 않는다")
void doesNotSendNotificationWhenChangeFails() {
    // given

    // when

    // then
}
```

이름만으로 transaction의 실제 rollback이나 commit 이후 실행을 보장할 수는 없다. 그런 이름을 사용한다면 테스트 환경과 assertion이 해당 동작을 실제로 관찰해야 한다.

## 5. 테스트 이름과 구조

DCI(Describe, Context, It)는 사용하지 않는다. 테스트 class와 method에 `Describe_*`, `Context_*`, `it_*` 형식을 강제하지 않는다.

### Method 이름

테스트 method는 영문으로 작성하고 관찰 가능한 동작이나 결과를 간결하게 표현한다.

```java
void changesPasswordSuccessfully()
void rejectsPasswordShorterThanMinimumLength()
void doesNotPublishEventWhenTransactionRollsBack()
```

구현 방법보다 업무 결과를 이름에 담는다. 하나의 이름에 여러 조건과 결과를 이어 붙여 읽기 어렵게 만들기보다 테스트를 의미 있는 사례로 나눈다.

테스트 method 이름은 사람이 주도적으로 검토한다. AI agent는 검증 대상과 실패 위험을 근거로 이름을 제안할 수 있지만, 새 정책이나 중요한 use case를 표현하는 최종 이름은 Human-in-the-Loop로 확정한다. 이름이 정책을 잘못 설명하거나 구현 세부 사항에 묶이지 않았는지 사람이 확인한다.

AI agent가 새 정책이나 중요한 use case의 테스트를 작성할 때는 다음 순서를 따른다.

1. 검증 대상, 실행 범위와 테스트 목적을 정리한다.
2. 테스트 시나리오별 영문 method 이름과 `@DisplayName`을 제안한다.
3. 사람이 시나리오의 범위와 이름을 검토하고 확정한다.
4. 확정된 이름으로 Given, When, Then 본문을 구현한다.

사람이 이미 이름을 지정했거나, 합의된 이름을 같은 의미로 반복 적용하는 경우에는 다시 확인하지 않는다. 기존 테스트의 구현만 수정하고 업무 의미와 이름이 달라지지 않는 경우에도 별도의 이름 검토를 요구하지 않는다.

### `@DisplayName`

`@DisplayName`은 한국어 업무 표현으로 테스트의 조건, 기대 결과 또는 이 보장이 필요한 이유를 분명하게 설명한다. method 이름을 단순히 번역하지 않는다. 실패 보고서만 읽어도 어떤 업무 보장이 깨졌는지 알 수 있어야 한다.

### `@Nested`

`@Nested`는 여러 테스트가 하나의 정책이나 의미 있는 업무 조건을 공유할 때만 사용한다. 파일의 모든 테스트를 형식적으로 묶거나 DCI 구조를 재현하기 위해 사용하지 않는다. 중첩하지 않아도 class와 method 이름으로 범위가 분명하면 평평한 구조를 유지한다.

```java
@Nested
@DisplayName("비밀번호 길이 정책")
class PasswordLengthPolicy {

    @Test
    @DisplayName("8자는 허용한다")
    void acceptsMinimumLength() {
        // given

        // when

        // then
    }

    @Test
    @DisplayName("8자보다 짧으면 거절한다")
    void rejectsPasswordShorterThanMinimumLength() {
        // given

        // when

        // then
    }
}
```

## 6. Given, When, Then

모든 테스트 본문은 `given`, `when`, `then` 순서로 작성하고 각 구간을 주석으로 명시한다.

- `given`: 검증에 필요한 입력, 선행 상태와 협력자의 동작을 준비한다.
- `when`: 검증 대상의 행위를 실행한다.
- `then`: 반환값, 상태 변화, 저장 결과, side effect 또는 실패를 관찰한다.

준비와 검증을 helper 안에 과도하게 숨기지 않는다. 테스트를 읽는 사람이 중요한 입력과 기대 결과를 본문에서 확인할 수 있어야 한다. 예외를 검증할 때도 실행과 검증을 한 줄에 합쳐 `when`을 감추기보다, 가능하면 실행 결과를 `when`에서 포착하고 `then`에서 예외와 부수 효과를 검증한다.

여러 assertion이 하나의 업무 결과를 설명한다면 한 테스트에 함께 둘 수 있다. 서로 독립적인 규칙이나 실패 이유를 검증한다면 테스트를 나눈다.

## 7. Fixture와 검증 데이터

Instancio로 검증 대상과 무관한 유효한 기본값을 만들 수 있다. 그러나 경계값, 상태, 시간, 식별자와 같이 policy나 invariant의 결과에 영향을 주는 값은 테스트 본문에 명시한다.

공통 fixture는 반복을 줄이면서도 각 테스트의 선행 상태와 data flow가 보이는 범위에서만 사용한다. 테스트 기반만을 위한 공통 Fixture 계층, generic test builder, repository interface 또는 container 추상화를 미리 만들지 않는다.

현재 시각이 결과에 영향을 주는 테스트는 [Time Policy](time-policy.md)의 `Clock` 기준을 따른다. API 문서 생성 테스트는 Spring REST Docs 결과를 source of truth로 사용하는 [Architecture](architecture.md)의 원칙을 따른다.

## 8. 작성 전 확인 사항

- 이 테스트가 검증하는 대상과 가장 위험한 실패는 무엇인가?
- 이 동작을 실제로 관찰할 수 있는 가장 작은 실행 범위는 어디까지인가?
- 테스트가 verification, regression safety, specification, design feedback와 documentation 중 어떤 목적을 수행하는가?
- domain policy의 허용·거절 조건과 경계값이 드러나는가?
- use case의 주요 data flow, transaction 결과와 side effect 순서가 드러나는가?
- mock interaction이 실제 DB, transaction, framework 또는 외부 protocol의 동작을 대신하고 있지는 않은가?
- method 이름과 `@DisplayName`이 구현 방식이 아니라 업무 보장을 설명하는가?
- 테스트 이름을 사람이 검토해야 하는 지점이 남아 있는가?
- Given, When, Then에서 중요한 입력, 실행과 관찰 결과를 바로 찾을 수 있는가?
- 테스트만을 위해 production abstraction을 추가하지 않았는가?
