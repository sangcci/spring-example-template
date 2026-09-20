# Explicit Data-Flow Architecture

## 1. 문서의 역할

이 문서는 이 저장소의 아키텍처 원칙을 정의하는 source of truth다. `AGENTS.md`, 다른 AI agent용 지침, 예제 코드와 ADR은 이 문서를 참조해야 하며, 같은 내용을 각자의 표현으로 복제하지 않는다.

이 저장소는 완성된 framework나 production starter를 제공하는 것이 목적이 아니다. 실제 구현을 통해 가설을 검증하고, 발견한 문제를 ADR과 규칙에 반영하는 architecture laboratory다.

```text
architecture hypothesis
  -> real implementation
  -> pain point discovery
  -> critique and ADR
  -> rule refinement
  -> template improvement
```

## 2. 문제의식: 구현 비용보다 검증 비용

AI가 boilerplate와 반복 구현의 비용을 낮추면, 사람이 직접 코드를 작성하는 시간보다 AI가 만든 코드를 읽고 검증하는 시간이 상대적으로 중요해진다. 이 환경에서 아키텍처는 다음 비용을 줄여야 한다.

- 코드 탐색 비용
- 추상화의 실제 의미를 해석하는 비용
- transaction boundary를 추적하는 비용
- 실행되는 SQL을 추론하는 비용
- framework의 hidden behavior를 확인하는 비용
- side effect와 실패 지점을 파악하는 비용
- 생성된 코드의 correctness를 검증하는 비용

과거의 중요한 질문이 “어떻게 적은 코드로 구현할 것인가?”였다면, 앞으로의 질문은 “어떻게 실제 동작을 적게 추론하면서 검증할 것인가?”다.

> 코드를 적게 쓰는 것이 아니라, 시스템의 동작을 적게 추론하게 만든다.

추상화 자체를 피하지는 않는다. 추상화가 감추는 비용보다 경계를 명확하게 하거나 복잡성을 줄이는 가치가 클 때 사용한다.

## 3. 핵심 철학

> 도메인으로 시스템을 나누되, 데이터가 어떻게 변하는지는 숨기지 않는다.

> 복잡성이 존재하는 곳에만 모델을 만든다.

이 철학은 다음 원칙으로 구체화한다.

- Strategic DDD로 business boundary와 ownership을 정의한다.
- generic service 대신 명시적인 use case를 애플리케이션 진입점으로 둔다.
- persistence는 jOOQ와 SQL-first 접근을 기본으로 한다.
- transaction과 side effect의 순서를 코드에 드러낸다.
- consistency는 가능한 범위에서 DB constraint와 atomic operation으로 보장한다.
- domain model은 business invariant가 실제로 복잡할 때 도입한다.
- 통제할 수 없는 외부 시스템은 선택적인 port와 infrastructure client로 격리한다.
- 인증과 암호화처럼 이미 해결된 문제는 표준과 검증된 library를 따른다.

이 아키텍처는 Anti-DDD가 아니다. **DDD for boundaries, explicit data flow for behavior**를 지향한다.

## 4. 기본 실행 흐름

가장 단순한 기능의 기본 흐름은 다음과 같다.

```text
HTTP / Message / Schedule
  -> Presentation
  -> Use Case
       [Transaction Boundary]
       -> Persistence Mapper
       -> Database
  -> Result
```

복잡성이 생기면 필요한 방향으로만 확장한다.

```text
                    +-> Domain Model / Policy
                    |
Presentation -> Use Case -> Persistence Mapper -> Database
                    |
                    +-> External Port -> Infrastructure Client -> External System
```

각 use case를 열었을 때 다음 내용을 빠르게 확인할 수 있어야 한다.

- 입력과 출력
- 읽는 데이터와 변경하는 데이터
- 실행하거나 위임하는 SQL
- transaction boundary
- domain rule을 적용하는 지점
- 외부 시스템 호출과 side effect의 순서
- 실패의 종류와 처리 정책

