package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class MissionEngineTest {
    private val time=Instant.parse("2026-11-04T13:00:00Z").toEpochMilli() // 22:00 Korea
    private val initial=GameState(districtId="sasang")
    private val station=SampleData.stations.first()
    private fun sample(soc: Float=42f,charge: ChargeSignal=ChargeSignal.ON,at: Long=time,source: DataSource=DataSource.MOCK)=VehicleState(soc,charge,source,at)
    private fun started()=MissionEngine.start(initial,station,sample(),time,"${station.id}:test")
    @Test fun chargingAndGainCompleteWithExactlyOnePersonalAndCityReward() {
        val completed=MissionEngine.observe(started(),sample(44f,at=time+1000),time+1000)
        assertNull(completed.mission);assertEquals(120,completed.xp);assertEquals(120,completed.cityXp("sasang"))
        assertEquals(0,completed.cityXp("haeundae"));assertTrue(completed.history.single().demo)
    }
    @Test fun repeatedSamplesNeverPayTwice() {
        var s=started();repeat(100){s=MissionEngine.observe(s,sample(44f,at=time+1000+it),time+1000+it)}
        assertEquals(120,s.xp);assertEquals(1,s.history.size)
    }
    @Test fun socGainWithSwitchOffDoesNotComplete() { assertEquals(0,MissionEngine.observe(started(),sample(44f,ChargeSignal.OFF,time+1000),time+1000).xp) }
    @Test fun switchOnWithoutGainDoesNotComplete() { assertEquals(0,MissionEngine.observe(started(),sample(42f,at=time+1000),time+1000).xp) }
    @Test fun socDecreaseIsNotV2gReward() { assertEquals(0,MissionEngine.observe(started(),sample(30f,at=time+1000),time+1000).xp) }
    @Test fun staleSampleCannotPay() { assertEquals(0,MissionEngine.observe(started(),sample(44f,at=time+1000),time+12000).xp) }
    @Test fun sourceSwitchCannotPay() { assertEquals(0,MissionEngine.observe(started(),sample(44f,at=time+1000,source=DataSource.PLEOS),time+1000).xp) }
    @Test fun preMissionSampleCannotPay() { assertEquals(0,MissionEngine.observe(started(),sample(44f),time+1).xp) }
    @Test fun unknownChargingCannotPay() { assertEquals(0,MissionEngine.observe(started(),sample(44f,ChargeSignal.UNKNOWN,time+1000),time+1000).xp) }
    @Test fun futureSampleCannotPay() { assertEquals(0,MissionEngine.observe(started(),sample(44f,at=time+2000),time+1000).xp) }
    @Test fun resumedPersistedMissionCanComplete() {
        val persisted=started().copy(mission=started().mission!!.copy(progress=.5f))
        assertEquals(120,MissionEngine.observe(persisted,sample(44f,at=time+1000),time+1000).xp)
    }
    @Test fun dailyBonusIsPaidOnce() {
        var s=initial
        SampleData.dailyBonusStations.forEachIndexed { i,station ->
            val t=time+i*3000
            s=MissionEngine.start(s,station,sample(at=t),t,"${station.id}:$i")
            s=MissionEngine.observe(s,sample(44f,at=t+1000),t+1000)
        }
        assertEquals(390,s.xp);assertEquals(1,s.history.count{it.kind=="DAILY"})
    }
    @Test fun levelThresholdsCoverTwelveStages() {
        CityLevels.thresholds.forEachIndexed{i,xp->assertEquals(i+1,CityLevels.level(xp))}
        assertEquals(1,CityLevels.level(-1));assertEquals(12,CityLevels.level(Int.MAX_VALUE));assertEquals(1f,CityLevels.progress(5000))
    }
    @Test fun monthlyXpIsSeparateFromLifetime() {
        val old=Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
        val s=initial.copy(history=listOf(PointEvent("a","a","sasang",100,old),PointEvent("b","b","sasang",120,time)))
        assertEquals(220,s.xp);assertEquals(120,s.seasonXp(time));assertEquals(0,s.seasonXp(time,"haeundae"))
    }
    @Test fun unknownSafetyFailsClosed() { assertFalse(SafetyState().allowsGame(time)) }
    @Test fun zeroSpeedIsNeverASafetyInput() {
        assertFalse(SafetyState(false,true,time,DataSource.PLEOS).allowsGame(time))
        assertFalse(SafetyState(true,false,time,DataSource.PLEOS).allowsGame(time))
        assertTrue(SafetyState(true,true,time,DataSource.PLEOS).allowsGame(time))
        assertFalse(SafetyState(true,true,time-11000,DataSource.PLEOS).allowsGame(time))
    }
    @Test fun highSocAvoidsExtraChargingRecommendations() { assertTrue(SampleData.recommend(null,null,95f).isEmpty()) }
    @Test fun nearestDestinationSortsFixtures() { assertEquals("centum",SampleData.recommend(35.169,129.136,40f).first().id) }
    @Test(expected=IllegalArgumentException::class) fun doubleStartIsRejected() { MissionEngine.start(started(),station,sample(),time,"second") }
    @Test(expected=IllegalArgumentException::class) fun wrongModeCannotStart() { MissionEngine.start(initial,station,sample(source=DataSource.PLEOS),time,"bad") }
    @Test(expected=IllegalArgumentException::class) fun impossibleGainIsRejected() { MissionEngine.start(initial,station,sample(99f),time,"bad") }
    @Test(expected=IllegalArgumentException::class) fun nanSocIsRejected() { MissionEngine.start(initial,station,sample(Float.NaN),time,"bad") }
    @Test(expected=IllegalArgumentException::class) fun sameStationCannotBeFarmedAgainOnSameDay() {
        val completed=MissionEngine.observe(started(),sample(44f,at=time+1000),time+1000)
        MissionEngine.start(completed,station,sample(44f,at=time+2000),time+2000,"${station.id}:another")
    }
    @Test fun rankChangesWithLocalReward() {
        val before=Rankings.personal(initial,time).indexOfFirst{it.mine}
        val s=initial.copy(history=listOf(PointEvent("a","a","sasang",120,time)))
        assertTrue(Rankings.personal(s,time).indexOfFirst{it.mine}<before)
        assertTrue(Rankings.cities(s,time).first{it.mine}.xp>Rankings.cities(initial,time).first{it.mine}.xp)
    }
}
