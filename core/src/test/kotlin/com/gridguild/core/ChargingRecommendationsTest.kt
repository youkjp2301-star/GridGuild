package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ChargingRecommendationsTest {
    @Test fun destinationsChangeCandidatesAndTheirOrderWithoutMovingStations() {
        val original=SampleData.stations.toList()
        fun ids(id: String)=DemoDestinations.places.first{it.id==id}.let {
            ChargingRecommendations.around(it.lat,it.lon,42f).nearby.map{candidate->candidate.station.id}
        }
        assertEquals(listOf("sasang-river"),ids("samnak"))
        assertEquals(listOf("centum","gwangalli"),ids("centum"))
        assertEquals(listOf("gwangalli","centum"),ids("gwangalli"))
        assertTrue(ids("gijang").isEmpty());assertEquals(original,SampleData.stations)
    }
    @Test fun unknownCoordinatesAndHighSocAreDifferentFromAnEmptyArea() {
        assertEquals(RecommendationStatus.NO_DESTINATION,ChargingRecommendations.around(null,null,42f).status)
        assertEquals(RecommendationStatus.NO_DESTINATION,ChargingRecommendations.around(Double.NaN,129.0,42f).status)
        assertEquals(RecommendationStatus.NO_DESTINATION,ChargingRecommendations.around(91.0,129.0,42f).status)
        assertEquals(RecommendationStatus.NO_DESTINATION,ChargingRecommendations.around(0.0,0.0,42f).status)
        val home=DemoDestinations.places.first()
        assertEquals(RecommendationStatus.HIGH_SOC,ChargingRecommendations.around(home.lat,home.lon,90f).status)
        val empty=DemoDestinations.places.last()
        assertEquals(RecommendationStatus.NO_NEARBY,ChargingRecommendations.around(empty.lat,empty.lon,42f).status)
    }
    @Test fun allCandidatesAreWithinRadiusAndHaveStableIncreasingDistances() {
        for(place in DemoDestinations.places) {
            val result=ChargingRecommendations.around(place.lat,place.lon,42f)
            assertTrue(result.nearby.all{it.distanceKm in 0.0..5.0})
            assertEquals(result.nearby.map{it.distanceKm}.sorted(),result.nearby.map{it.distanceKm})
        }
        assertEquals(0.0,ChargingRecommendations.distanceKm(35.1,129.0,35.1,129.0),.00001)
        assertEquals(111.19,ChargingRecommendations.distanceKm(0.0,0.0,1.0,0.0),.05)
    }
    @Test fun randomSelectionAlwaysChangesTheCurrentPreparedLocation() {
        for(current in DemoDestinations.places) repeat(40) { seed ->
            assertNotEquals(current.id,DemoDestinations.randomOther(current.lat,current.lon,Random(seed)).id)
        }
        assertTrue(DemoDestinations.places.contains(DemoDestinations.randomOther(Double.NaN,null,Random(0))))
    }
}
