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

## 2026-11-05 — 직접 테스트 실행 안내

1. **구현 기능:** 앱 기능 변경 없음. 현재 PC에서 에뮬레이터를 열고 기존 APK를 설치·실행하는 순서를 README에 추가했다.
2. **SDK 현황:** 기존 결과 유지. 실제 Vehicle 현재값/P/UX 응답 확인, Navi 목적지 콜백 미수신은 미완료다. 첫 직접 테스트는 Mock 모드를 안내했다.
3. **주요 변경 파일:** `README.md`, `DEV_LOG.md`.
4. **확인 결과:** APK와 emulator/adb 파일, `GridGuild_Mock_API34` 및 `GridGuild_Pleos_API34` AVD 존재 확인. 안내 전 실행 중 기기는 없었다. 이번에는 앱·에뮬레이터를 자동 실행하거나 빌드·테스트를 재실행하지 않았다.
5. **다음 작업:** 사용자가 Mock 충전→개인/도시 +120 XP→D단 차단→P단 복귀→재실행 저장 유지를 직접 확인한다. 실제 Navi 목적지/경유지 검증과 GitHub 원격 연결은 별도 진행한다.

## 2026-11-05 — 0.2.0 도시 그래픽·미리보기·Mock·점멸 수정

### 1. 현재까지 구현한 기능

- 사용자 참조 분위기의 중세·현대·미래 디오라마 아틀라스 3개를 built-in image_gen으로 생성하고 프로젝트 리소스로 저장했다. 레벨마다 다른 12개 프레임을 사용한다.
- 다음 레벨 미리보기는 목표 레벨·랜드마크·설명·그림을 함께 바꾸며 현재 도시 복귀와 전체 성장 단계 미리보기를 제공한다. 실제 XP/도시 레벨은 변경하지 않는다.
- 배터리와 미션 UI가 오래된 composition 시각으로 각각 유효성을 재판정하던 경로를 제거하고 ViewModel의 동일 시점 VehicleReading을 사용한다. 실제 오래된/미래 데이터 차단 기준은 유지한다.
- Mock heartbeat는 timestamp만 원자 갱신한다. SOC/충전 조작과 heartbeat 사이에서 이전 값으로 덮어쓰는 경쟁 조건을 제거했다.
- 상단 데모(Mock) 표시에 설명창·조작 메뉴 진입을 연결했다. 같은 모드 재선택은 진행 미션을 유지하고 다른 모드 전환은 취소를 확인한다.
- 미션 버튼에 시작 중·진행 중·완료·시간 외·차량 확인 등의 문구와 설명, 요청 중 중복 클릭 방지를 추가했다.
- Aside의 읽기 전용 UX 검토를 받아 모드 재선택 취소, 비활성 이유, 미리보기 전후 구분 개선에 반영했다.

### 2. Pleos SDK 연동 완료·미완료

- 실제 SDK 호출·권한·주행 제한 정책은 변경하지 않았다. Vehicle 이전 실응답 검증 성공, Navi 목적지 콜백 미수신/경유지 종단 미검증 현황은 그대로다.
- Mock 데이터와 실제 차량 데이터 출처, 모든 미인증 데모 XP 표시는 유지했다. 아트는 실제 지형/건물 또는 실시간 3D라고 표시하지 않는다.

### 3. 변경된 주요 파일

- `GridGuildApp.kt`, `CityCanvas.kt`: 설명/미션 UX와 12단계 아트 렌더링.
- `GridViewModel.kt`, `Repositories.kt`, `Models.kt`: 공통 판정 스냅샷·원자 heartbeat·시작 요청 상태.
- `CityStage.kt`, `drawable-nodpi/city_*_v2.png`: 12단계 정의와 3개 아틀라스.
- `VehicleReadingTest.kt`, `UiRegressionTest.kt`: 점멸/미리보기/모드 회귀 테스트.
- `app/build.gradle.kts`: versionName 0.2.0 / versionCode 2.
- `README.md`, `docs/CITY_ART.md`, `docs/TEST_REPORT.md`, `DEV_LOG.md`: 변경 내용·생성 사양·검증 기록.

