package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class NeutralCityTest {
    @Test fun resolvesOnlySupportedNearbyDestinations() {
        for(city in NeutralCity.entries) {
            assertEquals(city,NeutralCity.at(city.destination.lat,city.destination.lon))
            assertEquals(city,NeutralCity.at(city.destination.lat+.001,city.destination.lon+.001))
        }
        for(id in listOf("samnak","gijang")) {
            val place=DemoDestinations.places.first{it.id==id}
            assertNull(NeutralCity.at(place.lat,place.lon))
        }
        assertNull(NeutralCity.at(null,null));assertNull(NeutralCity.at(Double.NaN,129.13))
        assertNull(NeutralCity.at(0.0,0.0));assertNull(NeutralCity.at(91.0,129.0))
    }
    @Test fun nearestLandmarkWinsWhereAreasOverlap() {
        assertEquals(NeutralCity.GWANGALLI,NeutralCity.at(35.158,129.12))
        assertEquals(NeutralCity.CENTUM,NeutralCity.at(35.164,129.126))
    }
    @Test fun mockNeutralCannotAuthorizeActualNeutralOrRestrictedGameplay() {
        val now=100_000L
        val parked=SafetyState(true,true,now,DataSource.MOCK)
        assertTrue(NeutralCity.canEnter(AppMode.MOCK,parked,true,now))
        assertFalse(NeutralCity.canEnter(AppMode.PLEOS,parked,true,now))
        assertFalse(NeutralCity.canEnter(AppMode.MOCK,parked.copy(source=DataSource.PLEOS),true,now))
        assertFalse(NeutralCity.canEnter(AppMode.MOCK,parked.copy(parked=false),true,now))
        assertFalse(NeutralCity.canEnter(AppMode.MOCK,parked.copy(uxAllowed=false),true,now))
        assertFalse(NeutralCity.canEnter(AppMode.MOCK,parked,false,now))
        assertFalse(NeutralCity.canEnter(AppMode.MOCK,parked,true,now+10_001))
        assertFalse(NeutralCity.canEnter(AppMode.MOCK,parked.copy(source=DataSource.UNAVAILABLE),true,now))
    }
}
