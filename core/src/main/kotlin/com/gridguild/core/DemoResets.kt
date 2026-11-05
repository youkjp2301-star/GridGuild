package com.gridguild.core

enum class DemoResetTarget { CITY_LEVEL, CHARGING_MISSIONS, PERSONAL_POINTS, ALL }

/** Independent test controls. Never alter a state that is currently using the real SDK. */
object DemoResets {
    fun reset(state: GameState,target: DemoResetTarget): GameState {
        if(state.mode!=AppMode.MOCK) return state
        return when(target) {
            DemoResetTarget.CITY_LEVEL -> if(Busan.find(state.districtId)==null) state else
                state.copy(history=state.history.map { if(it.districtId==state.districtId) it.copy(cityCredit=false) else it })
            DemoResetTarget.CHARGING_MISSIONS -> state.copy(mission=null,history=state.history.map { it.copy(countsForMission=false) })
            DemoResetTarget.PERSONAL_POINTS -> state.copy(history=state.history.map { it.copy(personalCredit=false) })
            DemoResetTarget.ALL -> GameState()
        }
    }
}
