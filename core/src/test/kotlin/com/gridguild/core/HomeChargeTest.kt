package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HomeChargeTest {
    private val time=Instant.parse("2026-11-05T13:00:00Z").toEpochMilli() // 22:00 Seoul
    private fun vehicle(soc: Float=42f,at: Long=time,charge: ChargeSignal=ChargeSignal.ON,source: DataSource=DataSource.MOCK)=VehicleState(soc,charge,source,at)
    private fun initial(preferences: HomeChargePreferences=HomeChargePreferences("우리 집"))=GameState(districtId="sasang",homeCharge=preferences)
    private fun start(preferences: HomeChargePreferences=HomeChargePreferences("우리 집"),real: Boolean=false,at: Long=time)=
        HomeChargeEngine.start(initial(preferences).copy(mode=if(real)AppMode.PLEOS else AppMode.MOCK),vehicle(at=at,source=if(real)DataSource.PLEOS else DataSource.MOCK),at,"home:test")
    @Test fun noDepartureLowBatteryAndUnmetTargetNeverRecommendWaiting() {
        assertEquals(HomeAdvice.UNKNOWN_DEPARTURE,HomeChargePolicy.plan(HomeChargePreferences("집"),42f,time).advice)
        assertEquals(HomeAdvice.LOW_BATTERY,HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=480),20f,time).advice)
        assertEquals(HomeAdvice.BELOW_TARGET,HomeChargePolicy.plan(HomeChargePreferences("집",80,480),42f,time).advice)
        assertEquals(HomeAdvice.DEPARTURE_SOON,HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=60),42f,time).advice)
        assertEquals(HomeAdvice.UNKNOWN_BATTERY,HomeChargePolicy.plan(HomeChargePreferences("집"),null,time).advice)
    }
    @Test fun twoDailyWindowsAndDepartureBufferUseActualSeoulDates() {
        val plan=HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=480),42f,time)
        assertTrue(plan.scheduled);assertEquals(time,plan.start);assertEquals(time+3_600_000,plan.end)
        assertTrue(HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=120),42f,time).scheduled)
        assertFalse(HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=119),42f,time).scheduled)
        val afternoon=time-6*3_600_000
        assertEquals(afternoon,HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=23*60),42f,afternoon).start)
    }
    @Test fun aSingleJoinAutomaticallyPaysPersonalCityAndRankOnce() {
        val before=start()
        val after=MissionEngine.observe(before,vehicle(44f,time+1000),time+1000)
        assertNull(after.mission);assertEquals(120,after.xp);assertEquals(120,after.cityXp("sasang"));assertEquals("HOME",after.history.single().kind)
        assertEquals(120,after.seasonXp(time));assertTrue(Rankings.personal(after,time).indexOfFirst{it.mine}<Rankings.personal(initial(),time).indexOfFirst{it.mine})
        assertEquals(after,MissionEngine.observe(after,vehicle(46f,time+2000),time+2000))
        assertThrows(IllegalArgumentException::class.java){HomeChargeEngine.start(after,vehicle(44f,time+2000),time+2000,"home:again")}
    }
    @Test fun realSourcePreWindowGainsNeverBecomeScheduledRewards() {
        var state=start(HomeChargePreferences("家",departureMinutes=480),real=true,at=time-3_600_000)
        val from=state.mission!!.windowStart!!
        state=MissionEngine.observe(state,vehicle(60f,from-1000,source=DataSource.PLEOS),from-1000)
        assertEquals(0,state.xp);assertNull(state.mission!!.windowBaselineSoc)
        state=MissionEngine.observe(state,vehicle(60f,from,source=DataSource.PLEOS),from)
        assertEquals(0,state.xp);assertEquals(60f,state.mission!!.windowBaselineSoc!!,0f)
        state=MissionEngine.observe(state,vehicle(62f,from+1_800_000,source=DataSource.PLEOS),from+1_800_000)
        assertEquals(120,state.xp);assertNull(state.mission)
    }
    @Test fun staleFutureOffUnknownWrongSourceAndReorderedSamplesCannotPay() {
        val original=start()
        for(sample in listOf(vehicle(44f,time-1),vehicle(44f,time+20_000),vehicle(44f,time+1000,ChargeSignal.OFF),
            vehicle(44f,time+1000,ChargeSignal.UNKNOWN),vehicle(44f,time+1000,source=DataSource.PLEOS)))
            assertEquals(0,MissionEngine.observe(original,sample,time+1000).xp)
        val newer=MissionEngine.observe(original,vehicle(43f,time+3000),time+3000)
        assertEquals(newer,MissionEngine.observe(newer,vehicle(44f,time+2000),time+3001))
        assertEquals(0,MissionEngine.observe(original,vehicle(44f,time+1000),time+12_000).xp)
    }
    @Test fun lowBatteryAfterEnrollmentFallsBackToNowWithoutImmediateReward() {
        val before=time-3_600_000
        val waiting=start(HomeChargePreferences("집",departureMinutes=480),real=true,at=before)
        val changed=MissionEngine.observe(waiting,vehicle(20f,before+1000,source=DataSource.PLEOS),before+1000)
        assertEquals(before+1000,changed.mission!!.windowStart);assertEquals(20f,changed.mission!!.windowBaselineSoc!!,0f);assertEquals(0,changed.xp)
        assertEquals(120,MissionEngine.observe(changed,vehicle(22f,before+2000,source=DataSource.PLEOS),before+2000).xp)
    }
    @Test fun expiredNightEndsWithoutRewardAndBothKindsShareOneActiveSlot() {
        val waiting=start(HomeChargePreferences("집",departureMinutes=480),real=true,at=time-3_600_000)
        val end=waiting.mission!!.windowEnd!!
        assertNull(MissionEngine.observe(waiting,vehicle(80f,end,source=DataSource.PLEOS),end).mission)
        assertEquals(0,MissionEngine.observe(waiting,vehicle(80f,end,source=DataSource.PLEOS),end).xp)
        assertThrows(IllegalArgumentException::class.java){MissionEngine.start(waiting,SampleData.stations.first(),vehicle(),time,"sasang-river:new")}
        assertThrows(IllegalArgumentException::class.java){HomeChargeEngine.start(waiting,vehicle(),time,"home:2")}
    }
    @Test fun noRegistrationHighSocAndChargingBeyondTargetCannotEnroll() {
        assertThrows(IllegalArgumentException::class.java){HomeChargeEngine.start(GameState(districtId="sasang"),vehicle(),time,"h")}
        assertThrows(IllegalArgumentException::class.java){HomeChargeEngine.start(initial(),vehicle(90f),time,"h")}
        assertThrows(IllegalArgumentException::class.java){HomeChargeEngine.start(initial(HomeChargePreferences("집",43)),vehicle(),time,"h")}
    }
    @Test fun newFixturesDoNotChangeTheOriginalDailyBonusAndHomeCannotTriggerIt() {
        var state=initial()
        SampleData.dailyBonusStations.forEachIndexed{i,station->
            val at=time+i*3000
            state=MissionEngine.start(state,station,vehicle(at=at),at,"${station.id}:$i")
            state=MissionEngine.observe(state,vehicle(44f,at+1000),at+1000)
        }
        assertEquals(390,state.xp);assertEquals(1,state.history.count{it.kind=="DAILY"})
        state=HomeChargeEngine.start(state,vehicle(at=time+10_000),time+10_000,"home:extra")
        state=MissionEngine.observe(state,vehicle(44f,time+11_000),time+11_000)
        assertEquals(510,state.xp);assertEquals(1,state.history.count{it.kind=="DAILY"})
        assertEquals(6,SampleData.stations.size);assertEquals(3,SampleData.dailyBonusStations.size)
    }
    @Test fun mockChargingOutsideBothSlotsUsesSameLedgerButRealSourceDoesNotSkipTime() {
        val outside=time-9*3_600_000 // 13:00 Korea
        val preferences=HomeChargePreferences("집",departureMinutes=8*60)
        val mock=start(preferences,at=outside)
        assertEquals(120,HomeChargeEngine.observe(mock,vehicle(44f,outside+1000),outside+1000).xp)
        val real=start(preferences,real=true,at=outside)
        assertEquals(0,HomeChargeEngine.observe(real,vehicle(44f,outside+1000,source=DataSource.PLEOS),outside+1000).xp)
        assertNull(real.mission!!.windowBaselineSoc)
        assertThrows(IllegalArgumentException::class.java){HomeChargeEngine.start(initial(preferences).copy(mode=AppMode.PLEOS),vehicle(at=outside),outside,"bad-source")}
    }
    @Test fun realWindowEndIsExclusiveForBothAfternoonAndEvening() {
        for((hour,departure) in listOf(16 to 23*60,22 to 8*60)) {
            val from=time+(hour-22)*3_600_000L
            val waiting=start(HomeChargePreferences("집",departureMinutes=departure),real=true,at=from-3_600_000)
            val baseline=HomeChargeEngine.observe(waiting,vehicle(42f,from,source=DataSource.PLEOS),from)
            val ended=HomeChargeEngine.observe(baseline,vehicle(44f,from+3_600_000,source=DataSource.PLEOS),from+3_600_000)
            assertNull(ended.mission);assertEquals(0,ended.xp)
        }
    }
}
