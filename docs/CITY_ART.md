# 도시 아트 v4 (0.7.0)

**최종 적용:** 사용자가 “지금 하던건 그대로 진행하고 도시 사진만 이렇게 바꾸자”라고 제공한 최신 건축 모형 레퍼런스2장을 따른다. UI/모션 구성은 유지하고 도시12장과 장식 로딩4프레임만 낮은 채도·실제 건축 비례·정밀 재질의 건축 모형 스타일로 교체했다.

런타임 자산은 `drawable-nodpi/city_medieval_v4.png`, `city_modern_v4.png`, `city_future_v4.png`, `city_loading_v2.png`다. 모두1774×887의 2×2 아틀라스이며 단계 매핑·잠금·디코딩 규칙은 아래와 같다. 초기 마을은 석재/회벽/목재/슬레이트, 중간은 근대~현대 업무·연구·교통, 후기는 정원 주거·고층 빌딩·철도·에너지 시설과 보존 역사 지구를 조합한다.

생성 도구는 built-in image_gen이며4개 정확한 프롬프트는 [CITY_ART_V4_PROMPTS.md](design/CITY_ART_V4_PROMPTS.md)에 저장했다. 중간 v3와 로딩v1은 [보존 폴더](design/previous-assets/)로 옮겨 APK에 포함하지 않는다. 원래 커밋된 v2 파일은 기존 위치에 보존했다.

## 중간 v3 아트 기록 (교체됨)

2026-11-05 승인 시안02와 최초 재질 디테일 요청을 반영한 중간 산출물이다. 아래 v3는 현재 런타임에 연결하지 않는다.

| 자산 | 용도 | 구성 |
|---|---|---|
| `city_medieval_v3.png` | Lv.1~4 | 강변 마을 / 성문·물레방앗간 / 시장·종탑 / 벽돌 산업 지구 |
| `city_modern_v3.png` | Lv.5~8 | 근대 광장 / 현대 도심·트램 / 태양광 연구 / 스마트 교통·도시 농장 |
| `city_future_v3.png` | Lv.9~12 | 생태 수변 / 초연결 / 하늘 정원 / 미래 랜드마크 |
| `city_loading_v1.png` | 준비 중 장식 | 같은 작은 마을의 기초 / 목재 골조 / 벽·지붕 조립 / 완성 |

v3는 현재 `docs/design/previous-assets/`에 보존한다. 각각1774×887 PNG의 2×2 구성이다. v4도 같은 구조이며 각 타일은 약887×443, 프레임 경계2px 안쪽을 렌더링한다. 현재 런타임12개 프레임의 픽셀 지문을 실제 Android 디코더로 검사한다.

`CityCanvas`는 해금 검사 후 IO 스레드에서 디코딩하고 최대3개 시대 아틀라스를 캐시한다. 리소스 키가 바뀐 직후 이전 이미지가 남아 있더라도 새 레벨의 프레임 좌표로 그리지 않는다. 초기화·시대 전환 시 미해금 장면이 잠깐 나타나는 것을 방지한다. 실패 시 다시 불러오기 동작을 제공한다.

로딩4프레임은 미획득 레벨과 관계없는 동일한 소규모 건물의 장식용 조립이다. 준비 여부는 실제 저장소/이미지 상태에 따르고 임의의 최소 로딩 시간을 추가하지 않는다. 정지 그림을 영상이라고 표시하지 않으며 런타임 프레임 전환은 Compose로 구현한다.

## v3 생성 사양

built-in image_gen 4회, 불투명 배경. 사용자 제공 해안 마을·재질 확대 사진을 스타일 레퍼런스로 사용하고 승인 시안02를 구성 참고로 사용했다. UI 보드에서 잘라내거나 기존 타일을 재색칠한 자산이 아니다. 아래는 생성에 사용한 설계 사양의 기록이다.

- Original premium stylized 3D city-building game diorama assets, not UI. Exactly four equally sized landscape scenes, 2×2 row-major grid, consistent elevated three-quarter view. Warm cream surroundings, turquoise river foreground, rich foliage, precise readable architectural geometry. No text, UI, logos, watermarks, yellow glow or reward halo.
- Show individual uneven terracotta tiles and ridge caps, rough stone blocks with mortar, timber beams with grain, worn plaster, brick courses, slate, copper patina, varied glass and metal. Distinct silhouettes and building functions rather than repeated cottages. Directional warm sunlight, cool contact shadows, recesses, restrained warm interior windows. Dense small props, flower boxes, stairs, railings, paving seams, workshops, planting and riverbank details.
- Medieval stages: varied cottage, timber/plaster two-storey home, stone workshop and inn; expanded gate/watermill; civic market/bell tower with terracotta/slate/copper roofs; brick foundry/warehouse/cranes. Each stage is separately composed and visibly developed.
- Modern stages: heritage masonry and civic market; contemporary glass/concrete/tram/fountain; solar research/greenhouses/brick-metal workshops; smart transit/wood-steel facades/urban farms. Diverse materials, roof lines, heights and window patterns.
- Future stages: eco mid-rise/wood-white surfaces/green roofs; connected district with physical ring sculpture; varied slender towers, domes, sky gardens; developed sustainable landmark skyline and bridges. No luminous rings or sci-fi glow.
- Loading atlas: same fixed footprint, river/dock/trees/camera and three structures across all four frames. Foundations→posts/frames→walls/roofs→completed stone cottage, timber workshop and plaster/timber house. This is construction assembly of one decorative scene, not four city levels.