### 4. 빌드 및 테스트 결과

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 성공 (1분 17초). Kotlin 테스트 29개, 실패 0. Lint 오류 0 / 경고 9개.
- 두 화면 테스트 클래스 실행 결과 **6개 모두 통과**, 20.169초. 정상 갱신 중 배터리/미션 버튼 안정성, 미리보기 프레임·XP 보존, Mock 설명 및 재선택 시 미션 유지를 검증했다.
- 새 테스트의 문자열 부분 일치 옵션 누락을 수정한 뒤 재실행했다. 제품 기능을 우회해 통과 처리하지 않았다.
- 실제 에뮬레이터 캡처로 현재/다음/미래 도시와 홈·미션·Mock 안내를 확인했다. 산출물은 `artifacts/city-v2`이며 Git 제외다.
- 테스트 전 사용자 데모 기록을 백업했고 테스트 후 백업/복원 파일 SHA256 일치를 확인했다. 새 APK를 설치하고 원래 기록으로 앱 재실행했다.

### 5. 남아 있는 오류와 다음 작업

- 실제 Navi 목적지/경유지 콜백 종단 검증과 SDK 모드 충전 변화→보상 흐름은 별도 필요하다. 이번 변경으로 완료됐다고 주장하지 않는다.
- 도시 아트는 레벨별 대표 장면을 지역별 테마 설명과 함께 공유한다. 지역별 독립 3D 모델/실제 주변 건물 재현은 아직 없다.
- GitHub origin은 여전히 미설정이며 원격 생성·인증·push는 수행하지 않는다.

## 2026-11-05 — 0.3.0 UI/UX 고도화와 도시 해금 정책

### 1. 현재까지 구현한 기능

- 네이비·청록·골드, 밝은 배경과 흰 카드로 색상 체계를 통일했다. 제목/수치의 위계, 카드 모서리·테두리·여백, 헤더·탐색·버튼·대화상자를 정리했다. 홈의 배터리 게이지와 성과, 미션의 주요 버튼을 강조한다.
- 현재 지역의 XP로 해금한 도시만 컬렉션에서 다시 방문할 수 있다. 선택은 실제 XP/원장을 바꾸지 않으며 현재 도시로 돌아갈 수 있다.
- 미해금 도시는 공통 기하학 잠금 이미지의 비활성 타일로 대체했다. `다음 단계 확인하기`는 필요 XP와 잠금 안내만 제공하며 실제 다음 아트·이름·랜드마크는 공개하지 않는다.
- `CityUnlocks`에서 선택을 검증하고 `CityCanvas`가 이미지 로딩 전에 다시 제한한다. XP 초기화·지역 변경 이후 전환 중인 이전 프레임에도 실제 해금 한도를 적용한다. 최고 단계에는 완료 상태를 표시한다.
- Aside에 기존 코드/이미지의 읽기 전용 UX 검토를 위임했다. 이미지 렌더링 전 잠금, 컬렉션/필요 XP 구분, 일관된 색 의미와 출처 안내 개선을 반영했다.
- 사용자의 명시적 변경 요청을 PRD F06 및 도시 화면 요건에 반영했다. 원본 `기획서.txt`, `docs/MASTER_REQUIREMENTS.md`와 기존 도시 PNG는 그대로 보존했다.

### 2. Pleos SDK 연동 완료·미완료

- SDK 호출·권한·기어 P 및 CarUxRestrictions 정책은 변경하지 않았다. 공식 Vehicle/NaviHelper 타입을 포함한 새 APK 컴파일은 성공했다.
- 실제 Vehicle 현재값/P/UX 수신은 이전 검증 성공 상태다. 실제 Navi 목적지 콜백 미수신, 경유지 등록과 충전 변화→성과 종단 검증은 미완료 상태다. 이번 화면 테스트를 실제 SDK 성공으로 표시하지 않는다.
- 차량 Mock, 샘플 충전소·전력망·경쟁자, 위치 미인증 XP 출처 안내를 유지했다.