실행 흐름과 코드 의존성은 구분한다. `presentation`은 `usecase`를 호출하고, use case는 domain과 concrete persistence Mapper를 사용할 수 있다. 외부 시스템의 infrastructure client는 use case가 소유한 port를 구현한다. domain model은 Spring, presentation DTO, jOOQ generated type, 외부 SDK에 의존하지 않는다.

이 구조는 모든 입출력을 port로 감싸는 엄격한 Hexagonal Architecture가 아니다. **domain-oriented layered architecture with selective ports**를 지향한다. DB 접근은 명시성을 위해 concrete Mapper를 직접 사용할 수 있고, port는 통제할 수 없는 외부 경계에 선택적으로 도입한다.

## 5. Domain boundary: Strategic DDD는 유지한다

코드는 controller, service, repository 같은 전역 기술 계층이 아니라 business capability와 bounded context를 기준으로 나눈다.

유지하는 DDD 개념은 다음과 같다.

- Bounded Context
- Ubiquitous Language
- Domain Boundary
- Context ownership
- Business invariant 식별

기본 구조로 강제하지 않는 Tactical DDD 요소는 다음과 같다.

- 모든 데이터에 Rich Entity 만들기
- 모든 primitive를 Value Object로 감싸기
- 모든 aggregate에 Repository 만들기
- 모든 규칙에 Domain Service 만들기
- 모든 변경을 Aggregate method로 표현하기

domain boundary를 지키는 일과 rich domain model을 사용하는 일은 별개의 선택이다. 단순한 context도 자신의 언어, 데이터 ownership, write boundary를 가져야 한다.

## 6. Use case가 애플리케이션의 단위다

`RecruitmentService`에 `create`, `join`, `cancel`, `close`, `search`를 모으는 대신 `CreateRecruitmentUseCase`, `JoinRecruitmentUseCase`, `CancelRecruitmentUseCase`, `CloseRecruitmentUseCase`, `SearchRecruitmentUseCase`처럼 사용자의 의도별 진입점을 둔다. 애플리케이션 진입점에는 일관되게 `UseCase` 접미사를 사용하며, 교체할 구현이 없다면 interface와 `Impl`을 기계적으로 나누지 않는다.

하나의 use case는 다음을 책임진다.

- 요청을 애플리케이션 입력으로 변환한 이후의 orchestration
- 필요한 validation과 authorization policy 호출
- transaction 시작과 종료
- data access와 domain logic 호출 순서
- 외부 시스템과의 상호작용 순서
- 결과와 예상 가능한 실패의 반환

use case를 작게 유지하는 목적은 class 수를 늘리는 데 있지 않다. 변경 범위, transaction, side effect를 한곳에서 검토할 수 있게 하는 데 있다. `presentation`은 protocol 처리에 집중하고 business orchestration을 가져가지 않는다.

`usecase` package는 처음에는 평평하게 유지한다. `command`, `query` 또는 use case별 하위 package를 기본으로 만들지 않는다. 구조 확장이 필요해 보이더라도 AI agent는 독자적으로 재구성하지 않고, 탐색이나 변경 비용이 발생한 근거와 대안을 먼저 제시한다.

## 7. SQL-first persistence

관계형 데이터 문제는 SQL과 DB의 언어로 먼저 이해한다. 동시성, atomic update, uniqueness, foreign key, aggregation, projection, locking, transaction은 Java object mapping보다 DB behavior가 핵심인 경우가 많다.

기본 기술 조합은 다음과 같다.

```text
Spring Boot
+ jOOQ
+ Flyway
+ PostgreSQL or MySQL
+ Spring Transaction
+ Testcontainers
```

스키마 흐름은 다음을 기준으로 한다.

```text
DDL / Migration
  -> Database Schema
  -> jOOQ Code Generation
  -> Java Data Access
```

- migration과 실제 DB schema를 persistence의 source of truth로 둔다.
- generated code는 직접 수정하지 않는다.
- SQL에는 필요한 column, join, filter, ordering, projection, lock을 명시한다.
- 중요한 query는 index와 execution plan까지 함께 검토한다.
- jOOQ type과 record는 해당 data access 범위 밖으로 불필요하게 전파하지 않는다.
- 운영 DB와 의미가 다른 in-memory DB로 SQL correctness를 증명하지 않는다.

