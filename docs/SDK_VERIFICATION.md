# Pleos SDK 공식 지원 검증

확인일: 2026-11-04. 문서와 바이너리를 대조했다. 문서 지원, 컴파일, 실제 서비스 동작은 다른 검증 단계다.

## 실제 배포 SDK

- Vehicle: `ai.pleos.playground:Vehicle:2.3.2`
- NaviHelper: `ai.pleos.playground:NaviHelper:3.0.5` (전이 의존 navi-data 3.0.4)
- 공식 Maven: https://nexus-playground.pleos.ai/repository/maven-releases/
- 공식 배포 AAR 다운로드 성공. JDK javap로 공개 타입·시그니처·RequiresPermission 확인.
- Android API min 28 / compile·target 35, JDK 17, AGP 8.8.2 / Gradle 8.10.2. 공식 Vehicle 샘플의 도구 기준을 사용.

## API별 판정

| 요구 | 검증된 API | 판단 / 앱 처리 |
|---|---|---|
| SOC | `getEvBattery().checkEvBatteryStateOfChargeCapabilities`, `getEvBatteryStateOfCharge((Int?) -> Unit, onFailed)` | 지원. Int %, null은 미확인. READ/READ_WRITE 확인 |
| 충전 상태 | `checkEvChargeSwitchCapabilities`, `getEvChargeSwitchState((Boolean?) -> Unit, onFailed)` | 지원. 앱은 정확히 충전 스위치 ON/OFF로 표시. 별도 실제 전력 유입·대기 세분 상태를 만들어내지 않음 |
| 변경 | SOC `registerEvBatteryStateOfCharge` / `unregister...` 및 Get | SOC 구독 가능. MVP 배터리는 2초 Get 갱신. 충전 스위치 구독 공개 메서드는 확인 안 됨 → Get |
| 주차 | `getGear().getCurrentGearSelectionState`, `registerGearSelection` / `GearSelectionListener.onSelected`, `GEAR_PARK` | SDK 바이너리에 지원. CarUxRestrictions와 함께 사용 |
| 플랫폼 UX | `CarUxRestrictionsManager.currentCarUxRestrictions`, `registerListener` | AAOS 공식 API. unknown/끊김/요구최적화 true 시 제한 |
| 목적지 | `NaviHelper.getDestinationInfo`, `onDestinationInfo(DestinationInfo)` | 지원. 이벤트 수신 전 출처 UNAVAILABLE |
| 목적지 설정 | `requestRoute(RouteInfo)` | 지원. 요청 전송을 성공 등록으로 표시하지 않음 |
| 경유지 | `addWaypoint(RequestWaypointInfo)`, `getWaypointInfo`, `onWaypointChanged`, `onWayPointInfo` | 지원. 요청 뒤 실제 경유지 좌표를 재조회하여 일치할 때만 확인 표시; 12초 타임아웃 |
| 재탐색 | `requestReRoute`, `onRoutingCompleted` | 지원. 전송·완료를 구분 |
| 정밀 경로 형상 | 공개 NaviHelper API에서 MVP에 필요한 전체 polyline 조회 계약 미확인 | 구현/지원으로 주장하지 않음. UI에 목적지 주변 거리 정렬·형상 미사용 표시 |
| 충전소/계통/인증 | 현재 SDK 연동만으로 실제 충전소 운영·계통 여유·충전 위치 인증 보장 없음 | 전부 명시적 fixture. 모든 XP는 데모 성과 |

## 최소 권한

| 권한 | 사용 |
|---|---|
| `pleos.car.permission.CAR_ENERGY` | SOC/충전 capability 및 읽기 |
| `pleos.car.permission.CAR_DRIVE` | 기어 읽기/구독 (실제 AAR RequiresPermission으로 확인) |
| `pleos.car.permission.NAVI_ROUTE` | 목적지·경유지 조회 |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | 경유지·목적지·재탐색 요청 |

차량 제어 권한, 지도 오버레이 권한, 위치 권한을 선언하지 않는다. Pleos App Market 배포에는 공식 문서가 요구하는 프로젝트/API 승인 범위를 별도로 충족해야 한다. 선언만으로 서비스 접근이나 시장 심사를 보장하지 않는다.

## 공식 근거

- [Vehicle SDK 가져오기](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/project-setup)
- [Vehicle 샘플 기준 환경](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk-sample-app)
- [Capability](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/check-capability)
- [SOC API](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/ev-battery/get-ev-battery-state-of-charge)
- [충전 스위치 API](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/ev-battery/get-ev-charge-switch-state)
- [NaviHelper 개요](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/intro)
- [Navi 권한](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/declaring-permissions)
- [Navi 경유지 설정](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/waypoint-settings)
- [AAOS 주차 앱](https://developer.android.com/training/cars/parked/automotive-os)
- [Pleos 환경/이미지 주소](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [공식 Windows/Linux 이미지 XML](https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml)

XML 및 SDK 바이너리는 공개 접근 가능. 이미지 설치·사용은 해당 SDK 라이선스 조건이 적용된다. 로그인 여부와 API 사용 승인 여부는 구분한다.

## 2026-11-05 실제 런타임 결과

- Pleos API 34 전용 이미지 / 사용자 10에서 Vehicle 프로브 통과:
  SOC 100.0%, 충전 스위치 OFF, GEAR_PARK, CarUxRestrictions 허용 수신.
- Navi Service는 연결됐으나 목적지 콜백이 30초 내 오지 않아 프로브 실패.
  목적지 설정 상태와 Navigation 앱의 전달 상태를 확인해야 하며, 현재는 실제 목적지/경유지 연동 완료로 표시하지 않는다.
- Mock 화면 흐름 3개는 별도 표준 Android 이미지에서 통과했다. 실제 SDK 런타임 결과와 혼합하지 않는다.
