# Architecture Decision Guide

## 1. 이 문서를 사용하는 방법

이 문서는 [architecture.md](architecture.md)의 원칙을 기능 설계, 코드 리뷰, 기존 시스템 분석에 적용하기 위한 판단 도구다. 모든 기능을 같은 형태로 만들기 위한 규칙집이 아니다. 먼저 복잡성이 어디에 있는지 찾고, 그 복잡성을 가장 직접적으로 다루는 수단을 선택한다.

> 복잡성이 존재하는 곳에만 모델을 만든다.

## 2. 첫 번째 질문: 복잡성은 어디에 있는가

| 관찰한 복잡성 | 우선 선택 | 주의할 점 |
|---|---|---|
| DB consistency, uniqueness, concurrency | SQL, constraint, transaction | 애플리케이션 사전 조회만으로 보장하지 않는다. |
| 복잡한 business invariant와 state transition | Domain Model 또는 Domain Policy | 단순 data mapping model을 만들지 않는다. |
| 여러 context와 작업 순서 조율 | Use Case | orchestration을 entity나 controller에 숨기지 않는다. |
| 외부 시스템과 독립적인 실패 | Port / Infrastructure Client | provider API를 그대로 port에 복사하지 않는다. |
| OAuth, OIDC, crypto 같은 표준 문제 | Standard / Library / Framework | 직접 재구현하지 않는다. |
| join, aggregation, projection 중심 조회 | SQL / Query Model | write model을 억지로 거치지 않는다. |

둘 이상의 복잡성이 함께 존재하면 수단도 조합한다. 예를 들어 결제 use case는 transaction을 조율하고, Order domain model로 환불 규칙을 판단하며, PaymentGateway port로 외부 PG를 호출할 수 있다.

context 전체를 하나의 유형으로 단정하지 않는다. 설계 전에 Industry/Protocol, Business Policy, Data Consistency, Orchestration 복잡성을 각각 식별하고, 위치마다 다른 수단을 선택한다.

## 3. 기능 설계 순서

1. 사용자의 의도를 하나의 use case 이름으로 표현한다.
2. 기능과 데이터의 owning context를 정한다.
3. 입력, 출력, 읽기, 쓰기, 외부 side effect를 나열한다.
4. 반드시 함께 성공하거나 실패해야 하는 변경을 표시한다.
5. invariant를 DB, domain model, use case 중 어디에서 보장할지 정한다.
6. persistence 요구를 어느 Mapper가 소유할지 판단한다.
7. 통제할 수 없는 경계에만 port와 infrastructure client를 둔다.
8. concurrency, retry, idempotency와 실패 후 관찰 가능한 상태를 검토한다.
9. 가장 위험한 가정을 검증하는 테스트를 선택한다.

설계 후에는 데이터 흐름을 한 줄로 설명할 수 있어야 한다.

```text
input -> presentation -> use case -> domain/Mapper/external port -> output
```

### 명시되지 않은 실패 가능성을 다루는 방법

요구사항의 빈틈을 안전장치로 채우지 않는다. 실제 경로가 확인되지 않은 `null`, 잘못된 type, 과거 client, 미래 확장과 일어나지 않아야 할 내부 상태를 추측해 validation, fallback, retry, 호환 경로 또는 abstraction을 추가하지 않는다.

방어 로직은 다음 중 하나로 필요성이 확인될 때만 추가한다.

- 신뢰할 수 없는 외부 입력이나 외부 시스템 응답을 검증해야 한다.
- nullable type, API 명세 또는 library contract가 해당 상태를 허용한다.
- business rule이 그 실패를 정상적인 결과로 정의한다.
- 운영 장애, 재현 가능한 bug 또는 테스트로 실제 failure mode가 확인되었다.
- security, protocol 또는 concurrency 특성상 별도의 최종 방어선이 필요하다.

추가하기 전에는 다음 질문에 답한다.

1. 그 상태가 발생하는 구체적인 경로는 무엇인가?
2. 정상 입력, business failure, 외부 장애와 programming error 중 무엇인가?
3. 현재 boundary가 처리할 owner인가?
4. 처리하지 않고 실패하게 두면 어떤 구체적인 문제가 생기는가?

