# Test Guide

테스트는 업무 정책, 실패 경계, data flow와 트랜잭션 결과를 실행 가능한 명세로 표현한다.

## 테스트를 설계하는 세 가지 축

| 축 | 선택 |
| --- | --- |
| 검증 대상 | Policy, 상태 전이, 계산, 불변식, Use Case, DB, 외부 연동, HTTP와 사용자 시나리오 |
| 실행 범위 | Unit, Component, Integration, End-to-end |
| 목적 | Verification, Regression Safety, Specification, Design Feedback, Documentation |

| 실행 범위 | 함께 실행할 대상 |
| --- | --- |
| Unit | Domain, Policy, 계산과 격리할 가치가 있는 Use Case 판단 |
| Component | 기술 Adapter와 필요한 infrastructure |
| Integration | Use Case, 실제 DB, 트랜잭션과 application wiring |
| End-to-end | 실제 진입점부터 주요 infrastructure까지 제한된 시나리오 |

세 축은 독립적으로 선택한다. 검증 대상만으로 실행 범위를 고정하지 않는다.

## 테스트 범위를 선택하는 기준

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
| message 발행, 소비와 serialization | broker contract 또는 component test |
| HTTP contract, validation, authentication과 error response | HTTP component 또는 integration test |
| application wiring과 핵심 사용자 시나리오 | 제한된 end-to-end test |

운영 DB의 semantics가 검증 대상이면 Testcontainers로 운영과 같은 종류와 가능한 한 가까운 version의 DB를 사용한다. in-memory DB나 mock Mapper로 SQL correctness, constraint, isolation 또는 rollback을 증명하지 않는다.

### 선택 예시: 회원가입 후 Redis 저장 실패

피할 검증: mock의 `save()` 호출 횟수만 확인하고 account rollback이 일어났다고 결론 내린다.

권장 사례: [`SignUpUseCaseIntegrationTest`](../../src/test/java/com/example/lab/module/auth/usecase/SignUpUseCaseIntegrationTest.java). 실제 PostgreSQL 트랜잭션에서 실패 후 DB 상태를 확인한다.

```java
when(refreshSessionStore.issue(anyLong(), any(), any()))
        .thenThrow(new DataAccessResourceFailureException("redis unavailable"));

Throwable thrown = catchThrowable(() -> signUpUseCase.execute("user@example.com", "Password1!", false));

assertThat(thrown).isInstanceOf(DataAccessResourceFailureException.class);
assertThat(dsl.fetchCount(USER_ACCOUNT)).isZero();
```

Redis 경계 실패와 DB rollback을 검증한다. Redis 프로토콜은 검증하지 않는다.

순수한 입력 정책은 더 작은 범위가 적절하다. 예를 들어 [`EmailPolicyTest`](../../src/test/java/com/example/lab/module/user/domain/EmailPolicyTest.java)는 이메일 정규화와 거절 조건을 검증한다.

## Domain과 Use Case 테스트

### Domain이 있는 경우

policy, state transition, 계산 규칙과 invariant는 domain unit test에서 우선 검증한다. 허용되는 대표 사례만 확인하지 않고, 규칙이 바뀌는 경계값과 거절 조건을 negative test case로 명시한다.

최소 허용값과 바로 바깥 값을 함께 검증한다. Domain과 SQL에 같은 규칙이 있으면 같은 경계값과 상태 조합을 각각 검증한다.

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

비밀번호 변경 테스트의 가상 시나리오:

| 조건과 결과 | Method 이름 |
| --- | --- |
| 현재 비밀번호 확인 후 변경 | `changePasswordSuccessfully` |
| 사용자 부재 시 rollback | `failsWhenUserDoesNotExist` |
| 변경 완료 후 알림 | `sendsNotificationAfterPasswordChanged` |
| 변경 실패 시 알림 금지 | `doesNotSendNotificationWhenChangeFails` |

rollback과 commit 이후 실행을 이름에 적으면 실제 테스트 환경과 assertion으로 관찰해야 한다. 예시로 새 정책을 확정하지 않는다.

## 테스트 이름과 구조

DCI(Describe, Context, It)는 사용하지 않는다. 테스트 class와 method에 `Describe_*`, `Context_*`, `it_*` 형식을 강제하지 않는다.

### Method 이름

