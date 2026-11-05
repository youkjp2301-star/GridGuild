# GridGuild 디자인 시안 01

작성일: 2026-11-05 (Asia/Seoul). 상태: **반려·보존본**. 도시 표현과 XP 노란 원을 수정한 시안02를 사용자가 승인했다. [현재 적용 방향](VISUAL_DIRECTION_V2.md)을 따른다.

[한 장 시안 보기](gridguild-visual-direction-v1.png)

## 이번 산출물

사용자 제공 네 이미지의 색감·편집 구성·반투명 탐색·호버 영역을 참고한 GridGuild 홈 및 모션 방향 시안이다.
실행 앱 캡처나 실제 영상이 아니며, 앱 소스/리소스/PRD는 이번 단계에서 바꾸지 않았다.
하단 세 칸은 도시 로딩·XP 획득·탭 전환의 정지 스토리보드다. 실제 영상 및 애니메이션은 승인 후 구현한다.

## 적용 방향 제안

- 따뜻한 아이보리, 세이지/포레스트 그린, 차콜, 소량의 앰버. 얇은 선과 부드러운 카드 그림자.
- 홈의 도시를 중심 콘텐츠로 크게 배치한다. 작은 도시 카드 대신 넓은 풍경, 수치/참여 동작은 간결한 보조 패널로 둔다.
- 메뉴 명칭은 홈 · 미션 · 내 도시 · 랭킹. 선택 상태와 호버 상태를 구분하고 마우스/터치/키보드 포커스 모두 누를 범위를 표시한다.
- 반투명 패널은 글자 대비를 유지하며, 성능/플랫폼 제약이 있으면 불투명 크림 패널로 대체한다.
- 도시 로딩은 같은 해금 단계의 건물/식생이 올라오는 짧은 연출로 제안한다. 미래 잠금 도시나 실제 미획득 레벨을 노출하지 않는다.
- 로딩은 데이터 준비와 연결하고 준비 완료 후 임의로 오래 기다리게 하지 않는다. 미확정 진행률을 실제 비율처럼 표시하지 않는다.
- XP는 에너지 입자 → 성장 표시 흡수 → 수치 증가/부드러운 파동으로 제안한다. 실제 저장된 보상 이벤트에서만 발생한다.
- 탭은 이동 순서에 맞춘 좌우 슬라이드와 짧은 누름/복원 효과를 조합한다. 0.97배 누름과 약220~300ms 전환은 구현 시작점이며 확정 성능 값이 아니다.
- 주차/플랫폼 허용 조건은 그대로 유지한다. 주행 제한 시 모션을 중단하며, 모션 축소/낮은 성능에서는 정지 이미지·짧은 전환으로 대체한다.
- HOME/ROUTE 동작, XP 중복 방지, 데모 출처, 미해금 도시 잠금 정책은 유지한다.

## 다음 단계

사용자가 색감·도시 비중·메뉴/모션 방향을 확인한 뒤 실제 Compose UI·로딩 리소스·이펙트 구현, 빌드 및 에뮬레이터 검증을 진행한다.
이번에 앱 빌드/테스트를 다시 실행하지 않았으며, 시안 생성 성공을 구현 완료로 표시하지 않는다.

## 생성 기록

내장 **image_gen** 도구로 생성했다. 별도 CLI/API 키를 사용하지 않았다.
참고 이미지는 스타일용이며 Agora 텍스트·상표·Fuji 콘텐츠를 복제하지 않는다. 원본 첨부 파일은 보존했다.
프로젝트 저장 경로: `docs/design/gridguild-visual-direction-v1.png`.

### 최종 생성 프롬프트

```text
Use case: ui-mockup.
Create ONE polished high-resolution landscape approval board for the actual Korean Android Automotive city-building app GRIDGUILD, not an advertisement or a generic website. This is an approval-only design concept, not a screenshot of shipped software.

Input references: Images 1 and 2 are style references for warm off-white editorial composition, hairline grid borders, botanical painterly low-poly detail, restrained amber accents, and soft raised cards. Do not copy Agora branding or its text. Image 3 is a style reference ONLY for frosted translucent navigation and an immersive large hero image, not the Fuji content or Apple device frames. Image 4 is the exact interaction inspiration for a large warm-beige pill hover target. Image 5 is the existing GridGuild home for product identity, Korean copy, city content, and demo disclosure; redesign it substantially rather than duplicating its small city card.

Composition: A single coherent design approval sheet, approximately 16:10 landscape. The upper 76% is a very large, crisp, front-facing wide in-car/tablet app screen with no device frame, no perspective tilt. Beneath it, a narrow clean strip has three small storyboard panels for proposed motion treatments. Enough breathing room, legible Korean text, no dense paragraphs.

Main app: warm ivory #F7F4ED canvas, faint precise grid/rules, charcoal typography #242722, sage/forest green #54745D, warm amber #DCA63B, very subtle thin borders and feathered shadows. Stylish editorial sophistication and tactile understated game UI. Elegant serif accents in the Latin wordmark only, clean highly legible Korean sans-serif labels. A frosted translucent left sidebar about 15% of app width shows GRIDGUILD and a small bolt mark, then simple line icons with EXACT labels "홈", "미션", "내 도시", "랭킹". Home has a restrained active indicator. A visible mouse cursor hovers "미션", revealing a generous rounded taupe/beige pill target like reference4, with clear contrast from the active indicator. Bottom of sidebar a small "데모 모드" pill.

The home city is the unmistakable star: enlarge it to dominate around 65% of the main body. A beautifully detailed original riverside medieval garden village, slate-blue and warm timber roofs, stone bridge, clear river, lush painterly trees and tiny gardens, sunlight, gentle warm miniature game-art charm, slight isometric elevation. It should feel like a living diorama with soft atmospheric depth and botanical forms related to reference1, artistically richer than flat icon art. It blends into the airy ivory scene, not trapped in a small dark rectangular card. No futuristic towers or locked future city previews.

At upper left of the main content, use only a short clear headline "충전이 쌓이면, 도시가 자랍니다." and small "사상구 · 내 도시". Small glass-like HUD chips over empty margins show "현재 배터리" with a large "42%", and "도시 성장" with "120 XP", plus a thin amber growth meter. A restrained action button reads "충전 미션 참여하기". Tiny, unobtrusive footer text "샘플 데이터 · 데모 XP". Keep the city large and do not overload it with text or many cards.

Motion strip: clearly separated from the app screen by a fine rule, three refined small panels with EXACT captions:
1. "도시 성장 로딩" — three tiny sequential frames of the SAME basic village: foundation, small houses rising, complete leafy village. Only the current village tier, no new future levels. Suggest a brief graceful growth loop with subtle upward motion lines.
2. "XP 획득" — amber energy particles and soft sparks flow into a city growth ring, with a beautiful compact "+120 XP" and one gentle expansion ripple. This is a reward animation keyframe, not a plain disappearing text banner.
3. "탭 전환" — a beige pill menu hover/pressed state and two restrained ghost panels with a directional slide arrow; a small scale-down/return impression shows tactile click feedback, no violent effects.
These are STATIC STORYBOARD KEYFRAMES representing animation intent, not a claimed video.

Add a tiny board caption "GRIDGUILD · 디자인 시안 01" outside the app at the bottom. No extra prose, no invented metrics, no photos of people, no unrelated logos, no garbled pseudo-text, no excessive neon, no heavy dark UI, no confetti explosion, no giant chart. Produce exactly one complete image with all elements elegantly composed.
```