Bean Validation을 통과한 use case 입력, non-null로 선언된 내부 type, DB constraint로 보호되는 값과 명시적인 Mapper contract는 해당 boundary 안에서 신뢰한다. 내부 계약이 깨졌다면 임의의 기본값으로 계속 진행하지 않고 원인이 있는 schema, SQL projection, type 또는 호출 경로를 수정한다.

요구사항의 빈틈이 business behavior, data contract 또는 architecture 선택을 바꾼다면 추측하지 않고 가정을 드러내 사용자와 합의한다. 결과를 바꾸지 않는 사소한 구현 선택은 현재 요구를 만족하는 가장 단순한 형태를 선택한다.

이 원칙은 구현 범위에 적용하며 검증 범위를 줄이는 근거로 사용하지 않는다. 코드와 설정은 가상의 가능성을 위해 늘리지 않되, type contract, DB semantics, framework behavior와 외부 protocol은 실제 자료와 테스트로 확인한다.

## 4. Bounded Context와 ownership 결정

다음 질문으로 경계를 찾는다.

- 이 기능에서 사용하는 업무 용어의 의미를 누가 정하는가?
- 어떤 context가 해당 데이터의 schema와 lifecycle을 소유하는가?
- invariant를 변경할 권한은 누구에게 있는가?
- 어떤 기능들이 같은 이유와 주기로 변경되는가?
- 다른 context가 장애를 일으켜도 독립적으로 동작해야 하는가?

기술이 같거나 같은 DB를 사용한다는 이유로 context를 합치지 않는다. 반대로 작은 차이마다 context를 나누지도 않는다. 언어, 규칙, data write ownership이 달라지는 지점이 경계의 주요 신호다.

### Cross-domain 규칙

```text
Cross-domain READ  -> 허용 가능
Cross-domain WRITE -> owner를 통해서만 허용
```

cross-domain read를 허용하기 전에 query의 schema 결합도, 접근 권한, join 비용과 별도 read model 또는 snapshot의 필요성을 확인한다. query는 `mypage` 같은 화면 이름으로 별도 context를 만들기보다 최종 business outcome의 owner가 소유한다. 자연스러운 owner가 없고 독립적인 언어와 변경 주기가 생길 때만 read-oriented context를 검토한다.

| Data topology | 기본 선택 |
|---|---|
| 단일 애플리케이션, 단일 DB | owning context의 직접 SQL join과 projection을 허용한다. |
| 동일 DB, context별 schema | cross-schema read 또는 owner가 공개한 view를 검토한다. |
| 물리적으로 분리된 DB, 강한 최신성·낮은 호출량 | context API를 호출해 결과를 composition한다. |
| 물리적으로 분리된 DB, 높은 조회량·장애 격리 필요 | event 또는 CDC 기반 local read model을 만든다. |

다른 context의 table을 직접 update하지 않는다. 변경은 owning context의 use case, contract 또는 event를 통해 요청한다.

## 5. Use Case를 어떻게 나눌 것인가

class 이름은 table이 아니라 사용자의 의도를 표현하고 `UseCase` 접미사를 사용한다.

```text
피한다:
RecruitmentService.create/update/delete/join/cancel/close/search

선호한다:
CreateRecruitmentUseCase
JoinRecruitmentUseCase
CancelRecruitmentUseCase
CloseRecruitmentUseCase
GetRecruitmentUseCase
SearchRecruitmentUseCase
```

`usecase` package는 평평하게 시작하며 `command`, `query`, use case별 하위 package로 미리 분류하지 않는다. transaction boundary, authorization, side effect, 실패 방식 또는 사용자 의도가 다르면 use case class를 분리하되 package까지 나누지는 않는다.

다음 중 하나가 실제로 발생하면 구조 변경안을 먼저 제안한다.

- 하나의 use case에만 속하는 입출력과 협력 type이 3개 이상 생겼다.
- package-private로 use case 전용 구현을 격리할 필요가 생겼다.
- 하나의 context 안에서 독립적인 하위 capability가 업무 언어로 식별된다.
- 관련 파일 탐색이나 동시 변경 충돌이 반복된다.

파일 수만으로 재구성하지 않는다. AI agent는 문제의 근거, 대안과 예상 비용을 제시하고 합의하기 전에는 package 구조를 확장하지 않는다. 이름만 다른 class가 같은 generic private service에 모든 동작을 위임해서도 안 된다.

## 6. Persistence Mapper를 어떻게 구성할 것인가

