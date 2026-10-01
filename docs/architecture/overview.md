# Explicit Data-Flow Architecture

아키텍처 원칙의 원본이다. 선택 조건은 [Decision Guide](decision-guide.md)를 따른다.

## 기본 원칙

- Strategic DDD로 Bounded Context, Ubiquitous Language와 데이터 소유권을 정의한다.
- 애플리케이션 진입점은 사용자 의도별 `*UseCase`로 둔다.
- 영속성은 jOOQ와 SQL-first를 기본으로 한다.
- 트랜잭션 경계, SQL, 부수 효과와 실패 처리 순서를 코드에 드러낸다.
- 정합성은 DB 제약과 원자적 연산으로 보장한다.
- Domain Model은 실제 업무 복잡성을 줄일 때 도입한다.
- 통제할 수 없는 외부 경계에는 선택적 Port를 사용한다.
- 인증과 암호화는 표준과 검증된 라이브러리를 사용한다.

완성된 프레임워크나 운영용 starter가 아닌 아키텍처 검증용 저장소다.

```text
가설 -> 구현 -> 문제 발견 -> 검토 -> 규칙 갱신 -> 템플릿 개선
```

## 실행 흐름과 의존성

```text
HTTP / Message / Schedule
  -> Presentation
  -> UseCase [Transaction Boundary]
       -> Domain Model / Policy       필요한 경우
       -> Persistence Mapper -> DB
       -> External Port -> Infrastructure Adapter -> External Client
  -> Result
```

- `presentation`은 프로토콜 처리, Use Case는 업무 조율을 담당한다.
- Use Case는 Domain과 concrete Mapper를 직접 사용할 수 있다. 모든 입출력을 Port로 감싸지 않는다.
- 외부 연동 Adapter는 Use Case가 소유한 Port를 구현한다.
- Domain은 Spring, presentation DTO, jOOQ generated type과 외부 SDK에 의존하지 않는다.
- Use Case에서 입력과 출력, read/write, SQL, 트랜잭션, 정책 적용 지점, 외부 호출과 실패 순서를 확인할 수 있어야 한다.

## Bounded Context와 Domain Model

전역 기술 계층 대신 업무 기능과 Bounded Context로 코드를 나눈다. 단순한 Context도 자신의 언어, 데이터와 write 경계를 소유한다.