### 3. 변경된 주요 파일

- `GridTheme.kt`, `GridGuildApp.kt`: 전역 색상·글꼴·형태와 홈/미션/탐색 UI.
- `CityCollectionScreen.kt`, `CityCanvas.kt`: 해금 컬렉션·현재 도시 복귀·잠금 안내·이미지 렌더링 제한.
- `CityUnlocks.kt`, `CityUnlocksTest.kt`, `UiRegressionTest.kt`: 접근 정책과 XP 경계/선택/초기화 회귀 검증.
- `app/build.gradle.kts`: versionName 0.3.0 / versionCode 3.
- `PRD.md`, `README.md`, `docs/CITY_ART.md`, `docs/TEST_REPORT.md`, `DEV_LOG.md`: 요청한 정책·직접 테스트 방법·아트 및 검증 기록.

### 4. 빌드 및 테스트 결과

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 최종 성공 (14초). Kotlin 단위 테스트 **33개 통과**, Lint 오류 0 / 경고 9개.
- API 34 에뮬레이터의 `GridGuildFlowTest,UiRegressionTest` **7개 모두 통과**, 21.852초. 잠긴 Lv.12 비활성·미래 아트 비노출, Lv.3→Lv.1 방문→복귀 시 XP 유지, 초기화 후 재잠금 및 기존 배터리/미션/안전/저장 흐름을 확인했다.
- 첫 컴파일에서 Kotlin 문자열의 `$current개` 식별자 오류를 `${current}개`로 고친 뒤 빌드 성공. 실제 화면 검토에서 기본 대화상자 배경도 새 색상 체계에 맞췄다.
- 실제 에뮬레이터 PNG로 홈·도시·잠금 안내·잠금 컬렉션·과거 도시·미션의 배치를 검토했다. 로컬 `artifacts/premium`은 Git 제외다.
- 테스트 전 사용자 기록을 앱 내부에 백업했고, 최종 복원 후 백업/저장 파일의 SHA256 일치를 확인했다. APK 0.3.0 설치 및 기존 기록으로 재실행 성공.
- Git index 57개 파일에 대한 `scripts/check-secrets.ps1` 검사와 `git diff --cached --check`를 통과했다. 로컬 설정·도구·빌드·캡처 파일은 추적하지 않는다.

### 5. 남아 있는 오류와 다음 개발 작업

- 이번 범위의 빌드/자동 화면 테스트 실패는 없다. Lint의 기존 의존성 업데이트 권고 및 데이터 추출 규칙 관련 경고 9개는 남아 있다.
- 실제 Pleos 내비 목적지/경유지 콜백 및 충전 상태 변화→보상 종단 검증을 이어간다.
- 표준 API 34 에뮬레이터 화면을 검토했다. 실제 차량 디스플레이의 해상도·분할 화면·시인성 검증은 별도로 필요하다.
- GitHub origin은 미설정이다. 로컬 커밋을 남기며 원격 생성·인증·push는 사용자 저장소 연결 단계에서 진행한다.

## 2026-11-05 — 0.4.0 테스트 항목별 초기화

### 1. 현재까지 구현한 기능