### 기본 위치와 이름

`DSLContext`를 사용하는 코드는 처음부터 `module/<context>/infra/persistence`로 분리한다. SQL 실행 객체에는 MyBatis에 익숙한 개발자가 역할을 바로 이해할 수 있도록 `*Mapper` 접미사를 사용한다.

```text
post/
└── infra/
    └── persistence/
        └── PostMapper.java
```

Mapper는 interface가 아니라 concrete infrastructure component로 시작한다. jOOQ statement를 실행하고 결과를 use case 또는 domain이 이해하는 type으로 변환한다. jOOQ의 `RecordMapper`와 혼동하지 않도록 `*RecordMapper`를 SQL 실행 객체의 이름으로 사용하지 않는다.

### Mapper를 분리하는 기준

처음에는 context의 핵심 persistence 작업을 하나의 Mapper에 둔다. 다음 조건이 실제로 생기면 capability 단위로 분리한다.

- 검색 조건과 projection이 독립적으로 복잡해지고 함께 변경된다.
- 통계 query가 별도의 성능, index와 운영 기준을 가진다.
- batch 또는 locking 작업이 일반 persistence와 다른 실행 특성을 가진다.
- 하나의 Mapper가 서로 무관한 capability 때문에 반복적으로 변경된다.

```text
PostMapper
PostSearchMapper
PostStatisticsMapper
```

statement마다 `InsertPostSql`, `SearchPostsSql` 같은 class를 만들지 않는다. 단순한 파일 수나 read/write 구분만으로 Mapper를 나누지도 않는다. `findById`, `save`, `delete`에만 머무르는 generic repository를 만들지 않고 `incrementParticipant`, `publish`, `findDetail`처럼 data behavior를 method 이름에 드러낸다.

### SQL이 복잡해질 때

다음 순서로 대응한다.

1. 현재 Mapper 안에서 SQL과 mapping을 명시적으로 유지한다.
2. 검색, 통계, batch, locking처럼 독립적인 capability와 변경 이유가 생겼는지 확인한다.
3. 근거가 있다면 capability별 Mapper 분리를 제안한다.
4. use case의 transaction과 orchestration은 Mapper로 이동하지 않는다.
5. 분리한 Mapper마다 실제 DB integration test와 필요한 execution plan 검증을 둔다.

## 7. Domain Model 도입 결정

다음 질문에 여러 개가 명확하게 참이면 domain model을 고려한다.

- 여러 rule이 상호작용해 순서와 조합이 중요한가?
- 허용되는 state transition을 한곳에서 보호해야 하는가?
- 같은 business condition이 여러 use case의 `if`로 반복되는가?
- 계산이나 판단에 이름을 붙이면 business knowledge가 더 잘 드러나는가?
- 항상 유효한 상태를 유지하는 객체가 절차적 코드보다 이해하기 쉬운가?
- DB constraint만으로 표현하기 어려운 invariant가 있는가?

단순 CRUD, 한두 column의 변경, 조회 DTO, 외부 payload mapping, getter와 setter만 있는 객체 또는 미래의 복잡성에 대한 추측만 있다면 우선 만들지 않는다.

domain model은 자신의 invariant만 소유해야 한다. 여러 context의 객체를 하나의 거대한 aggregate에 넣지 않으며, 여러 context의 조율은 use case가 담당한다.

Entity, Value Object, Domain Service와 Domain Policy는 `domain`에 둔다. use case의 요청·결과와 orchestration 전용 type은 `usecase`에 둔다. 타입의 모양이 아니라 business meaning과 ownership으로 위치를 결정한다.

### 여러 값을 반환하는 type 결정

언어가 다중 값 반환을 직접 지원하지 않더라도 반환 편의를 위해 의미 없는 `Map`, 배열 또는 임시 내부 `record`로 값을 묶지 않는다. 먼저 값들이 왜 함께 반환되는지 판단한다.

| 함께 반환하는 값의 의미 | 표현과 위치 |
|---|---|
| 하나의 business concept와 invariant | `domain`의 Value Object |
| use case 실행 결과 또는 orchestration 전용 값 | `usecase`의 result type |
| HTTP request/response와 JSON 구조 | `presentation`의 DTO |
| SQL 조회 결과와 화면 중심 shape | `usecase`의 query result 또는 `infra/persistence`의 projection |
| 외부 provider의 request/response | 해당 `infra/client` 또는 `external` boundary의 DTO |

