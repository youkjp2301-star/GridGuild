# GridGuild 개발 기록

개발 기준: [PRD.md](PRD.md), 제품 기획서 v2.0 / 2026-11-04.
원본 기획서와 기존 파일은 보존한다. 다음 작업자는 이 기록과 README를 먼저 읽는다.

## 2026-11-04 — 초기 MVP 구현 및 Git 협업 환경 구성

### 1. 현재까지 구현한 기능

- Kotlin / Jetpack Compose / AAOS 프로젝트와 Gradle Wrapper 생성.
- 부산 16개 구·군 선택, 홈·충전 미션·가상 도시·랭킹 4탭.
- 명시적인 Mock / Pleos SDK 모드, 데이터 출처 및 오류 표시.
- 충전 ON + SOC 2%p 증가 미션, 1일 1충전소 보상, 중복 지급 방지, 개인·도시 XP 원자 저장.
- DataStore 진행 미션/지역/원장 저장, 월간 시즌, 12단계 도시, 3개 시대 Canvas 2.5D 비주얼.
- 기어 P + CarUxRestrictions 안전 제한, Mock SOC/충전/P·D/UX 테스트 콘솔.
- 로컬 Git `main` 초기화, 자격증명·SDK·빌드 제외 규칙, 플랫폼 간 줄바꿈 규칙, CI 워크플로 추가.

### 2. Pleos SDK 연동 완료·미완료

| 항목 | 현재 상태 |
|---|---|
| 공식 Vehicle 2.3.2 / NaviHelper 3.0.5 Maven | 다운로드·컴파일 완료 |
| SOC / 충전 스위치 Capability·Get | 실제 typed 코드 구현 완료, 런타임 값 검증 진행 중 |
| 기어 Get·변경 구독 | 구현 완료. 실제 Pleos에서 등록 로그 확인. deprecated 경고 존재 |
| CarUxRestrictions | 구현 완료. 자동차 Mock에서도 실제 플랫폼 제한 적용 |
| Navi 목적지·경유지·재탐색 | typed 코드 구현 완료. 연결 콜백 로그 수신, 경유지 실제 성공 검증 진행 중 |
| Pleos Emulator v2.1.0 | 공식 이미지 SHA1 일치 확인, 설치·부팅·앱 설치/실행 완료 |
| Virtual Console | Aside로 SOC 42%, 충전 OFF, 기어 P 설정 확인 |
| 실제 충전 위치 / 전력망 / 온라인 랭킹 | 미연동, 명시적인 Mock / 데모 성과로 분리 |

### 3. 변경된 주요 파일

- `PRD.md`: 원문 기획서 보존 사본. 원래 루트에 PRD.md가 없어 기존 원문을 그대로 복사.
- `app/src/main/java/com/gridguild/`: Compose UI, ViewModel, 실제/Mock 저장소, DataStore.
- `core/src/main/kotlin/com/gridguild/core/`: 미션, 레벨, 시즌, 랭킹.
- `core/src/test/`, `app/src/androidTest/`: 핵심 로직 및 화면 흐름 테스트.
- `.gitignore`, `.gitattributes`, `.github/workflows/android.yml`, `AGENTS.md`: Git 협업 규칙.
- `README.md`, `docs/SDK_VERIFICATION.md`, `docs/IMPLEMENTATION_PLAN.md`: 실행 및 근거.
- `scripts/`: 빌드, 기기 테스트, 비밀정보 검사, 로컬 도구·이미지 설치 보조.

### 4. 빌드 및 테스트 결과

- `:app:assembleDebug`, `:app:lintDebug`, `:app:assembleDebugAndroidTest`: 성공.
- `:core:test`: **24개 통과, 실패 0**.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (Git 제외).
- 화면 테스트: **2개 통과**, 최종 `OK (2 tests)`, 8.241초.
  미션/Mock 경유지/개인·도시 120 XP/D단 차단/P단 복귀와 SDK 미지원→Mock 복귀를 검증했다.
- 추적 파일만 별도 폴더에 내보낸 구성에서도 APK 빌드·24개 단위 테스트·Lint가 모두 성공했다.
- 첫 화면 테스트 실패 원인(지연 생성 목록, 최신 샘플과 UI 타이머 비교, 화면 밖 문구·중복 문구 선택)을 수정 후 재실행했다.
- 비밀정보 검사: 초기 Git index 45개 파일에서 자격증명 패턴 없음. .tools/로컬 설정/APK/키 파일 제외, Wrapper JAR 포함 확인.

### 5. 남아 있는 오류와 다음 작업

1. Pleos 실제 서비스에 대한 최종 통합 검증을 이어간다. Mock 화면 흐름 테스트는 통과했다.
2. Pleos 실제 SOC/충전/목적지/경유지 응답과 재시작 저장 유지 검증.
3. 검증되지 않은 SDK 항목은 완료 처리하지 않고 정확한 원인·오류를 기록.
4. 다른 PC에서 JDK 17 / Android SDK 35 설정 후 clone 빌드 검증.
5. 사용자 제공 Git 작성자 정보를 이 저장소의 local config에만 설정했다. 전역 설정은 변경하지 않았다. GitHub CLI 및 원격 저장소는 미설정.
6. 사용자 GitHub 인증 및 비공개 저장소 URL 확인 후 origin 연결·첫 push 진행.