- Mock 연결·테스트 패널에 `도시 Lv 초기화`, `충전 미션 초기화`, `내 포인트 초기화` 버튼과 각각의 영향 범위/취소/확인창을 추가했다. 전체 데모 초기화는 별도 버튼으로 유지한다.
- 도시 Lv 초기화는 선택 지역의 도시 XP를 0으로 만들어 Lv.1로 돌아가고 미래 도시를 다시 잠근다. 다른 지역 XP, 개인 포인트 및 미션 상태는 보존한다.
- 충전 미션 초기화는 진행 미션 취소 및 완료/일일 보너스 판정만 초기화한다. 기존 XP를 유지하며 같은 날 같은 충전소를 다시 테스트할 수 있다.
- 내 포인트 초기화는 개인 누적·시즌 XP만 0으로 만든다. 도시 성장과 미션 완료 제한은 보존한다. 새 미션 보상은 다시 개인·도시에 적립된다.
- 원래 지급 이벤트의 ID/시각/XP는 보존하고 개인 반영·도시 반영·미션 판정 플래그를 분리했다. schema 1 기존 기록은 손실 없이 읽고 schema 2로 부분 초기화 상태를 저장한다.
- 미션 엔진과 UI의 완료 판정을 공유하고 일일 보너스의 중복 방지와 ID 고유성을 유지했다. 초기화 버튼은 Mock·주차 허용 시에만 보이며 저장 트랜잭션 안에서도 현재 모드/안전/도시 확인을 재검증한다.
- Aside 읽기 전용 검토에서 제안한 월간 점수 분리, 과거 ID 재사용 방지, schema 2 누락 필드 오류, 선택 지역/안전 재검증, 원장 출처 설명을 반영했다.

### 2. Pleos SDK 연동 완료·미완료

- 공식 Vehicle/NaviHelper 호출·권한과 기어 P + CarUxRestrictions 제한은 변경하지 않았다. 공식 SDK 타입을 포함한 새 APK 컴파일 성공.
- 실제 Vehicle 현재값/P/UX 응답의 이전 검증 성공 상태는 유지한다. Navi 목적지 콜백 미수신, 실제 경유지 등록 및 충전 변화→보상 종단 검증은 미완료다.
- 초기화는 Mock 상태에서만 동작한다. 현재 원장은 SDK에서 얻은 위치 미인증 데모 XP도 포함하는 공통 데모 원장임을 README에 명시했다.

### 3. 변경된 주요 파일

- `DemoResetControls.kt`, `GridGuildApp.kt`: 세 초기화/전체 초기화 버튼, 확인창, 현재 잔액, 완료 상태·포인트 표시.
- `GridViewModel.kt`, `DemoResets.kt`: 주차·Mock·선택 지역 재확인과 원자적 항목별 초기화.
- `Models.kt`, `MissionEngine.kt`, `GameStore.kt`: 독립 반영 플래그·완료 판정·고유 보너스 ID·저장 형식 호환 처리.
- `DemoResetsTest.kt`, `DemoResetFlowTest.kt`, `GameStateCodecTest.kt`, `GridGuildFlowTest.kt`: 분리·중복 방지·초기화 UI·영속성·제한 상태 검증.
- `app/build.gradle.kts`: versionName 0.4.0 / versionCode 4.
- `PRD.md`, `README.md`, `docs/TEST_REPORT.md`, `DEV_LOG.md`: 요청한 항목별 초기화 요건 및 사용법/실제 결과.

### 4. 빌드 및 테스트 결과

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 성공 (48초). 단위 테스트 **41개 통과**, Lint 오류 0 / 기존 경고 9개.
- API 34 에뮬레이터에서 `DemoResetFlowTest,GameStateCodecTest,GridGuildFlowTest,UiRegressionTest` **14개 통과** (화면 11 + 저장 코덱 3), 32.13초. 이번 작업의 컴파일/테스트 실패 없음.
- 도시/개인/미션의 독립 초기화, 취소, 전체 초기화, 재실행 후 유지, 당일 미션 재시작 및 개인/도시 +120 XP를 확인했다. 기존 미션·잠금·배터리/버튼 안정성·P/D 제한 회귀도 통과했다.
- 실제 버튼·확인창을 `artifacts/reset-controls/`의 에뮬레이터 캡처로 시각 검토했다. 테스트 전 기록을 백업했고 종료 후 복원 파일과 SHA256 일치 확인. 0.4.0에서 사용자 기존 Lv.3·도시 360 XP·개인 360 XP 표시 확인, 테스트 패널을 열어 두었다.
- Git index 62개 파일에 대한 `scripts/check-secrets.ps1` 및 `git diff --cached --check` 통과. 로컬 설정·SDK 도구·빌드·로그·캡처는 Git에서 제외했다.

### 5. 남아 있는 오류와 다음 개발 작업