값들이 서로 독립적이고 함께 변경되거나 검증될 이유가 없다면 하나의 반환 type으로 묶기 전에 책임 있는 method로 분리할 수 있는지 검토한다. 반대로 하나의 SQL snapshot, transaction 또는 business decision으로 함께 반환되어야 한다면 호출 횟수를 늘리기 위해 억지로 method를 분리하지 않고 그 의미를 드러내는 result type을 사용한다.

DTO를 domain Value Object로 재사용하지 않는다. JSON 계층 구조, field 이름이나 외부 contract가 바뀐다는 이유로 domain이 함께 변경되어서는 안 된다. 같은 필드를 가지더라도 소유하는 boundary와 변경 이유가 다르면 별도의 type으로 유지한다.

policy 이름에는 `PostPolicy`처럼 context 전체를 포괄하는 이름보다 `PostPublicationPolicy`, `PostEditingPolicy`, `OrderCancellationPolicy`처럼 판단하는 business concept를 사용한다. 어느 use case가 호출하는지가 아니라 어떤 정책을 소유하는지를 기준으로 이름 붙인다. 새로운 domain abstraction의 이름은 AI agent가 독자적으로 확정하지 않고 의미, owner, 사용 use case와 대안 이름을 먼저 제시한다.

### 식별자별 조회를 하나의 Policy로 수렴할 때

이메일, 외부 provider ID처럼 use case 입력의 식별자가 달라도 조회의 결과가 같은 domain concept라면, Mapper는 각 식별자에 맞는 명시적인 query로 그 domain 값을 반환한다. Policy는 조회 경로가 아니라 반환된 domain 값을 기준으로 판단한다.

```text
email 가입       -> email로 User 조회       -> RejoinPolicy(User)
social 가입      -> provider ID로 User 조회 -> RejoinPolicy(User)
```

이 원칙은 모든 조회를 하나의 generic method로 통합하라는 뜻이 아니다. SQL의 join과 predicate, index는 식별자별로 명시성을 유지한다. 수렴해야 하는 것은 User 상태, 탈퇴 시각, 권한처럼 같은 업무 사실을 해석하는 Policy다.

Policy에 repository나 Mapper를 주입해 식별자 조회까지 맡기지 않는다. Use Case가 조회와 transaction, 호출 순서를 조율하고 Policy는 이미 확보한 User와 필요한 업무 입력만 판단한다. Policy의 결과가 언제나 같은 business failure라면 boolean을 반환해 호출부가 다시 분기하게 하지 않고 `ensureRejoinAllowed`처럼 실패를 직접 표현하는 command 형태를 선택할 수 있다.

email과 provider ID가 같은 사람, 같은 계정 또는 같은 재가입 제한의 단위인지 확정되지 않았다면 공통 identifier type을 먼저 만들지 않는다. 이는 persistence 리팩터링이 아니라 user identity와 policy 범위를 바꾸는 결정이므로, 정책 문서와 schema를 포함해 별도로 합의한다.

### SQL-first context에 domain logic을 추가할 때

domain의 성숙은 프로젝트 기간이나 파일 수가 아니라 다음과 같은 관찰된 business complexity를 의미한다.

- 여러 rule이 함께 판단되어야 한다.
- 같은 policy가 여러 use case에서 반복된다.
- state transition의 허용 조건이 늘어난다.
- 절차적인 조건문보다 model이나 policy가 business knowledge를 더 잘 압축한다.

이 신호가 확인되면 기존 SQL-first 흐름을 폐기하지 않고 필요한 Domain Model 또는 Domain Policy를 추가한다. Use Case가 domain decision을 실행하고, Mapper가 그 결과를 명시적인 SQL로 반영한다.

### Domain policy와 SQL 조건이 겹칠 때

동시성 때문에 SQL predicate가 같은 조건을 다시 확인해야 한다면 이중 구현을 허용한다.

```text
Domain Policy
-> 사용자의 요청이 왜 허용되거나 거절되는지 판단

Conditional SQL WHERE
-> 경쟁 요청 사이에서도 잘못된 write가 발생하지 않도록 보장
```

다음 조건을 모두 지킨다.

