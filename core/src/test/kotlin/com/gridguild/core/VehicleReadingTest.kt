package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class VehicleReadingTest {
    private fun sample(at: Long=20_000,source: DataSource=DataSource.MOCK) =
        VehicleState(44f,ChargeSignal.OFF,source,at)
    @Test fun normalPeriodicSamplesStayFreshAcrossTheObservationClock() {
        repeat(100) { tick ->
            val at=20_000L+tick*1_000
            val reading=VehicleReading.capture(sample(at),at+1)
            assertTrue(reading.fresh)
            assertEquals("데모 데이터 정상",reading.statusLabel)
        }
    }
    @Test fun aCapturedReadingDoesNotDependOnAnOlderCompositionClock() {
        val reading=VehicleReading.capture(sample(20_001),20_002)
        assertTrue(reading.fresh)
        assertFalse(reading.vehicle.usable(20_000)) // This was the obsolete clock used by the old UI.
        assertEquals("데모 데이터 정상",reading.statusLabel)
    }
    @Test fun staleAndUnavailableSamplesRemainBlocked() {
        assertFalse(VehicleReading.capture(sample(),30_001).fresh)
        assertEquals("차량 데이터 갱신 지연",VehicleReading.capture(sample(),30_001).statusLabel)
        assertFalse(VehicleReading.capture(sample(source=DataSource.UNAVAILABLE),20_001).fresh)
    }
    @Test fun genuinelyFutureSamplesAreNotMadeValidByTheUiFix() {
        assertFalse(VehicleReading.capture(sample(21_000),20_000).fresh)
    }
    @Test fun everyCityLevelHasItsOwnArtworkFrame() {
        val stages=(1..12).map(CityStage::at)
        assertEquals(12,stages.map{it.era to it.frame}.toSet().size)
        stages.forEachIndexed { index,stage -> assertEquals(CityLevels.names[index],stage.name) }
        assertEquals(1,CityStage.at(0).level)
        assertEquals(12,CityStage.at(13).level)
    }
}
