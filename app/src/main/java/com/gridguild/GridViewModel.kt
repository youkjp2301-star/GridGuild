package com.gridguild

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gridguild.core.*
import com.gridguild.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.UUID

class GridViewModel(application: Application) : AndroidViewModel(application) {
    private val store = GameStore(application)
    val game = MutableStateFlow(GameState())
    val vehicle = MutableStateFlow(VehicleState())
    val safety = MutableStateFlow(SafetyState())
    val navigation = MutableStateFlow(NavigationState())
    private val availabilityRepository = MockChargingAvailabilityRepository()
    val chargerAvailability: StateFlow<Map<String,StationAvailability>> = availabilityRepository.snapshots
    val demoConnector = MutableStateFlow(ChargerConnector.DC_COMBO)
    fun compatibleConnectors(): Set<ChargerConnector>? = if(game.value.mode==AppMode.MOCK)setOf(demoConnector.value) else null
    val ready = MutableStateFlow(false)
    val message = MutableStateFlow("")
    val neutralPreview = MutableStateFlow(false)
    var neutralOriginalOrientation: Int? = null
    val introSeen = MutableStateFlow(false)
    private val rewardQueue = Channel<XpVisualEvent>(Channel.UNLIMITED)
    val rewards = rewardQueue.receiveAsFlow()
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
                val presentation=if(ready.value)XpVisualEvent.between(old,next) else null
                game.value=next; ready.value=true
                if(next.mode!=AppMode.MOCK)neutralPreview.value=false
                // Publish the committed balance before waking an immediate UI collector.
                presentation?.let{rewardQueue.trySend(it)}
                if(next.mission?.stationId==startingMission.value || old.mode!=next.mode) startingMission.value=null
                if (foreground && activeMode!=next.mode) connect(next.mode)
            }
        }
        viewModelScope.launch { while (isActive) {
            now.value=System.currentTimeMillis()
            if(automotive && foreground) {
                platformAllowed.value=platform?.readAllowed()==true
                if(!platformAllowed.value)neutralPreview.value=false
            }
            if(foreground)availabilityRepository.refreshIfDue()
            delay(500)
        } }
    }
    fun resume() {
        foreground=true
        if(!automotive) platformAllowed.value=true
        if(automotive) platform=PlatformSafetyMonitor(getApplication()) {
            platformAllowed.value=it
            if(foreground && !it)neutralPreview.value=false
        }.also { it.connect() }
        if (ready.value && (activeMode!=game.value.mode || connectionJob?.isActive!=true)) connect(game.value.mode)
    }
    fun pause(changingConfiguration: Boolean=false) {
        // Navigation may take the foreground while processing a request. Keep its callbacks and
        // mission observations alive until this ViewModel is cleared; never keep game UI unlocked.
        foreground=false
        if(!changingConfiguration)neutralPreview.value=false
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
            launch { v.safety.collect {
                safety.value=it
                if(it.source!=DataSource.UNAVAILABLE && (!it.parked || !it.uxAllowed))neutralPreview.value=false
            } }
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
        if(mode!=AppMode.MOCK)neutralPreview.value=false
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
            if(!mutate {
                require(platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis())){"주차 상태를 다시 확인하세요"}
                val summary=ChargingAvailability.summarize(chargerAvailability.value[station.id]?.takeIf{it.stationId==station.id},System.currentTimeMillis(),compatibleConnectors())
                require(summary.status!=AvailabilityStatus.NONE){"이용 가능한 충전기가 없습니다. 다른 후보를 확인하세요"}
                require(summary.compatibleAvailable!=0){"차량에 맞는 충전기에 빈자리가 없습니다"}
                require(ChargingAvailability.compatibility(station,compatibleConnectors())!=ConnectorCompatibility.INCOMPATIBLE){"차량 커넥터와 맞지 않는 충전소입니다"}
                MissionEngine.start(it,station,vehicle.value,System.currentTimeMillis(),"${station.id}:${UUID.randomUUID()}")
            }) startingMission.value=null
        }
    }
    fun registerHomeCharger(name: String) = action {
        val label=name.trim()
        if(label.isEmpty() || label.length>40){message.value="충전소 이름은 1~40자로 입력하세요";return@action}
        mutate{it.copy(homeCharge=it.homeCharge.copy(chargerName=label))}
    }
    fun saveHomePreferences(targetSoc: Int?,departureMinutes: Int?) = action {
        mutate { it.copy(homeCharge=it.homeCharge.copy(targetSoc=targetSoc,departureMinutes=departureMinutes)) }
    }
    fun startHomeCharge() {
        if(!(platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis()))) {
            message.value="안전한 주차 상태에서 참여하세요";return
        }
        if(!startingMission.compareAndSet(null,HomeChargeEngine.stationId))return
        viewModelScope.launch {
            if(!mutate { state ->
                require(platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis())){"주차 상태를 다시 확인하세요"}
                HomeChargeEngine.start(state,vehicle.value,System.currentTimeMillis(),"home-charge:${UUID.randomUUID()}")
            })startingMission.value=null
        }
    }
    fun simulateHomeCharge() = action {
        if(game.value.mode!=AppMode.MOCK || vehicleRepo !is MockVehicleRepository)return@action
        if(!startingMission.compareAndSet(null,HomeChargeEngine.stationId))return@action
        try {
            val started=mutate { state ->
                require(state.mode==AppMode.MOCK && platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis())){"데모 주차 상태를 확인하세요"}
                val current=state.mission
                require(vehicle.value.usable(System.currentTimeMillis()) && vehicle.value.source==DataSource.MOCK && (vehicle.value.soc ?: 100f)<90f){"최신 데모 배터리 상태를 확인하세요"}
                require(current==null || current.kind==MissionKind.HOME){"ROUTE 미션을 먼저 완료하세요"}
                if(state.mission==null)HomeChargeEngine.start(state,vehicle.value,System.currentTimeMillis(),"home-charge:${UUID.randomUUID()}") else state
            }
            if(started) {
                val persisted=store.state.first()
                val mission=persisted.mission?.takeIf{it.kind==MissionKind.HOME && it.source==DataSource.MOCK} ?: return@action
                val target=maxOf(vehicle.value.soc ?: 0f,(mission.windowBaselineSoc ?: mission.baselineSoc)+mission.requiredGain)
                require(target<=90f && persisted.homeCharge.targetSoc?.let{target<=it}!=false){"목표 배터리를 초과하지 않는 상태에서 테스트하세요"}
                delay(25)
                if(game.value.mode==AppMode.MOCK && platformAllowed.value && safety.value.allowsGame(System.currentTimeMillis()))
                    (vehicleRepo as? MockVehicleRepository)?.set(soc=target,charge=ChargeSignal.ON)
            }
        } catch(e: CancellationException){throw e} catch(e: Exception){message.value=e.message ?: "충전 테스트 실패"}
        finally {startingMission.value=null}
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
    fun changeDemoDestination(destination: DemoDestination) = navigationAction { repository ->
        if(game.value.mode==AppMode.MOCK) (repository as? MockNavigationRepository)?.setDemoDestination(destination)
    }
    fun randomDemoDestination() = navigationAction { repository ->
        if(game.value.mode==AppMode.MOCK) (repository as? MockNavigationRepository)?.randomDestination()
    }
    fun enterNeutralPreview() {
        if(!NeutralCity.canEnter(game.value.mode,safety.value,platformAllowed.value,System.currentTimeMillis()) || !ready.value || game.value.districtId==null) {
            message.value="N단 체험은 주차·UX 허용이 확인된 MOCK에서 이용할 수 있어요";return
        }
        neutralPreview.value=true
    }
    fun exitNeutralPreview() { neutralPreview.value=false }
    fun setDemoAvailability(stationId: String,scenario: DemoAvailabilityScenario) = action {
        if(game.value.mode==AppMode.MOCK)availabilityRepository.setScenario(stationId,scenario)
    }
    fun setDemoConnector(connector: ChargerConnector) = action { if(game.value.mode==AppMode.MOCK)demoConnector.value=connector }
    fun shuffleDemoAvailability() = action { if(game.value.mode==AppMode.MOCK)availabilityRepository.shuffle(keepFirstUsable=false) }
    fun refreshChargerAvailability() = action {
        availabilityRepository.refresh()
        message.value="MOCK 충전기 현황을 다시 불러왔어요. 실제 충전소 조회는 아닙니다."
    }
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
            while(rewardQueue.tryReceive().isSuccess) { /* Reset also dismisses pending presentations. */ }
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
