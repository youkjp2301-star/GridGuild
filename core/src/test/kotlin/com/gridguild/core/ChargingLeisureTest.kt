package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class ChargingLeisureTest {
    private val now=1_000_000L
    private val station=SampleData.stations.first()
    private fun input()=ChargingTimeInput(42.0,62.0,60.0,36.0,DataSource.MOCK,now)
    private fun estimate()=checkNotNull(ChargingTimeEstimator.estimate(input(),now))
    private fun atDistance(meters: Double,id: String="boundary")=LeisurePlace(id,station.id,"가상 경계 카페",LeisureCategory.CAFE,
        station.lat+Math.toDegrees(meters/6_371_000.0),station.lon,10,"경계 조건 검사용 가상 장소")

    @Test fun sixStationsEachHaveFiveDistinctFictionalPlacesAndAllFourCategories() {
        val ids=mutableSetOf<String>()
        SampleData.stations.forEach { site ->
            val places=MockChargingLeisurePlaces.forStation(site)
            assertEquals(5,places.size)
            assertEquals(LeisureCategory.entries.toSet(),places.map{it.category}.toSet())
            assertEquals(2,places.count{it.category==LeisureCategory.CAFE})
            assertEquals(1,places.count{it.isAdvertisement})
            places.forEach { place ->
                assertTrue(ids.add(place.id));assertTrue(place.name.contains("가상"))
                assertEquals(LeisureDataSource.MOCK,place.source)
                assertEquals(site.id,place.stationId)
                assertTrue(ChargingRecommendations.distanceKm(site.lat,site.lon,place.lat,place.lon)*1000<=300.0)
            }
            assertEquals(5,ChargingLeisure.recommend(site).size)
        }
        assertEquals(30,ids.size)
    }

    @Test fun radiusIncludesExactlyThreeHundredMetersAndRejectsOutsideInvalidAndWrongStation() {
        val valid=atDistance(300.0)
        val places=listOf(valid,atDistance(300.01,"outside"),atDistance(10.0,"wrong").copy(stationId="another"),
            atDistance(10.0,"invalid").copy(lat=Double.NaN),atDistance(5.0,"empty").copy(activityMinutes=0))
        val result=ChargingLeisure.recommend(station,places)
        assertEquals(listOf(valid.id),result.map{it.place.id})
        assertEquals(300.0,result.single().straightLineMeters,0.000001)
        assertTrue(ChargingLeisure.recommend(station.copy(lat=Double.NaN)).isEmpty())
    }

    @Test fun categoryFilterPreservesItsFixedStationScopeAndNeverCreatesWalkingTime() {
        LeisureCategory.entries.forEach { category ->
            val result=ChargingLeisure.recommend(station,category=category)
            assertTrue(result.isNotEmpty())
            assertTrue(result.all{it.place.category==category && it.walkingMinutes==null})
            assertTrue(result.all{it.timeFit==LeisureTimeFit.UNKNOWN && it.activityBudgetMinutes==null})
        }
    }

    @Test fun twentyMinuteEstimatePrioritizesTenToFifteenMinuteActivitiesWithReturnReserve() {
        val time=estimate()
        assertEquals(20,time.minutes)
        assertEquals(20.0,time.preciseMinutes,0.000001)
        assertEquals(12.0,time.energyKwh,0.000001)
        val result=ChargingLeisure.recommend(station,estimate=time)
        val fits=result.takeWhile{it.timeFit==LeisureTimeFit.ACTIVITY_FITS}
        assertEquals(4,fits.size)
        assertTrue(fits.all{it.place.activityMinutes in 10..15 && it.activityBudgetMinutes==15 && it.returnReserveMinutes==5})
        assertEquals(LeisureCategory.RESTAURANT,result.last().place.category)
        assertEquals(LeisureTimeFit.TOO_LONG,result.last().timeFit)
        assertTrue(result.all{it.walkingMinutes==null})
    }

    @Test fun displayedCeilingDoesNotRoundUpTheActivityBudget() {
        val time=checkNotNull(ChargingTimeEstimator.estimate(input().copy(effectivePowerKw=37.0),now))
        assertEquals(20,time.minutes)
        assertTrue(time.preciseMinutes<20)
        val result=ChargingLeisure.recommend(station,estimate=time)
        assertTrue(result.all{it.activityBudgetMinutes==14})
        assertEquals(LeisureTimeFit.TOO_LONG,result.first{it.place.category==LeisureCategory.POPUP_EVENT}.timeFit)
    }

    @Test fun advertisementAndBenefitMetadataNeverChangeRecommendationWeightOrIssueRewards() {
        val places=MockChargingLeisurePlaces.forStation(station)
        val original=ChargingLeisure.recommend(station,places,estimate=estimate())
        val toggled=ChargingLeisure.recommend(station,places.map{it.copy(isAdvertisement=!it.isAdvertisement,
            benefit=LeisureBenefit("다른 메타데이터","발급되지 않는 표시 정보"))},estimate=estimate())
        assertEquals(original.map{it.place.id},toggled.map{it.place.id})
        assertTrue(original.first().straightLineMeters<original.first{it.place.isAdvertisement}.straightLineMeters)
        assertNotNull(original.first{it.place.isAdvertisement}.place.benefit)
    }

    @Test fun estimateRejectsMissingNonfiniteOutOfRangeAndAlreadyReachedTargets() {
        val samples=listOf(
            input().copy(currentSoc=null),input().copy(targetSoc=null),input().copy(capacityKwh=null),input().copy(effectivePowerKw=null),
            input().copy(currentSoc=Double.NaN),input().copy(targetSoc=Double.POSITIVE_INFINITY),
            input().copy(capacityKwh=Double.NaN),input().copy(effectivePowerKw=Double.NEGATIVE_INFINITY),
            input().copy(currentSoc=-1.0),input().copy(currentSoc=101.0),input().copy(targetSoc=101.0),input().copy(targetSoc=-1.0),
            input().copy(capacityKwh=0.0),input().copy(capacityKwh=-60.0),input().copy(effectivePowerKw=0.0),input().copy(effectivePowerKw=-1.0),
            input().copy(targetSoc=42.0),input().copy(targetSoc=40.0),input().copy(effectivePowerKw=Double.MIN_VALUE),
        )
        samples.forEach{assertNull(it.toString(),ChargingTimeEstimator.estimate(it,now))}
    }

    @Test fun everyObservedInputNeedsAKnownMatchingSourceAndFreshNonfutureTimestamp() {
        val invalid=listOf(
            input().copy(source=DataSource.UNAVAILABLE),input().copy(capacitySource=DataSource.PLEOS),input().copy(powerSource=DataSource.PLEOS),
            input().copy(sampledAt=0),input().copy(sampledAt=now-10_001),input().copy(sampledAt=now+1),
            input().copy(capacitySampledAt=now-10_001),input().copy(powerSampledAt=now-10_001),
            input().copy(capacitySampledAt=now+1),input().copy(powerSampledAt=0),
        )
        invalid.forEach{assertNull(it.toString(),ChargingTimeEstimator.estimate(it,now))}
        assertNotNull(ChargingTimeEstimator.estimate(input().copy(sampledAt=now-10_000,
            capacitySampledAt=now-10_000,powerSampledAt=now-10_000),now))
        assertNotNull(ChargingTimeEstimator.estimate(input().copy(source=DataSource.PLEOS,
            capacitySource=DataSource.PLEOS,powerSource=DataSource.PLEOS),now))
    }

    @Test fun explicitlyStaticMockAssumptionsDoNotPretendToBeFreshMeasurements() {
        val assumption=input().copy(mockStaticAssumptions=true,capacitySampledAt=0,powerSampledAt=0)
        val first=checkNotNull(ChargingTimeEstimator.estimate(assumption,now))
        assertTrue(first.usesDemoAssumptions)
        assertFalse(estimate().usesDemoAssumptions)
        assertEquals(20,first.minutes)
        // Static capacity/power remain inputs after a minute, but a genuinely fresh SOC is required.
        assertNotNull(ChargingTimeEstimator.estimate(assumption.copy(sampledAt=now+60_000),now+60_000))
        assertNull(ChargingTimeEstimator.estimate(assumption,now+10_001))
        assertNull(ChargingTimeEstimator.estimate(assumption.copy(mockStaticAssumptions=false),now))
        assertNull(ChargingTimeEstimator.estimate(assumption.copy(source=DataSource.PLEOS,
            capacitySource=DataSource.PLEOS,powerSource=DataSource.PLEOS),now))
        assertNull(ChargingTimeEstimator.estimate(assumption.copy(capacitySource=DataSource.PLEOS),now))
    }

    @Test fun insufficientEstimateKeepsPlacesWithoutInventingTimeFitOrWalkingDurations() {
        val result=ChargingLeisure.recommend(station,estimate=ChargingTimeEstimator.estimate(input().copy(targetSoc=null),now))
        assertEquals(5,result.size)
        assertTrue(result.all{it.timeFit==LeisureTimeFit.UNKNOWN && it.activityBudgetMinutes==null && it.walkingMinutes==null})
        assertEquals(result.sortedBy{it.straightLineMeters}.map{it.place.id},result.map{it.place.id})
    }
}
