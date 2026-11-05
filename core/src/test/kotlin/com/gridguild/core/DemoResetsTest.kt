package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class DemoResetsTest {
    private val time=Instant.parse("2026-11-05T13:00:00Z").toEpochMilli()
    private val station=SampleData.stations.first()
    private fun sample(soc: Float=42f,at: Long=time)=VehicleState(soc,ChargeSignal.ON,DataSource.MOCK,at)
    private fun seeded()=GameState(districtId="sasang",mission=Mission("gwangalli:active","gwangalli","sasang",120,42f,DataSource.MOCK,time),
        history=listOf(PointEvent("old1","sasang-river:old","sasang",120,time),
            PointEvent("old2","centum:old","sasang",250,time),PointEvent("old3","gwangalli:old","haeundae",100,time)))

    @Test fun cityResetOnlyRemovesSelectedCityCredit() {
        val before=seeded();val after=DemoResets.reset(before,DemoResetTarget.CITY_LEVEL)
        assertEquals(0,after.cityXp("sasang"));assertEquals(1,CityLevels.level(after.cityXp("sasang")))
        assertEquals(100,after.cityXp("haeundae"));assertEquals(before.xp,after.xp)
        assertEquals(before.mission,after.mission);assertTrue(after.completedToday(station.id,time))
        assertEquals(0,after.seasonXp(time,"sasang"));assertEquals(470,after.seasonXp(time))
        assertEquals(before.history.map{it.xp},after.history.map{it.xp})
    }
    @Test fun pointResetPreservesCityAndCompletionState() {
        val before=seeded();val after=DemoResets.reset(before,DemoResetTarget.PERSONAL_POINTS)
        assertEquals(0,after.xp);assertEquals(0,after.seasonXp(time));assertEquals(370,after.cityXp("sasang"))
        assertEquals(370,after.seasonXp(time,"sasang"));assertEquals(before.mission,after.mission)
        assertTrue(after.completedToday(station.id,time));assertEquals(0,Rankings.personal(after,time).single{it.mine}.xp)
        assertEquals(Rankings.cities(before,time),Rankings.cities(after,time))
    }
    @Test fun missionResetAllowsReplayButNeverReusesAnOldMissionId() {
        val after=DemoResets.reset(seeded(),DemoResetTarget.CHARGING_MISSIONS)
        assertNull(after.mission);assertTrue(after.completionHistory.isEmpty());assertFalse(after.completedToday(station.id,time))
        assertEquals(470,after.xp);assertEquals(370,after.cityXp("sasang"))
        val replay=MissionEngine.start(after,station,sample(),time,"${station.id}:new")
        val awarded=MissionEngine.observe(replay,sample(44f,time+1000),time+1000)
        assertEquals(590,awarded.xp);assertEquals(490,awarded.cityXp("sasang"))
        assertTrue(awarded.completedToday(station.id,time))
        assertThrows(IllegalArgumentException::class.java) { MissionEngine.start(after,station,sample(),time,"old1") }
        assertThrows(IllegalArgumentException::class.java) { MissionEngine.start(awarded,station,sample(),time,"${station.id}:again") }
    }
    @Test fun pointAndCityResetsDoNotUnlockCompletedStations() {
        for(target in listOf(DemoResetTarget.CITY_LEVEL,DemoResetTarget.PERSONAL_POINTS)) {
            val state=DemoResets.reset(seeded().copy(mission=null),target)
            assertThrows(IllegalArgumentException::class.java) { MissionEngine.start(state,station,sample(),time,"${station.id}:new") }
        }
    }
    @Test fun dailyBonusIsReearnedOnlyAfterAllMissionsInTheNewTestRun() {
        fun completeAll(initial: GameState,run: Int): GameState {
            var state=initial
            SampleData.stations.forEachIndexed { i,s ->
                val at=time+run*20_000+i*2_000
                state=MissionEngine.start(state,s,sample(at=at),at,"${s.id}:$run:$i")
                state=MissionEngine.observe(state,sample(44f,at+1000),at+1000)
                if(i<2) assertEquals(run,state.history.count{it.kind=="DAILY"})
            }
            return state
        }
        val first=completeAll(GameState(districtId="sasang"),0)
        val second=completeAll(DemoResets.reset(first,DemoResetTarget.CHARGING_MISSIONS),1)
        assertEquals(780,second.xp);assertEquals(2,second.history.count{it.kind=="DAILY"})
        assertEquals(1,second.completionHistory.count{it.kind=="DAILY"})
        assertEquals(second.history.size,second.history.map{it.id}.toSet().size)
        assertEquals(second,MissionEngine.observe(second,sample(46f,time+30_000),time+30_000))
    }
    @Test fun resetsAreIdempotentAndNewRewardsAccrueFromZero() {
        var state=seeded()
        for(target in listOf(DemoResetTarget.CITY_LEVEL,DemoResetTarget.PERSONAL_POINTS,DemoResetTarget.CHARGING_MISSIONS)) {
            state=DemoResets.reset(state,target)
            assertEquals(state,DemoResets.reset(state,target))
        }
        assertEquals(0,state.xp);assertEquals(0,state.cityXp("sasang"))
        state=MissionEngine.start(state,station,sample(),time,"${station.id}:fresh")
        state=MissionEngine.observe(state,sample(44f,time+1000),time+1000)
        assertEquals(120,state.xp);assertEquals(120,state.cityXp("sasang"))
    }
    @Test fun allResetAndMissingDistrictAreHandledExplicitly() {
        assertEquals(GameState(),DemoResets.reset(seeded(),DemoResetTarget.ALL))
        val missing=seeded().copy(districtId=null)
        assertEquals(missing,DemoResets.reset(missing,DemoResetTarget.CITY_LEVEL))
    }
    @Test fun sdkStateCannotBeAlteredByAnyDemoReset() {
        val state=seeded().copy(mode=AppMode.PLEOS)
        DemoResetTarget.entries.forEach { assertEquals(state,DemoResets.reset(state,it)) }
    }
}
