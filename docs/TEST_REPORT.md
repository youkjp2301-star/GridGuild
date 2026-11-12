# GridGuild 검증 기록

확인일 2026-11-04~12 (Asia/Seoul). 문서 지원·컴파일·실제 서비스 성공을 구분한다.

| 검증 | 결과 |
|---|---|
| 공식 SDK 배포 | Vehicle 2.3.2 / NaviHelper 3.0.5 AAR 실제 다운로드 |
| 공식 Pleos 이미지 | v2.1.0 API 34 x86_64, SHA1 `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa` 공식 목록과 일치 |
| APK 빌드 | `:app:assembleDebug` 성공 |
| Lint | `:app:lintDebug` 성공. 버전 업데이트 권고 등 경고는 남음 |
| 단위 테스트 | 최신 0.13.0: `:core:test` **96개**, 실패/오류 0 |
| 에뮬레이터 테스트 | 0.14.0 모바일360dp/가로1280×800의 고유33개를 여러 실행에서 확인. 초기 실패·수정·재검사 결과는 아래에 구분. 전체72개 및 실제 SDK 프로브를 재실행한 것은 아님 |
| Git 추적 파일 구성 | 로컬 설정·SDK 폴더를 제외한 Git index export에서 assembleDebug + core:test + lintDebug 모두 성공 |
| 실제 로컬 Clone | 첫 커밋 4a5fd0e를 `git clone --no-local`로 복제. JAVA_HOME/ANDROID_HOME만 지정해 APK + 24개 단위 테스트 + Lint 성공 (36초) |
| 최종 코드 Clone | 코드 커밋 cfd0141로 fast-forward 후 같은 검사 재통과 (41초). local.properties 미포함 확인 |
| 표준 Android 실행 | API 34, 앱 설치·화면 표시 확인. 호스트 에뮬레이터 GPU/SystemUI ANR 발생 이력 있음 |
| Pleos 실행 | 설치·부팅·앱 설치·최초 설정 화면 확인, 실제 권한 부여 확인 |
| Virtual Console | SOC 42%, 충전 OFF, 기어 P 설정을 Aside가 UI에서 확인 |
| Vehicle SDK 런타임 프로브 | **통과**. 실제 source=PLEOS, SOC=100.0%, 충전 스위치 OFF, GEAR_PARK, UX 허용 수신 |
| Navi SDK 런타임 프로브 | **실패**. Navi Service 연결은 완료됐지만 30초 내 목적지 콜백 미수신. 실제 경유지 등록 성공은 미검증 |

## 0.14.0 Premium Minimal Bento 전면 리디자인 검증

- 첨부9.70초 영상에서19프레임을 추출해 카드 비율·명도·수치/선택/진행 모션을 확인했다. 새 비트맵을 생성하거나 레퍼런스 자산을 복사하지 않고 Compose UI로 재구성했다. Aside의 브리프 기반 사용성 검토6항목(대비/비율/결측/원장/샘플/모션)도 반영했으며 Aside가 실제 앱을 시각 검증한 것으로 집계하지 않는다.
- 공통 BentoCard/StatCard/SocGauge/AnimatedNumber/BentoProgress/StatusBadge/PrimaryButton/SectionHeader와 새 HomeDashboard/BentoRankingScreen을 추가했다. HOME·ROUTE·도시·내비/충전소·즐길거리는 기존 콜백과 태그를 유지하며 새 토큰·레이아웃·누름/진행 효과를 적용했다. XP 파티클·강한 그림자·바운스를 제거했다.
- core96개 실패/오류0, APK/테스트 APK/core/Lint 첫 빌드54초 성공. 추가 정리50초·CTA22초·모바일 보완16초·통합34초 성공. 작은화면 게이지 크기 보완 중 DSL implicit receiver 컴파일 오류1회를 발견해 폭 조건을 명시 변수로 캡처하여 수정했고 재빌드32초 성공. 테스트 표시 보완 후 테스트APK/Lint11초 성공. Lint0오류/기존14경고다.
- 첫 모바일 신규3개는44.971초에2통과/1실패였다. 홈 CTA가 이전에 저장된 HOME/ROUTE 선택에 덮이는 실제 이동 오류를 발견했다. 단발 요청 ID/소비 상태를 사용해 명시적 CTA를 우선하고 보통 탭 복귀는 기존 선택을 유지하도록 수정했다. 지역 랭킹 CTA에도 같은 처리와 검사를 적용했다.
- 모바일360dp 회귀27개202.354초에26통과/1실패: 새 대시보드3/HOME3/기본 미션·SDK 미확인·재생성3/잠금·모드·SOC 안정4/가용 현황4/내비UI2/즐길거리 목적지1/도시·XP 실제화면2/모션·자산5. 실패1개는 이전 버전 긴 경유지 문구를 기다리던 검사였다. 새 요청ID·WAYPOINT·confirmed 및 실제 표시문구를 확인하도록 보완했다. 가용0 신규 차단·진행 미션/120XP 유지·호환·네 상태와 대수도 확인했다.
- 가로1280×800 검사11개104.137초에9통과/2실패였다. 신규4개(실제 랭킹 변화·범위 리셋 포함), 지역3~11/개인33명 샘플, Mock 갱신, 보완한 경유지·SDK 미확인·N 안전이 통과했다. 추천 시간은 새 카드의 시작/종료 Text 분리로 기존 문자열 검사가 실패해 두 구간 각각을 확인하도록 수정했다. N 방향 검사는 자연 패널 축을 전제하는데 가로 override를 유지한 상태에서 실행하여 진입 전 조건에서 실패했다. 앱 방향 오류로 집계하지 않고 정상 패널 조건에서 재실행했다.
- 최종360dp 신규4+추천시간/원클릭120XP1+N양방향1은78.942초에6개 모두 통과했다. 실제 ON+43%에서 미션50%,44%에서 개인/도시120XP·Lv2·완료 상태, 재생성/재진입/오늘 중복 차단, D/UX에서 화면·CTA 숨김과 원장 보존을 확인했다. 지역/개인 순위는 같은 범위에서 저장 점수가 변한 경우만 이전→현재를 표시하고 필터 변경 시 초기화했다.
- 고유33개를 여러 실행에서 확인한 범위이며 전체72개 일괄 통과나 실제 Vehicle/Navi 서비스 종단 성공을 의미하지 않는다. 초기 실패를 숨기거나 전체 일괄 성공으로 바꾸지 않는다. 표기와 실제 레이아웃 보완을 구분한다.
- QA는 기존 AVD를1회 실행한 뒤 재부팅·재생성·초기화 없이 사용했다. 수집된 event의am_anr/am_crash0건이며 앱 AndroidRuntime FATAL 기록도 없었다. 실제 캡처는 활성 패키지com.gridguild 확인·안정화 대기 후 저장했다. 모바일/가로의 홈·참여전·50%진행·완료·랭킹과 기존 도시/ROUTE/XP 캡처를 직접 검토했다.
- QA 진행을 사전 백업으로 복원하고 원래 size/density/회전 설정으로 되돌렸다. 원본 기기에는 자동 fixture를 실행하지 않고 이전 APK·진행을 앱 내부/PC에 백업한 뒤0.14.0/code15로 업데이트했다. 진행SHA256은 업데이트 전후·실제 Home 확인 후 동일했다. 실제SDK 미연동과 원본 AVD의 기존 논리PORT/N여백 한계는 이전과 동일하다.
- 지역 공동 점수와 개인 기여를 혼동하지 않도록 랭킹 강조 카드의 숫자 이름을 ‘랭킹 점수’로 정리했다. 표기만 보완한 최종 APK/테스트/core/Lint31초 성공. 전달 APK는 `artifacts/bento-v014/release/GridGuild-0.14.0-debug.apk`, SHA256 `cd26fa4da622d0a203ee81c4da0edd57384bf02523e728d9c0e8625140e8c76b`다. 중간 검수 APK도 보존했다.
- 최종 APK의 실제 랭킹 변화/범위 리셋 단독1개도13.927초에 통과하고 현재 기기/QA 실제 설치base.apk와 전달 APK의 SHA256 일치를 확인했다. QA는 다시 사전 진행SHA256 `191fdbdcae7323ead5f53ced0bacb248c205e71cd1b9d574226f8e91b0356631`로 복원했다. 원본 진행SHA256 `5d93202a8f7a74516b89e1797f028ff97243c6b1f26f2c4c48ab13fe2fd46dcb`도 최종 Home 확인 후 동일했다.

