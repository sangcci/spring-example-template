# Code Style

## 1. 문서의 역할

이 문서는 코드의 동작을 사람이 빠르게 읽고 검증할 수 있도록 표현 방식을 정한다. 코드 길이나 중복 제거보다 실행 흐름과 조건을 한곳에서 직접 확인할 수 있는지를 우선한다.

## 2. 조건 표현

3항 연산자는 사용하지 않는다. 조건에 따라 값이 달라지면 `if` 문으로 분기를 드러낸다.

```java
String field = parameterName;
if (field == null) {
    field = "request";
}
```

짧은 표현이라도 조건과 결과를 한 줄에 압축하지 않는다.

## 3. 상수

literal을 분리했다는 이유만으로 상수를 만들지 않는다. 상수의 이름이 원래 값보다 더 많은 의미를 전달하고, 다음 중 하나를 표현할 때만 상수로 둔다.

- business rule의 기준값
- protocol의 header나 field 이름
- 보안 또는 validation 정책
- 여러 위치가 반드시 같은 값으로 변경되어야 하는 계약

한 메서드 안에서 한 번 사용하며 값 자체로 의미가 분명한 문자열과 숫자는 그대로 표현한다. 단순히 중복을 없애기 위한 상수나 원래 literal을 다시 읽어 주는 이름의 상수는 만들지 않는다.

## 4. Private helper method

코드는 우선 호출되는 메서드 안에서 직접 표현한다. 코드 길이를 줄이거나 중복을 제거하기 위해 private helper method를 만들지 않는다.

private method 추출은 다음 조건을 모두 만족할 때만 검토한다.

- 호출 지점과 분리해도 독립적인 책임과 이름이 있다.
- 추출한 뒤에도 주요 data flow와 조건을 호출 메서드에서 파악할 수 있다.
- 실제 반복이나 변경 이유가 확인되었다.

조건이 명확하지 않으면 메서드 안에 유지한다. 여러 줄이라는 이유나 미래에 재사용할 수 있다는 추측만으로 추출하지 않는다.

## 5. 메서드 인자

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

## 6. 반복과 분기

`Stream`을 습관적으로 사용하지 않는다. 반복 과정에 조건 분기, 상태 변경, 예외 처리 또는 여러 단계의 중간값이 포함되면 실행 흐름이 직접 드러나는 `if`, `for` 또는 `switch` 문을 우선한다.

`Stream`은 단순한 filtering, mapping과 aggregation처럼 위에서 아래로 한 번에 읽히는 경우에만 사용한다. pipeline이 길어지거나 lambda 내부의 조건이 늘어나면 명령문으로 풀어 쓴다. 코드 길이보다 사람이 실행 순서와 분기 결과를 빠르게 파악할 수 있는지를 기준으로 선택한다.

특히 `Stream` 내부에서 외부 상태를 변경하거나, 중첩된 `Stream`과 `flatMap`으로 반복 구조를 숨기지 않는다.

## 7. Null, fallback과 예외 처리

`null` 가능성은 값의 출처와 contract에서 확인한다. 발생 경로를 설명할 수 없는 `null`을 가정해 모든 method에 같은 검사를 반복하지 않는다.

- HTTP request, message와 외부 provider 응답처럼 신뢰 경계에서 들어오는 값은 해당 boundary에서 검증한다.
- Bean Validation을 통과한 입력과 non-null로 선언된 내부 method 인자는 다시 검사하지 않는다.
- DB `NOT NULL` column을 non-null field로 조회하는 Mapper 결과에는 같은 의미의 방어 검사를 추가하지 않는다.
- 조회 결과가 없을 수 있는 것은 값의 nullability와 구분하고, `Optional`, 명시적인 result 또는 업무상 실패 중 실제 contract에 맞는 형태로 표현한다.

내부 invariant가 깨진 상태를 빈 문자열, 빈 collection, 임의의 enum, 현재 시각이나 다른 기본값으로 바꾸어 계속 진행하지 않는다. fallback은 그 값이 contract에 정의되어 있고 호출자가 원래 실패와 구분할 수 있을 때만 사용한다.

`try/catch`는 구체적으로 예상한 예외를 현재 계층이 복구하거나 업무상 실패로 변환할 때만 둔다. 원인을 알 수 없는 예외를 넓게 잡아 로그만 남기거나 성공 값으로 바꾸지 않는다. retry도 일시적 실패임이 확인되고 operation의 idempotency와 횟수 제한을 설명할 수 있을 때만 추가한다.

방어 분기가 필요하다면 조건 가까이에서 다음 내용을 읽을 수 있어야 한다.

- 어떤 contract 또는 실제 failure mode 때문에 존재하는가?
- 이 분기에서 실패를 반환하는지, 복구하는지 또는 다시 던지는지?
- fallback과 retry가 있다면 호출자가 관찰하는 결과와 side effect는 무엇인지?

## 8. 검토 기준

- 조건과 실패 지점을 호출 메서드에서 바로 확인할 수 있는가?
- 상수나 helper 이름을 따라가야만 실제 값을 알 수 있지는 않은가?
- 짧게 만들기 위한 표현이 동작과 실행 순서를 숨기지 않는가?
- 중복 제거가 서로 다른 변경 이유를 억지로 묶지는 않았는가?
- `Stream`이 반복과 분기를 명령문보다 더 읽기 어렵게 만들지는 않았는가?
- 발생 경로가 없는 `null`과 invalid state를 추측해 분기를 추가하지 않았는가?
- fallback이나 broad catch가 contract 위반과 원래 실패를 숨기지는 않는가?
