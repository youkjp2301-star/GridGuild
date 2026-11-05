# GridGuild 검증 기록

확인일 2026-11-04~05 (Asia/Seoul). 문서 지원·컴파일·실제 서비스 성공을 구분한다.

| 검증 | 결과 |
|---|---|
| 공식 SDK 배포 | Vehicle 2.3.2 / NaviHelper 3.0.5 AAR 실제 다운로드 |
| 공식 Pleos 이미지 | v2.1.0 API 34 x86_64, SHA1 `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa` 공식 목록과 일치 |
| APK 빌드 | `:app:assembleDebug` 성공 |
| Lint | `:app:lintDebug` 성공. 버전 업데이트 권고 등 경고는 남음 |
| 단위 테스트 | 최신 0.6.0: `:core:test` **54개**, 실패/오류 0 |
| 에뮬레이터 테스트 | 최신 0.6.0: **28개 통과** (화면 19 + 저장 코덱 4 + 내비 테스트 전송 계층 5). HOME 등록·참여·자동 XP·설정 저장·저배터리, ROUTE·초기화·잠금·배터리/미션·P/D 제한 |
| Git 추적 파일 구성 | 로컬 설정·SDK 폴더를 제외한 Git index export에서 assembleDebug + core:test + lintDebug 모두 성공 |
| 실제 로컬 Clone | 첫 커밋 4a5fd0e를 `git clone --no-local`로 복제. JAVA_HOME/ANDROID_HOME만 지정해 APK + 24개 단위 테스트 + Lint 성공 (36초) |
| 최종 코드 Clone | 코드 커밋 cfd0141로 fast-forward 후 같은 검사 재통과 (41초). local.properties 미포함 확인 |
| 표준 Android 실행 | API 34, 앱 설치·화면 표시 확인. 호스트 에뮬레이터 GPU/SystemUI ANR 발생 이력 있음 |
| Pleos 실행 | 설치·부팅·앱 설치·최초 설정 화면 확인, 실제 권한 부여 확인 |
| Virtual Console | SOC 42%, 충전 OFF, 기어 P 설정을 Aside가 UI에서 확인 |
| Vehicle SDK 런타임 프로브 | **통과**. 실제 source=PLEOS, SOC=100.0%, 충전 스위치 OFF, GEAR_PARK, UX 허용 수신 |
| Navi SDK 런타임 프로브 | **실패**. Navi Service 연결은 완료됐지만 30초 내 목적지 콜백 미수신. 실제 경유지 등록 성공은 미검증 |

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