### 변경 파일

- 공통 디자인/모션: `GridTheme.kt`, `BentoComponents.kt`, `CityMotion.kt`.
- 화면/흐름: `GridGuildApp.kt`, `HomeDashboard.kt`, `HomeChargeScreen.kt`, `BentoRankingScreen.kt`, `CityCollectionScreen.kt`.
- 충전/내비/위치도: `ChargingAvailabilityView.kt`, `ChargingLeisureSheet.kt`, `NavigationControls.kt`, `NearbyStationMap.kt`.
- 버전/검사: `app/build.gradle.kts`, `BentoDashboardFlowTest.kt`, `ChargingAvailabilityFlowTest.kt`, `DemoPolishFlowTest.kt`, `VisualAppFlowTest.kt`.
- 문서: `PRD.md`, `README.md`, `DEV_LOG.md`, `docs/design/BENTO_UI.md`, `docs/TEST_REPORT.md`.
- core·ViewModel·Repository·SDK 의존 버전·DataStore schema·원장은 변경하지 않았다. 렌더링 시 결측·현재 모드에 맞는 출처·신선도를 확인하고 실제 충전 제어/ETA/목표/순위변동을 꾸미지 않는다.

## 0.13.0 시연 화면 정리·장소 목적지·N 방향 검증

- 신규 단위4개 포함 core96개 통과: 장소 자체 좌표 전달, 잘못된 원천/다른 충전소/무효 좌표/300m 외부 차단, 정확한300m 경계와 일반 목적지 유효성.
- APK/테스트 APK/core:test/lintDebug 첫 빌드6분43초 성공. 추가 검수의 stale SOC/방향 고정 보완 후 최종 빌드1분38초 성공. Lint0오류/기존14경고다.
- 첫 QA 선택20개는303.953초에19통과/1실패였다. 즐길거리6개, N방향2개, 내비 저장소6개, 내비UI2개, 목적지UI2개, 배터리 안정1개가 성공했다. highSOC 목적지 검사 시작 시 Compose 테스트 프레임의 Default 작업 스레드에서 Choreographer `The current thread must have a looper!` 예외가 발생했다. 실제 앱 AndroidRuntime 종료와 구분하고 첫 실행을 전체 통과로 표시하지 않는다.
- 즐길거리 목적지 버튼으로 장소 좌표/이름 반영, 진행 미션·XP 유지, 기존 충전 완료120XP,4필터/상세/광고/20분·결측/UX닫힘을 확인했다. 공식 RouteInfo를 사용하는 일반 좌표 요청은 fake-client 테스트이며 실제 Navi Service 성공이 아니다.
- 독립 코드 검수에서 오래된 raw SOC 노출과 가로 진입 후 세로 전환 가능성을 발견했다. Home/ROUTE의 결측 표시는 짧게 유지하고, N은 현재 가로 방향을 LOCKED로 유지하도록 보완했다. 양쪽 가로 진입/진입 후 세로 회전 요청/원장 불변/실제 Global Back 및 X버튼/이전 요청 복원 검사를 포함한다.
- QA 첫 부팅에서 새 앱 실행 전 System UI/전화/Google 서비스 ANR가 나타났다. 로그를 보존하고 Wait1회 뒤 정상 앱 포커스를 확인했다. 재부팅·초기화·AVD 재생성을 반복하지 않았다. 이전 시스템 종료 최초 원인이 해결되었다고 주장하지 않는다.
- 보완 후 재검사6개는90.834초에 모두 통과했다. 앞서 실패한highSOC1개, 가로90/270·진입 후 세로 회전 시 방향 고정·뒤로가기 및 세로 진입/X복귀2개, 간결한 Home/ROUTE·장소 목적지/원장 보존2개, 배터리 안정1개다. 첫 묶음 실행의 Looper 예외 원인을 확정하거나 재발이 불가능하다고 주장하지 않는다.
- 실제 원본 기기에는 `wm size 1280x800`/density160 override가 있었다. 물리 패널1080×1920·rotation90이지만 논리 화면은800×1280/PORTRAIT이므로 기존 SENSOR_LANDSCAPE도 진입 시 rotation0으로 바뀌는 문제를 재현했다. 패널 크기·회전축도 함께 확인해 이미 가로인 물리 화면은 현재 방향을 고정하도록 수정했다. 원본 설정은 변경하지 않았다.
- QA에서만 같은 override를 임시 재현하고 finally복원하는 검사를 추가했다. 최종 빌드36초 및 최종 테스트 APK/Lint13초 성공, 최종 방향3개34.028초에 모두 통과. 정상90/270·portrait 진입/복원과 override의 논리PORT/rotation90 유지·이미지 실행을 확인했다. 정사각 물리 패널의 방향 분류는 미검증이다.
- 원본5554에도 최종0.13.0/code14를 설치했다. 이전 설치 APK/진행을 앱 내부와 PC에 백업하고 각 업데이트 전후·실제 N/ROUTE/즐길거리 화면 확인 후 진행SHA256 동일(`5d93202a8f7a74516b89e1797f028ff97243c6b1f26f2c4c48ab13fe2fd46dcb`)을 확인했다. 레벨/XP fixture 검사는 원본에 실행하지 않았다.
- 원본 N에서 광안리 현재Lv.7을 실제 열어 진입·Back 전후rotation90 동일, 정상 앱 포커스를 확인했다. 기존 logicalPORT 설정은 그대로여서 이미지 위아래 여백은 남는다. 논리 화면을 가로로 교정하거나 원본 AVD 설정 문제 전체를 해결한 것으로 표시하지 않는다. QA의 광안리Lv.2/센텀Lv.12는 정상 가로 캡처로 직접 검토했다.
- QA 진행도 사전 백업으로 복원해 재실행 후SHA256 동일(`191fdbdcae7323ead5f53ced0bacb248c205e71cd1b9d574226f8e91b0356631`), wm size/density override 없음 및 기존 자동회전1/사용자회전0 복원을 확인했다. 시험 시작 이후 QA event log의am_anr/am_crash는0건이다.
- 최종 APK는 `artifacts/showcase-v013/verified/GridGuild-0.13.0-debug.apk`, SHA256 `0fc8ba9d67e10c781e08fb65fbc712ab2d4925a50e6136d8e9964797c8057a6e`다. 원본/QA에 실제 설치된 base.apk와 일치한다. 정상 캡처는 같은 폴더의screens/ 및 current-device-*.png에 보존했다. 중간 검수본과 이전0.12.0 APK는 덮어쓰지 않았다.

