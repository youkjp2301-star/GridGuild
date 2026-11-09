package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class ChargingAvailabilityTest {
    private val now=2_000_000L
    private fun snapshot(states: List<ChargerUseState>,time: Long=now)=StationAvailability("s",states.mapIndexed{i,s->
        ChargerObservation("$i",ChargerConnector.DC_COMBO,s,time)},states.size,time,AvailabilitySource.MOCK)
    private fun summary(vararg states: ChargerUseState)=ChargingAvailability.summarize(snapshot(states.toList()),now)
    @Test fun thresholdsIncludeExactlyHalfAndExcludeFaultsAndMaintenance() {
        assertEquals(AvailabilityStatus.FREE,summary(ChargerUseState.AVAILABLE,ChargerUseState.IN_USE).status)
        val busy=summary(ChargerUseState.AVAILABLE,ChargerUseState.IN_USE,ChargerUseState.FAULT,ChargerUseState.MAINTENANCE,ChargerUseState.OUT_OF_SERVICE)
        assertEquals(AvailabilityStatus.BUSY,busy.status);assertEquals(1,busy.available)
        assertEquals("5기 중 1기 이용 가능",busy.countLabel)
        assertEquals(AvailabilityStatus.NONE,summary(ChargerUseState.IN_USE,ChargerUseState.FAULT,ChargerUseState.MAINTENANCE).status)
    }
    @Test fun missingStaleFutureAndUnknownAreNeverPresentedAsFreeOrZeroAvailable() {
        assertEquals(AvailabilityStatus.UNKNOWN,ChargingAvailability.summarize(null,now).status)
        for(time in listOf(now-ChargingAvailability.maxAgeMs-1,now+1)) {
            val data=ChargingAvailability.summarize(snapshot(listOf(ChargerUseState.AVAILABLE),time),now)
            assertEquals(AvailabilityStatus.UNKNOWN,data.status);assertFalse(data.complete);assertEquals(0,data.available)
            assertFalse(data.countLabel.contains("0기 이용 가능"))
        }
        assertEquals(AvailabilityStatus.UNKNOWN,summary(ChargerUseState.UNKNOWN).status)
        assertEquals(AvailabilityStatus.FREE,ChargingAvailability.summarize(snapshot(listOf(ChargerUseState.AVAILABLE),now-ChargingAvailability.maxAgeMs),now).status)
    }
    @Test fun partialRecordsPreserveOnlyFreshConfirmedAvailability() {
        val original=snapshot(listOf(ChargerUseState.AVAILABLE,ChargerUseState.AVAILABLE,ChargerUseState.UNKNOWN))
        val result=ChargingAvailability.summarize(original.copy(chargers=original.chargers.mapIndexed{i,c->if(i==1)c.copy(observedAt=now-ChargingAvailability.maxAgeMs-1) else c}),now)
        assertEquals(AvailabilityStatus.UNKNOWN,result.status);assertEquals(1,result.available)
        assertTrue(result.countLabel.contains("일부 미확인"));assertNull(result.compatibleAvailable)
    }
    @Test fun inconsistentOrDuplicateInventoryIsUnknown() {
        val original=snapshot(listOf(ChargerUseState.AVAILABLE))
        for(data in listOf(original.copy(totalChargers=0),original.copy(totalChargers=2),original.copy(chargers=original.chargers+original.chargers)))
            assertEquals(AvailabilityStatus.UNKNOWN,ChargingAvailability.summarize(data,now).status)
    }
    @Test fun refetchingOldStatusDoesNotFreshenItsObservationButOldChangeTimeIsValid() {
        val old=snapshot(listOf(ChargerUseState.AVAILABLE),now-ChargingAvailability.maxAgeMs-1)
        assertEquals(AvailabilityStatus.UNKNOWN,ChargingAvailability.summarize(old.copy(collectedAt=now),now).status)
        val changedLongAgo=snapshot(listOf(ChargerUseState.AVAILABLE)).let{it.copy(chargers=it.chargers.map{c->c.copy(statusChangedAt=1)})}
        assertEquals(AvailabilityStatus.FREE,ChargingAvailability.summarize(changedLongAgo,now).status)
    }
    @Test fun connectorCountsDoNotTreatAnUnidentifiedPlugAsCompatibleOrKnownEmpty() {
        val original=snapshot(listOf(ChargerUseState.AVAILABLE,ChargerUseState.IN_USE))
        val mixed=original.copy(chargers=listOf(original.chargers[0].copy(connector=ChargerConnector.AC_TYPE1),original.chargers[1]))
        assertEquals(0,ChargingAvailability.summarize(mixed,now,setOf(ChargerConnector.DC_COMBO)).compatibleAvailable)
        assertNull(ChargingAvailability.summarize(mixed,now).compatibleAvailable)
        assertNull(ChargingAvailability.summarize(mixed.copy(chargers=mixed.chargers.map{it.copy(connector=null)}),now,setOf(ChargerConnector.DC_COMBO)).compatibleAvailable)
    }
    @Test fun availabilityDistanceAndCompatibilityAllContributeToStableOrdering() {
        val stations=listOf(
            Station("near","near","sasang",35.001,129.0,120,0,24),
            Station("free","free","sasang",35.003,129.0,120,0,24),
            Station("far","far","sasang",35.04,129.0,120,0,24),
            Station("ac","ac","sasang",35.0,129.0,120,0,24,connectors=setOf(ChargerConnector.AC_TYPE1)),
        )
        val states=stations.associate{it.id to snapshot(listOf(ChargerUseState.AVAILABLE,ChargerUseState.IN_USE,if(it.id=="near")ChargerUseState.IN_USE else ChargerUseState.AVAILABLE)).copy(stationId=it.id)}
        val result=ChargingRecommendations.around(35.0,129.0,42f,stations,states,now,setOf(ChargerConnector.DC_COMBO))
        assertEquals(listOf("free","near","far","ac"),result.nearby.map{it.station.id})
        val detour=ChargingRecommendations.around(35.0,129.0,42f,stations,states,now,setOf(ChargerConnector.DC_COMBO),mapOf("free" to 4.0,"near" to .1,"far" to .2))
        assertEquals("far",detour.nearby.first().station.id)
    }
}
