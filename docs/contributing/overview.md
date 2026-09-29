# Contributing Overview

기능 변경은 [설계 원칙](../architecture/overview.md)과 해당 [업무 정책](../policies/overview.md)을 확인한 뒤 시작한다. 이 영역의 문서는 원칙을 코드와 테스트에 적용하는 방법을 설명한다.

## 변경 순서

1. use case의 입력, 출력, 읽기, 쓰기, transaction과 외부 side effect를 적는다.
2. [Decision Guide](../architecture/decision-guide.md)로 구조를 선택하고 주변 구현을 확인한다.
3. 코드, 설정과 문서를 함께 변경한다.
4. 변경이 깨뜨릴 수 있는 가장 위험한 가정을 [Testing](testing.md)에 따라 검증한다.
5. 커밋 전에는 변경 파일 종류와 관계없이 `./gradlew spotlessApply`를 실행하고 적용된 변경을 검토한다.
6. 커밋 요청이 있으면 [Git Guide](git.md)의 형식과 Commitlint를 확인한다.

## 작업별 문서

| 작업 | 문서 | 저장소 참고 |
| --- | --- | --- |
| Java 구현과 표현 | [Code Style](code-style.md) | `src/main/java/com/example/lab/module` |
| profile 설정 | [Configuration](configuration.md) | `src/main/resources/application-*.yml`, `src/test/resources/application-test.yml` |
| 테스트 설계와 작성 | [Testing](testing.md) | `src/test/java/com/example/lab` |
| 업무 정책 문서 작성 | [Policy Writing](policy-writing.md) | `docs/policies` |
| 커밋 | [Git Guide](git.md) | `commitlint.config.cjs`, `.githooks/commit-msg` |

각 세부 문서는 기준을 먼저 설명하고, 선택이 혼동되는 곳에 피할 예시와 권장 예시, 적용 이유, 실제 코드 참고 경로를 둔다. 예시는 규칙을 이해하기 위한 것이며 복사할 새 추상화의 목록이 아니다.
