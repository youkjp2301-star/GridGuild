# 도시 아트 v2

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
