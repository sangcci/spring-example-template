# Contributing Overview

기능 변경 전 [설계 원칙](../architecture/overview.md)과 [업무 정책](../policies/overview.md)을 확인한다.

## 변경 순서

1. use case의 입력, 출력, 읽기, 쓰기, transaction과 외부 side effect를 적는다.
2. [Decision Guide](../architecture/decision-guide.md)로 구조를 선택하고 주변 구현을 확인한다.
3. 코드, 설정과 문서를 함께 변경한다.
4. 변경이 깨뜨릴 수 있는 가장 위험한 가정을 [Testing](testing.md)에 따라 검증한다.
5. 커밋은 [Git Guide](git.md)의 포맷, 메시지와 Commitlint 검증을 따른다.

## 작업별 문서

| 작업 | 문서 | 저장소 참고 |
| --- | --- | --- |
| 기능 추가 | [Adding Feature](adding-feature.md) | `module/auth/usecase/SignUpUseCase.java` |
| 버그 수정 | [Fixing Bugs](fixing-bugs.md) | `src/test/java/com/example/lab/module` |
| 리팩터링 | [Refactoring](refactoring.md) | [Decision Guide](../architecture/decision-guide.md) |
| 오류 경계 확인 | [Error Handling](error-handling.md) | `global/web/GlobalExceptionHandler.java`, `module/auth/infra/security` |
| Java 구현과 표현 | [Code Style](code-style.md) | `src/main/java/com/example/lab/module` |
| profile 설정 | [Configuration](configuration.md) | `src/main/resources/application-*.yml`, `src/test/resources/application-test.yml` |
| 테스트 설계와 작성 | [Testing](testing.md) | `src/test/java/com/example/lab` |
| 문서 작성 | [Documentation](documentation.md) | `AGENTS.md`, `docs/` |
| 업무 정책 문서 작성 | [Policy Writing](policy-writing.md) | `docs/policies` |
| 커밋 | [Git Guide](git.md) | `commitlint.config.cjs`, `.githooks/commit-msg` |
