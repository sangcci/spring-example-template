# Time Policy

## 업무 시간대

서비스의 업무 시간대는 `Asia/Seoul`로 고정한다. 날짜의 시작과 종료, 요일처럼 서비스의 지역 시간에 따라 결과가 달라지는 업무 판단은 Spring이 제공하는 `Clock`을 주입받아 처리한다.

JVM의 기본 timezone은 변경하지 않는다.

## 저장과 전송

하나의 시점을 나타내는 값은 `Instant`로 표현하고 UTC 기준으로 저장하고 전송한다. API timestamp는 ISO 8601 UTC 형식을 사용한다. 요청에 offset이 포함된 시각이 들어오면 해당 offset을 반영해 같은 시점의 `Instant`로 변환하며, 서울의 벽시계 값으로 덮어쓰지 않는다.

사용자의 지역 날짜와 시간이 업무 값이라면 timezone 또는 offset을 요청 계약에 함께 받는다. timezone이나 offset 없이 전달된 지역 시각을 서버가 임의로 `Asia/Seoul`로 해석하지 않는다. 서비스 정책상 서울 시각으로 해석하는 입력이라면 해당 API 계약에 그 사실을 명시한다.

생성, 수정과 만료 시점에 `LocalDateTime`을 사용하지 않는다. 달력상의 날짜와 시간이 업무 값일 때만 `LocalDate`, `LocalTime`, `LocalDateTime`을 사용한다.

DB를 구성할 때 connection과 session timezone은 UTC로 맞추고, 대상 DB가 제공하는 timezone semantics를 실제 DB integration test로 검증한다.

## 테스트

만료, 상태 전이와 날짜 경계처럼 현재 시각이 업무 결과에 영향을 주는 코드는 `Instant.now()`나 `LocalDate.now()`를 직접 호출하지 않는다. 주입받은 `Clock`을 사용하고 테스트에서는 `Clock.fixed()`로 경계 시각을 고정한다.

HTTP 응답 생성 시각과 로그 시각처럼 업무 판단에 참여하지 않는 기술 시각은 `Instant.now()`를 직접 사용할 수 있다.

탈퇴 계정의 보관 기간을 검증할 때는 [`DeleteWithdrawnAccountsUseCaseIntegrationTest`](../../src/test/java/com/example/lab/module/user/usecase/DeleteWithdrawnAccountsUseCaseIntegrationTest.java)처럼 현재 시각을 고정하고 경계 바로 전후를 비교한다.

```java
Instant now = Instant.parse("2026-09-20T00:00:00Z");
Instant boundary = now.minus(30, ChronoUnit.DAYS);
Clock clock = Clock.fixed(now, ZoneOffset.UTC);

userAccountMapper.withdraw(expiredAccountId, boundary);
userAccountMapper.withdraw(retainedAccountId, boundary.plusSeconds(1));

int deletedCount = new DeleteWithdrawnAccountsUseCase(userAccountMapper, clock).execute();

assertThat(deletedCount).isOne();
assertThat(userAccountMapper.findById(expiredAccountId)).isEmpty();
assertThat(userAccountMapper.findById(retainedAccountId)).isPresent();
```

예시의 `Clock.fixed`는 테스트에서만 사용한다. 운영 시각은 [`TimeConfig`](../../src/main/java/com/example/lab/global/time/TimeConfig.java)가 제공하는 `Clock`에서 읽는다.
