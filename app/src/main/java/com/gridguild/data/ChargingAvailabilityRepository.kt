package com.gridguild.data

import com.gridguild.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** Future public-API adapters provide normalized observations; UI never interprets API codes. */
interface ChargingAvailabilityRepository {
    val snapshots: StateFlow<Map<String,StationAvailability>>
    fun refresh()
}

enum class DemoAvailabilityScenario(val label: String) {
    FREE("여유"), BUSY("혼잡"), NONE("이용 가능 없음"), UNKNOWN("정보 없음"), STALE("오래된 정보"), MISSING("미수신")
}

class MockChargingAvailabilityRepository(private val clock: ()->Long=System::currentTimeMillis,private val random: Random=Random.Default) : ChargingAvailabilityRepository {
    private val choices=mutableMapOf<String,DemoAvailabilityScenario>()
    private var refreshedAt=0L
    override val snapshots=MutableStateFlow<Map<String,StationAvailability>>(emptyMap())
    init { shuffle() }
    fun shuffle(keepFirstUsable: Boolean=true) {
        choices.clear()
        if(!keepFirstUsable) {
            val all=listOf(DemoAvailabilityScenario.FREE,DemoAvailabilityScenario.FREE,DemoAvailabilityScenario.BUSY,DemoAvailabilityScenario.BUSY,DemoAvailabilityScenario.NONE,DemoAvailabilityScenario.UNKNOWN).shuffled(random)
            SampleData.stations.forEachIndexed{i,s->choices[s.id]=all[i]}
            refresh();return
        }
        // Keep the first walkthrough usable; the other five locations get a balanced mix.
        choices["sasang-river"]=DemoAvailabilityScenario.FREE
        val pool=listOf(DemoAvailabilityScenario.FREE,DemoAvailabilityScenario.BUSY,DemoAvailabilityScenario.BUSY,DemoAvailabilityScenario.NONE,DemoAvailabilityScenario.UNKNOWN).shuffled(random)
        SampleData.stations.filter{it.id!="sasang-river"}.forEachIndexed{i,s->choices[s.id]=pool[i]}
        refresh()
    }
    fun refreshIfDue() { if(clock()-refreshedAt>=60_000 || clock()<refreshedAt)refresh() }
    override fun refresh() {
        val time=clock();refreshedAt=time
        snapshots.value=SampleData.stations.mapNotNull{station->
            fixture(station,choices[station.id] ?: DemoAvailabilityScenario.UNKNOWN,time)?.let{station.id to it}
        }.toMap()
    }
    fun setScenario(stationId: String,scenario: DemoAvailabilityScenario) {
        if(SampleData.stations.none{it.id==stationId})return
        choices[stationId]=scenario
        // Changing one station must not refresh old readings at every other station.
        val updated=snapshots.value.toMutableMap()
        fixture(SampleData.stations.first{it.id==stationId},scenario,clock())?.let{updated[stationId]=it}
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
