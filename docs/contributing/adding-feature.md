# 기능 추가

새 기능의 업무 결과와 데이터 변경을 정한 뒤, 가장 위험한 실패까지 검증한다. 아래 회원가입 흐름은 현재 구현을 읽기 위한 사례이며 새 기능마다 같은 class 구성을 요구하지 않는다.

## 1. 확인된 요구와 미확정 정책을 나눈다

[User Policy](../policies/user.md)는 계정 생성과 이메일을, [Authentication Policy](../policies/authentication.md)는 로그인 상태를 정의한다. 문서에 없는 업무 조건은 추측하지 않는다.

```text
확인된 조건: 가입에 성공하면 일반 사용자 권한을 부여하고 로그인 상태를 시작한다.
미확정 조건: 이메일 인증을 가입 전에 요구할 것인가?
질문: 이메일 인증이 완료될 때까지 account 생성과 로그인 시작을 보류해야 하나요?
영향: 답에 따라 account 상태, DB write 순서, refresh session 발급이 달라진다.
```

요청이 미확정 조건에 의존하면 사용자에게 질문하고 답을 기다린다. 그동안 현재 코드와 정책을 읽고, 답과 무관한 경계는 조사할 수 있다. 질문과 구현 결정의 구분은 [AGENTS.md](../../AGENTS.md)의 "문서에 없는 정책을 다루는 방법"을 따른다.

## 2. 변경 범위를 한눈에 적는다

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

## 3. 시작점에서 결과까지 따라간다

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

## 4. 최종 방어선을 정한다

이메일 중복은 사전 조회만으로 막을 수 없다. 동시 가입에서는 두 요청이 모두 조회를 통과할 수 있으므로 [migration](../../src/main/resources/db/migration/V1__create_user_account.sql)의 활성 계정 이메일 `UNIQUE` index가 최종 방어선이다. [`CreateUserAccountUseCase`](../../src/main/java/com/example/lab/module/user/usecase/CreateUserAccountUseCase.java)는 DB 오류를 사용자에게 이해 가능한 실패로 바꾼다.

```java
try {
    long accountId = userAccountMapper.insert(normalizedEmail, passwordHash, now);
    return new CreatedUserAccount(accountId, UserRole.USER);
} catch (DataIntegrityViolationException exception) {
    throw new ApplicationException(UserErrorCode.EMAIL_ALREADY_REGISTERED, exception);
}
```

새 기능의 규칙도 사전 검사와 최종 방어선이 각각 어디에 있는지 적는다. DB 오류를 어떤 업무 실패로 바꿀지는 실제 constraint와 실패 원인을 확인한 뒤 결정한다. 오류 경계는 [Error Handling](error-handling.md)을 참고한다.

## 5. 실패 뒤 상태를 검증한다

회원가입에서 Redis 저장이 실패하면 account가 남지 않아야 한다. [`SignUpUseCaseIntegrationTest`](../../src/test/java/com/example/lab/module/auth/usecase/SignUpUseCaseIntegrationTest.java)는 실패를 주입한 다음 실제 DB 상태를 확인한다.

```java
when(refreshSessionStore.issue(anyLong(), any(), any()))
        .thenThrow(new DataAccessResourceFailureException("redis unavailable"));

Throwable thrown = catchThrowable(() -> signUpUseCase.execute("user@example.com", "Password1!", false));

assertThat(thrown).isInstanceOf(DataAccessResourceFailureException.class);
assertThat(dsl.fetchCount(USER_ACCOUNT)).isZero();
```

이 테스트는 Redis protocol을 검증하는 테스트가 아니다. DB rollback과 Redis 경계 실패를 함께 관찰한다. 새 기능의 테스트 범위는 [Testing](testing.md)에 따라 가장 위험한 가정을 기준으로 정한다.

## 6. 계약과 문서를 함께 바꾼다

HTTP 요청, cookie 또는 오류 응답이 바뀌면 [`AuthApiDocumentationTest`](../../src/test/java/com/example/lab/module/auth/presentation/AuthApiDocumentationTest.java)와 [`AuthHttpIntegrationTest`](../../src/test/java/com/example/lab/module/auth/presentation/AuthHttpIntegrationTest.java)를 확인한다. 업무 조건이 바뀌면 owner의 정책서를 먼저 갱신한다. Java 또는 Gradle 파일을 바꿨다면 작업에 필요한 테스트를 실행하고, 커밋 전에는 [Git Guide](git.md)에 따라 `spotlessApply`를 실행한다.