## 0.12.0 도시 레벨 ±1 및 충전 중 즐길거리 검증

- 신규 core14개를 포함한92개 통과/오류0: 도시 단계/원장/경계/실제 모드 차단,6충전소×5가상POI,300m 경계/외부·무효 좌표 차단,4필터,광고 정렬 중립,20분→활동10~15분 우선,반올림 예산,결측·NaN·출처·신선도·정적Mock 가정 검증.
- 첫 통합 빌드에서 테스트의 텍스트 입력 함수가 Unit을 반환하는데 IME 호출을 체인한2곳을 수정했다. AutoMirrored 아이콘으로 보완 후 APK/테스트 APK/core:test/lintDebug 성공(4분50초), Lint0오류/기존14경고.
- 신규 기기6개 첫 실행:5통과/1실패(51.367초). 도시±1·경계·저장·개인XP/미션 유지,4필터·장소/광고/혜택 상세·원장 불변,탐색 후 원래 ROUTE120XP/도시Lv.2,목적지 변경/UX 제한 닫힘을 확인했다. 실패는20분 값 확인 후 키보드 전환 중 캡처용 활성 창 root가 일시 null이었던 경우다. 정상 캡처로 집계하지 않았다.
- 캡처에서 실제 앱 창이 확인될 때까지 기다리도록 보완했다. 해당 시간/결측 검사1개+기존 회귀16개는17개 전부 통과(153.489초). 내비 UI2/현황·호환4/HOME3/기본 미션·안전·재생성3/초기화4를 포함한다. 합계 선택22개를 여러 실행에서 검증한 것이며60개 전체 일괄 통과라는 뜻은 아니다.
- 이전 내비 UI2개 실패는650ms 처리 중 상태의 순간 스냅샷 대신 실제 pending 전이를 관찰하고, 클릭 전보다 증가한 요청ID+완료+목적지를 기다려 수정했다. 작은 화면의 실제 버튼을 스크롤해 누르고 이전 완료 문구를 새 결과로 사용하지 않는다. 제품의 NaviHelper 구조/요청 정책은 바꾸지 않았다. 두 검사는 이번에 통과했으나 실제 SDK 서비스 성공이 아니다.
- 시각 검수에서 숫자 키보드가 목록을 가리는 것을 발견했다. 포커스/키보드 제어를 바텀시트 Dialog의 composition 안으로 옮기고 필터/상세 이동 때 입력을 정리했다. 최종 APK/테스트 APK/core:test/lintDebug 재성공(1분53초), 최신 캡처 검사 포함 테스트 APK/Lint도 성공(11초). 즐길거리4개를 최종 APK에서46.925초에 재통과했다. 기존16개는 UI 보완 전 실행 결과이고, 이 보완 후에는 새 시트4개(기존 미션120XP 포함)를 재검증했다.
- 최종 정상 화면5장:20분 추정/15분 활동 기준,키보드 없는 목록/필터,카페 상세·가상 좌표,광고/발급 불가 혜택,도시Lv.2±1·개인470XP 유지. `artifacts/leisure-v012/screens/`에 저장·직접 검수했다. 원본 AVD는 시작/설치/데이터 변경하지 않았다.
- 기존 QA AVD5556을1회 실행했다. 새 앱 설치 전부터 KeyguardService 실행 지연으로 System UI ANR가 있었고 원시 로그를 보존했다. 정상 Wait1회 후 앱 포커스를 확인하고 검사했다. 재부팅/AVD 재생성/wipe/앱 데이터 삭제를 반복하지 않았다. 시스템 전체 장애가 해결됐다는 의미가 아니다.
- QA DataStore를 사전 백업하고 검사/캡처 후 복원했다. SHA256 `191fdbdcae7323ead5f53ced0bacb248c205e71cd1b9d574226f8e91b0356631` 일치 및 재실행 후 유지 확인. 원본 AVD의 기존 전체 백업도 그대로 보존한다.
- 최종 APK:0.12.0/code13, `artifacts/leisure-v012/GridGuild-0.12.0-debug.apk`. SHA256 `14219ba9c5282970188140b1bc7af86f334329a77db76e65c7a06cc9907b2bce`가 QA 설치 APK와 일치한다. 이전0.10.0/0.11.0 APK를 덮어쓰지 않았다.
- 실제 장소/영업·행사/도보 경로·시간,SDK 용량/출력,결제·쿠폰 발급·소비 리워드 포인트·환전은 미연동/미구현이다. 이런 값이 없는 상태를 일반 가상 장소 추천으로 확인했으며 미지원 기능을 성공으로 표시하지 않는다.