## 2026-11-05 — Clone 검증 및 수명주기 회귀 검증

### 1. 현재까지 구현한 기능

- 내비게이션이 전면으로 전환되어도 ViewModel의 SDK 콜백과 미션 관찰을 유지하도록 수정했다.
- 화면이 숨겨지면 게임 UX는 즉시 제한하고, ViewModel 해제 시 SDK 리소스를 정리한다.
- SDK 차량 호출은 IO 디스패처에서 실행한다. 일반 Android의 재진입 시 플랫폼 허용 상태 복구도 수정했다.
- Git 작성자 정보는 사용자가 제공한 값으로 이 저장소의 local config에만 설정했다.

### 2. Pleos SDK 연동 완료·미완료

- 공식 SDK typed 코드와 컴파일은 완료했다. 실제 데이터 검증용 `PleosSdkProbeTest`를 추가했다.
- 프로브는 AAOS에서만 실행하며 SOC/충전/기어와 Navi 목적지 콜백을 제한 시간 내 실제로 받아야 통과한다.
- 실제 경유지 등록·충전 상태 변화에 따른 종단 보상·충전 장소 인증은 별도 미검증/미연동으로 유지한다.

### 3. 변경된 주요 파일

- `GridViewModel.kt`: UI 수명주기와 SDK 연결 수명주기 분리, IO 실행, 재진입 허용 복구.
- `GridGuildFlowTest.kt`: 앱 재생성·120 XP·안전 상태 회귀 테스트 추가.
- `PleosSdkProbeTest.kt`: 실제 AAOS SDK 응답 전용 검사.
- `.gitignore`, `README.md`, `docs/TEST_REPORT.md`, `DEV_LOG.md`: 보안 제외·실행 절차·검증 기록.

### 4. 빌드 및 테스트 결과

- 로컬 첫 커밋 `4a5fd0e` 생성 후 실제 `git clone --no-local`로 별도 경로에 복제했다.
- Clone에서 로컬 설정 파일 없이 ANDROID_HOME/JAVA_HOME만 지정해 APK 빌드·단위 테스트·Lint 성공 (36초).
- 수명주기 수정 후 APK·Lint·테스트 APK 빌드 성공. Lint는 오류 0, 경고 9개다.
- 핵심 단위 테스트 24개 통과. 화면 테스트 **3개 통과**, 최종 `OK (3 tests)`, 20.917초.
- 실제 SDK 프로브: **2개 중 Vehicle 1개 통과, Navi 1개 실패**, 48.561초. 실패를 통과로 처리하지 않았다.
  Vehicle SDK에서 `source=PLEOS, soc=100.0, chargeSwitch=OFF`, 기어 `GEAR_PARK`, UX 허용을 수신했다.
  Navi Service는 연결됐지만 목적지 콜백이 30초 내 오지 않았다. 목적지 응답 미수신 시 10초 후 오류를 표시하도록 수정했다.
- 코드 커밋 `cfd0141`을 별도 Clone에 fast-forward 반영한 뒤 APK·24개 단위 테스트·Lint 재검증 성공 (41초).
  Clone에는 `local.properties`가 없으며 JAVA_HOME/ANDROID_HOME만 사용했다.
- 최종 Git 추적 파일 **46개** 비밀정보 검사 통과. SDK 바이너리·로컬 설정·APK·개인 키가 추적되지 않고 Wrapper JAR만 포함됨을 확인했다.
- 로컬 `main` 커밋: `4a5fd0e` 초기 프로젝트, `cfd0141` SDK 수명주기/런타임 검증. GitHub origin은 미설정이다.

### 5. 남아 있는 오류와 다음 작업

- Pleos Emulator의 GPU 경고와 SystemUI/입력 ANR 발생 이력이 있다. SDK 런타임 성공과 UI 시연을 구분한다.
- 실제 경유지 등록 결과, 실제 SOC 변화→미션 보상 종단 흐름, SDK 모드 재실행 시연을 계속 검증한다.
- 초기 SDK 기어 API는 deprecated이며 장기적으로 AAOS GEAR_SELECTION 이전을 검토한다.
- GitHub 원격 저장소·인증·push는 아직 하지 않았다. Private 저장소 생성 및 실제 URL 확인이 필요하다.
- 다른 PC는 README대로 JDK 17 / Android SDK 35와 본인 GitHub 인증을 설정하고 Clone한다.
- Navi 재검증 전 내장 내비게이션의 실제 목적지/경로 설정 상태를 확인한다. Mock 목적지·경유지 시뮬레이션은 별도 모드에서 동작한다.

## 작업 종료 시 갱신 양식

아래 다섯 항목을 실제 결과로 작성하고 관련 코드와 같은 커밋에 포함한다.

1. 현재까지 구현한 기능
2. Pleos SDK 연동 완료·미완료 현황
3. 변경된 주요 파일
4. 실행한 빌드·테스트 명령과 실제 결과
5. 남아 있는 오류·제약과 다음 개발 작업
