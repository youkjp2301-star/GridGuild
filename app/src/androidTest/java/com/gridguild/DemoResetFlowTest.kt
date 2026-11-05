package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.GameStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoResetFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private fun state()=runBlocking { store.state.first() }
    @Before fun setup() {
        val now=System.currentTimeMillis()
        runBlocking { store.update { GameState(districtId="sasang",
            mission=Mission("gwangalli:active","gwangalli","sasang",120,42f,DataSource.MOCK,now),
            history=listOf(PointEvent("one","sasang-river:old","sasang",120,now),PointEvent("two","centum:old","sasang",250,now),PointEvent("three","gwangalli:old","haeundae",100,now))) } }
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab2").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("settings").performClick()
    }
    private fun request(tag: String) { ui.onNodeWithTag(tag).performScrollTo().performClick() }
    private fun confirm() { ui.onNodeWithTag("confirmDemoReset").performClick();ui.waitForIdle() }
    private fun reopenAfterRecreation() {
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab2").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name)
                .outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun cityResetRelocksFutureArtAndKeepsPersonalPointsAndMission() {
        request("resetCityLevel")
        screenshot("reset-city-confirm.png")
        confirm()
        ui.waitUntil(8_000){state().cityXp("sasang")==0}
        assertEquals(470,state().xp);assertEquals(100,state().cityXp("haeundae"));assertNotNull(state().mission)
        assertTrue(state().completedToday("sasang-river",System.currentTimeMillis()))
        reopenAfterRecreation()
        ui.onNodeWithTag("tab2").performClick()
        ui.onNodeWithTag("cityStageLabel").assertTextContains("Lv.1",substring=true)
        ui.onNodeWithTag("cityCollection").performScrollTo().performScrollToNode(hasTestTag("city-stage-3"))
        ui.onNodeWithTag("city-stage-3").assertIsNotEnabled()
        assertEquals(470,state().xp)
    }
    @Test fun personalResetPersistsAndKeepsCityAndCompletedStations() {
        request("resetPoints");confirm()
        ui.waitUntil(8_000){state().xp==0}
        assertEquals(370,state().cityXp("sasang"));assertNotNull(state().mission)
        assertTrue(state().completedToday("sasang-river",System.currentTimeMillis()))
        reopenAfterRecreation()
        ui.onNodeWithTag("tab0").performClick()
        ui.onNodeWithText("0 XP",substring=false).assertExists()
        assertEquals(370,state().cityXp("sasang"))
    }
    @Test fun missionResetAllowsAnotherChargeRewardWithoutErasingExistingXp() {
        request("resetMissions");confirm()
        ui.waitUntil(8_000){state().mission==null && state().completionHistory.isEmpty()}
        assertEquals(470,state().xp);assertEquals(370,state().cityXp("sasang"))
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag("mission-sasang-river"))
        ui.onNodeWithTag("mission-sasang-river").assertIsEnabled().performClick()
        ui.waitUntil(8_000){state().mission?.stationId=="sasang-river"}
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("charge").performClick()
        ui.onNodeWithTag("socPlus").performClick()
        ui.waitUntil(8_000){state().xp==590}
        assertEquals(490,state().cityXp("sasang"));assertNull(state().mission)
        reopenAfterRecreation()
        assertEquals(590,state().xp)
    }
    @Test fun confirmationCanBeCancelledAndFullResetStillStartsOver() {
        val original=state()
        for(tag in listOf("resetCityLevel","resetMissions","resetPoints","resetAll")) {
            request(tag);ui.onNodeWithTag("cancelDemoReset").performClick()
            assertEquals(original,state())
        }
        request("resetAll")
        ui.onNodeWithTag("cancelDemoReset").performClick()
        ui.onNodeWithTag("resetCityLevel").performScrollTo()
        screenshot("reset-controls.png")
        request("resetAll");confirm()
        ui.waitUntil(8_000){ui.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(GameState(),state())
    }
}
