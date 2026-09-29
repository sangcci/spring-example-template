# Time Policy

## 1. 업무 시간대

서비스의 업무 시간대는 `Asia/Seoul`로 고정한다. 날짜의 시작과 종료, 요일처럼 서비스의 지역 시간에 따라 결과가 달라지는 업무 판단은 Spring이 제공하는 `Clock`을 주입받아 처리한다.

JVM의 기본 timezone은 변경하지 않는다. 코드가 실행 환경의 전역 상태에 암묵적으로 의존하지 않게 한다.

## 2. 저장과 전송

하나의 시점을 나타내는 값은 `Instant`로 표현하고 UTC 기준으로 저장하고 전송한다. API timestamp는 ISO 8601 UTC 형식을 사용한다. 요청에 offset이 포함된 시각이 들어오면 해당 offset을 반영해 같은 시점의 `Instant`로 변환하며, 서울의 벽시계 값으로 덮어쓰지 않는다.

사용자의 지역 날짜와 시간이 업무 값이라면 timezone 또는 offset을 요청 계약에 함께 받는다. timezone이나 offset 없이 전달된 지역 시각을 서버가 임의로 `Asia/Seoul`로 해석하지 않는다. 서비스 정책상 서울 시각으로 해석하는 입력이라면 해당 API 계약에 그 사실을 명시한다.

`LocalDateTime`은 timezone과 offset이 없어 하나의 시점을 확정할 수 없으므로 생성 시각, 수정 시각과 만료 시각에 사용하지 않는다. 달력상의 날짜나 시간 자체가 업무 값인 경우에만 `LocalDate`, `LocalTime` 또는 `LocalDateTime`을 사용한다.

DB를 구성할 때 connection과 session timezone은 UTC로 맞추고, 대상 DB가 제공하는 timezone semantics를 실제 DB integration test로 검증한다.

## 3. 테스트

만료, 상태 전이와 날짜 경계처럼 현재 시각이 업무 결과에 영향을 주는 코드는 `Instant.now()`나 `LocalDate.now()`를 직접 호출하지 않는다. 주입받은 `Clock`을 사용하고 테스트에서는 `Clock.fixed()`로 경계 시각을 고정한다.

HTTP 응답 생성 시각과 로그 시각처럼 업무 판단에 참여하지 않는 기술 시각은 `Instant.now()`를 직접 사용할 수 있다.
