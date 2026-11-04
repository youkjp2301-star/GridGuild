# GridGuild

전기차 충전 미션으로 부산의 가상 도시를 성장시키는 Kotlin / Jetpack Compose Android 앱.
마스터 요구사항은 루트 [PRD.md](PRD.md)의 제품 기획서 v2.0이다.

## 실행

Android Studio에서 이 폴더를 열고 JDK 17, Android SDK 35를 설정한 뒤 `app`을 실행한다.
표준 Android API 28 이상에서는 **MOCK**, Pleos Connect API 34 에뮬레이터에서는 **Pleos SDK**를 선택할 수 있다.
이 PC에는 프로젝트 내부 `.tools`에 JDK/SDK를 구성했다. 일반 Android Studio 설치에서도 그대로 열 수 있다.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build.ps1
```

일반 터미널 환경에서는:

```text
gradlew.bat :app:assembleDebug :core:test :app:lintDebug
gradlew.bat :app:connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
자동 화면 테스트는 연결된 표준 Android 에뮬레이터에서 실행한다. Pleos 실제 SDK 통합은 별도 검증한다.

## 데모 흐름

1. 소속 구·군을 선택하고 MOCK으로 시작한다.
2. 충전 미션 → 삼락 강변 충전소 → 경유지 추가(MOCK) → 미션 시작.
3. 상단 연결·테스트에서 충전 ON, SOC +2%를 누른다.
4. 개인·도시 120 XP, 도시 Lv.2, 랭킹 반영을 확인한다.
5. D단 또는 UX 제한을 누르면 도시와 게임 조작이 차단된다. P단으로 돌아오면 도시가 열린다.
6. 앱을 종료·재실행해 지역/XP/진행 미션이 유지되는지 확인한다.

시간대는 Asia/Seoul. 센텀 야간 미션은 22–07시, 나머지 fixture는 상시다.
샘플 3곳의 미션을 같은 한국 날짜에 모두 완료하면 데모 50 XP를 추가 지급한다.

## 실제 SDK와 Mock 경계

- 실제 코드: Vehicle 2.3.2 capability/SOC/충전 스위치/기어, NaviHelper 3.0.5 목적지/경유지/재탐색, AAOS CarUxRestrictions.
- Mock: 충전소, 계통 여유, 경쟁자/공동 기본 점수. 실제 경로 형상·장소 인증·계통 기여·실물 보상은 제공하지 않는다.
- 실제 SDK가 없으면 값은 미확인으로 유지하며 오류를 표시한다. 사용자가 MOCK을 직접 선택할 수 있다.
- 모든 XP는 **충전 장소 미인증 데모 성과**다. SOC 증가만으로 실제 전력망 기여를 주장하지 않는다.
- Pleos에서 Mock을 선택해도 실제 플랫폼 UX 제한을 우회하지 않는다.
- 문서상의 지원과 실제 에뮬레이터 성공 여부는 [검증 보고서](docs/TEST_REPORT.md)에서 구분한다.

## 구조

```text
core/       순수 Kotlin 모델, 미션 판정, 12단계 레벨, 시즌/랭킹, 단위 테스트
app/        Compose 4탭 UI, ViewModel, DataStore, 실제/Mock 저장소, 화면 테스트
docs/       마스터 기획서, 요구사항별 계획, 공식 SDK 검증, 테스트 결과
scripts/    로컬 빌드·도구·에뮬레이터 설치 보조
```

주행 중 사용하는 앱으로 등록하지 않았으므로 OS가 앱 화면을 차단할 수 있다. 게임 화면은 주차 시에만 사용한다.
SDK 기어 API는 현재 SDK에 존재하지만 deprecated로 표시되어 있으며, 차후 AAOS GEAR_SELECTION로 이전할 수 있다.

- [MVP 구현 계획](docs/IMPLEMENTATION_PLAN.md)
- [공식 SDK 근거 및 권한](docs/SDK_VERIFICATION.md)
- [Aside 조사 통합](docs/ASIDE_RESEARCH.md)
- [검증 보고서](docs/TEST_REPORT.md)

## 교육장 PC / 집 PC에서 이어서 개발하기

개발 기준은 루트 [PRD.md](PRD.md)이며, [DEV_LOG.md](DEV_LOG.md)에 구현·SDK 검증·테스트·다음 작업을 기록한다.
`docs/MASTER_REQUIREMENTS.md`와 원래 `기획서.txt`는 원문 보관본으로 유지한다.

### 새 컴퓨터 최초 설정

1. Git, Android Studio, JDK **17**을 설치한다. Android Studio에 포함된 JBR 버전이 다르면 Gradle JDK를 별도로 17로 지정한다.
2. Android Studio SDK Manager에서 Android SDK Platform **35**, Build Tools **35.0.0**, Platform Tools를 설치하고 Android SDK 라이선스를 확인한다.
3. 본인 GitHub 계정으로 비공개 저장소 접근을 설정한다. HTTPS의 Git Credential Manager 또는 SSH 키를 사용하고, 토큰·비밀번호를 저장소 파일이나 원격 URL에 넣지 않는다.
4. 저장소를 clone하고 Android Studio에서 폴더를 연다.

