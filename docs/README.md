# Documentation Map

이 저장소의 문서는 설계 원칙, 개발 절차, 업무 정책과 운영 규칙을 각각 한곳에서 관리한다. 작업할 때는 [AGENTS.md](../AGENTS.md)의 작업별 읽기 경로를 사용한다.

| 영역 | 먼저 읽을 문서 | 다루는 내용 |
| --- | --- | --- |
| 설계 | [Architecture Overview](architecture/overview.md) | 실행 흐름, 경계, SQL과 transaction의 기본 원칙 |
| 설계 판단 | [Decision Guide](architecture/decision-guide.md) | 기능별 선택 기준, 반론과 변경 조건 |
| 개발 | [Contributing Overview](contributing/overview.md) | 기능 추가, 버그 수정, 리팩터링과 오류 처리의 읽기 경로 |
| 업무 정책 | [Policies Overview](policies/overview.md) | owner별 업무 조건과 관찰 가능한 결과 |
| 운영 규칙 | [Operations Overview](operations/overview.md) | 보안과 시간 표현의 기술 계약 |

`src/docs/asciidoc`은 Spring REST Docs가 만든 결과를 조합하는 API 문서 소스다. 업무 규칙은 `docs/policies`, API의 HTTP 계약은 REST Docs 테스트와 생성 결과에서 확인한다.

문서와 구현이 다르면 실제 동작을 문서의 정답으로 간주하지 않는다. 차이를 확인한 뒤 확정된 결정에 맞춰 관련 문서와 구현을 함께 갱신한다.