### 7.1 Repository는 기본값이 아니다

`findById`, `save`, `delete`만 제공하는 generic repository는 DB를 객체 저장소처럼 보이게 하고 실제 행위를 감출 수 있다. persistence는 `infra/persistence`에 두고, jOOQ를 사용하는 concrete `*Mapper`로 표현한다.

```text
RecruitmentMapper
  - incrementParticipant
  - close
  - findDetail

RecruitmentSearchMapper
  - search
```

Mapper는 MyBatis의 Mapper와 마찬가지로 SQL 실행 진입점이라는 의미로 사용하지만, jOOQ의 `RecordMapper`와는 구분한다. 이 저장소의 `*Mapper`는 statement 실행과 application type으로의 결과 변환을 함께 담당하는 concrete infrastructure component다. 별도의 interface나 구현체 쌍을 기본으로 만들지 않는다.

처음에는 context의 핵심 persistence를 하나의 Mapper에 둔다. 검색, 통계, batch처럼 독립적인 query와 운영 특성이 생기면 `PostSearchMapper`, `PostStatisticsMapper`처럼 capability 단위로 분리한다. statement마다 `*Sql` class를 만들거나 파일 수만으로 Mapper를 나누지 않는다. `DSLContext`는 Mapper 밖으로 전파하지 않는다.

### 7.2 SQL-first에서 시작해 필요한 방향으로 성장한다

새로운 context는 `UseCase + Mapper + SQL`을 기본 출발점으로 삼는다. 시간이 지났다는 이유만으로 domain이 성숙했다고 판단하지 않는다. 여러 business rule이 상호작용하고, state transition과 같은 판단이 반복되며, 하나의 model이나 policy가 그 복잡성을 줄일 수 있다는 증거가 생겼을 때 domain logic을 추가한다.

```text
초기
Presentation -> UseCase -> Mapper -> Database

business complexity 증가
Presentation -> UseCase -> Domain Model / Policy
                        -> Mapper -> Database

SQL complexity 증가
Presentation -> UseCase -> Capability-specific Mapper -> Database
```

domain logic이 생겨도 concurrency를 막는 SQL 조건을 제거하지 않을 수 있다. 예를 들어 domain policy가 참여 가능 여부를 설명하고, conditional update의 `WHERE`가 동시에 들어온 요청 사이에서 정원 초과를 막을 수 있다. 이 경우 같은 규칙이 application과 SQL에 의도적으로 이중 구현된다.

중복을 무조건 제거하지는 않지만 역할은 구분한다.

- Domain Model / Policy는 business decision과 구체적인 실패 이유를 표현한다.
- SQL의 predicate, constraint와 lock은 실제 write의 원자성과 최종 consistency를 보장한다.
- 두 구현의 경계값과 상태 조건은 integration test로 일치함을 검증한다.
- 규칙을 변경할 때 domain logic, SQL과 관련 테스트를 하나의 변경 단위로 검토한다.

반대로 SQL의 조건이 동시성 보장과 무관하고 application policy를 단순 복사할 뿐이라면 중복하지 않는다. 이중 구현은 correctness를 위해 두 실행 지점이 모두 필요한 경우에만 감수한다.

## 8. Database invariant와 transaction

동시에 들어오는 요청 앞에서 애플리케이션의 사전 검증만으로는 invariant를 보장할 수 없다. DB가 더 정확하게 보장할 수 있는 규칙은 DB를 최종 방어선으로 사용한다.

- 필수 값은 `NOT NULL`로 보호한다.
- 중복 불가 규칙은 `UNIQUE`로 보호한다.
- 참조 무결성은 `FOREIGN KEY`로 보호한다.
- 경쟁 상태가 있는 변경은 atomic SQL, conditional update 또는 적절한 lock을 사용한다.

request의 형식, 범위와 필수 조합은 Bean Validation으로 진입 시점에 검증한다. 우리 서비스가 결정하는 business rule은 use case 또는 domain policy에서 검증한다. `CHECK` constraint는 같은 검증을 DB에 중복 구현하므로 기본적으로 사용하지 않는다.

