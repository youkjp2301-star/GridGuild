package com.gridguild.data

import com.gridguild.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface VehicleRepository : AutoCloseable {
    val state: StateFlow<VehicleState>
    val safety: StateFlow<SafetyState>
    suspend fun run()
}
enum class NavigationOperation { REFRESH, REROUTE, WAYPOINT, DESTINATION }

data class NavigationState(
    val destination: String = "목적지 조회 대기", val lat: Double? = null, val lon: Double? = null,
    val source: DataSource = DataSource.UNAVAILABLE,
    val status: String = "내비게이션 연결 대기", val pending: Boolean = false,
    val confirmed: Boolean = false,
    val operation: NavigationOperation? = null,
    val requestId: Long = 0,
    val destinationUpdatedAt: Long? = null,
    val resultAt: Long? = null,
    val error: Boolean = false,
) {
    val hasDestination get(): Boolean {
        val latitude=lat ?: return false
        val longitude=lon ?: return false
        return source!=DataSource.UNAVAILABLE && latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 && (latitude!=0.0 || longitude!=0.0)
    }
}
interface NavigationRepository : AutoCloseable {
    val state: StateFlow<NavigationState>
    fun connect()
    fun refresh()
    fun addWaypoint(station: Station)
    fun setDestination(station: Station)
    fun reroute()
}

class MockVehicleRepository : VehicleRepository {
    override val state = MutableStateFlow(VehicleState(42f, ChargeSignal.OFF, DataSource.MOCK, System.currentTimeMillis(), "MOCK · 수동 시뮬레이션"))
    override val safety = MutableStateFlow(SafetyState(true, true, System.currentTimeMillis(), DataSource.MOCK, "MOCK · P단 / UX 허용"))
    fun set(soc: Float? = null, charge: ChargeSignal? = null) {
        state.update { current -> current.copy(soc = soc?.coerceIn(0f,100f) ?: current.soc,
            chargeSwitch = charge ?: current.chargeSwitch, sampledAt = System.currentTimeMillis()) }
    }
    fun park(parked: Boolean, ux: Boolean = parked) {
        safety.value = SafetyState(parked,ux,System.currentTimeMillis(),DataSource.MOCK,
            if (parked && ux) "MOCK · P단 / UX 허용" else "MOCK · 주행 또는 UX 제한")
    }
    override suspend fun run() {
        while (true) {
            // Heartbeats must never restore values captured before a user pressed a control.
            state.update { it.copy(sampledAt = System.currentTimeMillis()) }
            safety.update { it.copy(sampledAt = System.currentTimeMillis()) }
            kotlinx.coroutines.delay(1_000)
        }
    }
    override fun close() = Unit
}
class MockNavigationRepository(private val scope: CoroutineScope,private val responseDelayMillis: Long=650) : NavigationRepository {
    override val state = MutableStateFlow(NavigationState("부산시민공원 (MOCK)",35.1684,129.0574,DataSource.MOCK,"데모 목적지 · 실제 내비게이션과 연결되지 않습니다",destinationUpdatedAt=System.currentTimeMillis()))
    private val requests=NavigationRequests(state,scope)
    override fun connect() = Unit
    private fun simulate(operation: NavigationOperation,start: String,result: String,update: (NavigationState)->NavigationState={it}) {
        val id=requests.begin(operation,start) ?: return
        scope.launch {
            delay(responseDelayMillis)
            requests.complete(id,result) { update(it).copy(source=DataSource.MOCK) }
        }
    }
    override fun refresh() = simulate(NavigationOperation.REFRESH,"데모 목적지 조회 중…","데모 목적지 갱신 완료 · 같은 목적지이면 충전소 목록은 유지됩니다") {
        it.copy(destinationUpdatedAt=System.currentTimeMillis())
    }
    override fun addWaypoint(station: Station) {
        simulate(NavigationOperation.WAYPOINT,"데모 경유지 추가 중…","데모 경유지 반영 완료: ${station.name} · 실제 내비 등록 아님")
    }
    override fun setDestination(station: Station) {
        val id=requests.begin(NavigationOperation.DESTINATION,"데모 목적지 설정 중…") ?: return
        scope.launch {
            delay(responseDelayMillis)
            requests.complete(id,"데모 목적지 설정 완료 · 목적지 주변 샘플 충전소를 정렬했습니다") {
                it.copy(destination="${station.name} (MOCK)",lat=station.lat,lon=station.lon,destinationUpdatedAt=System.currentTimeMillis())
            }
        }
    }
    override fun reroute() = simulate(NavigationOperation.REROUTE,"데모 경로 재탐색 중…","데모 재탐색 완료 · 실제 경로 계산·내비 안내는 제공하지 않습니다")
    override fun close() = requests.close()
}
