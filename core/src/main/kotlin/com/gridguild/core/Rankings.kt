package com.gridguild.core

data class RankingRow(val name: String,val xp: Int,val mine: Boolean)
object Rankings {
    fun personal(g: GameState,now: Long,monthly: Boolean=true): List<RankingRow> = listOf(
        RankingRow("나",if(monthly)g.seasonXp(now) else g.xp,true),
        RankingRow("바다의 기사 (MOCK)",if(monthly)360 else 810,false),
        RankingRow("푸른 충전자 (MOCK)",if(monthly)220 else 520,false),
        RankingRow("에코 드라이버 (MOCK)",if(monthly)80 else 240,false),
    ).sortedWith(compareByDescending<RankingRow>{it.xp}.thenBy{it.name})
    fun cities(g: GameState,now: Long,monthly: Boolean=true): List<RankingRow> =
        Busan.districts.mapIndexed { index,d -> RankingRow(d.name,
            (if(monthly)(16-index)*24 else (16-index)*86)+(if(monthly)g.seasonXp(now,d.id) else g.cityXp(d.id)),d.id==g.districtId)
        }.sortedWith(compareByDescending<RankingRow>{it.xp}.thenBy{it.name})
}
