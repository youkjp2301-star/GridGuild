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
class DestinationDemoFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("routeCharge").performClick()
    }
    private fun show(tag: String)=ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag(tag))
    private fun select(id: String) {
        show("changeDemoDestination");ui.onNodeWithTag("changeDemoDestination").performClick()
        ui.onNodeWithTag("demo-destination-$id").performScrollTo().performClick()
        ui.waitUntil(8_000){ui.onAllNodesWithText("데모 목적지 설정 완료",substring=true).fetchSemanticsNodes().isNotEmpty()}
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name)
                .outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun changingDestinationAutomaticallyChangesVisibleNearbyStationsAndMap() {
        show("nearbySummary");ui.onNodeWithTag("nearbySummary").assertTextContains("2곳",substring=true)
        show("nearbyMap")
        ui.onNodeWithTag("nearbyMap").assertExists()
        screenshot("destination-samnak.png")
        select("centum")
        show("nearbySummary");ui.onNodeWithTag("nearbySummary").assertTextContains("4곳",substring=true)
        show("nearbyMap")
        screenshot("destination-centum.png")
        show("mission-centum");ui.onNodeWithTag("mission-centum").assertExists()
        ui.onNodeWithTag("mission-sasang-river").assertDoesNotExist()
        select("gijang")
        show("nearbyReason");ui.onNodeWithTag("nearbyReason").assertTextContains("샘플 충전소가 없습니다",substring=true)
        screenshot("destination-empty.png")
        ui.onNodeWithTag("mission-centum").assertDoesNotExist()
        assertEquals(0,runBlocking{store.state.first().xp})
    }
    @Test fun randomDestinationChangesLocationAndDoesNotCancelAnActiveMission() {
        show("mission-sasang-river");ui.onNodeWithTag("mission-sasang-river").performClick()
        ui.waitUntil(8_000){runBlocking{store.state.first().mission!=null}}
        val original=runBlocking{store.state.first()}
        show("changeDemoDestination");ui.onNodeWithTag("changeDemoDestination").performClick()
        screenshot("destination-picker.png")
        ui.onNodeWithTag("randomDemoDestination").performClick()
        ui.waitUntil(8_000){ui.onAllNodesWithText("데모 목적지 설정 완료",substring=true).fetchSemanticsNodes().isNotEmpty()}
        val text=ui.onNodeWithTag("navDestination").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString{it.text}
        assertFalse(text.contains("삼락생태공원"))
        assertEquals(original,runBlocking{store.state.first()})
        show("activeMission");ui.onNodeWithTag("activeMission").assertExists()
        if(!text.contains("기장"))select("gijang")
        show("activeMission");ui.onNodeWithText("삼락 강변 충전소",substring=false).assertExists()
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("charge").performClick();ui.onNodeWithTag("socPlus").performClick()
        ui.waitUntil(8_000){runBlocking{store.state.first().xp==120}}
        assertEquals(120,runBlocking{store.state.first().cityXp("sasang")})
    }
    @Test fun highSocSuspensionDoesNotClaimTheAreaHasNoStations() {
        ui.activityRule.scenario.onActivity { androidx.lifecycle.ViewModelProvider(it)[GridViewModel::class.java].mockSoc(90f) }
        // Compact QA displays do not compose the lower LazyColumn section until it is visible.
        show("nearbySummary")
        ui.waitUntil(8_000){ui.onAllNodesWithText("충전 추천 중지").fetchSemanticsNodes().isNotEmpty()}
        show("nearbyReason");ui.onNodeWithTag("nearbyReason").assertTextContains("SOC 90%",substring=true)
        ui.onNodeWithTag("nearbyMap").assertDoesNotExist()
        ui.activityRule.scenario.onActivity { androidx.lifecycle.ViewModelProvider(it)[GridViewModel::class.java].mockSoc(89f) }
        show("nearbySummary")
        ui.waitUntil(8_000){ui.onAllNodesWithText("목적지 주변 충전소 · 2곳").fetchSemanticsNodes().isNotEmpty()}
        show("nearbyMap");ui.onNodeWithTag("nearbyMap").assertExists()
    }
}
