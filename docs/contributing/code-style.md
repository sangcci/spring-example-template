# Code Style

실행 흐름과 조건을 호출 지점에서 확인할 수 있게 작성한다.

## 조건 표현

3항 연산자는 사용하지 않는다. 조건에 따라 값이 달라지면 `if` 문으로 분기를 드러낸다.

### 피할 예시

```java
String field = parameterName == null ? "request" : parameterName;
```

### 권장 예시

```java
String field = parameterName;
if (field == null) {
    field = "request";
}
```

구현 사례: [`GlobalExceptionHandler`](../../src/main/java/com/example/lab/global/web/GlobalExceptionHandler.java), [`EmailPolicy`](../../src/main/java/com/example/lab/module/user/domain/EmailPolicy.java). 분기별 결과는 유지한다.

한 분기는 한 업무 결정을 표현한다. 논리 연산자 개수는 제한하지 않되 독립적으로 변경되는 정책, 예외와 식별자 해석은 호출 조건식에 섞지 않는다.

```java
rejoinPolicy.ensureRejoinAllowed(user, command, now);
```

가상 예시다. 조건에 독립된 책임과 이름이 있으면 Policy 안에 둔다. 항상 같은 업무 예외로 거절한다면 boolean 대신 검증 결과를 직접 표현한다.

## 상수

literal을 분리했다는 이유만으로 상수를 만들지 않는다. 상수의 이름이 원래 값보다 더 많은 의미를 전달하고, 다음 중 하나를 표현할 때만 상수로 둔다.

- business rule의 기준값
- protocol의 header나 field 이름
- 보안 또는 validation 정책
- 여러 위치가 반드시 같은 값으로 변경되어야 하는 계약

한 메서드 안에서 한 번 사용하며 값 자체로 의미가 분명한 문자열과 숫자는 그대로 표현한다. 단순히 중복을 없애기 위한 상수나 원래 literal을 다시 읽어 주는 이름의 상수는 만들지 않는다.

## Private helper method

코드는 우선 호출되는 메서드 안에서 직접 표현한다. 코드 길이를 줄이거나 중복을 제거하기 위해 private helper method를 만들지 않는다.

private method 추출은 다음 조건을 모두 만족할 때만 검토한다.

- 호출 지점과 분리해도 독립적인 책임과 이름이 있다.
- 추출한 뒤에도 주요 data flow와 조건을 호출 메서드에서 파악할 수 있다.
- 실제 반복이나 변경 이유가 확인되었다.

조건을 충족하지 않으면 메서드 안에 유지한다. 미래 재사용을 추측해 추출하지 않는다.

## 메서드 인자

메서드 인자 내부에서 계산하거나 다른 메서드와 생성자를 호출하지 않는다. 전달할 결과를 의미가 드러나는 지역 변수로 먼저 추출한 뒤 인자로 사용한다.

```java
Instant expiresAt = clock.instant().plus(accessTokenTtl);
AccessToken accessToken = new AccessToken(tokenValue, expiresAt);
tokenStore.save(accountId, accessToken);
```

다음처럼 호출과 계산을 인자 안에 중첩하지 않는다.

```java
tokenStore.save(account.getId(), new AccessToken(tokenValue, clock.instant().plus(accessTokenTtl)));
```

단순 literal, 상수와 이미 준비된 변수는 그대로 전달할 수 있다. 지역 변수의 이름은 계산 방법이 아니라 호출받는 값의 의미를 설명해야 한다.

## 여러 값을 반환하는 type

