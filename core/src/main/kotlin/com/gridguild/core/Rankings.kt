package com.gridguild.core

/** Ranking score can cover one season; city growth always uses cumulative city credit. */
data class RankingRow(val name: String,val xp: Int,val mine: Boolean,
    val districtId: String?=null,val cityXp: Int?=null) {
    val cityLevel get()=cityXp?.let(CityLevels::level)
}
object Rankings {
    private val nicknames=listOf("바다의 기사","푸른 충전자","에코 드라이버","달빛 항해사","삼락 산책가","새벽 충전단","광안의 바람","초록 신호","도시 정원사","느긋한 거북이","노을 러너","센텀 메이커","반딧불","숲속 우체부","은빛 파도","주말 탐험가","따뜻한 전류","별빛 차고","작은 물결","낙동강 친구","하늘 정거장","오후의 산책","민트 드라이브","햇살 수집가","구름 여행자","달리는 고래","초록 지붕","도심 나무","저녁 별","마을 설계자","내일의 충전","바람개비")
    fun personal(g: GameState,now: Long,monthly: Boolean=true): List<RankingRow> =
        (listOf(RankingRow("나",if(monthly)g.seasonXp(now) else g.xp,true))+nicknames.mapIndexed{i,name->
            val score=when(i){0->360;1->220;2->80;else->240+(i*227)%6000}
            RankingRow(name,if(monthly)score else score+300+i*131,false)
        }).sortedWith(compareByDescending<RankingRow>{it.xp}.thenBy{it.name})
    fun cities(g: GameState,now: Long,monthly: Boolean=true): List<RankingRow> =
        Busan.districts.mapIndexed { index,d ->
            val mine=d.id==g.districtId
            // Other regions remain sample competitors. Never add a sample balance to my real city.
            val base=if(mine)0 else demoCityXp(index)
            RankingRow(d.name,base+(if(monthly)g.seasonXp(now,d.id) else g.cityXp(d.id)),mine,
                districtId=d.id,cityXp=base+g.cityXp(d.id))
        }.sortedWith(compareByDescending<RankingRow>{it.xp}.thenBy{it.name})
    fun demoCityXp(index: Int): Int {
        val level=3+index%9
        val low=CityLevels.thresholds[level-1];val width=CityLevels.thresholds[level]-low
        return low+(width*(.15+.07*(index%7))).toInt()
    }
}
