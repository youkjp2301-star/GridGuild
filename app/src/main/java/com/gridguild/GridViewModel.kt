package com.gridguild

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gridguild.core.*
import com.gridguild.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class GridViewModel(application: Application) : AndroidViewModel(application) {
    private val store = GameStore(application)
    val game = MutableStateFlow(GameState())
    val vehicle = MutableStateFlow(VehicleState())
    val safety = MutableStateFlow(SafetyState())
    val navigation = MutableStateFlow(NavigationState())
    val ready = MutableStateFlow(false)
    val message = MutableStateFlow("")
    val reward = MutableStateFlow(0)
    val now = MutableStateFlow(System.currentTimeMillis())
    private val automotive=application.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    val platformAllowed=MutableStateFlow(!automotive)
    private var platform: PlatformSafetyMonitor?=null
    val gameAllowed = combine(safety, now, platformAllowed) { s,_,p -> p && s.allowsGame(System.currentTimeMillis()) }
        .stateIn(viewModelScope,SharingStarted.Eagerly,false)
    private var vehicleRepo: VehicleRepository? = null
    private var navRepo: NavigationRepository? = null
    private var connectionJob: Job? = null
    private var foreground = false
    private var activeMode: AppMode? = null

    init {
        viewModelScope.launch {
            store.state.catch { message.value="저장 데이터 읽기 실패: ${it.message}" }.collect { next ->
                val old=game.value
                if (ready.value && next.xp > old.xp) {
                    reward.value=next.xp-old.xp
                    launch { delay(3_000); reward.value=0 }
                }
                game.value=next; ready.value=true
                if (foreground && activeMode!=next.mode) connect(next.mode)
            }
        }
        viewModelScope.launch { while (isActive) {
            now.value=System.currentTimeMillis()
            if(automotive && foreground) platformAllowed.value=platform?.readAllowed()==true
            delay(500)
        } }
    }
    fun resume() {
        foreground=true
        if(automotive) platform=PlatformSafetyMonitor(getApplication()) { platformAllowed.value=it }.also { it.connect() }
        if (ready.value) connect(game.value.mode)
    }
    fun pause() {
        foreground=false; connectionJob?.cancel(); connectionJob=null
        navRepo?.close();navRepo=null;vehicleRepo?.close();vehicleRepo=null
        activeMode=null;safety.value=SafetyState();vehicle.value=VehicleState();navigation.value=NavigationState()
        platform?.close();platform=null;platformAllowed.value=!automotive
    }
    private fun connect(mode: AppMode) {
        connectionJob?.cancel(); vehicleRepo?.close();navRepo?.close()
        vehicle.value=VehicleState();safety.value=SafetyState();navigation.value=NavigationState()
        activeMode=mode
        val v=if(mode==AppMode.MOCK) MockVehicleRepository() else PleosVehicleRepository(getApplication())
        val n=if(mode==AppMode.MOCK) MockNavigationRepository() else PleosNavigationRepository(getApplication(),viewModelScope)
        vehicleRepo=v;navRepo=n
        connectionJob=viewModelScope.launch {
            launch { v.run() }
            launch { v.state.collect { sample ->
                vehicle.value=sample
                if (sample.usable(System.currentTimeMillis())) mutate { MissionEngine.observe(it,sample,System.currentTimeMillis()) }
            } }
            launch { v.safety.collect { safety.value=it } }
            launch { n.state.collect { navigation.value=it } }
            n.connect()
        }
    }
    private suspend fun mutate(action: (GameState) -> GameState) {
        try { store.update(action) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { message.value=e.message ?: "데이터 저장 실패" }
    }
    private fun action(allowSetup: Boolean=false, block: suspend () -> Unit) {
        if (!allowSetup && !(platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis()))) { message.value="안전한 주차 및 플랫폼 허용 상태에서 이용하세요";return }
        viewModelScope.launch { block() }
    }
    fun setup(district: String,mode: AppMode) = action(allowSetup=true) {
        if(!platformAllowed.value) { message.value="플랫폼에서 주차 UX가 허용되어야 설정할 수 있습니다";return@action }
        if (Busan.find(district)!=null) mutate { it.copy(districtId=district,mode=mode,mission=null) }
    }
    fun setMode(mode: AppMode) = action(allowSetup=true) {
        // Can select Mock when the real SDK is unavailable; real mission is cancelled, never mixed.
        mutate { it.copy(mode=mode,mission=null) }
    }
    fun startMission(station: Station) = action {
        mutate { MissionEngine.start(it,station,vehicle.value,System.currentTimeMillis(),"${station.id}:${UUID.randomUUID()}") }
    }
    fun cancelMission() = action { mutate { it.copy(mission=null) } }
    fun addWaypoint(station: Station) = action { navRepo?.addWaypoint(station) }
    fun setDestination(station: Station) = action { navRepo?.setDestination(station) }
    fun refreshNavigation() = action { navRepo?.refresh() }
    fun reroute() = action { navRepo?.reroute() }
    fun mockSoc(value: Float) { (vehicleRepo as? MockVehicleRepository)?.set(soc=value) }
    fun mockCharge(on: Boolean) { (vehicleRepo as? MockVehicleRepository)?.set(charge=if(on) ChargeSignal.ON else ChargeSignal.OFF) }
    fun mockPark(parked: Boolean) { (vehicleRepo as? MockVehicleRepository)?.park(parked) }
    fun mockUx(allowed: Boolean) { (vehicleRepo as? MockVehicleRepository)?.let { it.park(it.safety.value.parked,allowed) } }
    fun reset() = action {
        if (game.value.mode==AppMode.MOCK) {
            mutate { GameState() };(vehicleRepo as? MockVehicleRepository)?.set(42f,ChargeSignal.OFF)
        }
    }
    override fun onCleared() { pause();super.onCleared() }
}
