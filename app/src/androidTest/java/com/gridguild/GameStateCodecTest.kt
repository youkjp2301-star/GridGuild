package com.gridguild

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gridguild.core.*
import com.gridguild.data.GameStateCodec
import org.json.JSONObject
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameStateCodecTest {
    private fun legacy(): String = """{"schema":1,"district":"sasang","mode":"MOCK","history":[{"id":"sasang-river:legacy","mission":"sasang-river:legacy","district":"sasang","xp":120,"at":1793883600000,"kind":"MISSION","demo":true}]}"""

    @Test fun legacyDataMigratesWithoutLosingExistingPoints() {
        val state=GameStateCodec.decode(legacy())
        assertEquals(120,state.xp);assertEquals(120,state.cityXp("sasang"))
        assertTrue(state.history.single().countsForMission)
        val upgraded=GameStateCodec.encode(state)
        assertEquals(3,JSONObject(upgraded).getInt("schema"))
        assertEquals(state,GameStateCodec.decode(upgraded))
    }
    @Test fun resetFlagsAndMissionStateSurvivePersistence() {
        var state=GameStateCodec.decode(legacy()).copy(mission=Mission("gwangalli:test","gwangalli","sasang",120,42f,DataSource.MOCK,1793883600001))
        for(target in listOf(DemoResetTarget.CITY_LEVEL,DemoResetTarget.PERSONAL_POINTS,DemoResetTarget.CHARGING_MISSIONS)) {
            state=DemoResets.reset(state,target)
            val reopened=GameStateCodec.decode(GameStateCodec.encode(state))
            assertEquals(state,reopened)
            state=reopened
        }
        assertEquals(0,state.xp);assertEquals(0,state.cityXp("sasang"))
        assertNull(state.mission);assertTrue(state.completionHistory.isEmpty())
        assertEquals(120,state.history.single().xp)
    }
    @Test fun invalidSchemaOrMissingFlagsFailRatherThanRestoreClearedCredit() {
        val schema2=JSONObject(GameStateCodec.encode(GameStateCodec.decode(legacy())))
        schema2.getJSONArray("history").getJSONObject(0).remove("personalCredit")
        assertThrows(JSONException::class.java) { GameStateCodec.decode(schema2.toString()) }
        assertThrows(IllegalArgumentException::class.java) { GameStateCodec.decode(JSONObject(legacy()).put("schema",99).toString()) }
        assertThrows(JSONException::class.java) { GameStateCodec.decode("broken") }
    }
    @Test fun schema2RouteAndSchema3HomePreferencesAndPendingBaselineSurvive() {
        val old=JSONObject(GameStateCodec.encode(GameStateCodec.decode(legacy()))).put("schema",2)
        old.remove("home")
        val migrated=GameStateCodec.decode(old.toString())
        assertEquals(HomeChargePreferences(),migrated.homeCharge);assertEquals(120,migrated.xp)
        val state=migrated.copy(homeCharge=HomeChargePreferences("우리 집",80,450),
            mission=Mission("home:test","home-charge","sasang",120,42f,DataSource.MOCK,1,
                kind=MissionKind.HOME,windowStart=100,windowEnd=200,windowBaselineSoc=null,lastSampleAt=2))
        assertEquals(state,GameStateCodec.decode(GameStateCodec.encode(state)))
    }
}
