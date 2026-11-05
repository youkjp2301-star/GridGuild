package com.gridguild.core

import java.time.Instant
import java.time.ZoneId

data class HomeChargePreferences(val chargerName: String?=null,val targetSoc: Int?=null,val departureMinutes: Int?=null) {
    init {
        require(chargerName==null || (chargerName.isNotBlank() && chargerName.length<=40))
        require(targetSoc==null || targetSoc in 1..100)
        require(departureMinutes==null || departureMinutes in 0..1439)
    }
}
enum class HomeAdvice { NIGHT_EXAMPLE, LOW_BATTERY, UNKNOWN_DEPARTURE, DEPARTURE_SOON, BELOW_TARGET, UNKNOWN_BATTERY }
data class HomeChargePlan(val start: Long,val end: Long,val advice: HomeAdvice) {
    val night get()=advice==HomeAdvice.NIGHT_EXAMPLE
}

object HomeChargePolicy {
    private val zone=ZoneId.of("Asia/Seoul")
    fun plan(preferences: HomeChargePreferences,soc: Float?,now: Long): HomeChargePlan {
        fun immediate(reason: HomeAdvice)=HomeChargePlan(now,now+24*60*60*1000L,reason)
        if(soc==null || !soc.isFinite())return immediate(HomeAdvice.UNKNOWN_BATTERY)
        if(soc<30f)return immediate(HomeAdvice.LOW_BATTERY)
        val departureMinutes=preferences.departureMinutes ?: return immediate(HomeAdvice.UNKNOWN_DEPARTURE)
        if(preferences.targetSoc?.let{soc<it}==true)return immediate(HomeAdvice.BELOW_TARGET)
        val local=Instant.ofEpochMilli(now).atZone(zone)
        val date=if(local.hour<3)local.toLocalDate().minusDays(1) else local.toLocalDate()
        val start=date.atTime(23,0).atZone(zone)
        val end=start.plusHours(4)
        var departure=local.toLocalDate().atStartOfDay(zone).plusMinutes(departureMinutes.toLong())
        if(!departure.isAfter(local))departure=departure.plusDays(1)
        if(departure.isBefore(end.plusHours(3)))return immediate(HomeAdvice.DEPARTURE_SOON)
        return HomeChargePlan(start.toInstant().toEpochMilli(),end.toInstant().toEpochMilli(),HomeAdvice.NIGHT_EXAMPLE)
    }
}

object HomeChargeEngine {
    const val stationId="home-charge"
    const val reward=120
    fun completedToday(state: GameState,now: Long)=state.completionHistory.any{it.kind=="HOME" && GameState.day(it.at)==GameState.day(now)}
    fun start(state: GameState,vehicle: VehicleState,now: Long,id: String): GameState {
        require(state.districtId!=null && state.homeCharge.chargerName!=null){"우리 집 충전소를 먼저 등록하세요"}
        require(state.mission==null){"진행 중인 미션이 있습니다"}
        require(vehicle.usable(now)){"최신 차량 데이터가 필요합니다"}
        require(vehicle.source==if(state.mode==AppMode.MOCK)DataSource.MOCK else DataSource.PLEOS){"차량 데이터 출처를 확인하세요"}
        require(vehicle.soc!!<90f){"배터리가 충분해 추가 충전을 권하지 않습니다"}
        require(state.homeCharge.targetSoc?.let{vehicle.soc+2f<=it}!=false){"보상을 위해 목표 SOC를 넘어 충전하지 마세요"}
        require(!completedToday(state,now)){"오늘의 우리 집 충전 보상은 이미 받았습니다"}
        require(state.history.none{it.id==id || it.missionId==id}){"이미 사용한 미션 ID"}
        val plan=HomeChargePolicy.plan(state.homeCharge,vehicle.soc,now)
        return state.copy(mission=Mission(id,stationId,state.districtId,reward,vehicle.soc,vehicle.source,now,
            kind=MissionKind.HOME,windowStart=plan.start,windowEnd=plan.end,
            windowBaselineSoc=if(now>=plan.start)vehicle.soc else null,lastSampleAt=now))
    }
    fun observe(state: GameState,vehicle: VehicleState,now: Long): GameState {
        val m=state.mission?.takeIf{it.kind==MissionKind.HOME} ?: return state
        val start=m.windowStart ?: return state
        val end=m.windowEnd ?: return state
        if(now>=end)return state.copy(mission=null)
        if(!vehicle.usable(now) || vehicle.source!=m.source || vehicle.sampledAt<=m.startedAt || vehicle.sampledAt<=m.lastSampleAt)return state
        if(now<start) {
            if(!HomeChargePolicy.plan(state.homeCharge,vehicle.soc,now).night)
                return state.copy(mission=m.copy(windowStart=now,windowEnd=now+24*60*60*1000L,windowBaselineSoc=vehicle.soc,lastSampleAt=vehicle.sampledAt,progress=0f,chargingObserved=false))
            return state
        }
        if(vehicle.sampledAt<start)return state
        val baseline=m.windowBaselineSoc ?: vehicle.soc!!
        val gain=(vehicle.soc!!-baseline).coerceAtLeast(0f)
        val updated=m.copy(windowBaselineSoc=baseline,chargingObserved=m.chargingObserved || vehicle.chargeSwitch==ChargeSignal.ON,
            progress=(gain/m.requiredGain).coerceIn(0f,1f),lastSampleAt=vehicle.sampledAt)
        if(vehicle.chargeSwitch!=ChargeSignal.ON || gain<m.requiredGain)return state.copy(mission=updated)
        if(state.history.any{it.missionId==m.id} || completedToday(state,now))return state.copy(mission=null)
        return state.copy(mission=null,history=state.history+PointEvent(m.id,m.id,m.districtId,m.reward,now,"HOME"))
    }
}