이 선택은 모든 write가 애플리케이션이 관리하는 검증 경로를 통과한다는 전제를 가진다. HTTP가 아닌 message, scheduler, batch 같은 진입점도 동일한 application validation과 policy를 거쳐야 하며, 운영 SQL이나 외부 writer가 table을 직접 변경하지 않도록 통제한다. `NOT NULL`, `UNIQUE`, `FOREIGN KEY`, atomic update와 lock처럼 Bean Validation만으로 보장할 수 없는 구조적 무결성과 동시성 수단은 계속 DB가 담당한다.

예를 들어 모집 인원 제한은 조회 후 Java에서 증가시키는 방식보다 다음과 같은 atomic update로 보호할 수 있다.

```sql
UPDATE recruitment
SET current_count = current_count + 1
WHERE id = :id
  AND current_count < capacity;
```

영향받은 row 수는 use case가 이해할 수 있는 성공 또는 정원 초과 결과로 해석한다. constraint 위반도 persistence Mapper에서 분류해 use case가 이해할 수 있는 실패로 변환한다.

transaction은 command use case 단위로 명시한다. annotation이 있다는 사실보다 어떤 변경이 함께 commit되고, lock을 언제 얻으며, 외부 호출 전후에 어떤 상태가 관찰되는지를 설명할 수 있어야 한다.

외부 네트워크 호출은 local DB transaction에 자동으로 참여하지 않는다. 둘 사이의 일관성이 필요하면 outbox, idempotency key, retry, compensation 또는 workflow 분리를 검토한다.

## 9. Query와 cross-domain ownership

query는 domain entity를 복원하기 위한 과정이 아니다. 사용자가 필요로 하는 결과를 만들기 위해 `JOIN`, `GROUP`, `FILTER`, `PROJECTION`, `AGGREGATION`을 적극적으로 사용한다. 마이페이지나 통계처럼 여러 context의 데이터를 조합하는 조회는 DTO projection으로 바로 반환할 수 있다.

하나의 DB를 공유하는 monolith에서도 table의 logical ownership은 명확해야 한다.

```text
Cross-domain READ  -> 허용할 수 있다.
Cross-domain WRITE -> owning context만 수행한다.
```

cross-domain read는 결합 비용, 성능, 접근 권한을 검토한 뒤 허용한다. query는 화면 이름으로 새로운 context를 만들기보다 결과의 business outcome을 가장 자연스럽게 소유하는 context에 둔다. 어떤 context도 자연스럽게 소유하지 않고 독립적인 언어와 변경 주기가 생길 때만 read-oriented context를 검토한다.

같은 DB를 사용하는 monolith에서는 owning context가 다른 table을 직접 join해 projection을 만들 수 있다. schema가 분리되어도 같은 DB라면 cross-schema read 또는 owner가 공개한 view를 사용할 수 있다. DB가 물리적으로 분리되면 runtime distributed join을 기본값으로 삼지 않고, 최신성과 처리량에 따라 API composition 또는 event/CDC 기반 replicated read model을 선택한다.

다른 context가 소유한 table을 직접 변경하지 않는다. 변경은 owner의 use case, 명시적인 contract 또는 event를 통한다. snapshot이나 read model처럼 의도적으로 복제한 데이터는 ownership을 별도로 정의한다.

## 10. Selective Domain Modeling

모든 bounded context는 자신의 업무 개념, data ownership과 policy를 직접 모델링한다. 그러나 그 결과가 항상 rich domain model이어야 하는 것은 아니다. 예를 들어 Post의 공개 범위, 수정 권한, 삭제 정책과 검색 규칙은 직접 정의해야 하지만, 규칙이 단순하다면 Use Case, Policy, SQL과 constraint만으로 충분할 수 있다.

domain model은 기본 folder를 채우기 위해 만들지 않는다. 다음과 같은 business complexity를 실제로 줄일 때 도입한다.

- 여러 business rule이 상호작용한다.
- 허용되는 state transition이 복잡하다.
- 같은 개념을 둘러싼 `if`가 여러 use case에 반복된다.
- 계산과 판단에 담긴 business knowledge를 하나의 model로 압축할 수 있다.
- 객체가 자신의 invariant를 유지하게 하는 편이 절차적 흐름보다 이해하기 쉽다.

