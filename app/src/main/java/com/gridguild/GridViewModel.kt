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
    val vehicleReading = combine(vehicle,now) { sample,_ -> VehicleReading.capture(sample,System.currentTimeMillis()) }
        .stateIn(viewModelScope,SharingStarted.Eagerly,VehicleReading(VehicleState(),false))
    val startingMission = MutableStateFlow<String?>(null)
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
                if(next.mission?.stationId==startingMission.value || old.mode!=next.mode) startingMission.value=null
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
        if(!automotive) platformAllowed.value=true
        if(automotive) platform=PlatformSafetyMonitor(getApplication()) { platformAllowed.value=it }.also { it.connect() }
        if (ready.value && (activeMode!=game.value.mode || connectionJob?.isActive!=true)) connect(game.value.mode)
    }
    fun pause() {
        // Navigation may take the foreground while processing a request. Keep its callbacks and
        // mission observations alive until this ViewModel is cleared; never keep game UI unlocked.
        foreground=false
        safety.value=SafetyState()
        platform?.close();platform=null;platformAllowed.value=false
    }
    private fun connect(mode: AppMode) {
        connectionJob?.cancel(); vehicleRepo?.close();navRepo?.close()
        vehicle.value=VehicleState();safety.value=SafetyState();navigation.value=NavigationState()
        activeMode=mode
        val v=if(mode==AppMode.MOCK) MockVehicleRepository() else PleosVehicleRepository(getApplication())
        val n=if(mode==AppMode.MOCK) MockNavigationRepository(viewModelScope) else PleosNavigationRepository(getApplication(),viewModelScope)
        vehicleRepo=v;navRepo=n
        connectionJob=viewModelScope.launch {
            launch(Dispatchers.IO) { v.run() }
            launch { v.state.collect { sample ->
                vehicle.value=sample
                if (sample.usable(System.currentTimeMillis())) mutate { MissionEngine.observe(it,sample,System.currentTimeMillis()) }
            } }
            launch { v.safety.collect { safety.value=it } }
            launch { n.state.collect { navigation.value=it } }
            n.connect()
        }
    }
    private suspend fun mutate(action: (GameState) -> GameState): Boolean {
        return try { store.update(action);true }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { message.value=e.message ?: "데이터 저장 실패";false }
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
        if(mode==game.value.mode) {
            message.value=if(mode==AppMode.MOCK) "현재 데모 모드입니다. 충전 ON과 SOC +2%로 미션을 테스트하세요." else "현재 Pleos SDK 모드입니다. 아래에서 연결 상태를 확인하세요."
            return@action
        }
        // Can select Mock when the real SDK is unavailable; real mission is cancelled, never mixed.
        mutate { it.copy(mode=mode,mission=null) }
    }
    fun startMission(station: Station) {
        if(!(platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis()))) {
            message.value="안전한 주차 및 플랫폼 허용 상태에서 이용하세요";return
        }
        if(!startingMission.compareAndSet(null,station.id)) return
        viewModelScope.launch {
            if(!mutate { MissionEngine.start(it,station,vehicle.value,System.currentTimeMillis(),"${station.id}:${UUID.randomUUID()}") }) startingMission.value=null
        }
    }
    fun cancelMission() = action { mutate { it.copy(mission=null) } }
    private fun navigationAction(request: (NavigationRepository)->Unit) = action {
        val repository=navRepo
        if(repository==null) message.value="내비게이션 연결 준비 중입니다. 잠시 후 다시 시도하세요."
        else if(!repository.state.value.pending) request(repository)
    }
    fun addWaypoint(station: Station) = navigationAction { it.addWaypoint(station) }
    fun setDestination(station: Station) = navigationAction { it.setDestination(station) }
    fun refreshNavigation() = navigationAction { it.refresh() }
    fun reroute() = navigationAction { it.reroute() }
    fun mockSoc(value: Float) { (vehicleRepo as? MockVehicleRepository)?.set(soc=value) }
    fun mockCharge(on: Boolean) { (vehicleRepo as? MockVehicleRepository)?.set(charge=if(on) ChargeSignal.ON else ChargeSignal.OFF) }
    fun mockPark(parked: Boolean) { (vehicleRepo as? MockVehicleRepository)?.park(parked) }
    fun mockUx(allowed: Boolean) { (vehicleRepo as? MockVehicleRepository)?.let { it.park(it.safety.value.parked,allowed) } }
    fun resetDemo(target: DemoResetTarget,expectedDistrictId: String?) = action {
        var authorized=false
        val saved=mutate { state ->
            // Recheck against the persisted mode inside the same atomic update as the reset.
            if(state.mode==AppMode.MOCK && platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis()) &&
                (target!=DemoResetTarget.CITY_LEVEL || state.districtId==expectedDistrictId)) {
                authorized=true
                DemoResets.reset(state,target)
            } else state
        }
        if(saved && authorized) {
            reward.value=0
            if(target==DemoResetTarget.CHARGING_MISSIONS || target==DemoResetTarget.ALL) startingMission.value=null
            if(target==DemoResetTarget.ALL) (vehicleRepo as? MockVehicleRepository)?.set(42f,ChargeSignal.OFF)
            message.value=when(target) {
                DemoResetTarget.CITY_LEVEL -> "선택 지역의 도시를 Lv.1 · 0 XP로 초기화했습니다."
                DemoResetTarget.CHARGING_MISSIONS -> "충전 미션을 초기화했습니다. 같은 충전소에 다시 도전할 수 있어요."
                DemoResetTarget.PERSONAL_POINTS -> "내 포인트를 0 XP로 초기화했습니다."
                DemoResetTarget.ALL -> "전체 데모 데이터를 초기화했습니다."
            }
        } else if(saved) {
            message.value="모드·선택 지역·주차 상태가 변경되었습니다. 초기화 항목을 다시 확인하세요."
        }
    }
    override fun onCleared() {
        pause();connectionJob?.cancel();connectionJob=null
        navRepo?.close();navRepo=null;vehicleRepo?.close();vehicleRepo=null
        super.onCleared()
    }
}