## 0.11.0 레벨별 도로 위 1인칭 도시 검증 (이전 기록)

- 내장 image_gen으로 두 목적지×12레벨=24개 개별 PNG 제작, 독립 [원본 시각 검수24개](design/driver-scenes/VISUAL_QA_V2.md) 완료. 원근/도로 하단/시대 재료/랜드마크 통합을 실제 열어 확인했다. 반려·수정 이력과 P2 한계를 남겼으며 무AI·실제 카메라 수치·구조 안전성 보장이 아니다.
- [FINAL_ASSETS_V2.json](design/driver-scenes/FINAL_ASSETS_V2.json)의24개 SHA256은 프로젝트 PNG와 native 생성 원본이 일치함을 확인한 기록이다. 실제 앱 화면 성공과 구분한다.
- APK0.11.0/code12, assembleDebug/assembleDebugAndroidTest/core:test/lintDebug 성공(4분14초). 단위78개/오류0, Lint오류0·경고14(기존12+보존한 구 사진 미사용2). 첫 Kotlin 변수 보간 오류1곳 수정 후 재빌드했다.
- 기존 AVD 첫 전체 검사는 System has crashed로 중단됐다. 소프트웨어 렌더러의 원본과 별도 QA에서 새10개 중 Global Back1개가 각각 실패했다. 시스템 ANR 모달이 포커스를 보유했으므로 다른9개 통과도 정상 화면 증거로 간주하지 않는다. 실패한 같은 검사를 반복하지 않았다.
- [장애 진단](EMULATOR_DIAGNOSIS_20261110.md): QA의 system ANR는 앱 최초 프로세스/Activity 시작 전의 패키지 무결성 검증 브로드캐스트 지연이다. CPU 부하가 높았고 원본의 DeadSystemException 최초 원인은 미확정이다. 앱 종료 뒤 남은 모달을 정상 Wait1회로 닫은 후, 실제 adb 터치로 홈/기존 도시/새 광안리 Lv.12와 시스템 Back 복귀를 정상 확인했다.
- 활성 창이 앱인지 검증하는 캡처 assertion을 추가했다. 두 목적지×현재 Lv.2/12·미해금 차단·원장 보존 +24개 네이티브 PNG 해독/픽셀 고유성 검사2개가33.834초에 통과했다. 실제 화면4장을 `artifacts/driver-v011/runtime-qa/`에 회수했다. 과거 모달 캡처와 구분한다.
- 원본 사용자 DataStore 복원/SHA256 일치, 정지 원본 AVD 전체31파일/약5.8GB 별도 복사본 SHA256 일치를 확인했다. 전체 AVD 백업의 재부팅 복구는 미검증이다.
- 11월11일 기존 QA AVD를1회 실행해 정상 런처/ANR 없음 확인 후 최종 APK를 업데이트했다. 반복 부팅·초기화는 하지 않았다. APK/테스트 APK/core:test/lintDebug 최종 빌드 재성공(2분8초). 실험적 창 복원 패치는 되돌렸고 제출 후보와 분리했다.
- QA의 첫 부팅 로그에는 GridGuild 실행 전부터 GMS/Bluetooth/YouTube 서비스의 ANR도 있었다. 패키지 검증만을 전체 장애의 유일 원인으로 단정하지 않는다.11월11일 정상 부팅/검사에서는 같은 ANR가 관찰되지 않았다.
- APK0.11.0 후보: `artifacts/driver-v011/GridGuild-0.11.0-debug.apk`, SHA256 `f62bb1cc1a4b86bf87d6061deb8c9d52ad10cd320b3b817225b4fb78808b454e`. 이전0.10.0 APK도 보존했다. 실제 Pleos N/목적지/충전 응답 검증이 아니다.
- 최종 APK의 회귀53개는552.554초에50개 통과/3개 실패였다. 새 장면8개와 리소스1개는 모두 통과했다.24단계 순회·원장 불변·하락 후 재잠금·지역 변경·콜드 로딩 취소·주행/UX 종료·재생성/백그라운드 제한을 포함한다. 두 번 실패한 기존 Global Back1개와 실제 SDK 프로브는 제외했으며54개 전체 통과로 표시하지 않는다.
- 실패1: `DestinationDemoFlowTest.highSocSuspensionDoesNotClaimTheAreaHasNoStations`는 작은 화면의 LazyColumn 아래 문구를 스크롤 전에 기다렸다. 검사에 명시적 스크롤만 추가하고 단독 재검사에서 통과했다. 앱 동작은 바꾸지 않았다.
- 실패2~3: `NavigationFlowTest.bothButtonsShowProgressAndProduceVisibleResultsEveryTime`의 순간 비활성 상태, `refreshKeepsTheSelectedDestinationAndItsNearbyRecommendations`의 충전소 목적지 변경 확인이 미통과다.650ms Mock 응답과 검사 프레임의 경합, 작은 화면의 클릭 범위가 가설이며 원인을 확정하지 않았다. 같은 실패를 무한 재시도하거나 assertion을 제거하지 않았다. 실제 adb 터치로 새로고침 요청#1→재탐색 완료#2→새로고침 완료#3은 정상 확인했고, 내비 저장소 검사5개도 통과했다. 특정 충전소 선택 자동검사의 실패는 별도 미해결로 남긴다.
- 장면8개 기능 검사가 통과해도 일부 순회 캡처가 진입 alpha=0 프레임이었다. 해당 캡처는 정상 증거에서 제외했다. 캡처 helper에서 애니메이션 시계를500ms 진행하고 실제 프레임 표시500ms를 기다리도록 보완했다. 대표4장+SOC 스크롤 최종2개를46.741초에 재통과했고, 최종 정상4장은 `artifacts/driver-v011/runtime-qa/final-apk/`에서 직접 확인했다. 나머지20단계는 선택/리소스/잠금 기능 검사 성공과 원본 시각 검수까지이며, 모든 기기 캡처가 정상이라고 주장하지 않는다.
- 최종 회귀 구간의 `am_anr`/`am_crash` 기록은 없었다.24장 순회 중 앱 PSS 약199~202MiB가 관찰됐고 앱 OOM은 없었다. 로그와 실패를 `.tools/driver-diagnostics-20261110/`에 보존했다. Lint 최종 재검사도 별도 로그에 남겼다.