복잡한 주문은 좋은 후보가 될 수 있다. 결제 후 가격 변경 금지, 배송 후 취소 금지, 환불 상한, 구매 가격 보존처럼 여러 규칙이 함께 움직인다면 `order.cancel()`, `order.calculateRefund()`, `order.canShip()` 같은 행위가 cognitive complexity를 낮춘다.

그렇더라도 Order가 User, Product, Inventory, Coupon, Payment, Shipping 전체를 품게 만들지 않는다. `PlaceOrder` use case가 여러 context와 port를 조율하고, Order model은 자신의 invariant만 보호한다.

### 10.1 Snapshot

다른 context의 현재 객체를 오래 참조하는 대신 특정 시점의 사실을 보존해야 할 때가 있다. 주문은 상품 객체 전체를 참조하기보다 `productId`, `productName`, `purchasePrice`, `quantity`를 구매 시점 snapshot으로 보존할 수 있다. snapshot은 중복 데이터가 아니라 역사적 사실이며, 원본 변경과 독립적인 ownership을 가진다.

## 11. JPA는 금지가 아니라 비기본값이다

JPA가 제공하는 dirty checking, persistence context, lazy loading, cascade, flush timing과 entity state는 생산성을 높일 수 있지만 실행 동작을 추론해야 하는 비용도 만든다. 이 가치와 비용은 모든 context에서 같지 않다.

기본 persistence는 jOOQ를 사용한다. 다만 aggregate lifecycle이 강하게 묶여 있고 object mapping이 business model을 실제로 단순하게 만든다면 제한된 context에서 JPA를 선택할 수 있다.

한 프로젝트에서 다음과 같이 혼합하는 것도 허용한다.

```text
Write: JPA + Domain Model
Read:  jOOQ + SQL Projection
```

JPA를 도입할 때는 적용 경계, transaction 공유 방식, query 전략, hidden behavior의 검증법과 철회 조건을 ADR로 기록한다. 기술 통일보다 문제 적합성을 우선하되, 선택에 따른 인지 비용은 명시적으로 부담한다.

## 12. External system에는 port를 선택적으로 사용한다

OAuth/OIDC provider, payment gateway, SMS, email, cloud service, Kafka, shipping API처럼 우리가 통제할 수 없는 시스템에는 abstraction의 가치가 높다.

use case는 자신의 언어로 필요한 capability를 port에 정의한다. `infra/client`의 구현체는 외부 SDK와 wire format, 인증, timeout, retry, rate limit, protocol error를 처리한다. provider DTO와 예외는 내부로 누출하지 않는다.

port는 외부 API 전체를 복사하지 않고 use case가 실제로 필요한 최소 행위를 표현한다. abstraction의 이유는 형식적인 DDD가 아니라 실제 시스템 경계와 독립적으로 변하는 실패 모드가 존재하기 때문이다.

port는 protocol별 package를 만들기 위한 분류 수단이 아니다. 예를 들어 `SocialIdentityProvider`는 OIDC 자체를 추상화하기보다 “외부 identity를 검증해 내부에서 신뢰할 수 있는 결과를 반환한다”는 capability를 표현한다. Apple, Google, Kakao의 protocol과 provider 차이는 infrastructure client가 처리하며, use case가 서로 다른 capability를 요구할 때만 port를 나눈다.

## 13. 표준화된 문제는 다시 발명하지 않는다

인증과 보안의 복잡성은 business domain보다 protocol과 security에 가까운 경우가 많다. OAuth, OIDC, JWT, PKCE, nonce, state, issuer, audience, signature, refresh token과 암호화는 검증된 표준과 library를 따른다.

예를 들어 social login use case는 다음 orchestration을 드러낸다.

```text
LoginWithSocial
  -> SocialIdentityProvider port
  -> external identity verification
  -> local user lookup or creation
  -> social account linking
  -> session or token creation
```

OIDC 검증 세부 사항은 `infra/security` 또는 `infra/client`에 둔다. use case가 표준 protocol 내부를 domain model로 다시 해석하지 않는다. 직접 구현할 수 있다는 사실은 직접 구현해야 한다는 근거가 아니다.

