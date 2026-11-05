package com.gridguild.core

/** Row-major four-frame atlases. Every level has a distinct authored scene. */
data class CityStage(val level: Int, val era: Int, val frame: Int, val landmark: String, val description: String) {
    val name get() = CityLevels.names[level - 1]
    companion object {
        private val landmarks = listOf("강변 목조 마을", "성문과 돌다리", "시장과 종탑", "강변 산업 지구", "도심 광장", "트램과 업무 지구", "태양광 연구 단지", "스마트 교통 허브", "수변 생태 공원", "공중 연결 산책로", "하늘 정원 타워", "미래 도시 랜드마크")
        private val descriptions = listOf(
            "석재·목재·회벽으로 지은 주택과 공방, 작은 농원이 강변에 자리 잡습니다.",
            "성문과 성벽이 들어서고, 돌다리와 물레방앗간이 마을을 연결합니다.",
            "중앙 시장과 종탑을 중심으로 풍성한 도시가 형성됩니다.",
            "벽돌 공방과 강변 산업 시설이 도시의 새 시대를 엽니다.",
            "오래된 성문과 새 도심 광장이 함께하는 근대 도시입니다.",
            "유리 건물과 트램, 분수 광장이 현대의 일상을 만듭니다.",
            "태양광 지붕과 온실, 연구 단지가 새로운 에너지를 모읍니다.",
            "도시 농장과 스마트 교통, 녹색 건축이 연결됩니다.",
            "수변 공원과 옥상 정원, 흰 아치교가 생태 도시를 완성합니다.",
            "공중 산책로와 전기 교통이 도시 곳곳을 이어 줍니다.",
            "하늘 정원과 미래 교통망이 고층 도시를 연결합니다.",
            "조형적인 타워와 입체 녹지, 미래의 도시 풍경이 펼쳐집니다.",
        )
        fun at(level: Int): CityStage {
            val safe = level.coerceIn(1,12)
            return CityStage(safe,(safe-1)/4,(safe-1)%4,landmarks[safe-1],descriptions[safe-1])
        }
    }
}
