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

## 6. 검토 기준

- 조건과 실패 지점을 호출 메서드에서 바로 확인할 수 있는가?
- 상수나 helper 이름을 따라가야만 실제 값을 알 수 있지는 않은가?
- 짧게 만들기 위한 표현이 동작과 실행 순서를 숨기지 않는가?
- 중복 제거가 서로 다른 변경 이유를 억지로 묶지는 않았는가?