- 빌드/자동 테스트 실패는 없다. 기존 Lint 경고 9개와 SDK 기어 API deprecation은 남아 있다.
- schema 2 저장은 이전 앱이 지원하지 않으므로 이전 APK로 다운그레이드하지 않고 최신 소스로 이어서 작업한다.
- 실제 Pleos Navi 목적지·경유지 콜백 및 충전 변화에 따른 종단 보상 검증은 별도 필요하다.
- GitHub origin은 미설정이며 이번 변경은 로컬 커밋으로 남긴다. 사용자 비공개 저장소 연결·인증 후 push가 필요하다.

## 2026-11-05 — 0.4.1 목적지 새로고침·재탐색 수정

### 1. 현재까지 구현한 기능

- Mock 목적지 새로고침의 빈 구현을 제거했다. 조회 중/완료, 최근 확인 시각과 요청 번호를 표시해 동일 목적지의 반복 조회도 처리 여부를 알 수 있다. 선택한 데모 목적지는 보존한다.
- 재탐색에 요청 중 표시·중복 클릭 방지·지속되는 처리 결과를 연결했다. Mock은 시뮬레이션임을 버튼과 결과에 명시하고 실제 경로 계산을 주장하지 않는다.
- 공식 SDK 전용 `onReRouteEnded()`로 재탐색 종료 알림을 반영한다. 일반 `onRoutingCompleted()`는 정보 재조회만 수행한다. 미응답은 12초 후 성공 미확인으로 표시하고 재시도를 허용한다.
- 목적지/경유지 설정도 같은 단일 요청 상태를 사용하며 실제 콜백 좌표가 일치할 때만 확인한다. UI는 요청 중 내비 버튼을 비활성화하며 미션 진행/XP를 변경하지 않는다.
- 콜백 처리를 Main 디스패처로 직렬화하고 타이머를 요청 전 설정/종료 시 취소한다. 연결 전 호출도 안내를 제공한다.
- Aside가 공식 문서와 3.0.5 바이너리의 전용 종료 이벤트·공유 콜백·목적지 미설정 계약 부재를 읽기 전용으로 검토했다. 확인된 사실을 SDK 검증 문서와 구현에 반영했다.

### 2. Pleos SDK 연동 완료·미완료

- 공식 NaviHelper 타입을 직접 호출하는 `PleosNaviClient`와 응답 처리 수정의 컴파일은 완료했다. 테스트용 전송 계층은 앱의 실제 실행 경로에 주입하지 않는다.
- 실제 Vehicle 현재값/P/UX의 이전 실응답 성공 상태는 유지한다. 실제 Navi 목적지 콜백 미수신 및 경유지/재탐색 종단 성공은 여전히 미완료다.
- 종료 이벤트는 요청 ID가 없는 공유 이벤트이므로 ‘종료 알림 수신’으로 표시하며 특정 요청의 인과관계나 실제 경로 형상을 보장하지 않는다. Mock을 실제 SDK 성공으로 처리하지 않았다.
- 기어 P + CarUxRestrictions, XP·도시 해금·초기화 정책은 유지했다.

### 3. 변경된 주요 파일

- `Repositories.kt`, `NavigationRequests.kt`: 요청 상태/번호/시간·타임아웃 및 동작하는 Mock 갱신/재탐색.
- `PleosNavigationRepository.kt`, `NaviClient.kt`: 공식 SDK 호출, Main 직렬화와 전용 콜백·오류 처리, 테스트 주입 경계.
- `GridViewModel.kt`, `GridGuildApp.kt`, `NavigationControls.kt`: 버튼 피드백·중복 방지·목적지 확인 조건.
- `NavigationFlowTest.kt`, `NavigationRepositoryTest.kt`: 두 버튼의 실제 UI 및 테스트 전송 계층 회귀 검증.
- `app/build.gradle.kts`: versionName 0.4.1 / versionCode 5.
- `README.md`, `docs/SDK_VERIFICATION.md`, `docs/TEST_REPORT.md`, `DEV_LOG.md`: 직접 테스트 방법과 SDK 계약/검증 범위.