- 두 구현이 필요한 이유를 코드나 테스트 이름으로 드러낸다.
- 동일한 경계값과 상태 조합을 domain test와 DB integration test에서 검증한다.
- SQL의 영향받은 row 수를 concurrency conflict 또는 business failure로 해석한다.
- 규칙 변경 시 policy, SQL과 테스트를 함께 검색하고 검토한다.
- 동시성이나 DB consistency와 무관한 단순 중복은 만들지 않는다.

### Snapshot 결정

다음 중 하나라도 해당하면 다른 context 객체의 현재 상태를 참조하는 대신 snapshot을 검토한다.

- 시간이 지나도 당시의 값을 보존해야 한다.
- 원본이 변경되어도 과거 기록의 의미가 달라지면 안 된다.
- 해당 값이 계약, 결제, 감사 또는 정산의 근거다.

상품명과 구매 가격처럼 복사한 값은 단순 denormalization이 아니라 시점의 business fact일 수 있다. snapshot의 생성 시점과 owner를 명확히 한다.

## 8. DB invariant 결정

| 보장하려는 규칙 | 최종 방어선 | 애플리케이션의 역할 |
|---|---|---|
| 필수 값 | `NOT NULL` | 빠르고 이해하기 쉬운 validation message를 제공한다. |
| 자연키와 중복 방지 | `UNIQUE` | 사전 확인은 UX 최적화로만 사용한다. |
| 참조 무결성 | `FOREIGN KEY` | lifecycle과 삭제 정책을 조율한다. |
| request의 형식·범위·필수 조합 | Bean Validation | inbound adapter에서 오류를 반환한다. |
| 서비스가 결정하는 business rule | Use Case 또는 Domain Policy | 업무 언어로 판단하고 실패를 반환한다. |
| 제한된 수량의 동시 변경 | conditional atomic update | 영향받은 row 수를 성공 또는 충돌로 해석한다. |
| 경쟁하는 상태 전이 | compare-and-set, version 또는 lock | 충돌 시 실패·재시도 정책을 정한다. |
| 여러 row에 걸친 규칙 | transaction과 필요한 lock | 처리 순서와 failure semantics를 정의한다. |

`CHECK` constraint는 Bean Validation이나 application policy와 같은 규칙을 중복하므로 기본적으로 사용하지 않는다. 이 결정은 모든 write가 애플리케이션의 검증 경로를 통과한다는 전제를 가진다. HTTP 이외의 message, scheduler와 batch도 동일한 validation과 policy를 거쳐야 하며, 외부 writer와 운영 SQL의 직접 변경은 통제한다.

`NOT NULL`, `UNIQUE`, `FOREIGN KEY`와 concurrency 제어는 Bean Validation으로 대체할 수 없으므로 DB에 유지한다. `SELECT`로 확인하고 나중에 `UPDATE`하는 check-then-act는 두 문장 사이의 경쟁을 고려해야 한다. 가능하면 constraint 또는 하나의 conditional statement로 합친다. constraint에는 운영 중 식별 가능한 이름을 붙이고 기술 예외를 업무상 실패로 변환한다.

## 9. Transaction 결정

1. 함께 commit되어야 하는 DB 변경을 나열한다.
2. 상태를 변경하는 use case를 기본 transaction boundary로 삼는다.
3. lost update, write skew, phantom, duplicate insert 등 가능한 anomaly를 적는다.
4. constraint, atomic SQL, optimistic version, pessimistic lock, isolation level 중 가장 작은 수단을 선택한다.
5. lock 획득 순서와 transaction 길이를 확인한다.
6. 충돌 시 즉시 실패, 제한된 retry, 사용자 재시도 중 하나를 정한다.
7. 외부 side effect 전후에 관찰 가능한 상태를 기록한다.

`@Transactional`만 보고 원자성을 가정하지 않는다. 참여하는 datasource와 connection, propagation, rollback 대상, flush가 있다면 그 시점까지 확인한다.

### 외부 호출이 transaction과 만날 때

- 외부 응답이 늦을 때 DB lock을 얼마나 오래 잡는가?
- 외부는 성공했지만 local commit이 실패하면 어떻게 되는가?
- retry가 중복 결제나 중복 발송을 만들지 않는가?
- idempotency key, outbox, compensation 중 무엇이 필요한가?

이 질문에 답하지 못하면 네트워크 호출을 DB transaction 안에 넣지 않는다. 분산 resource 사이의 원자성을 local transaction annotation으로 해결하려 하지 않는다.

