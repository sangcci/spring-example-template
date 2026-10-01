# 기능 추가

새 기능의 업무 결과와 데이터 변경을 정한 뒤, 가장 위험한 실패까지 검증한다. 아래 회원가입 흐름은 현재 구현을 읽기 위한 사례이며 새 기능마다 같은 class 구성을 요구하지 않는다.

## 확인된 요구와 미확정 정책을 나눈다

[User Policy](../policies/user.md)는 계정 생성과 이메일을, [Authentication Policy](../policies/authentication.md)는 로그인 상태를 정의한다. 문서에 없는 업무 조건은 추측하지 않는다.

```text
확인된 조건: 가입에 성공하면 일반 사용자 권한을 부여하고 로그인 상태를 시작한다.
추후 반영: 계정 생성 전 이메일 인증과 필수 약관 동의.
확인할 조건: 이번 변경에 추후 반영 정책을 포함하는가?
질문: 이번 변경에 계정 생성 전 이메일 인증을 포함하나요?
영향: 답에 따라 account 상태, DB write 순서, refresh session 발급이 달라진다.
```

미확정 정책은 [AGENTS.md](../../AGENTS.md#문서에-없는-정책)를 따른다.

## 변경 범위를 한눈에 적는다

현재 회원가입의 소유권과 실행 경로는 다음과 같다.

| 항목 | 현재 회원가입 |
| --- | --- |
| 입력과 출력 | 이메일, 비밀번호, 자동 로그인 선택 → 계정 ID, 인증 token과 만료 시각 |
| 읽기 | 등록된 이메일, 최근 탈퇴한 이메일 |
| DB 쓰기 | `user_account` INSERT, user context 소유 |
| 외부 side effect | Redis refresh session 발급, auth context 소유 |
| transaction | `SignUpUseCase.execute` 전체를 감싸는 PostgreSQL transaction |
| HTTP 결과 | auth controller가 인증 cookie를 구성 |

새 기능에서는 이 표의 각 칸을 실제 요구에 맞춰 채운다. `account`와 `refresh session`을 한 DB transaction으로 원자화할 수 있다고 가정하지 않는다. [Decision Guide](../architecture/decision-guide.md)에서 owner, SQL, transaction과 외부 호출을 결정한다.

## 시작점에서 결과까지 따라간다

```text
AuthController.signUp
  -> SignUpUseCase.execute
  -> CreateUserAccountUseCase.execute
  -> UserAccountMapper.insert                 PostgreSQL
  -> RefreshSessionStore.issue                Redis
  -> AccessTokenIssuer.issue
  -> AuthController.authenticationResponse   HTTP cookie
```

[`SignUpUseCase`](../../src/main/java/com/example/lab/module/auth/usecase/SignUpUseCase.java)는 호출 순서를 드러내고, [`UserAccountMapper`](../../src/main/java/com/example/lab/module/user/infra/persistence/UserAccountMapper.java)는 SQL을 소유한다. Controller는 HTTP 입력과 cookie 응답을 다룬다. 새 기능에서도 시작점에서 write와 side effect까지 한 번에 추적할 수 있어야 한다.

## 최종 방어선을 정한다

활성 이메일의 최종 방어선은 [migration](../../src/main/resources/db/migration/V1__create_user_account.sql)의 `UNIQUE` index다. 새 기능도 사전 검사와 최종 방어선을 구분한다. DB 오류 변환은 [Error Handling](error-handling.md#db-오류를-실제-원인에-맞춰-변환한다)을 따른다.

## 실패 뒤 상태를 검증한다

회원가입의 실패 결과는 [Security](../operations/security.md#회원가입의-실패-경계), DB rollback 검증 예시는 [Testing](testing.md#선택-예시-회원가입-후-redis-저장-실패)을 따른다. 새 기능의 테스트는 가장 위험한 가정을 기준으로 선택한다.

## 계약과 문서를 함께 바꾼다

HTTP 요청, cookie 또는 오류 응답이 바뀌면 [`AuthApiDocumentationTest`](../../src/test/java/com/example/lab/module/auth/presentation/AuthApiDocumentationTest.java)와 [`AuthHttpIntegrationTest`](../../src/test/java/com/example/lab/module/auth/presentation/AuthHttpIntegrationTest.java)를 확인한다. 업무 조건이 바뀌면 owner의 정책서를 먼저 갱신한다. Java 또는 Gradle 파일을 바꿨다면 작업에 필요한 테스트를 실행하고, 커밋 전에는 [Git Guide](git.md)에 따라 `spotlessApply`를 실행한다.