### 4. 빌드 및 테스트 결과

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 최종 성공 (24초). 단위 테스트 **41개 통과**, Lint 오류 0 / 기존 경고 9개.
- API 34의 내비 UI/전송 계층 + 기존 초기화/저장/흐름/점멸 회귀 클래스 **20개 모두 통과**, 40.154초. 실제 화면 테스트 13개, 코덱 3개, 내비 테스트 전송 계층 4개다.
- 새로고침 반복/목적지 보존/재탐색 결과·중복 클릭 차단, 전용 이벤트 대기, 무관 이벤트 비완료, 타임아웃·예외·SDK 오류·재시도를 검증했다. Fake 기반 검증을 실제 SDK 실응답 검증으로 집계하지 않았다.
- 실제 에뮬레이터 화면을 `artifacts/navigation-fix`에 캡처해 검토했다. 사용자 기록은 테스트 전 백업했고 최종 복원 파일과 SHA256 일치 확인. APK 0.4.1 설치 및 재실행 성공.
- Git index 67개 파일의 `scripts/check-secrets.ps1` 및 `git diff --cached --check` 통과. 복원 후 충전 미션 탭에서 두 버튼이 활성 상태로 보이는 것도 확인했다.

### 5. 남아 있는 오류와 다음 개발 작업

- 이번 범위 빌드/자동 테스트 실패 없음. 기존 Lint 경고 9개 및 SDK 기어 API deprecation은 남아 있다.
- 실제 Pleos에서 경로 안내 활성화 후 목적지 콜백·재탐색 종료·경유지 좌표를 검증해야 한다. 미응답 원인을 실제 내비 서비스/목적지 상태와 함께 확인한다.
- 공유 SDK 콜백에는 요청 ID가 없어 이전/외부 요청과의 완전한 상관관계는 검증할 수 없다. 앱의 요청 번호는 UI 피드백용이다.
- GitHub origin 미설정으로 로컬 커밋만 남긴다. 원격 생성·인증·push는 수행하지 않는다.

## 2026-11-05 — 0.5.0 목적지 변경·주변 충전소 및 실행 피드백

### 1. 현재까지 구현한 기능

- Mock 충전 미션 상단에 목적지 변경 버튼을 추가했다. 삼락·센텀·광안리·기장 선택과 현재와 다른 준비된 위치의 랜덤 변경을 제공한다. 실제 내비 목적지는 바꾸지 않는다.
- 목적지 반경 5 km의 가상 충전소만 Haversine 직선거리로 필터링·정렬한다. 충전소 좌표는 고정하며 목적지에 따라 후보 수/거리/순서가 바뀐다.
- 샘플 위치 관계도와 목록 번호·거리를 연결했다. 실제 도로 지도·경로·시설이라고 표시하지 않는다. 후보 없음·목적지 미확인·고SOC 추천 중지는 구분한다.
- 새로고침은 수동 재조회 보조 기능으로 설명하고 재탐색과 함께 ‘내비 연결 도구’에 접었다. 목적지 변경 후에는 별도 새로고침 없이 후보가 갱신된다.
- 진행 미션은 목적지 변경 후에도 유지하며 충전소 이름/설명을 표시한다. 목적지 변경만으로 XP를 지급하거나 완료 상태를 취소하지 않는다.
- Aside 읽기 전용 코드/UX 검토에서 지적한 핀 겹침, 고SOC 빈 후보 오해, 진행 미션 이름, SDK/Mock 안내, 가상 시설/접근성 설명을 반영했다. 실제 UI 검증과는 분리했다.
- 사용자 요청에 따라 PRD F03/F09에 반경 후보 갱신과 Mock 목적지 조작을 반영했다. 원문 보관본은 보존했다.

### 2. Pleos SDK 연동 완료·미완료

