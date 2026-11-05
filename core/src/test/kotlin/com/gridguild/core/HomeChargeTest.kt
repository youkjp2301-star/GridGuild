package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HomeChargeTest {
    private val time=Instant.parse("2026-11-05T13:00:00Z").toEpochMilli() // 22:00 Seoul
    private fun vehicle(soc: Float=42f,at: Long=time,charge: ChargeSignal=ChargeSignal.ON,source: DataSource=DataSource.MOCK)=VehicleState(soc,charge,source,at)
    private fun initial(preferences: HomeChargePreferences=HomeChargePreferences("우리 집"))=GameState(districtId="sasang",homeCharge=preferences)
    private fun start(preferences: HomeChargePreferences=HomeChargePreferences("우리 집"))=HomeChargeEngine.start(initial(preferences),vehicle(),time,"home:test")
    @Test fun noDepartureLowBatteryAndUnmetTargetNeverRecommendWaiting() {
        assertEquals(HomeAdvice.UNKNOWN_DEPARTURE,HomeChargePolicy.plan(HomeChargePreferences("집"),42f,time).advice)
        assertEquals(HomeAdvice.LOW_BATTERY,HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=480),20f,time).advice)
        assertEquals(HomeAdvice.BELOW_TARGET,HomeChargePolicy.plan(HomeChargePreferences("집",80,480),42f,time).advice)
        assertEquals(HomeAdvice.DEPARTURE_SOON,HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=60),42f,time).advice)
        assertEquals(HomeAdvice.UNKNOWN_BATTERY,HomeChargePolicy.plan(HomeChargePreferences("집"),null,time).advice)
    }
    @Test fun overnightWindowAndDepartureBufferUseActualSeoulDates() {
        val plan=HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=480),42f,time)
        assertTrue(plan.night);assertEquals(time+3_600_000,plan.start);assertEquals(time+5*3_600_000,plan.end)
        assertTrue(HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=360),42f,time).night)
        assertFalse(HomeChargePolicy.plan(HomeChargePreferences("집",departureMinutes=359),42f,time).night)
    }
    @Test fun aSingleJoinAutomaticallyPaysPersonalCityAndRankOnce() {
        val before=start()
        val after=MissionEngine.observe(before,vehicle(44f,time+1000),time+1000)
        assertNull(after.mission);assertEquals(120,after.xp);assertEquals(120,after.cityXp("sasang"));assertEquals("HOME",after.history.single().kind)
        assertEquals(120,after.seasonXp(time));assertTrue(Rankings.personal(after,time).indexOfFirst{it.mine}<Rankings.personal(initial(),time).indexOfFirst{it.mine})
        assertEquals(after,MissionEngine.observe(after,vehicle(46f,time+2000),time+2000))
        assertThrows(IllegalArgumentException::class.java){HomeChargeEngine.start(after,vehicle(44f,time+2000),time+2000,"home:again")}
    }
    @Test fun preWindowGainsNeverBecomeNightRewardsAndMidnightIsSupported() {
        var state=start(HomeChargePreferences("家",departureMinutes=480))
        val from=state.mission!!.windowStart!!
        state=MissionEngine.observe(state,vehicle(60f,from-1000),from-1000)
        assertEquals(0,state.xp);assertNull(state.mission!!.windowBaselineSoc)
        state=MissionEngine.observe(state,vehicle(60f,from),from)
        assertEquals(0,state.xp);assertEquals(60f,state.mission!!.windowBaselineSoc!!,0f)
        state=MissionEngine.observe(state,vehicle(62f,from+3_600_000+1000),from+3_600_000+1000)
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
        val waiting=start(HomeChargePreferences("집",departureMinutes=480))
        val changed=MissionEngine.observe(waiting,vehicle(20f,time+1000),time+1000)
        assertEquals(time+1000,changed.mission!!.windowStart);assertEquals(20f,changed.mission!!.windowBaselineSoc!!,0f);assertEquals(0,changed.xp)
        assertEquals(120,MissionEngine.observe(changed,vehicle(22f,time+2000),time+2000).xp)
    }
    @Test fun expiredNightEndsWithoutRewardAndBothKindsShareOneActiveSlot() {
        val waiting=start(HomeChargePreferences("집",departureMinutes=480))
        val end=waiting.mission!!.windowEnd!!
        assertNull(MissionEngine.observe(waiting,vehicle(80f,end),end).mission)
        assertEquals(0,MissionEngine.observe(waiting,vehicle(80f,end),end).xp)
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
}
