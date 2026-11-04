# Aside 조사 통합 기록

Aside CLI 내장 `aside-browser` 스킬을 통해 읽기 전용 조사를 위임했다.
세션: `YhgUKqE1f4RC71ec`, 확인일 2026-11-04.

반환받은 조사 결과:

- 공식 문서의 Windows/Linux Pleos 이미지 저장소:
  https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml
- 공식 환경 가이드:
  https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- Vehicle AAR와 이미지 목록 XML은 로그인 없이 접근 가능함을 확인.
- 설치·사용은 XML에 포함된 SDK 라이선스 조건이 적용됨.
- `getEvChargeSwitchState`는 공식 문서에서 충전 상태로 설명되지만,
  실제 전력 유입량·충전 세션·위치를 인증한다는 보장은 별도로 확인되지 않음.
- 포털의 프로젝트 생성 문서에는 Vehicle State와 NaviHelper는 별도 신청 없이 사용 가능하다고 안내한다.
  공개 다운로드·런타임 서비스 접근·App Market 권한 심사는 각각 별도 확인 항목이다.

통합 후 직접 재검증:

- 공식 XML을 내려받고 실제 이미지 zip 주소와 4,087,751,964 byte 크기 확인.
- Vehicle 2.3.2 / NaviHelper 3.0.5 AAR 공개 다운로드와 typed API 시그니처 확인.
- 실제 기능 동작 여부는 빌드 및 에뮬레이터에서 별도로 검증하며, 이 조사만으로 성공으로 표시하지 않는다.

Aside의 프로젝트 폴더 쓰기 작업은 접근 제한 때문에 취소했다.
이 파일은 상위 개발 에이전트가 전달받은 결과와 직접 검증한 내용을 정리한 기록이다.
