package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class DemoCityLevelsTest {
    private val time=1_799_000_000_000L
    private fun seed()=GameState(districtId="sasang",
        mission=Mission("active","gwangalli","sasang",120,42f,DataSource.MOCK,time),
        history=listOf(PointEvent("award","sasang-river:award","sasang",370,time),
            PointEvent("other","other","haeundae",250,time)))

    @Test fun upAndDownReachTheAdjacentThresholdAndPreserveEarnedRecords() {
        val before=seed()
        val up=DemoCityLevels.step(before,1,time,"up")
        assertEquals(450,up.cityXp("sasang"));assertEquals(4,CityLevels.level(up.cityXp("sasang")))
        assertEquals(before.history,up.history.take(before.history.size))
        val down=DemoCityLevels.step(up,-1,time,"down")
        assertEquals(250,down.cityXp("sasang"));assertEquals(3,CityLevels.level(down.cityXp("sasang")))
        assertEquals(before.xp,down.xp);assertEquals(250,down.cityXp("haeundae"))
        assertEquals(before.mission,down.mission);assertEquals(before.completionHistory,down.completionHistory)
        assertTrue(down.history.takeLast(2).all{it.demo && !it.personalCredit && !it.countsForMission})
    }

    @Test fun everyLevelIsReachableWithoutPersonalRewardsOrMissionCompletions() {
        var state=GameState(districtId="sasang")
        (2..12).forEach { level ->
            state=DemoCityLevels.step(state,1,time,"up-$level")
            assertEquals(level,CityLevels.level(state.cityXp("sasang")))
        }
        assertEquals(state,DemoCityLevels.step(state,1,time,"beyond"))
        (11 downTo 1).forEach { level ->
            state=DemoCityLevels.step(state,-1,time,"down-$level")
            assertEquals(level,CityLevels.level(state.cityXp("sasang")))
        }
        assertEquals(state,DemoCityLevels.step(state,-1,time,"below"))
        assertEquals(0,state.xp);assertEquals(0,state.cityXp("sasang"));assertTrue(state.completionHistory.isEmpty())
    }

    @Test fun realModeMissingDistrictInvalidDeltaAndReusedIdsCannotMutateState() {
        val before=seed()
        listOf(before.copy(mode=AppMode.PLEOS),before.copy(districtId=null),before.copy(districtId="invalid"))
            .forEach{assertEquals(it,DemoCityLevels.step(it,1,time,"up"))}
        listOf(Int.MIN_VALUE,-2,0,2,Int.MAX_VALUE).forEach{assertEquals(before,DemoCityLevels.step(before,it,time,"up"))}
        listOf("","award","sasang-river:award").forEach{assertEquals(before,DemoCityLevels.step(before,1,time,it))}
    }

    @Test fun adjustmentsDoNotReplayAnXpAwardAndARealMissionStillAwardsNormally() {
        val before=GameState(districtId="sasang")
        val adjusted=DemoCityLevels.step(before,1,time,"test")
        assertNull(XpVisualEvent.between(before,adjusted))
        val vehicle=VehicleState(42f,ChargeSignal.ON,DataSource.MOCK,time)
        val started=MissionEngine.start(adjusted,SampleData.stations.first(),vehicle,time,"sasang-river:real")
        val completed=MissionEngine.observe(started,vehicle.copy(soc=44f,sampledAt=time+1000),time+1000)
        assertEquals(120,completed.xp);assertEquals(220,completed.cityXp("sasang"))
        assertEquals(1,completed.completionHistory.size)
        assertEquals(120,XpVisualEvent.between(started,completed)?.personalGain)
    }
}
