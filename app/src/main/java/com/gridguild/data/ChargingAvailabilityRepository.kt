package com.gridguild.data

import com.gridguild.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Future public-API adapters provide normalized observations; UI never interprets API codes. */
interface ChargingAvailabilityRepository {
    val snapshots: StateFlow<Map<String,StationAvailability>>
    fun refresh()
}

enum class DemoAvailabilityScenario(val label: String) {
    FREE("여유"), BUSY("혼잡"), NONE("이용 가능 없음"), UNKNOWN("정보 없음"), STALE("오래된 정보"), MISSING("미수신")
}

class MockChargingAvailabilityRepository : ChargingAvailabilityRepository {
    private val choices=mutableMapOf(
        "sasang-river" to DemoAvailabilityScenario.FREE,"sasang-park" to DemoAvailabilityScenario.BUSY,
        "centum" to DemoAvailabilityScenario.NONE,"centum-campus" to DemoAvailabilityScenario.FREE,
        "gwangalli" to DemoAvailabilityScenario.BUSY,"gwangalli-marina" to DemoAvailabilityScenario.STALE,
    )
    override val snapshots=MutableStateFlow<Map<String,StationAvailability>>(emptyMap())
    init { refresh() }
    override fun refresh() {
        val time=System.currentTimeMillis()
        snapshots.value=SampleData.stations.mapNotNull{station->
            fixture(station,choices[station.id] ?: DemoAvailabilityScenario.UNKNOWN,time)?.let{station.id to it}
        }.toMap()
    }
    fun setScenario(stationId: String,scenario: DemoAvailabilityScenario) {
        if(SampleData.stations.none{it.id==stationId})return
        choices[stationId]=scenario
        // Changing one station must not refresh old readings at every other station.
        val updated=snapshots.value.toMutableMap()
        fixture(SampleData.stations.first{it.id==stationId},scenario,System.currentTimeMillis())?.let{updated[stationId]=it}
            ?: updated.remove(stationId)
        snapshots.value=updated
    }
    private fun fixture(station: Station,scenario: DemoAvailabilityScenario,now: Long): StationAvailability? {
        if(scenario==DemoAvailabilityScenario.MISSING)return null
        val sampled=if(scenario==DemoAvailabilityScenario.STALE)now-ChargingAvailability.maxAgeMs-60_000 else now
        val states=when(scenario) {
            DemoAvailabilityScenario.FREE,DemoAvailabilityScenario.STALE -> listOf(ChargerUseState.AVAILABLE,ChargerUseState.AVAILABLE,ChargerUseState.AVAILABLE,ChargerUseState.IN_USE,ChargerUseState.FAULT)
            DemoAvailabilityScenario.BUSY -> listOf(ChargerUseState.AVAILABLE,ChargerUseState.IN_USE,ChargerUseState.IN_USE,ChargerUseState.MAINTENANCE,ChargerUseState.FAULT)
            DemoAvailabilityScenario.NONE -> listOf(ChargerUseState.IN_USE,ChargerUseState.IN_USE,ChargerUseState.FAULT,ChargerUseState.MAINTENANCE,ChargerUseState.OUT_OF_SERVICE)
            else -> List(5){ChargerUseState.UNKNOWN}
        }
        return StationAvailability(station.id,states.mapIndexed{i,state->ChargerObservation("${station.id}:$i",station.connectors.firstOrNull(),state,sampled)},5,sampled,AvailabilitySource.MOCK)
    }
}
