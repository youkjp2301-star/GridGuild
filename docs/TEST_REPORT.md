# GridGuild 검증 기록

확인일 2026-11-04. 문서 지원·컴파일·실제 서비스 성공을 구분한다.

| 검증 | 결과 |
|---|---|
| 공식 SDK 배포 | Vehicle 2.3.2 / NaviHelper 3.0.5 AAR 실제 다운로드 |
| 공식 Pleos 이미지 | v2.1.0 API 34 x86_64, SHA1 `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa` 공식 목록과 일치 |
| APK 빌드 | `:app:assembleDebug` 성공 |
| Lint | `:app:lintDebug` 성공. 버전 업데이트 권고 등 경고는 남음 |
| 단위 테스트 | `:core:test` 24개, 실패/오류 0 |
| 화면 테스트 | **2개 통과**. 미션→경유지 Mock→개인/도시 120 XP→D단 차단→P단 복귀, SDK 불가→Mock 복귀 |
| Git 추적 파일 구성 | 로컬 설정·SDK 폴더를 제외한 Git index export에서 assembleDebug + core:test + lintDebug 모두 성공 |
| 표준 Android 실행 | API 34, 앱 설치·화면 표시 확인. 호스트 에뮬레이터 GPU/SystemUI ANR 발생 이력 있음 |
| Pleos 실행 | 설치·부팅·앱 설치·최초 설정 화면 확인, 실제 권한 부여 확인 |
| Virtual Console | SOC 42%, 충전 OFF, 기어 P 설정을 Aside가 UI에서 확인 |
| SDK 런타임 | Navi 목적지/경유지 콜백 로그와 기어 구독 등록 로그 확인. SOC 실제 수신·보상·경유지 성공 종단 검증은 미완료 |

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

## 아직 완료로 표시할 수 없는 항목

- Pleos SDK의 실제 SOC·충전 상태 변화 → 미션 완료 → XP 지급 전체 흐름.
- NaviHelper 경유지 실제 등록 성공 및 좌표 재조회 확인.
- Pleos 주행 재개 시 플랫폼 차단과 복귀를 포함한 종단 시연.
- GitHub CI: 원격 저장소를 생성·업로드하지 않아 아직 실행 안 함.

로컬 테스트 산출물은 Git에 넣지 않는다. 재실행 명령과 최신 결과는 README/DEV_LOG에서 관리한다.
