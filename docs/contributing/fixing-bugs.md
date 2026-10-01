# 버그 수정

버그는 실제 결과가 확정된 계약과 달라지는 경우다. 현재 동작이 마음에 들지 않는다는 이유만으로 미확정 업무 정책을 버그로 분류하지 않는다.

## 기대 결과의 근거를 찾는다

먼저 [업무 정책](../policies/overview.md), HTTP 계약과 기존 테스트를 확인한다. 정책 문서가 다루지 않는 결과라면 사용자에게 질문한다.

```text
보고: 탈퇴한 이메일로 다시 가입할 수 없다.
문서: 탈퇴 시점부터 7일 동안 재가입할 수 없다.
확인할 질문: 가입 시점이 탈퇴 후 7일 이전인가, 이후인가?
```

경계 처리가 미확정이면 [AGENTS.md](../../AGENTS.md#문서에-없는-정책)를 따른다.

## 실패 조건과 관찰 결과를 적는다

```text
입력: 정규화 전 이메일, 요청 시각
선행 상태: 같은 이메일의 기존 계정과 탈퇴 시각
기대 결과: 가입 허용 또는 정해진 오류
실제 결과: 받은 HTTP 응답, DB 상태, Redis session 상태
재현 범위: 단일 요청인지 동시 요청인지
```

로그나 제보만으로 원인을 확정하지 않는다. 인증과 시간 경계에서는 cookie, `Clock`, profile 설정도 함께 확인한다.

## 실패한 경계를 찾는다

```text
HTTP 입력과 validation
  -> Controller
  -> Use Case와 업무 정책
  -> Mapper와 SQL
  -> DB constraint 또는 외부 시스템
  -> HTTP 오류 변환
```

예를 들어 이메일 중복 오류가 예상과 다르면 [`CreateUserAccountUseCase`](../../src/main/java/com/example/lab/module/user/usecase/CreateUserAccountUseCase.java)의 사전 조회와 DB 예외 변환, [`GlobalExceptionHandler`](../../src/main/java/com/example/lab/global/web/GlobalExceptionHandler.java)의 응답을 차례로 확인한다. Security filter에서 난 오류는 ControllerAdvice에 도달하지 않으므로 [Error Handling](error-handling.md)의 별도 경로를 본다.

## 재현 테스트로 잘못된 결과를 관찰한다

규칙 자체는 domain 또는 use case 테스트에서, SQL과 transaction은 실제 DB integration test에서, cookie와 status는 HTTP 테스트에서 관찰한다. 먼저 실패를 재현한 뒤 원인을 수정한다. 재현할 수 없는 환경 문제라면 확인한 사실과 남은 가정을 기록하고, 근거 없는 테스트를 만들지 않는다.

실패 후 DB 상태를 관찰하는 사례는 [Testing](testing.md#선택-예시-회원가입-후-redis-저장-실패)을 참고한다. 실제 버그에 맞는 입력과 assertion을 사용한다.

## 원인을 고치고 영향 범위를 확인한다

수정한 경계에서 관련 테스트를 실행한다. 공유 policy, SQL predicate, HTTP 응답처럼 다른 use case도 사용하는 부분을 바꿨다면 영향을 받는 경로까지 넓힌다. 수정 후에는 DB에 남는 데이터, 외부 side effect, retry와 동시 요청 결과를 다시 확인한다. 정책을 새로 정한 경우에는 해당 정책서를 갱신한다.
