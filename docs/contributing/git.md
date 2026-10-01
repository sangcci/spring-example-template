# Git Guide

## 커밋 단위

하나의 의도가 드러나는 단위로 커밋한다. 기능, 리팩터링, 대규모 형식 변경과 의존성 갱신처럼 변경 이유가 다르면 가능한 한 별도 커밋으로 나눈다.

## 커밋 메시지

커밋 메시지는 `type: 변경 내용 요약` 형식을 사용한다. `type(scope): summary` 형식은 사용하지 않는다.

요약은 한글 개조식으로 변경 대상을 적는다.

```text
feat: 구조화된 HTTP 로깅 추가
fix: 만료된 access token 처리 수정
docs: 설정 파일 작성 규칙 추가
```

허용하는 type은 다음과 같다.

| Type | 사용하는 경우 |
| --- | --- |
| `feat` | 기능 추가 또는 사용자가 관찰하는 동작 변경 |
| `fix` | 잘못된 동작 수정 |
| `docs` | 문서만 변경 |
| `style` | 동작에 영향을 주지 않는 형식 변경 |
| `refactor` | 동작을 유지하는 코드 구조 변경 |
| `test` | 테스트 추가 또는 수정 |
| `chore` | 설정, build와 개발 도구 변경 |
| `rename` | 이름 또는 위치 변경 |
| `perf` | 성능 개선 |

여러 type이 섞이면 변경의 주된 목적을 기준으로 선택한다. 주된 목적을 하나로 설명하기 어렵다면 커밋을 나눌 수 있는지 먼저 검토한다.

## Commitlint

Commitlint는 type, scope 미사용과 제목을 검사한다. 한글 개조식과 변경 대상 표현은 리뷰로 확인한다.

의존성을 설치하고 저장소의 Git hook을 활성화한다.

```bash
npm ci
git config --local core.hooksPath .githooks
```

`.githooks/commit-msg`가 커밋마다 검사한다. 수동 검사:

```bash
printf '%s\n' 'feat: 구조화된 HTTP 로깅 추가' | npm run commitlint
```

`--no-verify`를 일상적으로 사용하지 않는다.

## 커밋 전 확인 사항

포맷 설정은 `lint.gradle`에 둔다. Palantir Java Format, annotation formatting, unused import 제거, trailing whitespace 제거와 파일 끝 newline을 적용한다.

```bash
./gradlew spotlessApply  # 파일 종류와 관계없이 커밋 전 실행, 적용 결과 검토
./gradlew spotlessCheck  # CI 또는 별도 포맷 검증
```

- 하나의 변경 의도를 설명하는 커밋인가?
- 허용된 type을 사용했는가?
- scope 없이 `type: 변경 내용 요약` 형식으로 작성했는가?
- 변경 내용이 한글 개조식이며 변경 대상을 설명하는가?
- Commitlint 검사를 통과하는가?
