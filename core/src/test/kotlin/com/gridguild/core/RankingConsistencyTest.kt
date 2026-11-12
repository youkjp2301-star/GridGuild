package com.gridguild.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class RankingConsistencyTest {
    private val now=Instant.parse("2026-11-12T07:00:00Z").toEpochMilli()
    private val old=Instant.parse("2026-10-12T07:00:00Z").toEpochMilli()
    private fun game()=GameState(districtId="sasang",history=listOf(
        PointEvent("old","old","sasang",1400,old),PointEvent("new","new","sasang",120,now),
        PointEvent("other","other","haeundae",700,old)))

    @Test fun everySelectedDistrictUsesExactlyItsSavedBalanceAndLevel() {
        Busan.districts.forEach{district->listOf(0,99,100,1520,5040).forEach{xp->
            val game=GameState(districtId=district.id,history=listOf(PointEvent("same","same",district.id,xp,now)))
            val row=Rankings.cities(game,now,false).single{it.mine}
            assertEquals(district.id,row.districtId)
            assertEquals(xp,row.xp);assertEquals(xp,row.cityXp)
            assertEquals(CityLevels.level(game.cityXp(district.id)),row.cityLevel)
        }}
    }
    @Test fun monthlyCumulativeAndNewSeasonNeverChangeTheCumulativeCityLevel() {
        val game=game()
        val monthly=Rankings.cities(game,now).single{it.mine}
        val cumulative=Rankings.cities(game,now,false).single{it.mine}
        val nextMonth=Rankings.cities(game,Instant.parse("2026-12-12T07:00:00Z").toEpochMilli()).single{it.mine}
        assertEquals(120,monthly.xp);assertEquals(1520,cumulative.xp);assertEquals(0,nextMonth.xp)
        listOf(monthly,cumulative,nextMonth).forEach{assertEquals(1520,it.cityXp);assertEquals(7,it.cityLevel)}
        val other=Rankings.cities(game,now).single{it.districtId=="haeundae"}
        val base=Rankings.demoCityXp(Busan.districts.indexOfFirst{it.id=="haeundae"})
        assertEquals(base,other.xp);assertEquals(base+700,other.cityXp)
        assertEquals(CityLevels.level(base+700),other.cityLevel)
    }
    @Test fun cityAndPersonalResetsPreserveTheirIndependentCreditRules() {
        val original=game();val row=Rankings.cities(original,now).single{it.mine}
        val points=DemoResets.reset(original,DemoResetTarget.PERSONAL_POINTS)
        assertEquals(row,Rankings.cities(points,now).single{it.mine})
        val city=DemoResets.reset(original,DemoResetTarget.CITY_LEVEL)
        val reset=Rankings.cities(city,now).single{it.mine}
        assertEquals(0,reset.xp);assertEquals(0,reset.cityXp);assertEquals(1,reset.cityLevel)
        assertEquals(original.xp,city.xp);assertEquals(original.history.map{it.xp},city.history.map{it.xp})
    }
    @Test fun negativeSeasonAdjustmentStillUsesTheSavedCumulativeGrowth() {
        val original=game()
        val adjusted=original.copy(history=original.history+PointEvent("adjust","adjust","sasang",-520,now,
            kind="TEST_LEVEL",personalCredit=false,countsForMission=false))
        val row=Rankings.cities(adjusted,now).single{it.mine}
        assertEquals(-400,row.xp);assertEquals(1000,row.cityXp);assertEquals(6,row.cityLevel)
        assertEquals(original.xp,adjusted.xp)
    }
    @Test fun districtChangesOnlySelectWhichRegionUsesItsAuthoritativeLocalBalance() {
        val original=game();val changed=original.copy(districtId="haeundae")
        val row=Rankings.cities(changed,now,false).single{it.mine}
        assertEquals(700,row.xp);assertEquals(700,row.cityXp);assertEquals(5,row.cityLevel)
        assertEquals(original.history,changed.history)
        assertEquals(original.xp,changed.xp)
        assertEquals(1520,changed.cityXp("sasang"))
    }
}
