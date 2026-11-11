# 에뮬레이터 장애 진단 — 2026-11-10~11

## 데이터 보호와 복구

- 기존 `GridGuild_Mock_API34`는 정지 상태로 보존했다. wipe-data, 앱 데이터 삭제, AVD 제거는 수행하지 않았다.
- 원본 앱 샌드박스 백업: `files/gridguild_pre_driver_0110_final_20261110_143335.pb`.
- 실제 DataStore `files/datastore/gridguild_v1.preferences_pb`로 복원한 후 백업과 SHA256 일치를 확인했다: `65f65b404a6f2ad741e389d4f44ee97fa16eae393542ab1165e6f0c2bb6d9882`.
- 11월11일 정지된 원본 AVD 디렉터리 전체와 외부 `.ini`도 `.tools/backups/GridGuild_Mock_API34_20261111/`에 복사했다. AVD 내부31파일, 5,818,709,092바이트 각각의 원본/복사본 SHA256이 일치했다. 목록은 해당 폴더의 `SHA256.json`이다. 설정·가상 디스크·암호화 키를 함께 보관한다.
- DataStore 복원은 실제 확인했다. 전체 AVD 백업을 재기동해 복구하는 검사는 원본을 건드리지 않기 위해 수행하지 않았다. 필요하면 기존 AVD를 덮어쓰지 않고 백업을 별도 경로에 복사·등록해 검사한다. 백업/기기 로그/APK는 Git 제외다.

## 1. 오류가 발생한 정확한 단계

| 환경/단계 | 실제 관찰 | 판정 범위 |
|---|---|---|
| 원본 AVD 최초 전체 테스트 | System has crashed, System UI/전화 앱 `DeadSystemException` | Android 시스템 프로세스 장애. 최초 원인은 미확정 |
| 원본 소프트웨어 렌더러 및 새 QA 자동 검사 | 각각10개 중 Global Back 검사1개 실패. 시스템 ANR 모달이 입력 포커스를 보유 | 9개 통과만으로 정상 화면·정상 Back이라고 판단하지 않음 |
| QA 앱 종료·런처 대조 | GridGuild PID가 없어도 `Process system isn't responding`이 남음 | 이미 발생한 시스템 ANR가 앱 종료 뒤에도 잔류 |
| QA 정상 Wait 버튼1회 후 | 런처 포커스,10초 동안 모달 재등장 없음 | 재부팅/데이터 삭제 없이 잔류 모달 해제 |
| 일반 홈 | 실제 adb 터치, 앱 포커스·스크린샷·10초 관찰 정상 | 해당 구간의 정상 실행 확인 |
| 기존 내 도시 이미지 | 실제 adb 터치, 앱 포커스·스크린샷·10초 관찰 정상 | 기존 이미지 화면 정상 실행 확인 |
| 새1인칭 화면/광안리 Lv.12 | 실제 adb 터치,12초 관찰·앱 포커스·스크린샷 정상 | 새 장면 정상 실행 확인 |
| 새 장면에서 실제 시스템 Back | `adb input keyevent KEYCODE_BACK` 후 기존 세로 화면 복귀 | 모달 해제 뒤 Back 수동 검사 성공. 앞선 실패한 자동 검사를 통과로 바꾸지 않음 |
| 대표 레벨/리소스 | 두 목적지의 현재 Lv.2/12, 잠금/원장 보존 +24개 네이티브 해독 검사:2개 통과,33.834초 | 정상 포커스 캡처 guard 포함. 실제 SDK/실차 검사가 아님 |

정상 캡처만 `artifacts/driver-v011/runtime-qa/`에 제공한다. 시스템 모달이 있는 이전 캡처는 진단 증거이며 정상 장면으로 집계하지 않는다.

## 2. 로그의 원인과 유력 가설

QA `stage01-logcat-all.txt`의 기기 시각 순서:

0. **06:15:19~06:16:49** Google Play Services의 도메인/SIM 검증·인증 서비스, YouTube/Music 미디어 서비스, Bluetooth HID 서비스에서도 ANR가 발생했다. 첫 기록은20975줄, Bluetooth는28040줄이다. GridGuild 최초 프로세스보다 앞선 **시스템 전반의 첫 부팅 응답 지연**이다.
1. **06:16:58** `am_anr`: `system`, `PACKAGE_NEEDS_INTEGRITY_VERIFICATION` 브로드캐스트.
2. **06:17:16** ActivityManager 상세: `AppIntegrityManagerServiceImpl$1` 응답 지연. 부하19.2/9.1/3.55, CPU PSI avg10 약76.76, 총 CPU 약94%·커널71%.
3. **06:17:43** 첫 `com.gridguild` 프로세스 시작 기록.
4. **06:18:15** 첫 MainActivity 시작 기록.

따라서 **이 QA의 system ANR는 새 장면 렌더링·화면 종료·Back 호출보다 먼저 발생한 패키지 무결성 검증 지연**이다. 부팅 시 여러 서비스가 함께 지연되고 CPU 압박이 높았으므로 시스템 전반의 처리 부하가 유력하다. 설치 요청과 부팅 부하 중 어느 요인이 얼마나 기여했는지는 미확정이다. 이 근거로 원본 AVD의 시스템 종료까지 같은 원인이라고 단정하지 않는다.

