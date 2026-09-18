# ADR 0001: 회원가입의 PostgreSQL과 Redis 일관성

## 상태

승인

## 문제

회원가입은 user context가 PostgreSQL에 account를 생성하고 auth context가 Redis에 refresh session을 생성해야 완료됩니다. PostgreSQL transaction과 Redis 명령은 하나의 원자적 transaction으로 묶이지 않습니다.

회원가입 성공 시 즉시 로그인 상태를 시작해야 하며, Redis 장애가 발생했는데 account만 생성되는 결과는 허용하지 않습니다. 반대로 Redis 저장 후 PostgreSQL commit이 실패하면 browser에 전달되지 않은 refresh session이 Redis에 남을 수 있습니다.

## 선택지

1. PostgreSQL commit 후 Redis에 refresh session을 생성합니다.
2. PostgreSQL transaction 안에서 account를 추가한 뒤 Redis에 refresh session을 생성하고, Redis 오류가 발생하면 PostgreSQL을 rollback합니다.
3. 2번을 사용하면서 PostgreSQL rollback 뒤 Redis session의 보상 삭제와 제한된 retry를 수행합니다.
4. Redis transaction support로 Redis 명령을 PostgreSQL transaction 완료 시점까지 미룹니다.

## 결정

2번을 사용합니다.

```text
회원가입 정책 확인
  -> PostgreSQL account INSERT
  -> Redis refresh session 저장
  -> PostgreSQL commit
  -> access·refresh cookie 응답
```

Redis 저장이 실패하면 예외를 반환하고 PostgreSQL transaction을 rollback합니다. Redis 저장이 성공한 뒤 PostgreSQL commit이 실패하면 refresh session을 보상 삭제하거나 retry하지 않습니다. 이 session은 browser에 전달되지 않고 account 존재 여부 검증을 통과할 수 없으며, 최초 로그인에서 정한 TTL이 지나면 Redis가 삭제합니다.

token 원문과 token hash는 로그에 남기지 않습니다. PostgreSQL commit 실패는 session 식별자와 실패 단계로 관찰합니다.

Redis의 `MULTI`, `EXEC`, `DISCARD`는 Redis 내부 명령을 묶는 수단이며 PostgreSQL과 Redis 사이의 분산 transaction을 제공하지 않으므로 이 문제의 해결 수단으로 사용하지 않습니다.

## 예상 결과

- Redis 장애 시 account가 생성되지 않아 회원가입은 fail closed합니다.
- PostgreSQL commit 실패 시 사용 불가능한 refresh session이 TTL 동안 남을 수 있습니다.
- 보상 삭제와 retry가 없어 실패 경로와 운영 동작이 단순합니다.
- 장애 중 회원가입 시도가 많으면 Redis 메모리에 고아 session이 누적될 수 있습니다.

## 검증 방법

- Redis 저장 실패 시 account INSERT가 rollback되는지 integration test로 검증합니다.
- PostgreSQL commit 실패를 주입해 cookie가 발급되지 않는지 검증합니다.
- refresh 시 account가 없거나 활성 상태가 아니면 session을 거절하는지 검증합니다.
- 실제 refresh session 한 건의 Redis 메모리 사용량과 TTL을 운영 환경에서 확인합니다.

## 변경 조건

고아 session이 Redis 용량이나 운영 비용에 유의미한 영향을 주거나, 회원가입 처리량 때문에 PostgreSQL transaction 안의 Redis 호출이 병목이 되면 보상 삭제, 짧은 pending TTL과 commit 후 활성화 또는 별도 workflow를 검토합니다.