모든 데이터에 Rich Entity, VO, Repository, Domain Service나 Aggregate method를 만들지 않는다. Domain Model의 도입 조건은 [Domain Model 결정](decision-guide.md#domain-model-결정)을 따른다.

Domain Model은 자신의 불변식만 보호한다. 여러 Context의 조율은 Use Case가 담당하며 다른 Context 전체를 Aggregate에 넣지 않는다.

```text
PlaceOrderUseCase -> User / Inventory / Payment Port 조율
Order            -> 취소, 환불, 배송 가능 조건 판단
```

### Snapshot

특정 시점의 사실은 생성 시점과 소유권을 정한 Snapshot으로 보존한다. 원본 변경에 따라 갱신하지 않는다.

```text
구매 시점 Snapshot: productId, productName, purchasePrice, quantity
```

## Use Case

- 이름은 사용자 의도를 표현하고 `UseCase` 접미사를 사용한다.
- Validation, 인가, 트랜잭션, 데이터 접근, Domain과 외부 호출 순서, 결과와 예상 실패를 담당한다.
- 교체할 구현이 없으면 interface와 `Impl`을 나누지 않는다.
- `usecase`는 평평하게 시작한다. 하위 패키지 확장은 [분리 기준](decision-guide.md#use-case-분리)을 확인하고 사용자와 합의한다.
- 이름만 다른 Use Case가 모든 동작을 generic service에 위임하지 않는다.

## SQL-first Persistence

```text
Spring Boot + jOOQ + Flyway + PostgreSQL 또는 MySQL
+ Spring Transaction + Testcontainers

Migration -> DB Schema -> jOOQ Code Generation -> Java Data Access
```

- Migration과 실제 스키마가 영속성의 원본이다. Generated code는 직접 수정하지 않는다.
- SQL에 column, join, filter, ordering, projection과 lock을 명시한다.
- 중요한 쿼리는 인덱스와 실행 계획을 검토한다.
- 운영 DB와 의미가 다른 in-memory DB로 SQL 정합성을 검증하지 않는다.
- `DSLContext`는 Mapper 안에서만 사용한다. jOOQ type과 record를 불필요하게 전파하지 않는다.

### Persistence Mapper

`module/<context>/infra/persistence`의 concrete `*Mapper`가 SQL 실행과 application type 매핑을 담당한다. MyBatis Mapper와 같은 SQL 진입점 의미이며 jOOQ `RecordMapper`와는 구분한다.

```text
RecruitmentMapper
  - incrementParticipant
  - close
  - findDetail

RecruitmentSearchMapper
  - search
```

Context의 핵심 영속성은 하나의 Mapper로 시작한다. capability별 분리는 [Mapper 분리 기준](decision-guide.md#mapper-분리)을 따른다. generic Repository, statement별 `*Sql` class, interface/구현체 쌍을 기본으로 만들지 않는다.

### Domain Policy와 SQL

```text
단순 기능       -> UseCase -> Mapper -> DB
업무 복잡성 증가 -> UseCase -> Domain Model / Policy -> Mapper -> DB
SQL 복잡성 증가  -> UseCase -> Capability-specific Mapper -> DB
```

동시성 때문에 Domain 판단과 SQL 조건이 모두 필요하면 이중 구현을 허용한다.

| 책임 | 보장 |
| --- | --- |
| Domain Model / Policy | 업무 판단과 거절 이유 |
| SQL predicate, constraint, lock | write 원자성과 최종 정합성 |

동일한 경계값과 상태 조합을 Domain 및 DB 통합 테스트로 검증한다. 정책, SQL과 테스트는 함께 변경한다. 동시성이나 정합성과 무관한 정책 복제는 하지 않는다.

### 조회와 Policy 분리

```text
email / provider ID -> 명시적 Mapper 조회 -> 동일 Domain 값 -> Policy
```

- Use Case가 조회, 트랜잭션과 호출 순서를 담당한다.
- Policy는 확보된 Domain 값, 시각과 명시적인 업무 입력만 판단한다. Mapper, SQL, SDK와 HTTP DTO를 호출하지 않는다.
- 항상 같은 업무 실패로 거절한다면 boolean 대신 실패를 직접 반환하거나 던질 수 있다.
- email과 provider ID가 같은 사람이나 권한을 뜻하는지는 업무 정책으로 확정한다. 공통 식별자로 임의 일반화하지 않는다.
- 식별 범위 변경은 정책, 스키마, Mapper와 테스트에 함께 반영한다.

## DB 불변식과 트랜잭션

DB 제약과 검증 위치는 [DB 불변식 결정표](decision-guide.md#db-불변식-결정)를 따른다.

- command Use Case에 트랜잭션을 명시한다. 함께 commit되는 변경, lock 획득 순서와 외부 호출 전후 상태를 확인한다.
- HTTP, message, scheduler와 batch의 모든 write는 application validation과 Policy를 통과해야 한다. 운영 SQL과 외부 writer의 직접 변경은 통제한다.
- local DB 트랜잭션에 외부 호출이 자동으로 참여하지 않는다. 일관성이 필요하면 outbox, idempotency key, retry, compensation이나 workflow 분리를 검토한다.

## Query와 Context 간 소유권

- 조회는 필요한 결과를 SQL JOIN, GROUP, FILTER, PROJECTION과 AGGREGATION으로 반환한다. Entity 복원을 강제하지 않는다.
- Context 간 read는 결합도, 성능과 접근 권한을 검토한 뒤 허용한다.
- write는 owner의 Use Case, contract 또는 event를 통한다. 다른 Context의 테이블을 직접 변경하지 않는다.
- Query는 업무 결과의 owner에 둔다. 화면 이름으로 Context를 만들지 않는다.
- 자연스러운 owner가 없고 독립적인 언어와 변경 주기가 생길 때만 read-oriented Context를 검토한다.
- 데이터 배치에 따른 조회 수단은 [Context 간 조회](decision-guide.md#context-간-조회)를 따른다.
- 복제한 Snapshot과 read model은 별도 소유권을 정의한다.

## JPA

기본값은 jOOQ다. Aggregate 생명주기가 강하게 묶이고 object mapping이 업무 모델을 단순화하면 제한된 Context에서 JPA를 사용할 수 있다.

```text
Write: JPA + Domain Model
Read:  jOOQ + SQL Projection
```

도입 전 [JPA 검토 기준](decision-guide.md#jpa-도입)을 확인하고 적용 경계, 검증법과 철회 조건을 문서화한다.

## 외부 연동과 표준 기술

- Port는 Use Case에 필요한 최소 capability를 표현한다. provider API 전체나 프로토콜별 분류를 복제하지 않는다.
- Context의 `infra/client` Adapter가 SDK, wire format, 인증, timeout, retry, rate limit과 오류 변환을 담당한다.
- provider DTO와 예외는 해당 infrastructure 경계 밖으로 누출하지 않는다.
- 여러 provider의 프로토콜 차이는 Adapter가 처리한다. Use Case가 다른 capability를 요구할 때만 Port를 나눈다.
- OAuth, OIDC, JWT, PKCE, nonce, state, issuer, audience, signature, refresh token과 암호화는 표준과 검증된 라이브러리를 따른다.
- Use Case는 표준 프로토콜을 Domain Model로 재구현하지 않는다.

```text
LoginWithSocial
  -> SocialIdentityProvider
  -> 외부 identity 검증
  -> local user 조회 또는 생성
  -> 계정 연결
  -> session / token 발급
```

## 패키지와 타입 소유권

```text
com.example.lab
├── module                       # 단일 Gradle module의 Bounded Context
│   ├── auth/infra/security      # 인증, JWT, Spring Security
│   └── <context>
│       ├── presentation         # HTTP, message, scheduler
│       ├── usecase              # 입력, 결과와 orchestration
│       ├── domain               # Entity, VO, Domain Service / Policy
│       └── infra
│           ├── persistence      # concrete jOOQ Mapper
│           ├── client           # Context의 외부 연동 Adapter
│           ├── messaging
│           └── security
├── external                     # provider 기술 Client
└── global                       # Context에 속하지 않는 공통 기술
```

빈 패키지는 확장 방향을 표시한다. 형식을 채우려고 class와 interface를 만들지 않는다. Context별 복잡성에 맞춰 내부 구조를 선택한다.

타입 위치는 [반환 타입 결정표](decision-guide.md#반환-타입)를 따른다. 같은 필드라도 Domain VO, Use Case result, Query projection과 presentation DTO는 서로 대체하지 않는다. Domain을 JSON이나 projection 구조에 맞춰 바꾸지 않는다.

Policy와 새 Domain abstraction의 이름은 [Domain 이름 결정](decision-guide.md#domain-이름)을 따르고 사용자와 합의한다.

### Global과 Auth

- `global`: Context가 소유하지 않는 공통 HTTP 응답, 오류 계약과 요청 로깅.
- 업무 규칙, Use Case, Domain Policy와 Context 고유 오류는 `global`에 두지 않는다.
- 인증과 인가는 `auth` 소유다. JWT claim, principal, 권한과 refresh session의 의미도 `auth`가 정한다.
- 인증 프로토콜 구현은 `module/auth/infra/security`에 둔다.
- 공통 기술 사용을 위한 `module -> global` 의존만 허용한다. `global`은 `module`을 알지 않는다.

### External Client와 Context Adapter

```text
module/<context>/usecase port
  <- module/<context>/infra adapter
     -> external technical client
        -> external system
```

- `external`은 S3, email provider, Slack과 외부 API의 기술 Client를 둔다. 업무 규칙, Domain 언어와 Context별 조율은 소유하지 않는다.
- Context Adapter는 Port를 구현하고 Domain 의미를 provider 요청으로 변환한다.
- 기술 Client는 Context Port를 직접 구현하거나 `module`에 의존하지 않는다.
- 파일 경로, metadata, 실패 해석과 호출 시점은 Context Adapter와 Use Case가 정한다.
- 같은 provider를 사용해도 업무 의미가 다른 Port와 Adapter를 합치지 않는다.

## 테스트와 API 문서

테스트 선택과 표현은 [Test Guide](../contributing/testing.md)를 따른다.

```text
Spring REST Docs 테스트 -> snippet -> restdocs-api-spec -> OpenAPI -> Swagger UI
```

REST Docs 테스트 결과가 HTTP 계약의 원본이다. production Controller의 문서 생성 annotation과 별도 OpenAPI 명세는 관리하지 않는다.

## 검토와 변경

중요한 선택은 [결정 변경 기준](decision-guide.md#결정-변경과-반론)에 따라 반론과 검증을 거친다. 최종 판단과 trade-off 수용은 사람이 담당한다.

검토 결과에는 data flow, ownership, SQL, 불변식, 트랜잭션, 부수 효과와 실패 경계를 드러낸다. class 수와 패턴 준수율을 성공 기준으로 삼지 않는다.

코드 표현은 [Code Style](../contributing/code-style.md), 포맷과 커밋 검증은 [Git Guide](../contributing/git.md)를 따른다.