Compose `performClick`는 앱 View에 직접 이벤트를 주입하므로 시스템 모달 뒤에서도 일부 테스트가 통과할 수 있다. 반면 시스템 Global Back은 모달에 가로막힐 수 있다. 장면 캡처 helper에 활성 창 패키지가 `com.gridguild`인지 확인하는 assertion을 추가했다. 실패한 Global Back 검사를 제거하거나 앱 Back 동작으로 대체하지 않았다.

정상 관찰 구간의 로그는 `.tools/driver-diagnostics-20261110/`의 단계별 logcat/window/lastanr/memory/PNG에 보존했다. 기존 `lastanr` 덤프가 비어 있는데 화면에는 모달이 있는 불일치도 있어, 단일 덤프만으로 ANR 부재를 판단하지 않는다.

### 환경과 렌더링 점검

- 호스트32GB RAM, 검사 당시 약11.4GB 여유. Intel Iris Xe 드라이버31.0.101.4032(2022-12-21).
- `emulator -accel-check`: WHPX10.0.26200 설치·사용 가능.
- 시스템 이미지: API34 Google APIs x86_64 revision14. 이미지 파일 손상을 입증한 근거는 없다. 공식 배포 원본 전체의 무결성을 이번에 새로 검증한 것은 아니다.
- QA: Pixel2 기본1080×1920/density420, RAM2048MB, SwiftShader. 원본 AVD의 화면·설정은 바꾸지 않았다.
- `-feature -Vulkan`은 Vulkan 기능을 끄는 옵션이다. 옵션 자체의 충돌을 입증하지 못했다. 임의의 조합으로 부팅을 반복하지 않았다.
- 새 장면은 **PNG + Compose HUD**이며 WebView/실시간3D 엔진을 사용하지 않는다. 각 PNG1672×941, ARGB 기준 약6MiB, LRU 최대3장(약18MiB), IO에서 해독한다.24장을 동시에 상주시켜 렌더링하지 않는다.
- 광안리 Lv.12 정상 관찰 시 앱 TOTAL PSS146,800KB(약143MiB). 앞선 게스트 가용 메모리 약1GB. 확인한 장애 시점에서 앱 OOM을 입증한 기록은 없었다.
- QA 종료 로그에는 `invalid color buffer`, GL context 복원 오류가 있다. 종료 시점 경고만으로 최초 패키지 검증 ANR나 앱 장면의 원인이라고 연결하지 않는다. 호스트 그래픽 경로 불안정 가능성은 남겨 둔다.
- 추측으로 넣었던 창 복원 `decorView.post` 실험은 원인이 입증되지 않아 소스에서 되돌렸다. 실험 APK는 `artifacts/driver-v011/diagnostics/`에 격리했으며 제출 APK로 사용하지 않는다.

## 3. 실제 성공한 검사

- APK/Android 테스트 APK 빌드, core 단위78개, Lint 오류0·경고14.
- 두 목적지×12단계 원본 자산24개 시각 검수와 최종 SHA256 대조.
- 위 표의 홈/기존 도시/새 Lv.12 장면·실제 Back 수동 확인.
- 대표 Lv.2/12 및24개 네이티브 PNG 디코딩/크기/픽셀 고유성 검사2개 통과.
- 최종 APK 회귀53개 중50개 통과(552.554초). 새 장면8개/리소스1개 모두 통과하며24개 단계 선택·원장 보존·재잠금 검사를 포함한다. 기존 UI3개 실패 중 SOC 문구는 스크롤을 추가한 재검사에서 통과했다. 대표 장면과 함께 최종2개 재검사46.741초 통과.
- 순회 캡처 일부가 진입 alpha=0 프레임임을 직접 확인했다. 정상 캡처로 사용하지 않고 캡처 시계/실제 프레임 대기를 보완했다. 최종 대표4장만 `artifacts/driver-v011/runtime-qa/final-apk/`의 정상 실행·시각 확인 증거로 제공한다.
- 실제 시스템 터치로 ROUTE 새로고침 요청#1→재탐색 완료#2→새로고침 완료#3을 확인했다. 기존 내비 저장소5개도 통과했다. 특정 충전소 목적지 선택/순간 비활성 자동검사는 미해결이다.
- 원본 DataStore 복원 hash 및 별도 오프라인 AVD 전체 백업 hash 검증.

세부 성공/실패는 [TEST_REPORT.md](TEST_REPORT.md)에 기록했다. 이전0.10.0의48개 성공을 현재0.11.0 전체 통과 수치로 사용하지 않는다.

## 4. 미해결과 다음 작업

- 원본 AVD의 SystemServer 종료 최초 원인, 호스트 GPU 드라이버 기여, QA 설치 검증 ANR의 재발 조건은 확정하지 못했다.
- 동일 실패 자동 Back 검사는 두 번 실패했으므로 무한 재시도하지 않았다. 모달 없는 상태의 시스템 Back 성공과 구분해 남긴다.
- 기존 `NavigationFlowTest`2개는 아직 미통과다.650ms 처리 중 상태의 관측 경합과 작은 화면 클릭 범위가 가설이며 앱 오류 가능성을 확정적으로 배제하지 않는다. 네이티브 실제 터치의 갱신/재탐색 성공을 해당 자동검사 전체 통과로 바꾸지 않는다.
- 전체 AVD 복구 부팅·다른 PC/실차·실제 Pleos N 자동 진입은 미검증이다. 기존 P/CarUxRestrictions 보호는 유지했다.
- 공모전 APK와 기존0.10.0 APK를 별도로 보존한다. 시스템 경고가 있는 실행을 성공으로 제출하지 않는다.