## 14. 기본 package 방향

```text
com.example.lab
├── module                       # bounded context를 묶는 단일 애플리케이션 영역
│   ├── auth
│   │   └── infra
│   │       └── security         # JWT, Spring Security와 browser 보안 구현
│   └── <context>
│       ├── presentation         # web, message, scheduler 진입점
│       ├── usecase              # 처음에는 평평하게 유지
│       ├── domain               # 빈 구조로 제공하고 필요할 때 사용
│       └── infra
│           ├── persistence      # concrete jOOQ Mapper
│           ├── client           # context가 소유하는 외부 연동 adapter
│           ├── messaging
│           └── security
├── external                     # 외부 시스템의 기술 client와 provider 계약
└── global                       # context에 속하지 않는 공통 기술 계약과 구현
```

빈 package는 사용 가능한 구조와 확장 방향을 보여주기 위해 template에 제공한다. 비어 있다는 이유로 class나 interface를 채우지 않는다. 각 context의 내부 layout은 복잡성의 출처에 따라 달라질 수 있으며, 모든 context에 같은 전술 패턴을 강제하지 않는다.

구조를 정하기 전에 복잡성을 네 가지로 분류한다.

| 복잡성 | 주요 수단 |
|---|---|
| Industry / Protocol | Standard, Library, Infrastructure Client |
| Business Policy | Use Case, Domain Model, Domain Policy |
| Data Consistency | SQL, Constraint, Transaction |
| Orchestration | Use Case, Workflow |

`module`은 단일 Gradle module 안의 bounded context를 한곳에서 식별하기 위한 package다. Gradle multi-module을 의미하지 않는다. `presentation`에는 controller, request/response, message listener와 scheduler처럼 들어오는 protocol을 처리하는 type을 둔다. Entity, Value Object, Domain Service와 Domain Policy는 `domain`에 둔다. use case의 입력·출력과 orchestration 전용 type은 `usecase`에 둔다. `infra`에는 jOOQ, context가 소유하는 외부 연동 adapter, messaging과 security의 구체적인 기술 구현을 둔다.

여러 값을 함께 전달한다는 이유만으로 type을 `domain`에 두지 않는다. 값들이 하나의 business concept와 invariant를 표현하면 Value Object로 모델링해 `domain`에 둔다. use case 실행 결과와 orchestration을 위한 값은 `usecase`가 소유하고, HTTP request/response와 JSON 표현 구조는 `presentation`이 소유한다. SQL 조회 결과는 사용 목적과 mapping boundary에 따라 `usecase`의 query result 또는 `infra/persistence`의 projection으로 표현한다. 외부 provider의 payload는 해당 infrastructure boundary 밖으로 전파하지 않는다.

따라서 domain Value Object, use case result, query projection과 presentation DTO는 같은 모양이더라도 서로 대체하지 않는다. protocol이나 화면 구조의 변경이 domain model을 변경하게 만들지 않고, domain model도 JSON 계층 구조나 persistence projection에 맞추어 변형하지 않는다.

policy에는 `PostPolicy`처럼 context 전체를 포괄하는 이름보다 `PostPublicationPolicy`, `OrderCancellationPolicy`처럼 구체적인 business concept의 이름을 붙인다. 새로운 policy와 package 이름은 AI agent가 독자적으로 확정하지 않고 의미, owner, 사용처와 대안을 먼저 제시한다.

`global`은 중복 코드를 임시로 옮기는 장소가 아니다. 특정 context가 소유하지 않는 HTTP 응답, 공통 오류 계약과 요청 로깅처럼 애플리케이션 전체에 같은 의미로 적용되는 기술 관심사만 둔다. business rule, use case, domain policy와 특정 context가 의미를 정하는 오류는 `global`에 두지 않는다.

인증·인가의 owner는 `auth` context다. 요청 앞단에서 모든 context에 적용되더라도 JWT claim, 인증 주체, 권한과 refresh session의 의미는 `auth`가 정한다. Spring Security filter와 token 검증 같은 protocol 구현은 `module/auth/infra/security`에 두며, 공통 응답 계약을 사용하기 위한 `module -> global` 의존만 허용한다. `global`은 `module`을 알지 않는다.

