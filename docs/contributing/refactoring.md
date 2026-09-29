# 리팩터링

리팩터링은 사용자가 관찰하는 결과를 유지하면서 코드의 탐색과 검증 비용을 줄이는 변경이다. 업무 규칙이나 HTTP 계약을 바꾼다면 기능 변경 또는 버그 수정으로 다룬다.

## 1. 현재 비용을 구체적으로 적는다

```text
관찰한 문제: 서로 다른 use case를 이해하려면 같은 큰 Mapper의 무관한 SQL을 계속 지나쳐야 한다.
근거: 검색 query와 계정 변경 SQL의 변경 이유, 테스트 범위, 실행 특성이 다르다.
목표: 해당 capability를 찾을 때 읽어야 하는 파일과 SQL을 줄인다.
```

파일 수가 많거나 코드가 길다는 사실만으로 package나 Mapper를 나누지 않는다. [Decision Guide](../architecture/decision-guide.md)의 분리 기준과 현재 변경 이력을 확인한다.

## 2. 유지할 동작을 먼저 적는다

```text
HTTP: status, 오류 code, response body, cookie
업무: 허용과 거절 조건, 경계 시각
DB: 읽기와 쓰기 대상, SQL predicate, 영향받은 행 수
실행: transaction 시작과 종료, lock 순서, 외부 호출 순서
```

예를 들어 [`UserAccountMapper.withdraw`](../../src/main/java/com/example/lab/module/user/infra/persistence/UserAccountMapper.java)의 조건은 정리 과정에서도 유지해야 한다.

```java
.where(USER_ACCOUNT.ID.eq(accountId))
.and(USER_ACCOUNT.STATUS.eq(UserAccountStatus.ACTIVE.name()))
```

이 조건을 use case의 사전 조회로만 옮기면 경쟁 요청 사이의 최종 방어선이 사라진다. 코드 모양이 비슷해도 동작을 유지한 리팩터링이 아니다.

## 3. 변경 전후의 흐름을 비교한다

아래는 Mapper 분리를 검토할 때 쓰는 설명용 예시다. 현재 저장소에 `UserSearchMapper`가 있다는 뜻은 아니다.

```text
변경 전: SearchUsersUseCase -> UserAccountMapper.search -> DB
변경 후: SearchUsersUseCase -> UserSearchMapper.search -> DB
유지 조건: 같은 projection, filter, ordering, transaction과 권한 검사
```

분리하면 검색 SQL의 owner와 검토 범위가 분명해지는지 확인한다. 새 class가 SQL을 한 번 더 감싸기만 한다면 분리 비용이 더 크다. package 구조를 확장하거나 새 domain abstraction의 이름을 정할 때는 [AGENTS.md](../../AGENTS.md)에 따라 근거와 대안을 사용자에게 제시하고 합의한다.

## 4. 동작 유지의 증거를 모은다

- 변경 전 테스트가 핵심 계약을 실제로 관찰하는지 확인한다. 부족하다면 위험한 경계만 보강한다.
- SQL이 바뀌면 조건, 정렬, lock과 영향받은 행 수를 비교한다.
- transaction과 외부 side effect 순서가 달라졌는지 확인한다.
- HTTP 계약이 관련되면 REST Docs와 HTTP 테스트 결과를 확인한다.

[Testing](testing.md)의 범위 선택 기준을 따른다. 테스트 통과만으로 동시성이나 숨겨진 framework 동작까지 같다고 결론 내리지 않는다. 변경 후 읽기 비용이 실제로 줄었는지, 추가된 파일과 추상화의 비용도 함께 설명한다.
