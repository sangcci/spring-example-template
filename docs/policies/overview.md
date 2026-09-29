# Policies Overview

업무 정책은 사용자가 관찰할 수 있는 조건과 결과의 source of truth다. 구현 경계, token 형식과 DB 구조 같은 기술 선택은 [Architecture](../architecture/overview.md)와 [Operations](../operations/overview.md)에서 관리한다.

| Owner | 정책 | 구현 진입점 |
| --- | --- | --- |
| user | [User Policy](user.md): 계정 생성, 상태, 권한, 탈퇴와 재가입 | `src/main/java/com/example/lab/module/user` |
| auth | [Authentication Policy](authentication.md): 로그인, 인증 유지와 로그아웃 | `src/main/java/com/example/lab/module/auth` |

회원가입처럼 두 context가 함께 참여하면 각 정책의 owner를 확인하고 [Decision Guide](../architecture/decision-guide.md)의 cross-domain 규칙을 따른다. 정책을 새로 쓰거나 수정할 때는 [Policy Writing](../contributing/policy-writing.md)을 사용한다.