## 0.10.0 N단 도시 체험 검증 (이전 기록)

- APK0.10.0/code11 빌드·Android 테스트 APK·core:test·lintDebug 성공. 순수 Kotlin71개 통과, Lint0오류/기존12경고. 실제 Pleos 런타임은 재검증하지 않았다.
- 새 단위3개: 두 지역/근접 좌표, 겹치는 영역의 최근접 선택, 삼락·기장·무효 좌표 차단, Mock + 신선한 P/UX + 실제 플랫폼 허용 조건과 실제/미수신 출처 차단.
- 새 기기4개: TEST 진입·가로 표시, 광안리↔센텀의 실제 Mock 목적지 좌표 변경, 선행 목적지 자동 장면 선택, 사진/크레딧, 원래 ROUTE 탭 복귀·미션/XP 원장 보존, D/UX 종료, 재생성·백그라운드·SDK 모드 차단. 실제 차량 N값을 조작하거나 검증하지 않는다.
- 기존44개와 합친 전체48개를265.208초에 통과했다. HOME/ROUTE·중복 방지·추천/가용 상태·목적지/경유지/재탐색·초기화·도시 잠금·모션/보상·기존 안전·저장 재생성을 포함한다.
- 처음 새4개는 Android 최초 전체 화면 안내가 입력을 가로막아 실패했다. 테스트에서 OS의 정상 확인 버튼을 닫도록 보완해4개를51.524초에 통과했다. 앱에서 시스템 설정을 우회하지 않는다.
- 전체 첫 실행에서는 새4개 이후 기존 LoadingDurationTest의 Activity 종료 중 Compose SlotTable 예외가1회 발생했다. 해당 로딩 단독2개는9.632초에 통과했고, 로딩을 먼저 실행한 전체48개 재실행도 통과했다. 원인을 확정한 것으로 기록하지 않는다.
- 출처 링크를 열 수 없는 환경의 처리까지 반영한 최종 APK에서 assembleDebug/assembleDebugAndroidTest/core:test/lintDebug 재성공(17초), 최초 오류와 동일한 N 체험→로딩 순서6개도61.725초에 재통과했다. 해당 종료 예외는 재현되지 않았으나 재발 시 별도 조사한다.
- 1280×800의 광안대교·영화의전당 화면을 캡처·시각 검토했다. 원본 실사 JPEG와 크레딧은 [NEUTRAL_CITY.md](NEUTRAL_CITY.md), 테스트 PNG/APK는 Git 제외 artifacts/neutral/에 있다.
- 시험 전 DataStore를 복원하고 백업/복원 SHA256 일치를 확인했다. 설치 버전0.10.0/code11·원래 기록 재실행과 애니메이터 배율1 유지를 확인했다. 실제 Pleos N 동작 성공을 의미하지 않는다.

## 0.9.0 간결한 데모 경험 검증 (이전 기록)

- 최종 :app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug 성공(49초), Lint0오류/기존12경고. 최초 새 코드의 모듈 간 smart-cast와 실제 출처 시험의 fixture 출처를 수정했다.
- 기존39개+신규5개44개 전부 통과. 새 로딩2개는 실제 경과3초/캐시 적중 시 깜빡임 없음, 데모3개는 두 시간대/원클릭120XP/재시작 후 중복 방지, 실제 지역/개인 리스트, 여러 시드의 혼합 상태·11분 후 자동 갱신/의도적 미확인을 검증했다.
- 새 단위4개와 기존 단위를 합친68개가 통과했다. 실제16/22시 시작·17/23시 종료 제외, Mock 시간 우회와 실제 출처 미우회, 지역3~11 분포·실제 XP 보존·개인32명도 검사한다.
- 첫 전체 검사43/44에서 인트로의 Compose 테스트 시계가 실제 경과 시간을 앞당기는 문제를 발견했다. 실제 elapsedRealtime/Default dispatcher 대기로 통일한 뒤44/44 재통과했다. OS 모션 축소와 무관한 최소 시간이며 안전 제한·취소는 즉시 우선한다.
- 카드/HOME/두 랭킹 화면을 `artifacts/demo-polish-v090/`에서 시각 검토하고 원본 DataStore 백업을 복원했다. APK0.9.0/code10이며 실제 SDK 종단 검증 결과와 구분한다. 항목별 [완료 체크리스트](DEMO_POLISH_CHECKLIST.md)를 따른다.

