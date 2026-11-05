package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.GameStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiRegressionTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Before fun setup() {
        runBlocking { store.update{GameState(districtId="sasang")} }
        ui.waitUntil(15_000) { ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            val file=java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name)
            file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun freshBatteryAndMissionButtonStayStableAcrossHeartbeats() {
        ui.onNodeWithTag("tab0").performClick()
        repeat(14) { ui.onNodeWithTag("batteryStatus").assertTextContains("데모 데이터 정상",substring=true);Thread.sleep(110) }
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag("mission-sasang-river"))
        repeat(36) {
            ui.onNodeWithTag("mission-sasang-river").assertIsEnabled().assertTextContains("미션 시작")
            Thread.sleep(120)
        }
        screenshot("missions-stable-v2.png")
        ui.onNodeWithTag("mission-sasang-river").performClick()
        ui.waitUntil(8_000) { runBlocking {store.state.first().mission!=null} }
        ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag("mission-sasang-river"))
        ui.onNodeWithTag("mission-sasang-river").assertIsNotEnabled().assertTextContains("미션 진행 중")
    }
    @Test fun previewChangesSceneAndNeverChangesXp() {
        ui.onNodeWithTag("tab2").performClick()
        ui.onNodeWithTag("cityStageLabel").assertTextContains("Lv.1",substring=true)
        screenshot("city-current-v2.png")
        ui.onNodeWithTag("cityPreview").performClick()
        ui.onNodeWithTag("cityStageLabel").assertTextContains("Lv.2",substring=true)
        ui.onNodeWithContentDescription("사상구 Lv.2 성벽 도시 · 성문과 돌다리").assertExists()
        screenshot("city-next-v2.png")
        ui.onNodeWithTag("city-stage-12").performScrollTo().performClick()
        ui.onNodeWithTag("cityStageLabel").performScrollTo().assertTextContains("Lv.12",substring=true)
        screenshot("city-future-v2.png")
        assertEquals(0,runBlocking{store.state.first().xp})
        ui.onNodeWithTag("cityCurrent").performScrollTo().performClick()
        ui.onNodeWithTag("cityStageLabel").assertTextContains("Lv.1",substring=true)
    }
    @Test fun modeBadgeExplainsMockAndRepeatedSelectionKeepsMission() {
        ui.onNodeWithTag("modeInfo").performClick()
        ui.onNodeWithText("데모 모드란?").assertExists()
        screenshot("mock-explanation-v2.png")
        ui.onNodeWithTag("openModeControls").performClick()
        ui.onNodeWithTag("charge").assertExists()
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag("mission-sasang-river"))
        ui.onNodeWithTag("mission-sasang-river").performClick()
        ui.waitUntil(8_000){runBlocking{store.state.first().mission!=null}}
        val id=runBlocking{store.state.first().mission!!.id}
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("modeMock").performClick()
        ui.waitForIdle()
        assertEquals(id,runBlocking{store.state.first().mission!!.id})
    }
}
