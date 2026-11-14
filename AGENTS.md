# GridGuild 개발 지침

- 개발 기준 문서는 루트 `PRD.md`다. 사용자의 명시적 요청 없이 요구사항을 변경하지 않는다.
- 원본 `기획서.txt`, 첨부 원문 보관본 `docs/MASTER_REQUIREMENTS.md` 및 기존 파일을 보존한다.
- 작업 시작 시 `DEV_LOG.md`, `README.md`, `PRD.md`를 읽고 이전 진행 상황을 확인한다.
- **매 작업 종료 시 `DEV_LOG.md`를 갱신한다.** 구현 기능, SDK 완료/미완료, 주요 변경 파일,
  실제 실행한 빌드·테스트와 결과, 남은 오류·다음 작업을 빠짐없이 기록한다.
- 문서 확인/컴파일/실제 에뮬레이터 응답 성공을 구분한다. 미검증을 완료로 표시하지 않는다.
- 공식 SDK 타입을 직접 사용한다. 충전소·계통·랭킹 fixture, 미인증 성과는 명확히 Mock/데모로 표시한다.
- 실제 플랫폼 주행 제한을 Mock 조작으로 우회하지 않는다. 기어 P + CarUxRestrictions 확인을 유지한다.
- SDK 인증정보, API 키, 비밀번호, 토큰, 개인 키, 로컬 SDK 경로, 빌드 산출물을 Git에 추가하지 않는다.
- 공모전 제출에 대한 사용자의 명시적 요청으로 `apk/HU-MOTION_GridGuild_release.apk`만 빌드 산출물 제외 규칙의 예외다. `scripts/verify-release.ps1`과 비밀정보 검사 통과 후 추적하며 서명 키는 계속 제외한다.
- 개발팀 표기는 `HU:MOTION`, 앱/저장소 이름은 `GridGuild`다. Windows 파일명에는 `HU-MOTION_GridGuild_release.apk`를 사용한다.
- 팀명 정정 제출 태그는 `submission-2026-11-15-humotion`이며 고정 후 이동하지 않는다.
- 1차 제출 태그 `submission-2026-11-15`와 해당 커밋은 보존한다. 후속 개발은 새 커밋으로 기록하고 강제로 태그를 옮기지 않는다.
- 커밋 전 `scripts/check-secrets.ps1` 및 변경에 필요한 빌드·테스트를 실행한다.
- 이 프로젝트를 다른 PC에서 작업하기 전 pull하고, 종료 시 DEV_LOG와 함께 commit/push한다.
- GitHub 원격 저장소 생성, 공개 범위 변경, 인증이 필요한 단계는 사용자에게 명확히 안내한다.

## User preference

When the user requests work, assess whether any part is better suited to the Aside AI agent.
When it is, proactively delegate that part to Aside and integrate its result.
The user does not need to split the task or separately request delegation each time.
Use judgment about the task and available permissions.
