package com.gridguild

import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
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
class NeutralCityFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var model: GridViewModel
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.activityRule.scenario.onActivity { model=ViewModelProvider(it)[GridViewModel::class.java] }
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty() && model.gameAllowed.value}
    }
    private fun enter() {
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("neutralTest").performScrollTo().performClick()
        ui.waitUntil(10_000){ui.onAllNodesWithTag("neutralScene").fetchSemanticsNodes().isNotEmpty()}
        // Android's one-time immersive-mode education overlays app input on a fresh emulator.
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val started=android.os.SystemClock.elapsedRealtime()
        ui.waitUntil(4_000) {
            val roots=listOfNotNull(automation.rootInActiveWindow)+automation.windows.mapNotNull{it.root}
            val button=roots.flatMap{it.findAccessibilityNodeInfosByText("Got it")}.firstOrNull()
            if(button!=null) {button.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);true}
            else android.os.SystemClock.elapsedRealtime()-started>1_200
        }
        ui.waitUntil(8_000){model.gameAllowed.value &&
            (ui.onAllNodesWithTag("neutralLocation").fetchSemanticsNodes().isNotEmpty() ||
                ui.onAllNodesWithText("어느 도시를 둘러볼까요?").fetchSemanticsNodes().isNotEmpty())}
    }
    private fun choose(city: NeutralCity) {
        ui.onNodeWithTag("neutralPlace-${city.destinationId}").performClick()
        ui.waitUntil(12_000){ui.onAllNodesWithTag("neutralPhoto-${city.destinationId}").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("neutralLandmark").assertTextEquals(city.landmark)
        assertEquals(city,NeutralCity.at(model.navigation.value.lat,model.navigation.value.lon))
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name)
                .outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun testButtonOpensWideLandmarksAndSwitchesExistingNavigationWithoutChangingRewardsOrMission() {
        val station=SampleData.stations.first{it.id=="sasang-river"}
        ui.activityRule.scenario.onActivity{model.startMission(station)}
        ui.waitUntil(8_000){model.game.value.mission!=null}
        val before=runBlocking{store.state.first()}
        ui.onNodeWithTag("tab1").performClick();ui.onNodeWithTag("routeCharge").performClick()
        enter()
        ui.onNodeWithText("어느 도시를 둘러볼까요?").assertExists()
        assertEquals(Configuration.ORIENTATION_LANDSCAPE,ui.activity.resources.configuration.orientation)
        choose(NeutralCity.GWANGALLI);screenshot("neutral-gwangalli.png")
        assertTrue(model.safety.value.parked);assertTrue(model.safety.value.uxAllowed)
        ui.onNodeWithText("N · TEST").assertExists()
        choose(NeutralCity.CENTUM);screenshot("neutral-centum.png")
        ui.onNodeWithTag("neutralCredits").performClick()
        ui.onNodeWithText("399scout",substring=true).assertExists()
        ui.onNodeWithText("닫기").performClick()
        assertEquals(before,runBlocking{store.state.first()})
        ui.onNodeWithTag("neutralExit").performClick()
        ui.waitUntil(8_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("routeCharge").assertIsSelected()
        ui.onNodeWithTag("neutralScene").assertDoesNotExist()
        // Existing route actions and the active mission still exist after immersive UI disposal.
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag("activeMission"))
        ui.onNodeWithTag("activeMission").assertExists()
        assertEquals(before,runBlocking{store.state.first()})
    }
    @Test fun existingDestinationImmediatelySelectsItsLandmarkAndBackReturnsToApp() {
        ui.activityRule.scenario.onActivity{model.changeDemoDestination(NeutralCity.CENTUM.destination)}
        ui.waitUntil(8_000){NeutralCity.at(model.navigation.value.lat,model.navigation.value.lon)==NeutralCity.CENTUM}
        enter()
        ui.waitUntil(12_000){ui.onAllNodesWithTag("neutralPhoto-centum").fetchSemanticsNodes().isNotEmpty()}
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitUntil(8_000){!model.neutralPreview.value}
        ui.onNodeWithTag("neutralScene").assertDoesNotExist()
    }
    @Test fun drivingAndUxRestrictionImmediatelyClosePreviewAndNeverSimulateActualNeutral() {
        enter();choose(NeutralCity.GWANGALLI)
        ui.activityRule.scenario.onActivity{model.mockPark(false)}
        ui.waitUntil(8_000){!model.neutralPreview.value}
        ui.onNodeWithTag("neutralPhoto-gwangalli").assertDoesNotExist()
        ui.activityRule.scenario.onActivity{model.enterNeutralPreview()}
        assertFalse(model.neutralPreview.value)
        ui.activityRule.scenario.onActivity{model.mockPark(true)}
        ui.waitUntil(8_000){model.gameAllowed.value && ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        enter();choose(NeutralCity.CENTUM)
        ui.activityRule.scenario.onActivity{model.mockUx(false)}
        ui.waitUntil(8_000){!model.neutralPreview.value}
        ui.onNodeWithTag("neutralScene").assertDoesNotExist()
        ui.activityRule.scenario.onActivity{model.mockUx(true)}
    }
    @Test fun recreationKeepsPreviewButBackgroundAndSdkModeCannotKeepItOpen() {
        enter();choose(NeutralCity.CENTUM)
        ui.activityRule.scenario.recreate()
        ui.waitUntil(12_000){ui.onAllNodesWithTag("neutralPhoto-centum").fetchSemanticsNodes().isNotEmpty()}
        ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(model.neutralPreview.value)
        ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        ui.waitUntil(8_000){model.gameAllowed.value}
        ui.onNodeWithTag("neutralScene").assertDoesNotExist()
        ui.activityRule.scenario.onActivity{model.setMode(AppMode.PLEOS)}
        ui.waitUntil(8_000){model.game.value.mode==AppMode.PLEOS}
        ui.activityRule.scenario.onActivity{model.enterNeutralPreview()}
        assertFalse(model.neutralPreview.value)
        ui.activityRule.scenario.onActivity{model.setMode(AppMode.MOCK)}
    }
}