## 10. Query 결정

여러 table 또는 context의 조합, `JOIN`, `GROUP`, `FILTER`, aggregation, 화면 전용 shape가 핵심이고 domain behavior 없이 읽기만 한다면 SQL projection을 우선한다.

query review에서는 필요한 column, index, 결정적인 ordering과 pagination, 데이터 증가 시 확장성, 실제 execution plan, 개인정보와 cross-domain read 권한을 확인한다. query 재사용을 위해 의미가 다른 요구사항을 거대한 SQL 하나로 합치지 않는다.

## 11. JPA 도입 결정

기본 선택은 jOOQ다. JPA를 금지하지는 않지만 다음 질문에 답하고 ADR을 작성해야 한다.

- 어떤 aggregate lifecycle과 object graph가 JPA로 실제로 단순해지는가?
- jOOQ보다 줄어드는 business complexity는 무엇인가?
- lazy loading, dirty checking, persistence context, cascade와 flush timing을 어떻게 통제하는가?
- N+1과 예상하지 못한 query를 어떻게 발견하는가?
- JPA entity가 transport나 다른 context로 퍼지는 것을 어떻게 막는가?
- jOOQ와 함께 쓸 때 connection과 transaction을 어떻게 공유하는가?
- write는 JPA, read는 jOOQ로 분리할 필요가 있는가?
- 기대한 이점이 없을 때 철회 조건과 비용은 무엇인가?

단순 CRUD가 많거나 SQL 작성이 귀찮다는 이유만으로 JPA를 도입하지 않는다. 적용 범위는 필요한 context 또는 aggregate로 제한한다.

## 12. External Port와 Infrastructure Client 결정

외부 시스템에는 우리가 lifecycle과 availability를 통제하지 못하고, protocol·SDK·인증·실패가 독립적으로 변한다는 이유로 port를 둔다. port는 provider가 아니라 use case 관점의 작은 capability를 표현한다.

`infra/client` 구현체를 검토할 때 다음을 확인한다.

- 모든 호출에 timeout이 있는가?
- retry 가능한 오류를 구분하는가?
- 요청은 idempotent하거나 중복을 탐지할 수 있는가?
- provider DTO와 예외가 infrastructure 밖으로 새지 않는가?
- rate limit과 부분 장애를 관찰하는가?
- contract 변경을 감지할 테스트가 있는가?

내부의 단순하고 안정된 pure function까지 테스트 편의를 위해 interface로 만들지는 않는다.

## 13. 인증·보안·표준 기술 결정

OAuth, OIDC, JWT, PKCE, signature, password hashing과 cryptography는 직접 구현하지 않는다. 표준과 검증된 library, Spring Security 같은 framework를 사용한다.

- use case는 로그인, 계정 연결, local user 생성과 session 발급 순서를 조율한다.
- port는 외부 identity 검증 capability를 표현한다.
- `infra/security` 또는 `infra/client`는 issuer, audience, nonce, state, signature와 token parsing을 처리한다.
- policy는 우리 서비스 고유의 authorization rule을 표현한다.

표준 protocol을 독자적인 domain model로 다시 발명하지 않는다. 반대로 서비스 고유 권한 규칙은 framework 설정에 흩뜨리지 말고 명시적인 policy로 분리할 수 있다.

## 14. 대표 기능의 시작점

| 기능 | 권장 시작점 |
|---|---|
| 게시물 CRUD | 명시적인 `*UseCase` + SQL |
| 게시물 검색 | SQL Query + Projection |
| 좋아요 | `UNIQUE` + SQL |
| 모집 인원 증가 | Conditional Atomic SQL |
| 마이페이지 | Cross-domain SQL Projection |
| 비밀번호 변경 | `*UseCase` + standard password hashing library |
| Social Login | `*UseCase` + Identity Port/Infrastructure Client |
| JWT 검증 | Security Infrastructure |
| 서비스 고유 권한 검사 | Security + explicit Policy |
| 장바구니 | SQL-first로 시작하고 규칙 증가를 관찰 |
| 주문 | Domain Model 도입 가능성이 높음 |
| 결제 | Use Case + Payment Port |
| PG 연동 | Infrastructure Client |
| 환불 | Domain Model 또는 Policy 도입 가능성이 높음 |
| 재고 차감 | Atomic SQL + Transaction |
| 복잡한 할인 | Domain Policy |
| 정산 | Domain Model 도입 가능성이 높음 |
| 통계 | SQL Query + Projection |

