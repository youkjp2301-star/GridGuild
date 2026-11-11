# GridGuild

전기차 충전 미션으로 부산의 가상 도시를 성장시키는 Kotlin / Jetpack Compose Android 앱.
마스터 요구사항은 루트 [PRD.md](PRD.md)의 제품 기획서 v2.0이다.

## 0.11.0 — 현재 레벨의 도로 위에 들어가는 1인칭 도시

- **MOCK 시작 → 연결 · 테스트 → N단 도시 체험 · TEST**에서 현재 소속 지역의 도시 XP에 맞는 장면을 연다. 광안리·센텀 목적지에 따라 도로와 주변 건물, 랜드마크가 함께 달라진다. 목적지 변경은 기존 Mock 내비 경로를 사용한다.
- 장면 구성은 **두 지역 × Lv.1~12 = 24개 독립 PNG**다. Lv.2는 흙길·초가집과 시대에 맞는 교량/공연장 전신, Lv.12는 미래 도로와 진화한 광안대교/영화의전당을 같은 운전자 눈높이에 구성한다. 랜드마크를 보여주는 실사 슬라이드를 대체하는 사용자 정정 요구다.
- **단계 둘러보기**에서는 현재·이전에 해금한 단계만 선택한다. `지금의 도시`로 현재 레벨에 돌아오며 XP는 바뀌지 않는다. XP 초기화나 소속 지역 변경으로 레벨이 낮아지면 UI와 이미지 로더가 높은 단계 장면을 즉시 제한한다.
- 장면은 **AI로 생성한 가상 재구성**이다. 실제 촬영 사진·실시간 3D·차량 카메라·역사적 사실을 의미하지 않는다. 자연스러운 재질과 일관된 도로 위 시점을 검수하며, AI 흔적이 전혀 없다고 보장하지 않는다. [단계별 사양·자산·시각 검수 기준](docs/DRIVER_SCENES.md).
- 목적지와 정확한 레벨을 함께 키로 삼는 최대3장 LRU 캐시, IO 이미지 해독, OS 모션 설정에 따른 짧은 진입 효과를 사용한다. 과거 PNG/JPEG와 라이선스 기록은 보존한다.
- 기존 HOME/ROUTE 미션·XP·도시 성장·실제 SDK 구조는 유지한다. **N은 Mock 체험 상태이며 실제 P단·플랫폼 UX 보호를 완화하지 않는다. 실제 N단 자동 진입은 미완료다.** D/UX 제한·플랫폼 불허·백그라운드에서는 체험을 닫는다.
- 자산 생성·시각 검수·빌드·기기 실행의 최신 완료 범위는 [TEST_REPORT.md](docs/TEST_REPORT.md)에 구분해 기록한다. GitHub 원격은 미설정이므로 다른 PC로 옮기기 위한 원격 동기화는 별도 설정이 필요하다.
- System UI/전화 앱 종료의 [원인 진단·데이터 백업·실제 단계별 결과](docs/EMULATOR_DIAGNOSIS_20261110.md)를 남겼다. 원본 AVD를 초기화하지 말고 보존한다. QA의 설치 검증 ANR와 원본 시스템 종료의 미확정 원인을 구분한다.

## 0.10.0 — 목적지와 연결되는 N단 도시 체험 (이전 구현 기록)

아래는0.10.0 당시 구현의 보존 기록이다. 현재 도로 위1인칭 장면 기준은 위0.11.0과 [DRIVER_SCENES.md](docs/DRIVER_SCENES.md)를 따른다.

- **연결 · 테스트 → N단 도시 체험 · TEST**에서 가로 전체 화면을 연다. 광안리·센텀 목적지가 이미 설정되어 있으면 해당 풍경이 바로 열리고, 그 외 목적지는 두 지역 중 선택한다.
- 체험 하단 **광안리 / 센텀시티** 버튼도 기존 Mock 내비 목적지 변경 경로를 사용한다. 나가면 ROUTE 후보 목록·위치도에도 같은 목적지가 반영된다. X / 뒤로가기로 이전 탭에 돌아온다.
- 광안대교와 영화의전당은 **실제 촬영 사진**이다. AI 생성 이미지·실시간 카메라·3D 주행 시뮬레이션이 아니다. 사진을 넓게 유지하고 N TEST·현재 소속 지역/Lv/XP만 HUD에 얹었다. 앱은 화면 방향을 바꾸고 종료 시 복원하며, 차량의 고정 가로 화면에서도 대응한다.
- N은 **Mock 체험 상태**이며 실제 기어는 조작하지 않는다. P단·플랫폼 UX 제한을 계속 확인한다. 실제 N 자동 진입은 구현하지 않았다. D/UX 제한·앱 백그라운드 전환 시 체험이 닫힌다. 기록·미션·XP와 도시 해금 정책은 유지한다.
- 사진의 첫 로딩은 기존 최소3초 도시 연출을 사용하며 캐시 재방문은 즉시 표시한다. 출처·라이선스는 체험의 **사진 출처**와 [NEUTRAL_CITY.md](docs/NEUTRAL_CITY.md)에 있다.