- 공식 SDK 호출/타입 및 기어 P + CarUxRestrictions 정책은 유지한다. 기존 onDestinationChanged→목적지 재조회→후보 재계산 경로는 코드와 Fake 전송 계층에서 검증했다.
- Vehicle 실제 현재값/P/UX의 이전 검증 성공 상태를 유지한다. 실제 Navi 목적지 콜백 미수신, 경유지·재탐색 및 충전 변화→보상 종단 검증은 여전히 미완료다.
- 주변 시설/전력망/XP는 명시적인 샘플·데모다. 실제 충전소 API·지도·경로 형상은 연동하지 않았다.

### 3. 변경된 주요 파일

- `ChargingRecommendations.kt`, `ChargingRecommendationsTest.kt`: 테스트 목적지·랜덤·거리·반경·상태 구분.
- `Repositories.kt`, `GridViewModel.kt`: Mock 목적지 선택/랜덤 요청.
- `NavigationControls.kt`, `GridGuildApp.kt`, `NearbyStationMap.kt`: 목적지 선택창·내비 도구 접기·위치 관계도·거리·빈 상태·미션 안내.
- `DestinationDemoFlowTest.kt`, `NavigationFlowTest.kt`, `NavigationRepositoryTest.kt`: 목적지 UI와 기존 요청 흐름 및 SDK 이벤트 처리 회귀.
- `app/build.gradle.kts`: versionName 0.5.0 / versionCode 6.
- `PRD.md`, `README.md`, `docs/DESTINATION_FEEDBACK.md`, `docs/TEST_REPORT.md`, `DEV_LOG.md`: 요구/실행법·실측 피드백·결과.

### 4. 빌드 및 테스트 결과

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 최종 성공 (13초). 단위 **45개 통과**, Lint 오류 0 / 기존 경고 9개.
- 표준 API 34 에뮬레이터 7개 클래스 **24개 모두 통과**, 48.805초. 화면 16개 + 코덱 3개 + 내비 테스트 전송 계층 5개다.
- 삼락 1→센텀 2→기장 0, 랜덤 다른 위치, SOC 90/89, 목적지 변경 후 기존 미션 완료·개인/도시 각120 XP, 기존 안전/초기화/점멸 회귀를 확인했다.
- 별도 일반 앱 실행에서도 센텀→광안리→기장을 직접 선택해 후보 수/첫 후보 변경을 확인했다. PNG를 시각 검토하고 피드백 문서를 작성했다.
- 테스트 전 기록을 백업/복원했고 수동 QA 후에도 원래 저장 파일과 백업의 SHA256이 같았다. 0.5.0 설치 및 재실행 성공.

### 5. 남아 있는 오류와 다음 개발 작업

- 이번 범위 빌드/테스트 실패 없음. Lint 기존 경고 9개 및 SDK 기어 API deprecation은 남아 있다.
- 실제 Pleos 목적지 이벤트와 실제 충전소 데이터 검증이 우선이다. 현재는 가상 3곳·직선거리 반경이며 실제 우회 경로/도착 SOC 추천은 지원하지 않는다.
- 후보 목록까지 스크롤이 필요하고 위치 관계도의 공간감/핀 표기에는 개선 여지가 있다. 실제 차량 가로 화면·큰 글자 검증, 위치도 접기/넓은 화면 배치가 다음 UX 과제다.
- Mock 목적지는 세션 단위로 유지되어 프로세스 재시작 시 삼락으로 돌아간다. 반복 QA에서 마지막 목적지 저장/모의 시간 선택을 후속 검토한다.
- 이 저장소에서 병행 중인 다른 작업의 DEV_LOG 기록은 보존하고 이번 기능 커밋에는 해당 기록을 포함하지 않는다. GitHub origin 미설정으로 push는 수행하지 않는다.

## 작업 종료 시 갱신 양식

아래 다섯 항목을 실제 결과로 작성하고 관련 코드와 같은 커밋에 포함한다.

1. 현재까지 구현한 기능
2. Pleos SDK 연동 완료·미완료 현황
3. 변경된 주요 파일
4. 실행한 빌드·테스트 명령과 실제 결과
5. 남아 있는 오류·제약과 다음 개발 작업