이 표는 최종 설계를 고정하지 않는다. 실제 invariant와 workload가 확인되면 더 단순하거나 더 강한 구조로 바꾼다.

## 15. 테스트 선택

검증할 위험, 함께 실행할 범위와 테스트의 목적을 분리해 판단한다. 구체적인 선택 기준과 작성 형식은 [Test Guide](test-guide.md)를 따른다.

## 16. 코드 리뷰 체크리스트

- 파일 하나에서 use case의 주요 흐름과 side effect 순서를 파악할 수 있는가?
- transaction의 시작, 종료와 참여 resource가 분명한가?
- 실제 실행 SQL이나 그 위치를 빠르게 찾을 수 있는가?
- write 대상 table과 owning context가 일치하는가?
- invariant의 최종 방어선이 명확한가?
- 불필요한 entity, repository, service, interface가 추가되지 않았는가?
- use case가 `UseCase` 접미사를 사용하고 업무 의도를 표현하는가?
- package와 domain abstraction의 이름을 근거 없이 임의로 만들지 않았는가?
- domain model이 business complexity를 실제로 줄이는가?
- 외부 SDK, DTO, 예외가 infrastructure 밖으로 누출되지 않는가?
- 표준이나 검증된 library로 해결할 문제를 재구현하지 않았는가?
- 새 null check, fallback, retry, validation과 호환 경로에 실제 발생 경로와 owner가 있는가?
- 내부 계약 위반을 빈 값이나 기본값으로 숨기고 있지는 않은가?
- framework의 hidden behavior가 있다면 테스트와 관찰 방법이 있는가?
- AI가 수정할 범위와 사람이 검증할 범위가 좁고 명확한가?

## 17. 기존 시스템 분석 절차

1. business capability, ubiquitous language와 bounded context 후보를 찾는다.
2. table, schema와 write ownership을 표시한다.
3. 주요 entry point에서 DB와 외부 시스템까지 실제 data flow를 추적한다.
4. transaction boundary, flush, lock과 side effect 순서를 표시한다.
5. invariant가 controller, service, entity, DB 중 어디에서 보호되는지 확인한다.
6. 실제 SQL과 execution plan을 찾고 object mapping과의 차이를 기록한다.
7. cross-domain write, shared table과 암묵적 dependency를 찾는다.
8. timeout, retry, idempotency와 장애 복구 경로를 확인한다.
9. 문서상 구조와 runtime behavior의 차이를 기록한다.
10. 가장 위험한 use case 하나부터 더 explicit하게 만드는 개선안을 작성한다.

분석 결과는 framework와 pattern 목록보다 flow, ownership, invariant, transaction, failure mode를 중심으로 정리한다.

## 18. ADR과 반론이 필요한 결정

다음 선택은 `docs/adr/`에 기록한다.

- bounded context 또는 data ownership 변경
- JPA나 새로운 persistence paradigm 도입
- 전역 transaction 또는 isolation 정책 변경
- cross-domain 동기 호출이나 event 기반 협력 도입
- outbox, saga, compensation 등 분산 일관성 전략 채택
- 중요한 외부 시스템과 retry/idempotency 정책 도입
- 공통 abstraction 또는 framework를 전역 표준으로 채택
- architecture 원칙의 예외를 장기간 허용

ADR에는 문제, 맥락과 제약, 선택지, 결정, 예상 결과, 검증 방법과 철회 조건을 적는다.

중요한 결정은 현재 원칙에 동의하는 검토만 거치지 않는다. complex Order aggregate, distributed transaction, high-throughput batch, read-heavy system, multi-module monolith, MSA migration, complex authorization 같은 조건을 대입해 별도로 반론한다. AI의 critique는 판단 자료로 사용하고, 최종 결정과 trade-off는 사람이 책임진다.

## 19. Lint 검증

Java 또는 Gradle 변경 후에는 다음 task를 사용한다.

```text
./gradlew spotlessCheck
./gradlew spotlessApply
```

CI와 review에서는 `spotlessCheck`를 기준으로 사용한다. 자동 formatting이 필요할 때만 `spotlessApply`를 실행하고 변경 결과를 다시 검토한다.