테스트 method는 영문으로 작성하고 관찰 가능한 동작이나 결과를 간결하게 표현한다.

```java
void changesPasswordSuccessfully()
void rejectsPasswordShorterThanMinimumLength()
void doesNotPublishEventWhenTransactionRollsBack()
```

구현 방법보다 업무 결과를 이름에 담는다. 하나의 이름에 여러 조건과 결과를 이어 붙여 읽기 어렵게 만들기보다 테스트를 의미 있는 사례로 나눈다.

새 정책과 주요 Use Case의 테스트 이름은 다음 Human-in-the-Loop 절차를 따른다.

1. 검증 대상, 실행 범위와 테스트 목적을 정리한다.
2. 테스트 시나리오별 영문 method 이름과 `@DisplayName`을 제안한다.
3. 사람이 시나리오의 범위와 이름을 검토하고 확정한다.
4. 확정된 이름으로 Given, When, Then 본문을 구현한다.

지정되거나 합의된 이름의 재사용, 의미와 이름이 같은 기존 테스트 수정은 재승인을 요구하지 않는다.

### `@DisplayName`

`@DisplayName`은 한국어로 조건과 기대 결과를 적는다. 영문 method 이름만 번역하지 않는다.

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

## Given, When, Then

모든 테스트 본문은 `given`, `when`, `then` 순서로 작성하고 각 구간을 주석으로 명시한다.

- `given`: 검증에 필요한 입력, 선행 상태와 협력자의 동작을 준비한다.
- `when`: 검증 대상의 행위를 실행한다.
- `then`: 반환값, 상태 변화, 저장 결과, side effect 또는 실패를 관찰한다.

핵심 입력과 결과를 helper에 숨기지 않는다. 예외는 가능하면 `when`에서 포착하고 `then`에서 오류와 부수 효과를 검증한다.

여러 assertion이 하나의 업무 결과를 설명한다면 한 테스트에 함께 둘 수 있다. 서로 독립적인 규칙이나 실패 이유를 검증한다면 테스트를 나눈다.

## Fixture와 검증 데이터

Instancio로 검증 대상과 무관한 유효한 기본값을 만들 수 있다. 그러나 경계값, 상태, 시간, 식별자와 같이 policy나 invariant의 결과에 영향을 주는 값은 테스트 본문에 명시한다.

공통 fixture는 반복을 줄이면서도 각 테스트의 선행 상태와 data flow가 보이는 범위에서만 사용한다. 테스트 기반만을 위한 공통 Fixture 계층, generic test builder, repository interface 또는 container 추상화를 미리 만들지 않는다.

현재 시각이 결과에 영향을 주는 테스트는 [Time Policy](../operations/time.md)의 `Clock` 기준을 따른다. API 문서 생성 테스트는 Spring REST Docs 결과를 source of truth로 사용하는 [Architecture](../architecture/overview.md)의 원칙을 따른다.

## 작성 전 확인 사항

- 이 테스트가 검증하는 대상과 가장 위험한 실패는 무엇인가?
- 이 동작을 실제로 관찰할 수 있는 가장 작은 실행 범위는 어디까지인가?
- 테스트가 verification, regression safety, specification, design feedback와 documentation 중 어떤 목적을 수행하는가?
- domain policy의 허용, 거절 조건과 경계값이 드러나는가?
- use case의 주요 data flow, transaction 결과와 side effect 순서가 드러나는가?
- mock interaction이 실제 DB, transaction, framework 또는 외부 protocol의 동작을 대신하고 있지는 않은가?
- method 이름과 `@DisplayName`이 구현 방식이 아니라 업무 보장을 설명하는가?
- 테스트 이름을 사람이 검토해야 하는 지점이 남아 있는가?
- Given, When, Then에서 중요한 입력, 실행과 관찰 결과를 바로 찾을 수 있는가?
- 테스트만을 위해 production abstraction을 추가하지 않았는가?

## 참고 자료

- [Spring Boot Testing](https://docs.spring.io/spring-boot/reference/testing/): Spring context를 포함하는 테스트의 범위를 선택할 때 참고한다.
- [Gradle Java Testing](https://docs.gradle.org/current/userguide/java_testing.html): `test` task와 특정 테스트 실행 방법을 확인할 때 참고한다.
