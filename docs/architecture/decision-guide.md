# Architecture Decision Guide

[Architecture Overview](overview.md)의 원칙을 설계, 리뷰와 기존 시스템 분석에 적용한다.

## 복잡성 분류

| 복잡성 | 우선 선택 | 금지 사항 |
| --- | --- | --- |
| DB 정합성, 유일성, 동시성 | SQL, constraint, transaction | 사전 조회만으로 보장 |
| 업무 불변식과 상태 전이 | Domain Model / Policy | 단순 매핑용 모델 |
| Context와 작업 순서 조율 | Use Case | Entity나 Controller에 orchestration 숨기기 |
| 외부 시스템과 독립적인 실패 | Port / Infrastructure Client | provider API 복제 |
| OAuth, OIDC, 암호화 | 표준, 라이브러리, 프레임워크 | 직접 재구현 |
| JOIN, 집계와 projection | SQL / Query Model | write model 강제 |

Industry/Protocol, Business Policy, Data Consistency와 Orchestration을 위치별로 분류한다. 복잡성이 겹치면 수단을 조합한다.

## 기능 설계 순서

1. 사용자 의도를 Use Case 이름으로 표현한다.
2. 기능과 데이터의 owning Context를 정한다.
3. 입력과 출력, read/write, 외부 부수 효과를 적는다.
4. 함께 성공하거나 실패해야 하는 변경을 표시한다.
5. 불변식의 보장 위치를 정한다.
6. 영속성 Mapper와 필요한 외부 Port를 정한다.
7. 동시성, retry, idempotency와 실패 후 상태를 확인한다.
8. 가장 위험한 가정을 검증할 테스트를 선택한다.

```text
input -> presentation -> UseCase -> Domain / Mapper / External Port -> output
```

회원가입 사례: [`SignUpUseCase`](../../src/main/java/com/example/lab/module/auth/usecase/SignUpUseCase.java).

```text
HTTP -> user_account INSERT -> Redis session -> PostgreSQL commit -> cookie
```

