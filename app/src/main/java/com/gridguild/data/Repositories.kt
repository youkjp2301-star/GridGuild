package com.gridguild.data

import com.gridguild.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository : AutoCloseable {
    val state: StateFlow<VehicleState>
    val safety: StateFlow<SafetyState>
    suspend fun run()
}
data class NavigationState(
    val destination: String = "목적지 조회 대기", val lat: Double? = null, val lon: Double? = null,
    val source: DataSource = DataSource.UNAVAILABLE,
    val status: String = "내비게이션 연결 대기", val pending: Boolean = false,
    val confirmed: Boolean = false,
)
interface NavigationRepository : AutoCloseable {
    val state: StateFlow<NavigationState>
    fun connect()
    fun refresh()
    fun addWaypoint(station: Station)
    fun setDestination(station: Station)
    fun reroute()
}

class MockVehicleRepository : VehicleRepository {
    override val state = MutableStateFlow(VehicleState(42f, ChargeSignal.OFF, DataSource.MOCK, detail = "MOCK · 수동 시뮬레이션"))
    override val safety = MutableStateFlow(SafetyState(true, true, source = DataSource.MOCK, detail = "MOCK · P단 / UX 허용"))
    fun set(soc: Float? = state.value.soc, charge: ChargeSignal = state.value.chargeSwitch) {
        state.value = state.value.copy(soc = soc?.coerceIn(0f,100f), chargeSwitch = charge, sampledAt = System.currentTimeMillis())
    }
    fun park(parked: Boolean, ux: Boolean = parked) {
        safety.value = SafetyState(parked,ux,System.currentTimeMillis(),DataSource.MOCK,
            if (parked && ux) "MOCK · P단 / UX 허용" else "MOCK · 주행 또는 UX 제한")
    }
    override suspend fun run() {
        while (true) {
            set()
            safety.value = safety.value.copy(sampledAt = System.currentTimeMillis())
            kotlinx.coroutines.delay(1_000)
        }
    }
    override fun close() = Unit
}
class MockNavigationRepository : NavigationRepository {
    override val state = MutableStateFlow(NavigationState("부산시민공원 (MOCK)",35.1684,129.0574,DataSource.MOCK,"MOCK · 목적지 주변 샘플 충전소"))
    override fun connect() = Unit
    override fun refresh() = Unit
    override fun addWaypoint(station: Station) {
        state.value = state.value.copy(status = "MOCK · ${station.name} 경유지 등록 시뮬레이션", confirmed = true)
    }
    override fun setDestination(station: Station) {
        state.value = NavigationState("${station.name} (MOCK)",station.lat,station.lon,DataSource.MOCK,"MOCK · 목적지 설정 시뮬레이션")
    }
    override fun reroute() { state.value = state.value.copy(status = "MOCK · 재탐색 시뮬레이션") }
    override fun close() = Unit
}
