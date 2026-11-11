package com.gridguild.core

/** A fictional street-level reconstruction; it never represents a live camera or historical fact. */
data class DriverScene(val city: NeutralCity,val level: Int) {
    init { require(level in 1..12) { "Driver scene level must be in 1..12" } }

    val assetKey get()="driver_${city.destinationId}_lv${level.toString().padStart(2,'0')}_v2"
    val stageName get()=CityLevels.names[level-1]
    val roadLabel get()=roads[level-1]
    val landmarkLabel get()=when(city) {
        NeutralCity.GWANGALLI -> gwanganLandmarks[level-1]
        NeutralCity.CENTUM -> centumLandmarks[level-1]
    }

    companion object {
        /** Resolve before every render, including after an XP reset or a destination change. */
        fun resolve(city: NeutralCity,cityXp: Int,requestedLevel: Int?=null)=
            DriverScene(city,CityUnlocks.selection(requestedLevel,cityXp))

        private val roads=listOf(
            "개척 흙길 · 초가 마을",
            "흙길 · 초가집 · 성문",
            "돌포장 교역길 · 전통 시장",
            "자갈길 · 벽돌 산업 지구",
            "초기 포장도로 · 시민 거리",
            "현대 도로 · 트램 거리",
            "태양광 차양 · 연구단지 도로",
            "스마트 도로 · 교통 환승 거점",
            "생태 수변 도로 · 빗물 정원",
            "연결 도로 · 입체 보행로",
            "첨단 대로 · 수직 정원 타워",
            "미래 도로 · 초진보 도시",
        )
        private val gwanganLandmarks=listOf(
            "광안대교의 작은 목조 전신",
            "광안대교의 목조·석조 전신",
            "광안대교의 전통 교역 교량 전신",
            "광안대교의 산업시대 철교 전신",
            "광안대교의 근대 현수교 전신",
            "광안대교 · 현대 해안도로",
            "광안대교 · 태양광 교량",
            "광안대교 · 스마트 교통 연결",
            "광안대교 · 생태 수변 연결",
            "광안대교 · 입체 교통망",
            "광안대교 · 첨단 복합 교량",
            "광안대교 · 초진보 미래 교량",
        )
        private val centumLandmarks=listOf(
            "영화의전당의 마을 문화 마당 전신",
            "영화의전당의 목조 공연장 전신",
            "영화의전당의 전통 극장 전신",
            "영화의전당의 벽돌 공연장 전신",
            "영화의전당의 근대 시민 극장 전신",
            "영화의전당 · 현대 센텀 거리",
            "영화의전당 · 태양광 문화 지붕",
            "영화의전당 · 스마트 문화 거점",
            "영화의전당 · 생태 문화 공간",
            "영화의전당 · 공중 문화 연결",
            "영화의전당 · 하늘 정원 지붕",
            "영화의전당 · 초진보 미래 문화도시",
        )
    }
}
