package com.gridguild.core

/** Pure transitions. Persist the returned state as one atomic record before publishing it. */
object MissionEngine {
    fun start(state: GameState, station: Station, vehicle: VehicleState, now: Long, id: String): GameState {
        require(state.districtId != null) { "소속 지역을 먼저 선택하세요" }
        require(state.mission == null) { "진행 중인 미션이 있습니다" }
        require(vehicle.usable(now)) { "최신 차량 데이터가 필요합니다" }
        require(vehicle.source == if (state.mode == AppMode.MOCK) DataSource.MOCK else DataSource.PLEOS)
        require(vehicle.soc!! <= 98f) { "SOC가 너무 높아 +2% 미션을 시작할 수 없습니다" }
        require(station.inWindow(now)) { "권장 시간대에 시작하세요" }
        require(state.history.none { it.id == id }) { "이미 사용한 미션 ID" }
        require(state.history.none { it.missionId.startsWith("${station.id}:") && GameState.day(it.at) == GameState.day(now) }) { "오늘 이미 완료한 미션입니다" }
        return state.copy(mission = Mission(id, station.id, state.districtId, station.xp,
            vehicle.soc, vehicle.source, now))
    }

    fun observe(state: GameState, vehicle: VehicleState, now: Long): GameState {
        val m = state.mission ?: return state
        if (!vehicle.usable(now) || vehicle.source != m.source || vehicle.sampledAt <= m.startedAt) return state
        val station = SampleData.stations.firstOrNull { it.id == m.stationId } ?: return state
        if (!station.inWindow(now)) return state
        val gain = (vehicle.soc!! - m.baselineSoc).coerceAtLeast(0f)
        // A switch ON is permission to charge, not proof of actual energy flow.
        // Require a fresh ON sample plus SOC gain; all MVP rewards remain demo achievements.
        val observed = m.chargingObserved || vehicle.chargeSwitch == ChargeSignal.ON
        val updated = m.copy(chargingObserved = observed, progress = (gain / m.requiredGain).coerceIn(0f,1f))
        if (!observed || vehicle.chargeSwitch != ChargeSignal.ON || gain < m.requiredGain)
            return state.copy(mission = updated)
        if (state.history.any { it.missionId == m.id }) return state.copy(mission = null)
        val event = PointEvent(m.id, m.id, m.districtId, m.reward, now)
        val history = state.history + event
        val daily = history.filter { it.kind == "MISSION" && GameState.day(it.at) == GameState.day(now) }
        val bonusId = "daily:${GameState.day(now)}"
        val eligible = SampleData.stations.map { it.id }.toSet()
        // Fixture missions have station IDs embedded in unique mission IDs by the app.
        val done = daily.map { it.missionId.substringBefore(':') }.toSet()
        val bonus = if (done.containsAll(eligible) && eligible.isNotEmpty() && history.none { it.id == bonusId })
            listOf(PointEvent(bonusId, bonusId, m.districtId, 50, now, "DAILY")) else emptyList()
        return state.copy(mission = null, history = history + bonus)
    }
}
