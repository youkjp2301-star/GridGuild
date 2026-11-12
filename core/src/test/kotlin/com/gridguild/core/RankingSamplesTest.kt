package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test

class RankingSamplesTest {
    @Test fun citiesCoverEveryLevelFromThreeThroughElevenWithoutEditingMyLedger() {
        val levels=Rankings.cities(GameState(),0).map{it.cityLevel}.toSet()
        assertEquals((3..11).toSet(),levels)
        val mine=GameState(districtId="sasang",history=listOf(PointEvent("keep","keep","sasang",5040,0)))
        assertEquals(5040,mine.cityXp("sasang"))
        val row=Rankings.cities(mine,0,false).single{it.mine}
        assertEquals(5040,row.xp);assertEquals(5040,row.cityXp)
        assertEquals(12,row.cityLevel)
    }
    @Test fun personalContainsThirtyTwoDistinctRivalsAndKeepsMyActualScore() {
        val game=GameState(history=listOf(PointEvent("keep","keep","sasang",120,0)))
        val rows=Rankings.personal(game,0,false)
        assertEquals(33,rows.size);assertEquals(33,rows.map{it.name}.toSet().size)
        assertEquals(120,rows.single{it.mine}.xp)
        assertEquals(rows.map{it.xp}.sortedDescending(),rows.map{it.xp})
    }
}