실패 후 account, cookie와 session 상태는 [Security](../operations/security.md#회원가입의-실패-경계)와 [`SignUpUseCaseIntegrationTest`](../../src/test/java/com/example/lab/module/auth/usecase/SignUpUseCaseIntegrationTest.java)에서 확인한다. `save()` 호출만으로 원자성을 판단하지 않는다.

## 방어 로직

추측한 `null`, 잘못된 type, 과거 client, 미래 확장과 내부 오류를 위해 validation, fallback, retry나 호환 추상화를 추가하지 않는다.

다음 중 하나로 필요성이 확인된 경우에만 추가한다.

- 신뢰할 수 없는 입력이나 외부 응답
- nullable type, API 명세나 라이브러리 계약에 정의된 상태
- 업무 규칙에 정의된 실패
- 운영 장애, 재현 가능한 버그나 테스트로 확인된 실패
- 보안, 프로토콜이나 동시성의 최종 방어선

| 확인 항목 | 기록할 내용 |
| --- | --- |
| 발생 경로 | 해당 상태에 도달하는 실제 입력과 호출 |
| 분류 | 정상 입력, 업무 실패, 외부 장애, programming error |
| 책임 | 현재 경계가 처리할 owner인지 |
| 실패 결과 | 처리하지 않을 때 발생하는 구체적인 문제 |

검증된 입력, non-null 내부 type, DB 제약과 Mapper 계약은 해당 경계에서 신뢰한다. 내부 계약 위반은 기본값으로 숨기지 않고 스키마, projection, type이나 호출 경로를 수정한다.

업무 결과, 데이터 계약이나 아키텍처를 바꾸는 미확정 조건은 [AGENTS.md](../../AGENTS.md#문서에-없는-정책)를 따른다. 결과가 같은 구현 선택은 가장 단순한 형태로 정한다. 이 기준으로 type, DB, 프레임워크와 외부 프로토콜의 검증을 생략하지 않는다.

## Bounded Context 결정

- 업무 용어의 의미, 스키마와 생명주기는 누가 소유하는가?
- 불변식 변경 권한은 누구에게 있는가?
- 같은 이유와 주기로 변경되는 기능은 무엇인가?
- 다른 Context의 장애와 독립적으로 동작해야 하는가?

기술이나 DB가 같다는 이유로 합치거나 작은 차이마다 분리하지 않는다. 언어, 규칙과 write 소유권을 기준으로 판단한다.

### Context 간 조회

[소유권 원칙](overview.md#query와-context-간-소유권)에 따라 스키마 결합도, 접근 권한, JOIN 비용과 Snapshot/read model 필요성을 확인한다.

| 데이터 배치와 요구 | 선택 |
| --- | --- |
| 단일 DB | owning Context의 SQL JOIN과 projection |
| 동일 DB의 분리된 스키마 | cross-schema read 또는 owner의 공개 view |
| 물리적으로 분리된 DB, 강한 최신성과 낮은 호출량 | API composition |
| 물리적으로 분리된 DB, 높은 조회량과 장애 격리 | event/CDC 기반 local read model |

## Use Case 분리

```text
// 비권장
RecruitmentService.create/update/delete/join/cancel/close/search

// 권장
CreateRecruitmentUseCase
JoinRecruitmentUseCase
CancelRecruitmentUseCase
CloseRecruitmentUseCase
GetRecruitmentUseCase
SearchRecruitmentUseCase
```

트랜잭션, 인가, 부수 효과, 실패 방식이나 사용자 의도가 다르면 class를 분리한다. 패키지까지 함께 나누지는 않는다.

다음 조건이 실제로 생기면 패키지 확장안을 제안한다.

- 한 Use Case의 입출력과 협력 type이 3개 이상이다.
- 전용 구현에 package-private 격리가 필요하다.
- 독립적인 하위 capability가 업무 언어로 식별된다.
- 관련 파일 탐색이나 동시 변경 충돌이 반복된다.

파일 수만으로 재구성하지 않는다. 문제의 근거, 대안과 비용을 제시하고 사용자와 합의한 뒤 확장한다.

## Persistence Mapper 결정

기본 위치와 책임은 [Persistence Mapper](overview.md#persistence-mapper)를 따른다. SQL 실행 객체에 `*RecordMapper` 이름을 사용하지 않는다.

실제 사례: [`UserAccountMapper`](../../src/main/java/com/example/lab/module/user/infra/persistence/UserAccountMapper.java).

```java
int updated = dsl.update(USER_ACCOUNT)
        .set(USER_ACCOUNT.STATUS, UserAccountStatus.WITHDRAWN.name())
        .set(USER_ACCOUNT.WITHDRAWN_AT, timestamp)
        .where(USER_ACCOUNT.ID.eq(accountId))
        .and(USER_ACCOUNT.STATUS.eq(UserAccountStatus.ACTIVE.name()))
        .execute();
return updated == 1;
```

Use Case는 영향받은 행 수를 업무 결과로 해석한다. `DSLContext`와 SQL을 Use Case로 이동하지 않는다.

### Mapper 분리

다음 조건이 실제로 생기면 capability 단위로 분리한다.

- 검색 조건과 projection이 독립적으로 복잡해지고 함께 변경된다.
- 통계가 별도의 성능, 인덱스와 운영 기준을 가진다.
- batch나 locking의 실행 특성이 다르다.
- 무관한 capability 때문에 하나의 Mapper가 반복 변경된다.

```text
PostMapper
PostSearchMapper
PostStatisticsMapper
```

statement별 class, 파일 수와 read/write 구분만으로 분리하지 않는다. 메서드 이름은 `incrementParticipant`, `publish`, `findDetail`처럼 데이터 동작을 드러낸다.

분리한 Mapper에는 실제 DB 통합 테스트와 필요한 실행 계획 검증을 둔다. 트랜잭션과 orchestration은 Use Case에 유지한다.

## Domain Model 결정

다음 질문에 여러 개가 명확하게 참이면 도입한다.

- 여러 규칙의 순서와 조합이 중요한가?
- 복잡한 상태 전이를 한곳에서 보호해야 하는가?
- 같은 조건이 여러 Use Case에서 반복되는가?
- 모델이나 정책이 업무 지식을 더 명확히 표현하는가?
- 유효한 상태를 유지하는 객체가 절차적 코드보다 명확한가?
- DB 제약만으로 표현하기 어려운 불변식이 있는가?

단순 CRUD, 한두 column 변경, DTO, payload mapping, getter/setter나 미래 복잡성만으로 도입하지 않는다. 프로젝트 기간과 파일 수를 성숙도 기준으로 삼지 않는다. SQL-first 흐름은 유지하고 필요한 Domain Model/Policy만 추가한다.

### 반환 타입

| 값의 의미 | 위치 |
| --- | --- |
| 업무 개념과 불변식 | `domain`의 VO |
| Use Case 결과와 orchestration | `usecase`의 result |
| HTTP 요청, 응답과 JSON | `presentation`의 DTO |
| SQL 조회 결과 | `usecase`의 query result 또는 `infra/persistence`의 projection |
| provider payload | 해당 `infra/client` 또는 `external`의 DTO |

- 반환 편의만을 위한 `Map`, 배열과 임시 내부 `record`는 만들지 않는다.
- 독립된 책임은 메서드 분리를 검토한다.
- 하나의 SQL Snapshot, 트랜잭션이나 업무 판단으로 함께 반환할 값은 이름 있는 result로 둔다. 호출을 억지로 나누지 않는다.
- 필드가 같아도 경계와 변경 이유가 다르면 별도 타입을 유지한다.

### Domain 이름

```text
// 비권장
PostPolicy

// 권장
PostPublicationPolicy
PostEditingPolicy
OrderCancellationPolicy
```

호출 Use Case보다 소유하는 업무 개념을 기준으로 이름을 정한다. 새로운 Domain abstraction은 의미, owner, 사용처와 대안 이름을 제시하고 사용자와 합의한다.

### 식별자와 Policy

[조회와 Policy 분리](overview.md#조회와-policy-분리)를 따른다. SQL의 JOIN, predicate와 인덱스는 식별자별로 유지한다.

```text
email 가입  -> email로 User 조회       -> RejoinPolicy(User)
social 가입 -> provider ID로 User 조회 -> RejoinPolicy(User)
```

항상 같은 업무 실패로 거절하면 `ensureRejoinAllowed`처럼 실패를 직접 표현할 수 있다. email과 provider ID의 동일인 및 재가입 제한 단위가 미확정이면 공통 identifier부터 만들지 않는다. 정책과 스키마 변경을 포함해 합의한다.

### Domain과 SQL의 이중 구현

[책임 구분](overview.md#domain-policy와-sql)을 따르며 다음을 확인한다.

- 이중 구현 목적을 코드나 테스트 이름에 드러낸다.
- 같은 경계값과 상태 조합을 Domain 및 DB 통합 테스트로 검증한다.
- 영향받은 행 수를 동시성 충돌이나 업무 실패로 해석한다.
- 규칙 변경 시 Policy, SQL과 테스트를 함께 검토한다.

### Snapshot 결정

당시 값을 보존해야 하거나 원본 변경으로 과거 기록의 의미가 달라지면 Snapshot을 검토한다. 계약, 결제, 감사와 정산의 근거도 해당한다. 생성 시점과 owner를 정한다.

## DB 불변식 결정

| 규칙 | 최종 방어선 | 애플리케이션 책임 |
| --- | --- | --- |
| 필수 값 | `NOT NULL` | 입력 검증 메시지 |
| 유일성 | `UNIQUE` | 사전 확인은 UX 최적화 |
| 참조 무결성 | `FOREIGN KEY` | 생명주기와 삭제 정책 |
| 요청 형식, 범위와 필수 조합 | Bean Validation | 진입점 검증 |
| 서비스 업무 규칙 | Use Case / Domain Policy | 업무 판단과 거절 |
| 제한된 수량의 동시 변경 | conditional atomic update | 행 수를 성공/충돌로 해석 |
| 경쟁 상태 전이 | CAS, version 또는 lock | 실패와 재시도 정책 |
| 여러 행에 걸친 규칙 | transaction과 필요한 lock | 처리 순서와 실패 결과 |

- `CHECK`는 기본적으로 사용하지 않는다. 모든 write가 [애플리케이션 검증 경로](overview.md#db-불변식과-트랜잭션)를 통과해야 한다.
- DB 제약과 동시성 제어를 Bean Validation으로 대체하지 않는다.
- check-then-act는 경쟁을 확인하고 가능하면 제약이나 하나의 conditional statement로 합친다.
- 제약에는 운영 중 식별할 이름을 붙인다. 기술 예외는 원인을 분류해 업무 실패로 변환한다.

비밀번호 변경 사례: [`UserAccountMapper.updatePassword`](../../src/main/java/com/example/lab/module/user/infra/persistence/UserAccountMapper.java).

```sql
UPDATE user_account
SET password_hash = :new_hash, updated_at = :now
WHERE id = :account_id
  AND status = 'ACTIVE'
  AND password_hash = :current_hash;
```

영향받은 행이 0개면 사전 조회 이후 상태나 비밀번호가 변경됐을 수 있다.

## 트랜잭션 결정

1. 함께 commit할 DB 변경을 적는다.
2. command Use Case를 트랜잭션 경계로 삼는다.
3. lost update, write skew, phantom과 duplicate insert를 확인한다.
4. 제약, atomic SQL, optimistic version, pessimistic lock과 isolation 중 최소 수단을 선택한다.
5. lock 획득 순서와 트랜잭션 길이를 확인한다.
6. 즉시 실패, 제한된 retry와 사용자 재시도 중 충돌 처리를 정한다.
7. 외부 부수 효과 전후 상태를 기록한다.

`@Transactional`만으로 원자성을 가정하지 않는다. datasource, connection, propagation, rollback 대상과 flush 시점을 확인한다.

외부 호출 전에는 다음을 확인한다. 답을 정하지 못하면 DB 트랜잭션 안에 넣지 않는다.

- 외부 지연 시 DB lock 유지 시간
- 외부 성공 후 local commit 실패 결과
- 재시도 시 중복 실행 결과
- idempotency key, outbox와 compensation 필요성

회원가입의 실제 실패 경계는 [Security](../operations/security.md#회원가입의-실패-경계)를 따른다.

## Query 결정

Domain 동작 없이 JOIN, GROUP, FILTER, 집계와 화면 shape가 핵심이면 SQL projection을 우선한다.

필요한 column, 인덱스, 결정적 정렬, pagination, 데이터 증가, 실행 계획, 개인정보와 Context 간 read 권한을 검토한다. 재사용을 위해 의미가 다른 쿼리를 하나로 합치지 않는다.

## JPA 도입

[기본 원칙](overview.md#jpa)에 따라 도입 전 다음을 검토하고 기록한다.

- 단순해지는 Aggregate 생명주기, object graph와 업무 복잡성
- lazy loading, dirty checking, persistence context, cascade와 flush 통제
- N+1과 예상 밖의 쿼리 관찰
- Entity의 HTTP 및 Context 경계 누출 방지
- jOOQ와 connection 및 transaction 공유
- JPA write와 jOOQ read 분리 필요성
- 철회 조건과 비용

단순 CRUD가 많거나 SQL 작성이 번거롭다는 이유만으로 도입하지 않는다. 필요한 Context나 Aggregate로 한정한다.

## 외부 연동 검토

[Port와 Client 책임](overview.md#외부-연동과-표준-기술)에 따라 다음을 확인한다.

- timeout과 재시도할 오류
- 멱등성이나 중복 탐지
- provider DTO와 예외의 경계 격리
- rate limit과 부분 장애 관찰
- 계약 변경을 감지할 테스트

안정적인 내부 순수 함수를 테스트 편의만으로 interface로 만들지 않는다. 서비스 고유 인가는 명시적 Policy로 두고, 표준 프로토콜 구현은 infrastructure에 둔다.

## 대표 기능의 시작점

| 기능 | 시작점 |
| --- | --- |
| 게시물 CRUD | Use Case + SQL |
| 검색, 마이페이지, 통계 | SQL projection, 필요한 Context 간 read |
| 좋아요 | `UNIQUE` + SQL |
| 모집 인원 증가, 재고 차감 | atomic SQL + 필요한 transaction |
| 비밀번호 변경 | Use Case + 검증된 password hashing |
| 소셜 로그인 | Use Case + Identity Port/Client |
| JWT 검증 | Security Infrastructure |
| 서비스 고유 권한 | Security + Policy |
| 장바구니 | SQL-first, 규칙 증가 시 모델 검토 |
| 주문, 환불, 정산 | Domain Model/Policy 검토 |
| 결제, PG 연동 | Use Case + Payment Port/Client |
| 복잡한 할인 | Domain Policy |

최종 설계는 실제 불변식과 workload에 따라 선택한다. 테스트 범위는 [Test Guide](../contributing/testing.md)를 따른다.

## 코드 리뷰

- Use Case의 흐름, 트랜잭션, SQL 위치와 부수 효과 순서가 보이는가?
- write 대상과 owner가 일치하고 불변식의 최종 방어선이 명확한가?
- 불필요한 모델, Repository, service와 interface가 없는가?
- 이름과 구조 변경을 사용자와 합의했는가?
- Domain Model이 업무 복잡성을 줄이는가?
- 외부 SDK, DTO와 예외가 경계 밖으로 누출되지 않는가?
- 표준 기술을 재구현하지 않았는가?
- 방어 로직에 실제 발생 경로와 owner가 있는가?
- 기본값으로 계약 위반을 숨기지 않는가?
- 프레임워크의 암묵적 동작을 관찰하고 검증하는가?
- 변경과 검증 범위가 좁고 명확한가?

## 기존 시스템 분석

1. capability, 업무 언어와 Bounded Context 후보를 찾는다.
2. table, schema와 write owner를 표시한다.
3. 진입점부터 DB와 외부 호출까지 추적한다.
4. 트랜잭션, flush, lock과 부수 효과 순서를 적는다.
5. 불변식 보장 위치를 확인한다.
6. SQL, 실행 계획과 객체 매핑의 차이를 기록한다.
7. Context 간 write, 공유 table과 암묵적 의존성을 찾는다.
8. timeout, retry, idempotency와 장애 복구 경로를 확인한다.
9. 문서와 runtime 차이를 기록한다.
10. 가장 위험한 Use Case부터 개선안을 작성한다.

결과는 framework 목록 대신 flow, ownership, invariant, transaction과 failure mode로 정리한다.

## 결정 변경과 반론

다음 변경은 근거, 대안, 결과, 검증법과 철회 조건을 검토하고 문서화한다.

- Bounded Context와 데이터 owner
- JPA와 새 영속성 방식
- 전역 transaction과 isolation
- Context 간 동기 호출과 event 협력
- outbox, saga와 compensation
- 중요한 외부 시스템과 retry/idempotency
- 공통 추상화와 전역 framework
- 장기적인 아키텍처 예외

복잡한 Aggregate, 분산 트랜잭션, 고처리량 batch, read-heavy workload, multi-module, MSA 전환과 복잡한 인가 조건으로 반론한다. AI 검토는 판단 자료로만 사용하며 최종 결정과 trade-off는 사람이 책임진다.

포맷 검증은 [Git Guide](../contributing/git.md#커밋-전-확인-사항)를 따른다.