## 0.9.0 — 간결한 카드·두 추천 시간·풍성한 데모

- ROUTE 카드의 확인 시각/반복 Mock 문구/일반 미확인 경고를 제거했다. 상태와 대수는 크게 표시하고, 상태 테스트는 카드의 **⋮**에 있다. 추천 상세/커넥터/**상태 섞기**는 목록 상단 조절 아이콘에 접었다.
- Mock 충전소는 여유·혼잡·정보 없음·빈자리 없음이 섞여 시작한다. 첫 안내 충전소는 여유로 두고 다른 곳은 무작위 배정한다. 수동 상태 섞기는 전체를 섞는다. 전경에서60초마다 관측을 재발행해 방치 후 전부 정보 없음이 되는 문제를 막았다. 의도적인 미수신/오래된 정보 테스트는 그대로다.
- HOME에는 **16:00–17:00 / 22:00–23:00** 두 모의 구간을 표시한다. Mock에서는 시간 조건을 자동 적용하고 등록 후 **충전 테스트 · +120 XP**를 누르면 ON·SOC 상승을 같은 판정 경로로 모의해 개인/도시 보상을 받는다. 수동 ON/SOC 조작도 시간 때문에 기다리지 않는다. 일일 중복 지급·목표 SOC·출처/신선도·P/UX 제한은 유지한다.
- 실제 Pleos 모드는 Mock 시간 우회를 적용하지 않는다. 배터리/목표/출발 조건이 불확실하면 지연을 권하지 않는 기존 안전 정책을 유지한다. 모의 시간은 실제 계통 최적 시간이나 충전 제어가 아니다.
- 지역 샘플 기본 점수는 **Lv.3~11**을 고루 포함하고 개인 경쟁자는 **32명**이다. 내 저장 XP/도시 XP는 줄이거나 상한 처리하지 않아 내 기여에 따라 Lv.11을 넘을 수 있다.
- 앱 시작과 캐시 없는 도시 로딩은 실제 경과 시간으로 **최소3초** 표시한다. 현재 해금 아트를 인트로 중 미리 읽어 연속 대기를 줄이고, 캐시가 있으면 로딩을 깜빡 띄우지 않는다. 주행 제한·화면 취소는 즉시 우선한다.
- 반복 충전 시험은 연결·테스트의 **충전 미션 초기화** 후 진행한다. 모든 테스트 보상과 랭킹 경쟁자는 데모다.

## 0.8.0 — 이동 중 충전 이용 현황

- 기존 ROUTE CHARGE 카드에 **여유 / 혼잡 / 이용 가능 없음 / 정보 없음** 배지, `5기 중 3기 이용 가능`, 충전기 유형과 확인 시각을 통합했다. 고장·점검은 이용 가능 수에서 제외하고10분 초과 정보는 미확인으로 표시한다.
- 여유·거리·차량 커넥터 호환성을 함께 고려해 정렬한다. 가용0기/알려진 불일치는 새 미션 참여를 제한하고 기존 진행 미션·XP·도시 성장은 유지한다.
- 카드의 **상태 테스트**에서 네 상태·오래된 정보·미수신을 재현한다. 목록의 **내 커넥터(테스트)**로 호환성을 시험하고 **상태 새로고침(MOCK)**으로 데모를 갱신한다.
- 실제 한국환경공단 API는 미연동이다. 충전기 현황은 Pleos 차량 모드에서도 **MOCK**이며 대기시간·도착 시 자리 확보를 보장하지 않는다. 현재 거리는 목적지 직선거리이며 경로 이탈 거리는 미확인이다.
- 데이터 경계·추천 규칙·향후 API 연결 조건은 [CHARGER_AVAILABILITY.md](docs/CHARGER_AVAILABILITY.md)에 기록했다.

## 0.7.0 — 도시 디테일과 인터랙션

- 크림·포레스트 그린으로 UI를 정리하고 홈 도시를 확대했다. **홈 · 미션 · 내 도시 · 랭킹** 메뉴는 호버/포커스 영역과 누름 반응, 방향에 따른 슬라이드를 제공한다.
- Lv.1~12에 서로 다른 **정밀 건축 모형 스타일** 아트를 연결했다. 실제 비례와 낮은 채도, 석재·목재·벽돌·유리·콘크리트 재질, 창호·옥상 설비·도로·철도·태양광 시설의 세부를 반영했다. 미해금 도시는 계속 잠금 이미지다.
- 실제 초기 데이터/도시 이미지 준비 중에는 별도4프레임 도시 조립 애니메이션을 표시한다. **연결 · 테스트 → 도시 로딩 연출 미리보기**에서 다시 볼 수 있다. 평소에는 불필요한 대기 시간을 추가하지 않는다.
- 미션 보상이 저장되면 청록 조각이 성장 막대로 모이고 도시 XP가 증가한다. 과거 기록 재실행·초기화 때 재생하지 않는다. 노란 후광은 없다.
- 모션이 보이지 않으면 에뮬레이터 개발자 옵션의 애니메이터 배율이0인지 확인한다. 앱은 OS 모션 설정과 P단/플랫폼 UX 제한을 따른다.
- `scripts/test-device.ps1`은 테스트 중 끈 애니메이션 배율을 종료/실패 시 원래 값으로 복원한다.
- [승인 방향과 구현 범위](docs/design/VISUAL_DIRECTION_V2.md). 영상 파일이 아닌 PNG 키프레임·Compose 애니메이션이며, HOME/ROUTE 충전·XP 정책과 SDK 연동 범위는 유지한다.

## 0.6.0 — 간단한 우리 집 충전

- **충전 미션 → HOME CHARGE**가 기본이다. 집 충전소 이름을 최초 한 번 등록하고, 이후 ‘충전 미션 참여하기’ 한 번으로 SOC·충전 스위치를 자동 확인한다. 조건 달성 시 개인·도시에 각120 데모 XP가 반영된다.
- 목표 SOC·출발 시각은 톱니바퀴의 선택 고급 설정에 저장한다. 출발 시간을 모르는 기본 상태에서는 **필요할 때 바로 충전**을 표시한다. 저SOC·목표 미달·출발 여유 부족에서도 지연을 권하지 않는다.
- 조건이 맞을 때의 밤11시~새벽3시는 모의 추천이며 앱은 충전 시작/중지를 제어하지 않는다. 판정 및 제한은 [HOME_CHARGE.md](docs/HOME_CHARGE.md)에 기록했다.
- **ROUTE CHARGE**의 기존 목적지·경유지·XP 정책은 유지한다. 삼락·센텀·광안에 가상 충전소를 한 곳씩 추가해 총6곳이며 위치도는 후보 목록 아래에 있다. ‘미션 참여 시간 아님’은 시설이 닫혔다는 뜻이 아니다.

- 충전 미션 상단 **목적지 변경 (테스트)**에서 삼락·센텀·광안리·기장 중 선택하거나 현재와 다른 준비된 위치를 랜덤 선택한다. 실제 내비 목적지를 변경하지 않는 Mock 전용 조작이다.
- 목적지 반경 **5 km**의 가상 충전소만 직선거리로 필터링·정렬한다. 충전소 자체의 좌표는 고정이며 목적지가 달라질 때 후보와 거리순이 바뀐다. 위치 관계도에 같은 번호로 표시한다.
- 새로고침은 자동 목적지 수신이 늦거나 연결을 재확인할 때 쓰는 수동 재조회다. 재탐색과 함께 **내비 연결 도구**에 접어 두었으며 평소에는 목적지 변경만으로 후보가 갱신된다.
- 실제 Pleos 모드는 `onDestinationChanged` → 목적지 재조회 → 수신 좌표로 같은 후보 계산을 수행한다. 실제 목적지 콜백/경유지 종단 검증은 미완료다.
- 충전 미션의 목적지 새로고침과 재탐색은 처리 중 표시, 중복 클릭 방지, 요청 번호·처리 시각 및 완료/오류 결과를 제공한다.
- Mock 새로고침은 현재 데모 목적지를 다시 확인한다. 목적지가 같으면 충전소 순서도 유지되며, 반복해 눌러도 요청 번호와 확인 시각으로 처리 여부를 확인할 수 있다. 재탐색은 명확한 데모 시뮬레이션이며 실제 길 안내를 계산하지 않는다.
- Pleos 재탐색은 공식 `onReRouteEnded()` 알림을 기다린다. SDK 오류나 12초 내 응답 없음은 성공 미확인으로 표시하고 다시 시도할 수 있다. 실제 Navi 서비스 종단 검증은 여전히 미완료다.
- Mock의 **연결 · 테스트 → 테스트 데이터 초기화**에서 도시 Lv, 충전 미션, 내 포인트를 각각 초기화할 수 있다. 항목마다 영향 범위를 확인한 뒤 실행하며 `취소`하면 기록을 유지한다.
- 네이비·청록·차분한 골드와 밝은 배경으로 색상 체계를 통일했다. 카드, 버튼, 수치, 탐색 메뉴와 대화상자의 형태·여백도 정리했다.
- 도시 화면은 중세·현대·미래 디오라마 아트 **12개 장면**으로 성장한다. **현재 및 이전에 해금한 도시만** 컬렉션에서 다시 볼 수 있으며 XP는 변경되지 않는다.
- 미해금 도시는 공통 잠금 이미지로 표시하며 선택할 수 없다. `다음 단계 확인하기`는 잠금 이미지와 해금 기준·남은 XP만 보여준다. 실제 다음 도시 아트·이름·랜드마크는 해금 전 공개하지 않는다.
- 이미지 렌더링에서도 해금 여부를 확인한다. 지역 변경이나 데모 초기화로 XP가 내려가면 이전 선택과 전환 중 이미지도 다시 제한한다.
- 상단 `데모 (Mock)`을 누르면 의미를 설명하는 창이 열리고 `데모 조작 열기`로 SOC·충전·P/D 조작을 열 수 있다.
- 같은 모드를 다시 선택해도 진행 중인 미션을 취소하지 않는다. 다른 모드로 바꿀 때는 미션 취소를 확인한다.
- 배터리 표시와 미션 버튼은 같은 시점의 데이터 판정을 사용한다. 정상 데모는 `데모 데이터 정상`, 실제 오래된 데이터는 갱신 지연으로 표시한다.
- 미션 시작 버튼은 시작 중/진행 중/다른 미션/오늘 완료/시간 외/차량 미확인을 구분한다.

아트는 사용자 레퍼런스 분위기로 새로 생성한 2.5D 이미지다. 실제 주변의 건물이나 실시간 3D 시뮬레이션은 아니다.
자산 위치와 생성 사양은 [CITY_ART.md](docs/CITY_ART.md)에 기록했다.

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

### 이 PC에서 직접 앱 열기

현재 PC에는 `.tools`에 에뮬레이터가 설치되어 있고 APK도 생성되어 있다.
아래는 **현재 PC 전용** 빠른 실행 방법이다. 다른 PC에서는 아래의 Clone 환경 설정을 먼저 따른다.

PowerShell에서 다음을 실행하면 에뮬레이터 창이 열린다.

```powershell
cd C:\Users\User\Desktop\Gridguild
Start-Process -FilePath '.\.tools\android-sdk\emulator\emulator.exe' -ArgumentList '-avd GridGuild_Mock_API34 -gpu host -feature -Vulkan -no-snapshot' -WindowStyle Normal
```

Android 홈 화면이 나타날 때까지 기다린 뒤, 같은 PowerShell에서 아래를 실행한다.
이 명령의 `-e`는 에뮬레이터가 하나만 실행된 경우를 전제로 한다.

```powershell
$gridAdb = '.\.tools\android-sdk\platform-tools\adb.exe'
& $gridAdb -e install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& $gridAdb -e shell am start -n com.gridguild/.MainActivity
```

아래 순서로 직접 테스트한다. 앱 데이터가 이미 저장되어 있으면 첫 지역 선택 화면을 건너뛸 수 있다.

1. 소속 구·군을 선택하고 MOCK으로 시작한다.
2. 충전 미션 → ROUTE CHARGE → 삼락 강변 충전소 → 경유지 추가(MOCK) → 미션 시작.
3. 상단 연결·테스트에서 충전 ON, SOC +2%를 누른다.
4. 개인·도시 120 XP, 도시 Lv.2, 랭킹 반영을 확인한다.
5. D단 또는 UX 제한을 누르면 도시와 게임 조작이 차단된다. P단으로 돌아오면 도시가 열린다.
6. 앱을 종료·재실행해 지역/XP/진행 미션이 유지되는지 확인한다.

기존 XP가 있다면 개인·도시 XP가 각각 **120 증가**하는지 확인한다.
충전소 버튼이 이미 '오늘 완료'이면 다른 상시 미션을 선택하거나,
연결·테스트에서 아래의 '충전 미션 초기화'를 사용해 다시 도전할 수 있다.
처음부터 시작하려면 별도의 '전체 데모 데이터 초기화'를 사용한다. 전체 초기화는 기존 데모 기록을 지운다.
실제 SDK 테스트에는 `GridGuild_Pleos_API34`가 필요하며, 현재 Navi 목적지 콜백은 미완료 항목이다.

도시 해금 UI는 **가상 도시** 탭에서 테스트한다. 아래로 스크롤하면 컬렉션이 보인다.
현재와 이전 단계는 실제 이미지를 눌러 방문할 수 있고, 미래 단계는 잠금 이미지로 표시되어 열리지 않는다.
`다음 단계 확인하기`에서는 도시 그림 대신 필요한 XP가 나와야 한다.
Lv.2 이상이면 Lv.1을 눌렀다가 `내 도시로 돌아가기`로 복귀해 보자. 둘러보기 전후 실제 XP는 그대로다.
최고 단계에서는 다음 단계 버튼 대신 `최고 단계 달성`이 표시된다.

### 테스트 데이터 초기화

상단 **연결 · 테스트**를 열고 패널을 아래로 스크롤하면 버튼이 보인다.
또는 **데모 (Mock) → 데모 조작 열기**로 진입한다. Mock이며 P단·플랫폼 UX 허용 상태에서만 사용할 수 있다.

| 버튼 | 초기화하는 항목 | 유지하는 항목 |
|---|---|---|
| 도시 Lv 초기화 | 선택 지역 도시 XP → 0, Lv.1로 복귀, Lv.2 이상 이미지 재잠금 | 내 포인트, 진행/완료 미션, 다른 지역의 도시 XP |
| 충전 미션 초기화 | 진행 미션 취소, 충전소 완료 상태·일일 보너스 판정 초기화, 당일 재도전 허용 | 내 포인트, 도시 XP·레벨 |
| 내 포인트 초기화 | 개인 누적·이번 시즌 포인트 → 0 | 도시 XP·레벨, 진행/완료 미션 |
| 전체 데모 데이터 초기화 | 지역 선택, 모든 XP·미션 기록 삭제, SOC 42%·충전 OFF | 없음 |

부분 초기화는 원래 지급 기록을 보관하며 항목별 반영 여부만 해제한다. 이후 미션 완료 시에는 개인·도시 XP가 새로 적립된다.
도시 Lv나 포인트만 초기화하면 오늘 완료한 충전소는 계속 완료 상태다. 다시 테스트하려면 충전 미션 초기화도 실행한다.
도시 Lv/포인트 초기화는 진행 미션을 유지하므로, 해당 미션이 완료되면 다시 XP가 적립될 수 있다.
현재 모드가 Mock일 때 저장된 데모 원장 전체에 적용하며, SDK 모드에서 얻은 위치 미인증 데모 XP도 같은 원장에 포함된다.
도시 랭킹의 모의 공동 기본 점수는 별도 샘플이므로 도시 XP 초기화 후에도 0점으로 표시되지 않을 수 있다.

저장 형식은 schema 3이며 schema 1/2의 기존 기록을 포인트 손실 없이 읽는다. 집 충전 설정/진행 정보가 추가되었으므로 이전 버전 APK로 되돌려 사용하지 않는다.

시간대는 Asia/Seoul. 센텀 녹색 미션 참여 시간은 22–07시, 나머지 ROUTE fixture는 상시다. 시설 운영시간 정보가 아니다.
기본3곳(삼락 강변·센텀 녹색·광안 해변)을 같은 한국 날짜에 완료하면 데모50 XP를 추가 지급한다. 새 충전소와 HOME 참여로 이 조건을 늘리지 않는다.

### 우리 집 충전 테스트

1. **충전 미션 → HOME CHARGE**에서 우리 집 충전소 이름을 등록한다. 다음 실행부터 자동으로 불러온다.
2. 출발 시각을 입력하지 않아도 ‘필요할 때 바로 충전’ 상태에서 참여할 수 있다.
3. 연결·테스트에서 충전 ON, SOC +2%를 누르면 개인·도시에 각120 XP가 지급되고 도시 레벨·랭킹에 반영된다. 같은 한국 완료 날짜의 HOME 보상은1회다.
4. 톱니바퀴의 목표 SOC·출발 HH:mm은 선택 사항이며 저장값을 재사용한다. 빈칸으로 지우면 기본 정책으로 돌아간다.
5. SOC 30% 미만이면 충전 지연을 권하지 않는다. 90% 이상이거나 +2%p 조건이 목표 SOC를 넘기면 추가 충전 참여를 막는다. 차량의 실제 충전 목표를 제어하는 기능은 아니다.

앱 프로세스가 완전히 종료되면 지속 관찰하지 않는다. 저장된 미션은 재실행 후 신선한 샘플로 이어가며, 기록하지 못한 과거 충전을 소급 지급하지 않는다.

### 목적지·재탐색 버튼 테스트

**충전 미션 → ROUTE CHARGE → 내비 연결 도구**를 열고 `목적지 새로고침`을 누르면 조회 중 표시 뒤 `데모 목적지 갱신 완료`와 요청 번호가 나온다.
다시 눌러 요청 번호가 증가하는지 확인한다. `경로 재탐색 (데모)`는 대기 표시 뒤 시뮬레이션 완료를 보여준다.
충전소의 `목적지로 설정`을 눌러 데모 목적지를 바꾼 후 새로고침해도 해당 목적지는 유지된다.
처리 중에는 내비 요청 버튼을 비활성화하며 미션 진행/XP는 변경하지 않는다.
Pleos에서는 내비 경로 안내를 시작한 뒤 목적지 좌표를 조회해야 재탐색할 수 있다. 종료 알림은 SDK 공유 이벤트로 요청 ID가 없어 특정 요청과의 인과관계를 확정하지 못한다.

### 테스트 목적지 변경 순서

1. **충전 미션 → ROUTE CHARGE → 목적지 변경 (테스트)**를 연다. 기본 삼락 권역은 삼락 강변/공원 2곳이다.
2. **센텀시티** 선택 → 반경 내 센텀/광안 권역 4곳을 확인한다. **광안리 해변**으로 바꾸면 같은 권역의 가까운 순서가 바뀐다. 위치 관계도는 목록 아래에 있다.
3. **기장 해안** 선택 → 반경 내 샘플 없음 안내를 확인한다. 실제 충전소가 없다는 뜻은 아니다.
4. **랜덤 위치로 변경** → 현재와 다른 준비된 위치가 선택되고 후보가 자동 갱신된다. 임의의 모든 좌표를 생성하는 기능은 아니다.
5. 삼락에서 미션을 시작한 뒤 기장으로 목적지를 바꿔도 기존 미션은 유지된다. 해당 미션을 완료/취소한 뒤 다른 미션을 시작한다.
6. SOC 90% 이상에서는 ‘충전 추천 중지’로 표시하고 후보·위치도를 숨긴다. SOC를 낮추면 다시 표시한다.

위치 관계도는 가상 충전소 좌표를 목적지 중심으로 표시하며 실제 지도·도로·경로 형상이 아니다. 직선거리 5 km는 도로 주행 거리 5 km와 다르다.
Mock 목적지는 현재 연결 세션에 유지되며 앱 프로세스 재시작 시 기본 삼락으로 돌아간다. 실제 앱은 내비에서 목적지를 다시 읽어야 한다.
직접 테스트 결과와 개선 우선순위는 [목적지 테스트 피드백](docs/DESTINATION_FEEDBACK.md)을 참고한다.

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

Pleos 전용 런타임 확인용 `PleosSdkProbeTest`도 포함한다. 표준 Android에서는 SDK 성공으로 처리하지 않고 건너뛴다.
에뮬레이터의 현재 사용자 ID는 `adb shell am get-current-user`로 확인한다. 예를 들어 Pleos 사용자 10에서는:

```text
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s DEVICE_SERIAL shell am instrument --user 10 -w -e class com.gridguild.PleosSdkProbeTest com.gridguild.test/androidx.test.runner.AndroidJUnitRunner
```

SDK 값·콜백을 제한 시간 내 받지 못하면 실패 메시지를 기록한다. 이 검사는 실제 경유지 등록이나 충전 장소 인증을 대신하지 않는다.

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
