# N단 도시 체험 · 0.10.0

> **이 문서는0.10.0의 이전 구현·사진 라이선스 보존 기록이다.**
> 랜드마크 사진을 보여주는 방식은 사용자의 의도와 달라0.11.0에서 도로 위 운전자1인칭·레벨 대응 장면으로 수정한다.
> 현재 사양과 검수 기준은 [DRIVER_SCENES.md](DRIVER_SCENES.md), 실제 완료된 검증은 [TEST_REPORT.md](TEST_REPORT.md)를 따른다.
> 아래 실사 JPEG와 크레딧은 보존하며 신규 생성 장면의 출처로 사용하지 않는다.

## 실행과 범위

Mock에서 P단·UX 허용 상태를 확인한 후 **연결 · 테스트 → N단 도시 체험 · TEST**를 누른다.
미션의 목적지 변경에서 광안리/센텀을 먼저 선택해도 되고, 체험 하단에서 두 장소를 선택해도 된다.
후자는 기존 MockNavigationRepository의 확인 콜백을 거쳐 목적지를 바꾼다. ROUTE 추천에도 같은 좌표가 전달된다.
실제 NaviHelper로 목적지를 보냈다고 표시하지 않는다.

좌표에서 가장 가까운 준비된 장소를1.5km 이내에서만 선택한다. 삼락·기장·미수신/잘못된 좌표에는
무관한 랜드마크를 표시하지 않고 장소 선택을 안내한다. 사진은 현재 위치를 추적하지 않는다.

이 기능은 **정지된 실사 파노라마 + Compose HUD**다. 영상·실시간 렌더링·차량 카메라·실제 도시 진입 기능은 아니다.
N TEST는 ViewModel의 메모리 상태이며 기어 값이나 저장된 게임 기록을 쓰지 않는다.
XP/Lv는 현재 소속 지역의 실제 저장 데모 원장에서 읽고, 목적지 지역으로 소속을 바꾸지 않는다.
사진을 볼 때도 차량/플랫폼 허용 상태를 확인하며 미확인 시 사진을 숨긴다.

## 안전·화면 수명

- 실제 P + 플랫폼 UX 확인을 유지한다. Mock 체험을 이유로 실제 N/D에서 게임을 허용하지 않는다.
- 확인된 D 또는 UX 제한, 실제 플랫폼 불허, 모드 변경, 백그라운드 전환 시 종료한다.
- 구성 변경 중에는 연결 허용이 다시 확인되기 전 사진을 보여주지 않는다.
- 체험에 한해 가로·시스템 바 숨김을 요청하고 종료하면 원래 방향/시스템 바를 복원한다.
  고정 화면·멀티윈도 등 OS가 방향 요청을 무시하는 환경에서는 사용 가능한 크기에 맞춘다.
- normal SaveableStateHolder로 기존 탭/ROUTE 선택과 스크롤 상태를 보존한다. 미션 엔진·XP 규칙은 변경하지 않는다.
- 이미지 해독은 IO에서 수행한다. 최대2560×1600 기준 샘플링한 사진2개만 캐시한다.
  콜드 로딩은 최소3초, 캐시 재방문은 즉시 표시한다. 실패 시 재시도 버튼을 제공한다.

## 실제 사진 크레딧

Aside에 공식 Commons File 페이지·사진 구도·라이선스 확인을 위임하고 결과의 원본 파일을 다운로드했다.
아래 두 JPEG는 **재생성·색 보정·합성·자르기 없이 원본 그대로** 수록한다.
런타임은 ContentScale.Fit으로 비율을 유지해 축소/확대하고, 사진 위에는 별도 앱 HUD와 읽기용 그라데이션을 그린다.
사진 원본의 라이선스는 그대로 유지한다. 앱 코드 전체에 사진 라이선스를 적용하는 것은 아니다.
그라데이션/HUD는 원본 JPEG에 저장하지 않는다. 사진 출처 버튼에서 저작자·라이선스 링크를 항상 확인할 수 있다.

| 자산 | 광안리 | 센텀 |
|---|---|---|
| 파일 | immersive_gwangalli_v1.jpg | immersive_centum_v1.jpg |
| 작품 | Gwangan Bridge and Gwangalli Beach - Gwangalli2721 | Busan Cinema Center |
| 저작자 | lumoplank | 399scout |
| 크기 | 5082 × 3034 | 1800 × 1012 |
| 이용허락 | [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/) | [CC BY-SA 4.0 International](https://creativecommons.org/licenses/by-sa/4.0/) |
| 설명 페이지 | [Commons](https://commons.wikimedia.org/wiki/File:Gwangan_Bridge_and_Gwangalli_Beach_-_Gwangalli2721.jpg) | [Commons](https://commons.wikimedia.org/wiki/File:Busan_Cinema_Center.jpg) |
| 원본 | [JPEG](https://upload.wikimedia.org/wikipedia/commons/0/0e/Gwangan_Bridge_and_Gwangalli_Beach_-_Gwangalli2721.jpg) | [JPEG](https://upload.wikimedia.org/wikipedia/commons/e/e1/Busan_Cinema_Center.jpg) |
| SHA256 | 24d413f97a7c4c91551bf243d33c67f23c84cc2953a87b8737a048b42518a85f | 8277bb62b39f31f955706a29aa6640c655bb0551ee8c49179e1801165f94cfa9 |

광안리 사진은 교량과 수변 건물의 야경으로 모래사장은 보이지 않는다.
센텀 사진은 영화의전당 빅루프 하부·더블콘·광장이며 도시 전체 스카이라인을 담지는 않는다.
어느 사진도 현재의 날씨·시각·도로 상태를 의미하지 않는다. 저작자의 앱 후원을 의미하지 않는다.

## 실제 SDK 미완료

Pleos Vehicle 2.3.2 / NaviHelper 3.0.5 typed 연동 구조와 P·CarUxRestrictions 보호를 유지한다.
이번 작업에서 실제 Pleos N값·중립 정차·N 진입 콜백·실제 목적지에 따른 자동 체험은 검증/구현하지 않았다.
실제 N 자동 진입은 기어만으로 안전을 판정하지 말고 플랫폼 허용·정차 상태 등 별도 계약부터 확인해야 한다.
실제 충전소/전력망/온라인 랭킹도 계속 미연동이다.