## 0.8.0 ROUTE 충전기 이용 현황 검증 (이전 기록)

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 성공(2분58초). Lint 오류0/기존 경고12. 첫 컴파일의 기존 Compose FlowRow 미지원 인자를 제거해 수정했다.
- 새 단위7개: 정확히50% 경계·가용0/고장/점검 제외·미수신/미래/10분 경계·부분 갱신·중복/잘못된 재고·상태 변경 시각과 관측 시각 분리·혼합 커넥터·거리/여유/호환 순위.
- 새 Android 검사4개(화면3+저장소1): 카드 안 네 배지/수량·지연/미수신·가용0 신규 참여 방지·진행 중 가용0 전환 후120XP/도시Lv.2 유지·커넥터 불일치와 원래 Mock 경유지 요청·한 fixture 변경 시 다른 관측 시각 보존.
- 네 상태의 실제 화면을 로컬 `artifacts/availability-v080/`에 캡처했다(Git 제외). 네이티브 대화상자 닫힘/목록 재정렬 후 촬영하도록 캡처 helper를 조정하고 새 검사4개를25.73초에 재통과했다.
- 기존35개도 함께 통과했다. 보상 조건/중복 방지/HOME/목적지 변경/경유지/재탐색/초기화/잠금/안전/점멸 회귀를 유지한다.
- 확인 기기는 표준 API34의800×1280 에뮬레이터다. 실제 환경공단 API·실제 도로 이탈 거리·실차 커넥터 수신 또는 Navi SDK 성공을 검증한 결과가 아니다.
- 마지막 안내 문구 수정 뒤 APK·단위·Lint를 재확인(빌드1분58초)하고 새 Android4개도16.58초에 재통과했다. 테스트 전 백업을 복원하고 SHA256 일치를 확인했다.

## 0.7.0 시각·모션 검증 (이전 기록)

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 성공. Lint 오류0/경고12: 기존9개와 원본 보존으로 미사용인 v2 이미지3개다.
- 실제 Android BitmapFactory로 v4 시대별2×2 아틀라스를 디코딩해12개 프레임의 서로 다른 픽셀 지문을 확인했다.
- 메뉴 마우스 호버, 실제 화면 교체, 도시 조립 키프레임 변경과 준비 후 제거, 보상 소모 후 재구성/안전 복귀 재생 방지, XP 장식 아래 버튼 클릭 전달을 검사했다.
- 설치된 앱에서 HOME 등록→참여→충전ON/SOC+2%p→120XP 효과→Activity 재생성 시 원장 보존·효과 재생 방지, 미래 시대를 열어 본 뒤 초기화 시 잠금 복귀를 검사했다.
- 최초 실제 보상 효과 누락(잔액 공개 전 이벤트 전달)과 XP Material Surface가 뒤의 메뉴 클릭을 차단하던 문제를 수정했다. 장식은 터치를 가로채지 않는 Box로 바꾸고 화면 하단으로 옮겼다.
- 실제 API34 에뮬레이터800×1280에서 새 홈·내 도시·로딩·입자 효과 캡처를 확인했다. 시스템 애니메이터/전환/창 배율은1로 복구해 직접 모션을 볼 수 있게 했다. 실제 차량 가로 화면/큰 글자/프레임 성능은 추가 검증 대상이다.
- 마지막 안전 게이트 변경 뒤 빌드·57개 단위·Lint 재통과, 모션/설치 앱/안전 회귀10개도34.298초에 재통과했다. 테스트 전 진행 데이터를 복원하고 시작 전 SHA256 일치를 확인했다. 최종 캡처는 로컬 `artifacts/visual-v070-final/`에 보관한다(Git 제외).
- OS 애니메이터 배율0의 정지 대체 경로에서도 모션/설치 앱7개가14.79초에 통과했다. 이후 배율1·기존 데이터 복원. test-device.ps1은 배율 원상 복원과 JUnit 오류 검출을 추가했고 PowerShell 구문 검사 오류0(수정 helper 전체 실행은 별도로 미실행).
- 런타임 도시는 정적 건축 모형 아트이고 로딩은4키프레임·Compose 전환이다. 실시간3D 또는 생성 동영상 재생이라고 표시하지 않는다. 정확한 자산과 프롬프트는 [CITY_ART.md](CITY_ART.md)를 따른다.

## 핵심 단위 테스트 범위

중복 콜백, 같은 날 같은 충전소 재적립, 충전 OFF, SOC 감소, 데이터 지연·미래 시각·출처 변경,
시작 전 샘플, 저장된 진행 미션 재개, 일일 보너스, 12단계 경계, 시즌 XP, 랭킹 갱신,
목적지 주변 fixture 정렬, 고SOC 충전 비권장, P·UX·신선도 안전 조건.

## 수정 사항

- SDK RouteInfo / RequestWaypointInfo의 필수 POI 문자열 인자 누락 컴파일 오류 수정.
- Compose DSL 범위의 화면 폭 참조 컴파일 오류 수정.
- 최신 안전 샘플보다 뒤처진 UI 타이머를 사용해 화면이 반복 제한되던 문제 수정.
- 지연 생성 목록은 `performScrollToNode`로 탐색하도록 화면 테스트 수정.
- 보상 검증은 화면 밖 목록에만 의존하지 않고 DataStore에 원자 저장된 XP를 검사하도록 보강.
- 화면에 XP가 여러 번 표시되는 점을 반영해 테스트 문구를 정확히 선택하도록 수정. 최종 결과 `OK (2 tests)`, 실행 8.241초.
- 소스 모드 변경 때 진행 미션을 취소해 실제/Mock 샘플이 섞이지 않게 함.
- 2026-11-05 수명주기 수정 후 화면 회귀 테스트 `OK (3 tests)`, 실행 20.917초.
- 내비 전면 전환 시 SDK 응답을 유지하고, 화면 비활성화와 SDK 해제를 분리했다. 차량 호출은 IO에서 실행한다.
- 실제 SDK 프로브 2개 중 Vehicle 1개 통과, Navi 1개 실패 (48.561초). 목적지 미수신 시 10초 후 오류를 보여주도록 보강했다.

## 아직 완료로 표시할 수 없는 항목

- Pleos SDK의 실제 SOC·충전 상태 **변화** → 미션 완료 → XP 지급 전체 흐름. 현재값 읽기와 P/UX 확인은 실제 프로브 통과.
- Navi 목적지 콜백: 내장 내비게이션에 목적지가 설정되어 있는지와 서비스 전달 상태를 추가 확인해야 한다.
- NaviHelper 경유지 실제 등록 성공 및 좌표 재조회 확인.
- Pleos 주행 재개 시 플랫폼 차단과 복귀를 포함한 종단 시연.
- GitHub CI: 원격 저장소를 생성·업로드하지 않아 아직 실행 안 함.

