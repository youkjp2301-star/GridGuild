package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class CityUnlocksTest {
    @Test fun everyFutureCityIsLockedAndEveryEarlierCityCanBeVisited() {
        for(current in 1..12) for(candidate in 1..12)
            assertEquals("current=$current candidate=$candidate",candidate<=current,CityUnlocks.canView(candidate,current))
    }
    @Test fun lockedOrInvalidSelectionFallsBackToTheActualCity() {
        assertEquals(2,CityUnlocks.selection(12,120))
        assertEquals(2,CityUnlocks.selection(0,120))
        assertEquals(2,CityUnlocks.selection(null,120))
        assertEquals(1,CityUnlocks.selection(1,120))
    }
    @Test fun malformedAccessLimitsNeverRevealAnImage() {
        assertFalse(CityUnlocks.canView(1,0))
        assertFalse(CityUnlocks.canView(12,13))
        assertFalse(CityUnlocks.canView(13,12))
    }
    @Test fun rewardsUnlockOnlyWhenTheThresholdIsReached() {
        assertFalse(CityUnlocks.canView(3,CityLevels.level(249)))
        assertTrue(CityUnlocks.canView(3,CityLevels.level(250)))
        assertEquals(100,CityUnlocks.remainingXp(0))
        assertEquals(130,CityUnlocks.remainingXp(120))
        assertEquals(1,CityUnlocks.remainingXp(4999))
        assertEquals(0,CityUnlocks.remainingXp(5000))
    }
}