```powershell
git clone https://github.com/YOUR_ACCOUNT/GridGuild.git
cd GridGuild
git config user.name "YOUR_NAME"
git config user.email "YOUR_COMMIT_EMAIL"
```

위 URL과 이름·이메일은 예시다. 실제 생성된 **비공개** 저장소 주소와 본인 정보를 사용한다.
Android Studio의 Settings → Build Tools → Gradle에서 Gradle JDK **17**을 선택한다.
`local.properties`는 각 PC의 Android Studio가 생성하며 공유하지 않는다. 필요한 경우 각 PC에서만 다음 형식으로 만든다.

```properties
sdk.dir=C:/Users/YOUR_WINDOWS_USER/AppData/Local/Android/Sdk
```

Gradle 8.10.2는 저장소에 포함된 Wrapper가 내려받으며 별도 Gradle 설치는 필요 없다.
공식 Pleos SDK는 `settings.gradle.kts`에 등록된 공식 Maven에서 받는다. 현재 사용 버전은 공개 다운로드가 가능하며 인증정보를 저장소에 넣지 않는다.
향후 인증이 요구되면 각 PC의 사용자 홈 `~/.gradle/gradle.properties` 또는 환경 변수에만 저장하고 프로젝트 파일에는 기록하지 않는다.

```powershell
.\gradlew.bat :app:assembleDebug :core:test :app:lintDebug
```

macOS / Linux에서는 `chmod +x gradlew` 후 `./gradlew`로 같은 작업을 실행한다.
표준 API 34 이상 에뮬레이터에 `app`을 실행하면 Mock 개발을 계속할 수 있다.
실제 Pleos 기능은 전용 이미지·서비스가 필요하다. 이미지·AVD·JDK·Android SDK는 PC마다 설치하며 Git으로 복사하지 않는다.

### Pleos 에뮬레이터 설정

- 공식 [환경 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)를 따른다.
- SDK Update Sites의 Windows/Linux 이미지 URL:
  [Pleos System Image XML](https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml).
- API 34 / Pleos Connect v2.1.0 AVD, RAM 4GB 이상, Cold boot를 사용한다.
- 이 PC에서는 WHPX 사용 가능 여부를 `emulator -accel-check`로 확인했다. 다른 PC의 가속 환경은 별도 확인한다.
- Virtual Console: `adb -s DEVICE_SERIAL forward tcp:8088 tcp:8088` 후 [로컬 콘솔](http://127.0.0.1:8088/)을 연다.
- GPU 문제가 있으면 AVD 그래픽 설정을 Host / Software로 조정하고 Cold boot한다. 검증 결과는 DEV_LOG에 기록한다.

### 매 작업 시작과 종료

```powershell
# 시작: 이전 PC의 변경 사항 받기
git status
git pull --ff-only

# 기능 작업은 별도 브랜치 권장
git switch -c feat/YOUR_TASK

# 종료: DEV_LOG.md 갱신 후 검증·검토
.\gradlew.bat :app:assembleDebug :core:test :app:lintDebug
git add .
powershell -ExecutionPolicy Bypass -File scripts/check-secrets.ps1
git diff --cached --stat
git diff --cached
git commit -m "Describe the change"
git push -u origin HEAD
```

다른 PC에서는 같은 브랜치를 checkout하고 pull한다. 미커밋 변경이 있으면 먼저 확인하고 보관한다.
충돌이 나면 양쪽 변경을 검토하며 해결한다. `reset --hard`, `clean -fd`, 강제 push로 덮어쓰지 않는다.
SDK 인증정보·서명 키·`.env`·`local.properties`·`.tools`·IDE 상태·APK·빌드·로컬 테스트 산출물은 `.gitignore`로 제외한다.
Wrapper JAR는 clone 후 빌드에 필요하므로 의도적으로 추적한다. `.gitignore`는 파일 내용 속 비밀정보를 찾아주지 않으므로 커밋 전 검사를 함께 실행한다.

### GitHub 비공개 저장소 연결 단계

현재는 로컬 `main` 저장소만 준비하며 원격 생성·업로드는 수행하지 않았다.

1. 본인 GitHub 계정으로 로그인하고 New repository에서 **Private**를 선택한다. 조직 저장소라면 생성 권한을 확인한다.
2. 기존 로컬 프로젝트를 올릴 때 GitHub에서 README/.gitignore/라이선스를 추가 생성하지 않아 초기 이력 충돌을 피한다.
3. 각 PC에서 Git 작성자 이름·이메일과 GitHub 인증을 설정한다. 이 PC에는 사용자가 알려준 작성자 정보를 로컬로 설정했다. GitHub CLI와 원격 저장소는 아직 설정하지 않았다.
4. 실제 저장소 URL을 확인한 뒤 연결한다.

```powershell
git remote add origin https://github.com/YOUR_ACCOUNT/GridGuild.git
git add .
powershell -ExecutionPolicy Bypass -File scripts/check-secrets.ps1
git commit -m "Initialize GridGuild MVP"
git push -u origin main
```

이미 origin이 있으면 `git remote -v`로 먼저 확인하고 임의로 교체하지 않는다.
GitHub 저장소 생성 또는 이 에이전트를 통한 인증·업로드가 필요하면 계정·저장소 이름·Private 여부와 실행 승인을 확인한 뒤 진행한다.
