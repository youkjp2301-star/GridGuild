package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class DriverSceneTest {
    @Test fun everyDestinationAndLevelHasItsOwnCanonicalAssetAndMetadata() {
        val keys=mutableSetOf<String>()
        for(city in NeutralCity.entries) for(level in 1..12) {
            val scene=DriverScene.resolve(city,CityLevels.thresholds.last(),level)
            assertEquals(city,scene.city)
            assertEquals(level,scene.level)
            assertEquals("driver_${city.destinationId}_lv${level.toString().padStart(2,'0')}_v2",scene.assetKey)
            assertTrue(keys.add(scene.assetKey))
            assertEquals(CityLevels.names[level-1],scene.stageName)
            assertTrue(scene.roadLabel.isNotBlank())
            assertTrue(scene.landmarkLabel.contains(city.landmark))
        }
        assertEquals(24,keys.size)
    }

    @Test fun currentSceneChangesExactlyAtEveryXpThreshold() {
        for(city in NeutralCity.entries) for((index,threshold) in CityLevels.thresholds.withIndex()) {
            assertEquals(index+1,DriverScene.resolve(city,threshold).level)
            if(index>0)assertEquals(index,DriverScene.resolve(city,threshold-1).level)
        }
        assertEquals(1,DriverScene.resolve(NeutralCity.GWANGALLI,-1).level)
        assertEquals(12,DriverScene.resolve(NeutralCity.CENTUM,Int.MAX_VALUE).level)
    }

    @Test fun onlyCurrentAndPreviouslyUnlockedScenesCanBeInspected() {
        for(city in NeutralCity.entries) for(current in 1..12) {
            val xp=CityLevels.thresholds[current-1]
            for(requested in -1..14)assertEquals(
                "city=$city current=$current requested=$requested",
                if(requested in 1..current)requested else current,
                DriverScene.resolve(city,xp,requested).level,
            )
            assertEquals(current,DriverScene.resolve(city,xp,null).level)
        }
    }

    @Test fun loweringXpCannotKeepAFormerlyUnlockedHighLevelScene() {
        for(city in NeutralCity.entries) {
            val old=DriverScene.resolve(city,5_040,12)
            assertEquals(12,old.level)
            val reset=DriverScene.resolve(city,100,old.level)
            assertEquals(2,reset.level)
            assertFalse(reset.assetKey.contains("lv12"))
            assertEquals(1,DriverScene.resolve(city,0,old.level).level)
        }
    }

    @Test fun changingDestinationKeepsThePermittedLevelButNeverTheOldCityAsset() {
        for(level in 1..12) {
            val gwangan=DriverScene.resolve(NeutralCity.GWANGALLI,5_040,level)
            val centum=DriverScene.resolve(NeutralCity.CENTUM,5_040,gwangan.level)
            assertEquals(level,centum.level)
            assertNotEquals(gwangan.assetKey,centum.assetKey)
            assertTrue(centum.assetKey.startsWith("driver_centum_"))
            assertTrue(centum.landmarkLabel.contains("영화의전당"))
        }
    }

    @Test fun earlyAndFutureScenesHaveExplicitlyDifferentRoadAndLandmarkDescriptions() {
        for(city in NeutralCity.entries) {
            val village=DriverScene.resolve(city,5_040,2)
            val future=DriverScene.resolve(city,5_040,12)
            assertTrue(village.roadLabel.contains("흙길"))
            assertTrue(village.roadLabel.contains("초가집"))
            assertTrue(village.landmarkLabel.contains("전신"))
            assertTrue(future.roadLabel.contains("미래 도로"))
            assertTrue(future.landmarkLabel.contains("초진보"))
            assertNotEquals(village.roadLabel,future.roadLabel)
        }
    }

    @Test(expected=IllegalArgumentException::class)
    fun malformedDeclaredSceneCannotProduceAnOutOfRangeAssetKey() {
        DriverScene(NeutralCity.CENTUM,13)
    }
}