반환 타입은 [반환 타입 결정표](../architecture/decision-guide.md#반환-타입)를 따른다. 한 method 전용이라는 이유로 임시 중첩 `record`를 만들지 않는다.

최상위 HTTP response DTO는 presentation의 별도 파일에 `record`로 둔다. 순수한 `from` factory를 사용할 수 있다.

가상 예시:

```java
public record RecruitmentResponse(long recruitmentId, OwnerResponse owner) {

    public static RecruitmentResponse from(RecruitmentResult result) {
        long recruitmentId = result.recruitmentId();
        OwnerResult ownerResult = result.owner();
        OwnerResponse owner = OwnerResponse.from(ownerResult);
        return new RecruitmentResponse(recruitmentId, owner);
    }

    public record OwnerResponse(long id, String nickname) {

        private static OwnerResponse from(OwnerResult result) {
            long id = result.id();
            String nickname = result.nickname();
            return new OwnerResponse(id, nickname);
        }
    }
}
```

`from`은 Use Case result를 부수 효과 없이 매핑한다. Mapper, 외부 API와 `Clock`을 호출하거나 업무 판단을 하지 않는다. cookie와 header는 Controller가 설정한다.

HTTP 요청과 응답의 JSON 전용 구조에는 중첩 `record`를 사용할 수 있다.

중첩 DTO는 다음 조건을 만족해야 한다.

- 바깥 DTO의 JSON 구조에서만 의미가 있다.
- 독립적인 business concept, lifecycle 또는 invariant를 소유하지 않는다.
- 다른 boundary에서 재사용하기 위한 공통 type이 아니다.

중첩 type이 독립적으로 사용되거나 여러 contract에서 반복되면 단순 중복 제거가 아니라 의미, owner와 변경 이유를 확인한 뒤 별도 type으로 분리한다. 최상위 response DTO를 controller 내부에 선언하지 않는다. presentation DTO를 domain Value Object로 사용하거나 domain type에 JSON 구조를 반영하지 않는다.

## 반복과 분기

| 처리 | 우선 사용 |
| --- | --- |
| 단순 filter, map, aggregate | `Stream` |
| 분기, 상태 변경, 예외 처리와 여러 중간 단계 | `if`, `for`, `switch` |

긴 pipeline과 복잡한 lambda는 명령문으로 풀어 쓴다. `Stream` 안에서 외부 상태를 바꾸거나 중첩 `Stream`과 `flatMap`으로 반복 구조를 숨기지 않는다.

## Null, fallback과 예외 처리

`null` 가능성은 값의 출처와 contract에서 확인한다. 발생 경로를 설명할 수 없는 `null`을 가정해 모든 method에 같은 검사를 반복하지 않는다.

- HTTP request, message와 외부 provider 응답처럼 신뢰 경계에서 들어오는 값은 해당 boundary에서 검증한다.
- Bean Validation을 통과한 입력과 non-null로 선언된 내부 method 인자는 다시 검사하지 않는다.
- DB `NOT NULL` column을 non-null field로 조회하는 Mapper 결과에는 같은 의미의 방어 검사를 추가하지 않는다.
- 조회 결과가 없을 수 있는 것은 값의 nullability와 구분하고, `Optional`, 명시적인 result 또는 업무상 실패 중 실제 contract에 맞는 형태로 표현한다.

내부 invariant가 깨진 상태를 빈 문자열, 빈 collection, 임의의 enum, 현재 시각이나 다른 기본값으로 바꾸어 계속 진행하지 않는다. fallback은 그 값이 contract에 정의되어 있고 호출자가 원래 실패와 구분할 수 있을 때만 사용한다.

- `try/catch`: 현재 경계에서 복구하거나 업무 실패로 변환할 예상 예외만 처리한다. 넓은 catch로 로그만 남기거나 성공 값으로 바꾸지 않는다.
- Retry: 일시적 실패, 멱등성과 횟수 제한이 확인된 경우에만 추가한다.

방어 분기가 필요하다면 조건 가까이에서 다음 내용을 읽을 수 있어야 한다.

- 어떤 contract 또는 실제 failure mode 때문에 존재하는가?
- 이 분기에서 실패를 반환하는지, 복구하는지 또는 다시 던지는지?
- fallback과 retry가 있다면 호출자가 관찰하는 결과와 side effect는 무엇인지?

## 검토 기준

- 조건과 실패 지점을 호출 메서드에서 바로 확인할 수 있는가?
- 상수나 helper 이름을 따라가야만 실제 값을 알 수 있지는 않은가?
- 짧게 만들기 위한 표현이 동작과 실행 순서를 숨기지 않는가?
- 중복 제거가 서로 다른 변경 이유를 억지로 묶지는 않았는가?
- `Stream`이 반복과 분기를 명령문보다 더 읽기 어렵게 만들지는 않았는가?
- 여러 반환값을 의미 없는 `Map`, 배열 또는 임시 내부 `record`로 감추고 있지는 않은가?
- DTO, use case result, query projection과 domain Value Object의 owner가 구분되는가?
- 발생 경로가 없는 `null`과 invalid state를 추측해 분기를 추가하지 않았는가?
- fallback이나 broad catch가 contract 위반과 원래 실패를 숨기지는 않는가?

## 테스트 코드

테스트 표현은 [Test Guide](testing.md)를 따른다.