## 보존된 도시 아트 v2

사용자 제공 중세 마을·현대 도시·미래 도시 이미지를 스타일 참고로 삼아, built-in image_gen으로 새로 생성했다. 기존 이미지 자체를 복사한 것이 아니라 새로운 게임 아트다. 실제 주변 건물/지형을 재현하는 디지털 트윈이나 실시간 3D 시뮬레이션이 아니다.

## 프로젝트에 저장한 자산

- `app/src/main/res/drawable-nodpi/city_medieval_v2.png`: Lv.1~4
- `app/src/main/res/drawable-nodpi/city_modern_v2.png`: Lv.5~8
- `app/src/main/res/drawable-nodpi/city_future_v2.png`: Lv.9~12

각 PNG는 2×2 타일 아틀라스이며 좌상단→우상단→좌하단→우하단 순서다. `CityStage.at`이 매 레벨을 서로 다른 (era, frame)에 연결한다. 원본 PNG를 보존하고 렌더링 시 각 프레임 경계를 2px 안쪽에서 읽는다. 해금한 도시 둘러보기는 XP/원장을 변경하지 않는다.

## 0.3.0 해금 정책

`CityUnlocks`는 현재 지역의 도시 XP로 실제 해금 단계를 계산하며 현재 및 과거 단계만 허용한다.
`CityCanvas`는 아틀라스를 읽기 전에 `unlockedThrough`를 검사한다. 초기화 직후 전환 애니메이션의 이전 프레임에도 같은 제한을 적용한다.
미해금 컬렉션 및 다음 단계 안내는 `LockedCityArtwork`의 공통 기하학 배경·자물쇠만 사용한다. 실제 도시 이미지의 블러·잘라내기·이름·랜드마크 미리보기를 제공하지 않는다.
잠금 이미지는 Compose Canvas로 작성했으며 추가 생성 이미지나 외부 자산은 없다. 기존 도시 PNG는 변경하지 않았다.

## 생성 프롬프트

각 자산에 별도 built-in image_gen 호출을 사용했고 transparent_background=false였다. 사용자 참조 이미지는 스타일 reference 역할이다.

공통 사양: Original high-detail miniature 3D isometric city evolution sprite atlas. Exactly four equally sized landscape panels in a 2 by 2 grid, row-major, consistent camera and lighting. Wide 2:1 overall aspect ratio, river and bridge foreground, town behind, dark charcoal backdrop, complete self-contained dioramas, safe margins. Rich realistic architecture/vegetation/water textures, no text, numerals, labels, UI, logos or watermark.

### medieval

Match the reference's finely textured half-timber architecture, slate roofs, stonework, gardens, winding roads and river water. Muted natural greens, blue slate, warm wood, realistic stone.

Top left LEVEL 1: small riverside medieval hamlet, 8 half-timber cottages, blue slate roofs, a simple bridge, vegetable gardens, hillside trees. Top right LEVEL 2: recognizably expanded town, 14 houses, imposing stone gate and low city walls, larger arched stone bridge, watermill. Bottom left LEVEL 3: prosperous renaissance market town, 22 elaborate timber houses, church tower, market stalls and paved plaza, orchard terraces. Bottom right LEVEL 4: early industrial river city, old stone gate remains, brick workshops, tall masonry chimneys with faint steam, warehouses and busier bridge.

### modern

Match reference realistic miniature architectural materials, preserved stone gate, precise tram avenues, clean modern facades, fountain plaza, solar rooftops, greenhouses. Realistic city building game assets, natural sunlight.

Top left LEVEL 5: early modern waterfront town, low-rise masonry blocks, preserved medieval city gate, market square and bridge. Top right LEVEL 6: contemporary city center with glass offices, mid-rise apartments, tram avenue, square and fountain. Bottom left LEVEL 7: advanced clean industrial district, rooftop solar, greenhouses, research campus, electric buses, modern bridge. Bottom right LEVEL 8: thriving smart city, taller contemporary towers, electric tram hub, solar roofs, urban farms and green waterfront.

### future

Closely match reference lush photorealistic miniature futuristic architecture, white sculptural buildings, blue glass, green roof gardens, pink blossom trees, elegant arched bridge and turquoise river. Bright premium sophisticated look, no neon cyberpunk.

Top left LEVEL 9: beautiful green eco city with white mid-rise buildings, abundant trees, terraced gardens, quiet river promenade, clean arched bridge. Top right LEVEL 10: connected eco metropolis with elegant glass towers, electric transit, elevated pedestrian links, riverside plaza and ring sculpture. Bottom left LEVEL 11: high-tech future city with taller slender glass skyscrapers, sky gardens, elevated transit and sculptural architecture, flourishing greenery. Bottom right LEVEL 12: spectacular advanced sustainable future metropolis with landmark towers, interconnected sky bridges, rooftop parks, futuristic transit, ring monument and dramatic pristine river bridge.
