package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.GameStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GridGuildFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun reset() {
        runBlocking { GameStore(InstrumentationRegistry.getInstrumentation().targetContext).update { GameState() } }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun setup() {
        compose.onNodeWithTag("start").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun missionWaypointRewardAndSafety() {
        setup()
        compose.onNodeWithTag("tab1").performClick()
        compose.onNodeWithTag("missionList").performScrollToNode(hasTestTag("waypoint-sasang-river"))
        compose.onNodeWithTag("waypoint-sasang-river").performClick()
        compose.onNodeWithTag("mission-sasang-river").performClick()
        compose.onNodeWithTag("missionList").performScrollToNode(hasTestTag("activeMission"))
        compose.onNodeWithTag("activeMission").assertExists()
        compose.onNodeWithTag("settings").performClick()
        compose.onNodeWithTag("charge").performClick()
        compose.onNodeWithTag("socPlus").performClick()
        compose.waitUntil(15_000) { runBlocking {
            GameStore(InstrumentationRegistry.getInstrumentation().targetContext).state.first().xp==120
        } }
        compose.onNodeWithTag("park").performClick()
        compose.onNodeWithTag("restricted").assertExists()
        compose.onNodeWithTag("tab2").assertDoesNotExist()
        compose.onNodeWithText("MOCK · P단으로 전환").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("tab2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab0").performClick()
        compose.onNodeWithText("120 XP",substring=false).assertExists()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            val file=java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"mock-mission-complete.png")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
    }
    @Test fun sdkModeNeverInventsValuesAndCanReturnToMock() {
        setup()
        compose.onNodeWithTag("settings").performClick()
        compose.onNodeWithTag("modePleos").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("restricted").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab2").assertDoesNotExist()
        compose.onNodeWithText("SDK 사용 불가 · MOCK 모드 선택").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("tab2").fetchSemanticsNodes().isNotEmpty() }
    }
}