`external`은 S3, email provider, Slack과 public API처럼 애플리케이션 밖의 시스템을 호출하는 기술 client를 둔다. 파일 전송과 삭제, 이메일과 메시지 전송, 외부 API 요청처럼 provider protocol에 가까운 동작에만 집중한다. business rule, use case, domain 용어와 context별 orchestration을 소유하지 않는다.

외부 연동 adapter는 사용하는 context의 `infra`가 소유한다. adapter는 context가 정의한 port를 구현하고 domain 의미를 외부 요청으로 변환한 뒤 `external`의 기술 client를 호출한다. `external` client는 context의 port를 직접 구현하지 않고 `module`을 알지 않는다.

```text
module/<context>/usecase port
  <- module/<context>/infra adapter
     -> external technical client
        -> external system
```

예를 들어 `external/storage/s3`는 bucket, key와 content를 받아 파일을 전송하거나 삭제할 수 있지만 분실물 이미지나 사용자 프로필 이미지의 의미는 알지 않는다. 파일 경로, metadata, 실패 해석과 호출 시점은 해당 context의 adapter와 use case가 결정한다. 여러 context가 같은 provider를 사용해도 business 의미가 다른 port와 adapter를 편의를 위해 하나로 합치지 않는다.

## 15. 테스트 전략

테스트의 검증 대상, 실행 범위, 목적과 작성 형식은 [Test Guide](test-guide.md)를 따른다.

API 문서는 Spring REST Docs 테스트 결과를 source of truth로 사용한다. 같은 테스트에서 `restdocs-api-spec`용 snippet을 생성해 OpenAPI 명세로 변환하고, Swagger UI는 이 명세만 읽는다. production controller에 문서 생성을 위한 annotation을 추가하거나 별도의 OpenAPI 명세를 함께 관리하지 않는다.

## 16. AI-native 작업 방식

AI agent는 구현자이면서 비판자다. 구현을 생성하는 agent와 별도로 원칙의 실패 가능성을 검토하게 할 수 있다.

```text
AI implementation
  -> independent architecture critique
  -> human judgment
  -> ADR
  -> architecture refinement
```

critique는 complex aggregate, distributed transaction, high-throughput batch, read-heavy workload, multi-module monolith, MSA migration, complex authorization처럼 현재 원칙이 흔들릴 조건을 구체적으로 다룬다. AI의 동의는 검증이 아니다. 최종 판단과 trade-off 수용은 사람이 담당한다.

## 17. 아키텍처의 성공 기준

이 아키텍처가 성공했는지는 class 수나 pattern 준수율로 판단하지 않는다. 새로운 사람이 핵심 use case를 열고 제한된 범위 안에서 다음 질문에 답할 수 있는지를 본다.

- 데이터는 어디에서 들어와 어디로 나가는가?
- 어떤 row를 어떤 조건으로 읽고 변경하는가?
- invariant의 최종 보장 지점은 어디인가?
- transaction과 side effect의 순서는 무엇인가?
- 어떤 hidden behavior와 운영 위험이 남아 있는가?
- 변경의 owner와 영향받는 boundary는 어디인가?

답을 찾기 위해 여러 추상 계층과 runtime magic을 추론해야 한다면, 코드가 짧더라도 이 아키텍처의 목표를 달성하지 못한 것이다.

## 18. Code style과 lint

이 저장소는 Gradle project를 기본으로 하고 Spotless와 Palantir Java Format을 사용한다. lint 설정은 root `build.gradle`에 흩어놓지 않고 `lint.gradle`에 분리한다.

- `spotlessCheck`는 formatting 위반을 검증한다.
- `spotlessApply`는 formatting을 적용한다.
- Java source에는 Palantir Java Format, annotation formatting, unused import 제거, trailing whitespace 제거와 파일 끝 newline을 적용한다.
- format 결과를 개인 IDE 설정에 의존하지 않고 Gradle task로 재현할 수 있어야 한다.
