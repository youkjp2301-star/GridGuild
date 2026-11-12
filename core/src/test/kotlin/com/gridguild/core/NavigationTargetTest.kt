package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class NavigationTargetTest {
    private val station=SampleData.stations.first()
    private val place=MockChargingLeisurePlaces.forStation(station).first()
    @Test fun leisureTargetCarriesThePlaceNotTheChargerCoordinates() {
        val target=checkNotNull(NavigationTarget.fromLeisure(station,place))
        assertEquals(place.name,target.name);assertEquals(place.lat,target.lat,0.0);assertEquals(place.lon,target.lon,0.0)
        assertNotEquals(station.lon,target.lon,0.0)
    }
    @Test fun wrongStationUnknownSourceInvalidAndOutsidePlacesCannotBecomeTargets() {
        listOf(place.copy(stationId="other"),place.copy(source=LeisureDataSource.UNKNOWN),place.copy(source=LeisureDataSource.EXTERNAL),
            place.copy(lat=Double.NaN),place.copy(lon=Double.POSITIVE_INFINITY),place.copy(name=" "),
            place.copy(lat=station.lat+.01),place.copy(id="")).forEach{assertNull(NavigationTarget.fromLeisure(station,it))}
    }
    @Test fun exactRadiusIsIncludedWithoutAcceptingTheNextCentimeter() {
        fun north(distance: Double)=place.copy(lat=station.lat+Math.toDegrees(distance/6_371_000.0),lon=station.lon)
        assertNotNull(NavigationTarget.fromLeisure(station,north(300.0)))
        assertNull(NavigationTarget.fromLeisure(station,north(300.01)))
    }
    @Test fun genericTargetsRequireANonblankNameAndValidCoordinates() {
        assertTrue(NavigationTarget("목적지",35.1,129.1).isValid)
        listOf(NavigationTarget("",35.1,129.1),NavigationTarget("목적지",0.0,0.0),
            NavigationTarget("목적지",91.0,129.1)).forEach{assertFalse(it.isValid)}
    }
}
