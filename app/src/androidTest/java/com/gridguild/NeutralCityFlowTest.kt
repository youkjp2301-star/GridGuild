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
import com.gridguild.ui.clearDriverSceneMemoryCache
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
    private fun awaitScene(city: NeutralCity,level: Int) {
        ui.waitUntil(12_000){ui.onAllNodesWithTag("driverScene-${city.destinationId}-$level").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("neutralViewingLevel").assertTextContains("Lv.$level",substring=true)
        ui.onNodeWithTag("neutralLandmark").assertTextEquals(DriverScene(city,level).landmarkLabel)
        ui.onNodeWithContentDescription("도로 위 운전자 1인칭",substring=true).assertExists()
    }
    private fun choose(city: NeutralCity,level: Int=CityLevels.level(model.game.value.cityXp(model.game.value.districtId))) {
        ui.onNodeWithTag("neutralPlace-${city.destinationId}").performClick()
        awaitScene(city,level)
        assertEquals(city,NeutralCity.at(model.navigation.value.lat,model.navigation.value.lon))
    }
    private fun seedLevel(level: Int) {
        val xp=CityLevels.thresholds[level-1]
        runBlocking{store.update{it.copy(history=listOf(PointEvent("driver-scene-fixture","driver-scene-fixture","sasang",xp,1,
            kind="TEST",personalCredit=false,countsForMission=false)))}}
        ui.waitUntil(8_000){model.game.value.cityXp("sasang")==xp}
    }
    private fun inspectLevel(level: Int) {
        ui.onNodeWithTag("neutralLevels").performClick()
        ui.onNodeWithTag("neutral-level-$level").performScrollTo().assertIsEnabled().performClick()
    }
    private fun screenshot(name: String) {
        ui.waitUntil(5_000){ui.onAllNodesWithTag("xpReward").fetchSemanticsNodes().isEmpty()}
        // A scene's semantics can exist in its alpha=0 entrance frame. Finish that animation
        // and let the software renderer present it before capturing the system framebuffer.
        ui.mainClock.advanceTimeBy(500)
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(500)
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val focusedPackage=automation.rootInActiveWindow?.packageName?.toString()
        assertEquals("Do not save a scene obscured by System UI or another modal", "com.gridguild", focusedPackage)
        automation.takeScreenshot()?.let { bitmap ->
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
        ui.onNodeWithText("AI로 생성한 가상 재구성",substring=true).assertExists()
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
        awaitScene(NeutralCity.CENTUM,1)
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        ui.waitUntil(8_000){!model.neutralPreview.value}
        ui.onNodeWithTag("neutralScene").assertDoesNotExist()
    }
    @Test fun drivingAndUxRestrictionImmediatelyClosePreviewAndNeverSimulateActualNeutral() {
        enter();choose(NeutralCity.GWANGALLI)
        ui.activityRule.scenario.onActivity{model.mockPark(false)}
        ui.waitUntil(8_000){!model.neutralPreview.value}
        ui.onNodeWithTag("driverScene-gwangalli-1").assertDoesNotExist()
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
        awaitScene(NeutralCity.CENTUM,1)
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

    @Test fun bothDestinationsUseTheExactCurrentLevelTwoAndTwelveWithoutWritingRewards() {
        seedLevel(2)
        enter()
        val beforeTwo=runBlocking{store.state.first()}
        NeutralCity.entries.forEach { city ->
            choose(city,2)
            ui.onNodeWithTag("neutralGrowth").assertTextContains("Lv.2",substring=true)
            screenshot("driver-${city.destinationId}-lv02.png")
            ui.onNodeWithTag("neutralLevels").performClick()
            ui.onNodeWithTag("neutral-level-2").assertIsEnabled()
            ui.onNodeWithTag("neutral-level-3").assertIsNotEnabled()
            ui.onNodeWithTag("neutral-level-12").performScrollTo().assertIsNotEnabled()
            ui.onNodeWithText("닫기").performClick()
        }
        assertEquals(beforeTwo,runBlocking{store.state.first()})
        seedLevel(12)
        awaitScene(NeutralCity.CENTUM,12)
        val beforeTwelve=runBlocking{store.state.first()}
        NeutralCity.entries.forEach { city ->
            choose(city,12)
            ui.onNodeWithTag("neutralGrowth").assertTextContains("Lv.12",substring=true)
            screenshot("driver-${city.destinationId}-lv12.png")
            ui.onNodeWithTag("driverScene-${city.destinationId}-2").assertDoesNotExist()
        }
        assertEquals(beforeTwelve,runBlocking{store.state.first()})
        ui.onNodeWithTag("neutralExit").performClick()
    }

    @Test fun onlyUnlockedHistoryCanBeInspectedAndXpResetImmediatelyHidesHigherArtwork() {
        seedLevel(12)
        enter();choose(NeutralCity.GWANGALLI,12)
        val before=runBlocking{store.state.first()}
        inspectLevel(2);awaitScene(NeutralCity.GWANGALLI,2)
        ui.onNodeWithTag("neutralGrowth").assertTextContains("Lv.12",substring=true)
        ui.onNodeWithTag("neutralViewingLevel").assertTextContains("이전 도시 둘러보기",substring=true)
        choose(NeutralCity.CENTUM,2)
        assertEquals(before,runBlocking{store.state.first()})
        // The requested historical Lv.2 is no longer allowed after the owning XP falls to Lv.1.
        seedLevel(1)
        ui.waitForIdle()
        ui.onNodeWithTag("driverScene-centum-12").assertDoesNotExist()
        ui.onNodeWithTag("driverScene-centum-2").assertDoesNotExist()
        awaitScene(NeutralCity.CENTUM,1)
        ui.onNodeWithTag("neutralLevels").performClick()
        ui.onNodeWithTag("neutral-level-2").assertIsNotEnabled()
        ui.onNodeWithTag("neutral-level-12").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("닫기").performClick()
        ui.onNodeWithTag("neutralExit").performClick()
    }

    @Test fun districtChangesResolveThatDistrictsXpBeforeUsingAnyCachedImage() {
        seedLevel(12)
        enter();choose(NeutralCity.CENTUM,12)
        inspectLevel(2);awaitScene(NeutralCity.CENTUM,2)
        runBlocking{store.update{it.copy(districtId="haeundae")}}
        ui.waitUntil(8_000){model.game.value.districtId=="haeundae"}
        ui.waitForIdle()
        ui.onNodeWithTag("driverScene-centum-12").assertDoesNotExist()
        ui.onNodeWithTag("driverScene-centum-2").assertDoesNotExist()
        awaitScene(NeutralCity.CENTUM,1)
        ui.onNodeWithTag("neutralGrowth").assertTextContains("해운대구 · Lv.1",substring=true)
        runBlocking{store.update{it.copy(districtId="sasang")}}
        awaitScene(NeutralCity.CENTUM,12)
        ui.onNodeWithTag("neutralGrowth").assertTextContains("사상구 · Lv.12",substring=true)
        ui.onNodeWithTag("neutralExit").performClick()
    }

    @Test fun inFlightHigherLevelLoadCannotReturnAfterCityXpFalls() {
        ui.runOnIdle{clearDriverSceneMemoryCache()}
        seedLevel(12)
        enter()
        ui.onNodeWithTag("neutralPlace-centum").performClick()
        ui.waitUntil(8_000){!model.navigation.value.pending &&
            ui.onAllNodesWithTag("cityLoading").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("driverScene-centum-12").assertDoesNotExist()
        val resetAt=android.os.SystemClock.elapsedRealtime()
        seedLevel(2)
        ui.waitForIdle()
        ui.onNodeWithTag("driverScene-centum-12").assertDoesNotExist()
        awaitScene(NeutralCity.CENTUM,2)
        // Observe past the old cold presentation's deadline, not just the reset frame.
        ui.waitUntil(5_000){android.os.SystemClock.elapsedRealtime()-resetAt>=3_500}
        ui.onNodeWithTag("driverScene-centum-12").assertDoesNotExist()
        ui.onNodeWithTag("driverScene-centum-2").assertExists()
        ui.onNodeWithTag("neutralExit").performClick()
    }

    @Test fun allTwentyFourUnlockedRoadScenesKeepTheLedgerAndRelockAfterReset() {
        seedLevel(12)
        enter()
        val before=runBlocking{store.state.first()}
        NeutralCity.entries.forEach { city ->
            choose(city,12)
            (1..12).forEach { viewedLevel ->
                inspectLevel(viewedLevel)
                awaitScene(city,viewedLevel)
                ui.onNodeWithTag("neutralGrowth").assertTextContains("Lv.12",substring=true)
                screenshot("driver-all-${city.destinationId}-lv${viewedLevel.toString().padStart(2,'0')}.png")
                assertEquals("Viewing ${city.destinationId}/$viewedLevel must not write XP or missions",before,runBlocking{store.state.first()})
            }
        }
        // All future-era assets have now been decoded at least once. Cached art stays inaccessible.
        seedLevel(2)
        ui.waitForIdle()
        NeutralCity.entries.forEach { city ->(3..12).forEach { lockedLevel ->
            ui.onNodeWithTag("driverScene-${city.destinationId}-$lockedLevel").assertDoesNotExist()
        } }
        awaitScene(NeutralCity.CENTUM,2)
        screenshot("driver-all-reset-lv02.png")
        ui.onNodeWithTag("neutralLevels").performClick()
        ui.onNodeWithTag("neutral-level-3").assertIsNotEnabled()
        ui.onNodeWithTag("neutral-level-12").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("닫기").performClick()
        ui.onNodeWithTag("neutralExit").performClick()
    }
}
