package com.gridguild.core

/** Explicit Mock-only city adjustment. It never awards personal XP or completes a mission. */
object DemoCityLevels {
    const val eventKind="TEST_LEVEL"

    fun step(state: GameState,delta: Int,at: Long,eventId: String): GameState {
        if(state.mode!=AppMode.MOCK || Busan.find(state.districtId)==null || delta !in setOf(-1,1))return state
        if(eventId.isBlank() || state.history.any{it.id==eventId || it.missionId==eventId})return state
        val currentXp=state.cityXp(state.districtId)
        if(currentXp<0)return state
        val level=CityLevels.level(currentXp)
        val next=(level+delta).coerceIn(1,12)
        if(next==level)return state
        val adjustment=CityLevels.thresholds[next-1]-currentXp
        return state.copy(history=state.history+PointEvent(eventId,eventId,state.districtId!!,adjustment,at,
            kind=eventKind,demo=true,personalCredit=false,cityCredit=true,countsForMission=false))
    }
}
