# HU:MOTION · GridGuild 제출 APK

- 팀명: **HU:MOTION** (파일명은 Windows 호환을 위해 `HU-MOTION`)
- 파일: [`HU-MOTION_GridGuild_release.apk`](HU-MOTION_GridGuild_release.apk)
- 앱: GridGuild `0.14.1` / versionCode `16` / `com.gridguild`
- Android 최소 API28, compile/target API35
- 실제 PKCS12 Release 서명, `debuggable=false`, `testOnly=false`, R8/리소스 축소 적용
- 크기: **86,642,803 bytes** (약82.6MiB)
- SHA256: `f03e6b829d400af2473d0c0ed02d32cdc50eb9a4ec00238e6f9ee053031326c6`
- 공개 서명 인증서 SHA256: `f6ba5fa92acc980a5d8ee95eb892f706de5f0c4662e850060f698f6ecea65ebd`

고정 제출 버전은 `submission-2026-11-15-humotion` 태그다. APK와 전체 소스가 같은 제출 커밋에 포함된다. [태그 기준 APK 다운로드](https://github.com/youkjp2301-star/GridGuild/raw/refs/tags/submission-2026-11-15-humotion/apk/HU-MOTION_GridGuild_release.apk)

일반 Android에서는 체험 모드를 선택한다. 기존 Debug 앱의 서명과 다르므로 기존 앱/데이터를 지우지 말고 별도 QA AVD에 설치한다. 자세한 실행 방법과 검증 한계는 [README](../README.md), [제출 기록](../docs/SUBMISSION_2026.md)을 따른다.

```powershell
adb -s DEVICE_SERIAL install apk/HU-MOTION_GridGuild_release.apk
```

서명 키와 비밀번호는 이 폴더나 GitHub에 포함하지 않는다. Debug APK 및 테스트용 APK도 제출 파일에 포함하지 않는다.

이번 변경은 팀명·파일명 정정이다. 기존 제출 태그와 검증된 APK의 바이트·서명은 유지하며 재빌드하지 않았다. 현재 팀명 정정 태그를 메일에 사용한다.
