package com.gridguild.data

import android.content.Context
import android.content.pm.PackageManager
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import com.gridguild.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Direct typed SDK calls, no reflection or substituted SDK classes. */
class PleosVehicleRepository(private val context: Context) : VehicleRepository {
    override val state = MutableStateFlow(VehicleState())
    override val safety = MutableStateFlow(SafetyState())
    private var vehicle: Vehicle? = null
    private var monitor: PlatformSafetyMonitor? = null
    private val gearListener=object: GearSelectionListener {
        override fun onSelected(value: VehicleGear?) {
            safety.value=safety.value.copy(parked=value==VehicleGear.GEAR_PARK,
                sampledAt=System.currentTimeMillis(),detail="Pleos ${value?.name ?: "기어 미확인"}")
        }
        override fun onFailed(e: Exception) { safety.value=SafetyState(detail="기어 구독 오류: ${e.message}") }
    }

    private suspend fun <T> request(call: ((T) -> Unit, (Exception) -> Unit) -> Unit): T =
        withTimeout(5_000) {
            suspendCancellableCoroutine { continuation ->
                try {
                    call({ value -> if (continuation.isActive) continuation.resume(value) },
                        { error -> if (continuation.isActive) continuation.resumeWithException(error) })
                } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
            }
        }

    override suspend fun run() {
        if(!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            state.value=VehicleState(detail="Pleos SDK는 AAOS 차량 서비스가 필요합니다. MOCK 모드를 선택할 수 있습니다.")
            return
        }
        try {
            val sdk = Vehicle(context).also { vehicle = it; it.initialize() }
            monitor = PlatformSafetyMonitor(context) { allowed ->
                    if(!allowed) safety.value=safety.value.copy(uxAllowed=false)
                }.also { it.connect() }
            runCatching { sdk.getGear().registerGearSelection(gearListener) }
            while (currentCoroutineContext().isActive) {
                try {
                    val battery = sdk.getEvBattery()
                    val socCap = request<CapabilityConfig> { ok, fail -> battery.checkEvBatteryStateOfChargeCapabilities(ok,fail) }
                    val chargeCap = request<CapabilityConfig> { ok, fail -> battery.checkEvChargeSwitchCapabilities(ok,fail) }
                    check(socCap.accessType in listOf(AccessType.READ,AccessType.READ_WRITE)) { "SOC 읽기 미지원" }
                    check(chargeCap.accessType in listOf(AccessType.READ,AccessType.READ_WRITE)) { "충전 스위치 읽기 미지원" }
                    val soc = request<Int?> { ok, fail -> battery.getEvBatteryStateOfCharge(ok,fail) }
                    val charging = request<Boolean?> { ok, fail -> battery.getEvChargeSwitchState(ok,fail) }
                    state.value = VehicleState(soc?.toFloat(), when(charging) { true -> ChargeSignal.ON; false -> ChargeSignal.OFF; null -> ChargeSignal.UNKNOWN },
                        DataSource.PLEOS,System.currentTimeMillis(),"Pleos SDK · SOC / 충전 스위치 · 2초 갱신")
                } catch (e: CancellationException) { if (!currentCoroutineContext().isActive) throw e; fail(e) }
                catch (e: Exception) { fail(e) }
                try {
                    val gear = request<VehicleGear?> { ok, fail -> sdk.getGear().getCurrentGearSelectionState(ok,fail) }
                    val ux = monitor?.readAllowed() == true
                    safety.value = SafetyState(gear == VehicleGear.GEAR_PARK, ux, System.currentTimeMillis(),DataSource.PLEOS,
                        "Pleos ${gear?.name ?: "기어 미확인"} · ${if (ux) "UX 허용" else "UX 제한/미확인"}")
                } catch (e: CancellationException) { if (!currentCoroutineContext().isActive) throw e; safety.value = SafetyState(detail = "주차 확인 실패") }
                catch (e: Exception) { safety.value = SafetyState(detail = "주차 확인 실패: ${e.message}") }
                delay(2_000)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { fail(e) }
        finally { close() }
    }
    private fun fail(e: Exception) {
        state.value = VehicleState(detail = "Pleos 연결/권한/Capability 오류: ${e.message ?: e.javaClass.simpleName}")
    }
    override fun close() {
        monitor?.close(); monitor = null
        runCatching { vehicle?.getGear()?.unregisterGearSelection(gearListener) }
        runCatching { vehicle?.release() }; vehicle = null
        state.value = VehicleState(detail = "Pleos 연결 종료")
        safety.value = SafetyState()
    }
}