로컬 테스트 산출물은 Git에 넣지 않는다. 재실행 명령과 최신 결과는 README/DEV_LOG에서 관리한다.
최종 추적 파일 46개에 대한 비밀정보 패턴 검사와 Git 공백 검사를 통과했다. GitHub 원격 생성·인증·push는 수행하지 않았다.

## 2026-11-05 · 0.2.0 도시/미션 회귀 수정

- APK / 테스트 APK 빌드, `lintDebug` 성공. Lint 오류 0, 경고 9개.
- 순수 Kotlin 테스트 **29개 통과**: 기존 24개 + 동일 시점 차량 표시, 지연/미래 샘플 차단 유지, 12개 도시 프레임 고유성 등 5개.
- Android 에뮬레이터 화면 테스트 **6개 통과**, `OK (6 tests)`, 20.169초.
- 정상 데이터 반복 갱신 동안 배터리 표시 및 미션 시작 버튼 활성 유지, 시작 후 진행 문구/비활성 상태를 확인했다.
- 현재 Lv.1 → 미리보기 Lv.2 → 미래 Lv.12 → 현재 복귀, 미리보기 전후 XP 유지 확인.
- Mock 안내창과 조작 메뉴, 같은 Mock 재선택 시 진행 미션 보존을 확인했다.
- 기존 미션 보상·P/D 차단·SDK 불가 시 Mock 복귀·Activity 재생성 테스트도 통과했다.
- 현재·다음·미래 도시, 홈 보상, Mock 안내, 미션 화면의 실제 에뮬레이터 PNG를 보고 레이아웃을 검토했다.
- 사용자 데모 기록은 테스트 전에 앱 내부 파일로 백업했고, 테스트 후 복원본과 백업의 SHA256 일치를 확인했다. 앱 0.2.0 재실행 성공.
- 이번 수정은 실제 Pleos 런타임을 재검증하지 않았다. 앞서 확인한 Vehicle 성공 / Navi 목적지 콜백 미수신 상태는 유지된다.

## 2026-11-05 · 0.3.0 디자인/도시 해금 정책

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 최종 성공 (14초). Lint 오류 0, 경고 9개. 새로 추가한 상태 boxing 정보 및 Undo 아이콘 deprecation도 정리했다.
- 단위 테스트 **33개 통과**. 추가 4개에서 모든 현재/후보 레벨 조합, 잘못된 선택/해금 범위, 실제 XP 경계와 최고 단계의 필요 XP를 검사한다.
- `GridGuildFlowTest,UiRegressionTest` 실제 API 34 에뮬레이터 실행: **OK (7 tests)**, 21.852초.
- Lv.1에서 다음 단계 안내는 공통 잠금 이미지와 100 XP 조건만 표시한다. Lv.12 타일은 실제 화면에 표시되지만 비활성이고 미래 아트는 없다.
- 도시 XP 250으로 Lv.3 해금 → Lv.1 방문 → 현재 Lv.3 복귀 시 XP 250 유지 → 데모 초기화 후 Lv.3 재잠금 및 미래 아트 비노출을 검증했다.
- 정상 heartbeat 동안 배터리 상태/미션 버튼 안정성, Mock 설명·같은 모드 재선택 시 미션 유지, 보상·P/D 제한·SDK 불가 안내·Activity 재생성 회귀도 통과했다.
- `artifacts/premium/`의 실제 도시·잠금 안내·잠긴 컬렉션·과거 도시·홈 보상·미션 캡처를 시각 검토했다. 산출물은 Git 제외다.
- 사용자 진행 기록을 앱 내부 백업에서 복원하고 SHA256 일치를 확인했다. 설치 버전 0.3.0 / versionCode 3, 기존 기록으로 앱 재실행 성공.
- SDK 호출과 주행 제한 정책 변경 없음. 이번 작업은 표준 Android Mock 화면 검증이며 실제 Pleos 런타임을 재실행한 결과가 아니다. Vehicle 기존 성공 / Navi 목적지 콜백 미수신 상태는 유지된다.

## 2026-11-05 · 0.4.0 항목별 테스트 초기화

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 성공 (48초). Lint 오류 0 / 기존 경고 9개. SDK 기어 API의 기존 deprecation 경고도 유지된다.
- 순수 Kotlin 테스트 **41개 통과**. 새 8개에서 선택 지역의 도시 XP/개인 XP/미션 상태 분리, 월간 랭킹, 반복 초기화, 기존 ID 재사용 금지, 미션 재도전·일일 보너스 재적립, SDK 모드 변경 불가를 확인했다.
- 표준 API 34 에뮬레이터에서 `DemoResetFlowTest,GameStateCodecTest,GridGuildFlowTest,UiRegressionTest` 실행: **OK (14 tests)**, 32.13초.
- 도시 초기화 후 Lv.1 및 미래 아트 재잠금, 개인 포인트/다른 지역/진행 미션 보존을 확인했다. 개인 포인트 초기화는 도시 레벨과 완료 제한을 보존하며 재생성 후에도 유지된다.
- 미션 초기화 후 진행/완료 상태만 사라지고 기존 XP가 유지된다. 같은 날 같은 충전소 재시작→충전 ON·SOC +2%→개인 470→590 XP / 도시 370→490 XP를 확인했다.
- 4개 확인창의 취소 시 무변경, 전체 초기화 후 지역 선택 화면 복귀, 기존 P/D 제한·미션·도시/Mock 안정성 회귀를 확인했다.
- 저장 코덱 3개 테스트로 schema 1의 기존 120 XP 유지, schema 2의 false 플래그/진행 미션 왕복 저장, 누락 필드·지원하지 않는 버전·손상 JSON의 명시적 실패를 확인했다.
- 초기화 버튼/도시 초기화 확인창을 실제 PNG로 시각 검토했다. 테스트 후 사용자 백업/복원 파일 SHA256 일치 확인, 0.4.0 설치 및 기존 Lv.3·도시/개인 각 360 XP 표시 확인. Mock 테스트 패널을 열어 둔 상태다.
- 캡처와 로그는 `artifacts/reset-controls/`, `.tools/reset-*`에 보관하며 Git 제외다. 실제 Pleos 런타임 검증을 추가한 작업은 아니다.

## 2026-11-05 · 0.4.1 목적지/재탐색 버튼 수정

- 원인: Mock `refresh()`가 Unit이었고 재탐색은 같은 상태 문자열을 반복했다. SDK 재탐색은 pending/타임아웃 및 전용 종료 콜백 처리가 없었다.
- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 최종 성공 (24초). Kotlin 41개 통과, Lint 오류 0 / 기존 경고 9개.
- `NavigationFlowTest,NavigationRepositoryTest,DemoResetFlowTest,GameStateCodecTest,GridGuildFlowTest,UiRegressionTest` 표준 API 34 실행: **OK (20 tests)**, 40.154초.
- 화면 2개 추가: 새로고침 반복 시 요청 번호 증가, 요청 중 다른 내비 버튼 비활성, 재탐색 데모 결과, 목적지 변경 후 새로고침/재탐색 시 목적지 보존 및 XP 무변경을 검증했다.
- 테스트 전송 계층 4개 추가: Mock 반복/중복 방지/닫힌 뒤 상태 보존, SDK 전용 `onReRouteEnded` 대기, 일반 `onRoutingCompleted`로 잘못 완료하지 않음, 좌표 일치, 예외·미응답·오류와 재시도를 검증했다.
- 전송 계층 테스트는 공식 SDK 타입을 사용하는 **FakeNaviClient**로 콜백 계약을 검사한다. 실제 Pleos 내비 서비스에서 성공한 결과가 아니다. 기존 실제 목적지 콜백 미수신/경유지 종단 미검증 상태는 유지한다.
- `artifacts/navigation-fix/navigation-fixed.png` 실제 화면에서 버튼/처리 결과/요청 번호/시간/데모 설명을 검토했다. 사용자 기록은 복원 전후 SHA256 일치 확인, 앱 0.4.1 / versionCode 5 설치 및 재실행 성공.

## 2026-11-05 · 0.5.0 목적지 변경·반경 추천

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 최종 성공 (13초). 단위 45개 통과, Lint 오류 0 / 기존 경고 9개.
- `DestinationDemoFlowTest,NavigationFlowTest,NavigationRepositoryTest,DemoResetFlowTest,GameStateCodecTest,GridGuildFlowTest,UiRegressionTest` 실행: **OK (24 tests)**, 48.805초.
- 새 단위 4개: 목적지별 반경 필터/순서 및 원래 충전소 좌표 보존, 미확인/고SOC/후보 없음 구분, Haversine 거리/반경, 랜덤 위치의 현재 제외를 검증했다.
- 새 화면 3개: 삼락 1→센텀 2→기장 0, 랜덤 위치 변경, 진행 미션 보존 및 빈 후보 지역에서 기존 미션 +120 XP 완료, SOC 90/89 구분을 검증했다.
- 추가 전송 계층 1개는 SDK onDestinationChanged 이벤트를 FakeNaviClient로 전달해 자동 재조회/후보 변경을 검사한다. 실제 Pleos 내비 서비스 성공으로 집계하지 않는다.
- 일반 앱에서도 센텀→광안리→기장을 직접 눌러 후보 수 2→2→0 및 첫 후보 센텀→광안 변경을 확인했다. 테스트 전 원래 데이터를 복원하고 수동 QA 후에도 백업과 저장 파일 SHA256 일치를 확인했다.
- 0.5.0 / versionCode 6 설치. 위치도/목적지 선택/빈 후보 실제 PNG를 시각 검토했다. 자세한 피드백은 `docs/DESTINATION_FEEDBACK.md`, 캡처는 Git 제외 `artifacts/destination-demo/`다.

## 2026-11-05 · 0.6.0 HOME CHARGE 단순화

- `:app:assembleDebug :app:assembleDebugAndroidTest :core:test :app:lintDebug` 최종 성공 (31초). 단위54개 통과, Lint 오류0 / 기존 경고9개.
- HOME 3개 + 기존7개 클래스의 에뮬레이터 테스트 **OK (28 tests)**, 52.694초. 화면19개, 코덱4개, 내비 테스트 전송 계층5개다.
- HOME 등록→참여→충전 ON/SOC+2%p→개인/도시 각120 XP·도시Lv.2, 당일 재지급 차단, 고급 설정 저장/재사용, 저SOC 즉시 안내·고SOC 추가 참여 제한을 확인했다.
- 새로운 단위9개에서 야간 시간대/출발 여유, 미설정·저SOC·목표 미달의 즉시 정책, 미래 구간 첫 기준 SOC, 자정/만료, 지연·미래·OFF·UNKNOWN·다른 출처·역순 샘플, 중복, 목표 초과 참여 차단, ROUTE 추가50 XP 보존을 검증했다.
- 최초 전체 실행은28개 중1개 실패였다. 화면 재생성 시 주차 도착 자동 화면이 HOME을 도시 탭으로 덮는 문제를 수정했다. 임시 연결 미확인을 실제 D→P 전환으로 취급하지 않도록 한 뒤 전체28개 통과, 기존 D/P 제한 테스트도 통과했다.
- 새 충전소로 전체 fixture 수가 바뀐 단위/화면 기대값을 조정했다. 삼락2곳·센텀/광안4곳·기장0곳이며 기본 일일 보너스 필수3곳은 유지한다. 위치도가 목록 아래에 표시됨을 PNG로 확인했다.
- schema1/2→3 및 집 설정·HOME 미션의 비어 있는 야간 기준 SOC 왕복 저장을 확인했다. 일반 앱에서도 등록 뒤 프로세스를 종료/재실행해 이름 자동 복원을 확인하고, 선택 출발08:00에서23~03시 예시 표시를 확인했다.
- 테스트 전 사용자 기록을 백업하고 종료 후 원래 파일 복원 직후 SHA256 일치를 확인했다. 원래 기록으로0.6.0 재실행·HOME 화면 표시 확인. 이후 저장 형식3으로 정상 이행한다.
- 실제 Home Vehicle SDK 충전 변화 종단 검증은 수행하지 않았다. 기존 Vehicle 현재값 성공/Navi 목적지 콜백 미수신 상태를 유지한다. 캡처는 Git 제외 `artifacts/home-charge/`, 상세 정책/상시 관찰 한계는 `docs/HOME_CHARGE.md`다.
