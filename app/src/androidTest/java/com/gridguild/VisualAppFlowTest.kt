package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
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
class VisualAppFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab0").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun screenshot(name: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun enlargedHomeAndConstructionPreviewRunInTheInstalledApp() {
        ui.onNodeWithTag("tab0").performClick()
        ui.onNodeWithTag("homeCityHero").assertIsDisplayed()
        screenshot("v070-home.png")
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("loadingPreview").performScrollTo().performClick()
        ui.onNodeWithTag("cityLoading").assertIsDisplayed()
        Thread.sleep(2_000)
        screenshot("v070-loading.png")
        ui.onNodeWithText("닫기").performClick()
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("tab2").performClick()
        screenshot("v070-city.png")
        assertEquals(0,runBlocking{store.state.first().xp})
    }
    @Test fun realHomeMissionShowsRewardOnceAndResetStillLocksFutureEra() {
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("homeJoin").performScrollTo().performClick()
        ui.onNodeWithTag("homeRegisterSave").performClick()
        ui.waitUntil(8_000){runBlocking{store.state.first().homeCharge.chargerName!=null}}
        ui.onNodeWithTag("homeJoin").performScrollTo().performClick()
        ui.waitUntil(8_000){runBlocking{store.state.first().mission!=null}}
        ui.mainClock.autoAdvance=false
        ui.activityRule.scenario.onActivity{ViewModelProvider(it)[GridViewModel::class.java].let{model->model.mockCharge(true);model.mockSoc(44f)}}
        ui.waitUntil(8_000){runBlocking{store.state.first().xp==120}}
        ui.waitUntil(8_000){
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithTag("xpReward").fetchSemanticsNodes().isNotEmpty()
        }
        ui.mainClock.advanceTimeBy(450)
        ui.onNodeWithTag("xpReward").assertExists()
        screenshot("v070-xp-flight.png")
        ui.mainClock.autoAdvance=true
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab0").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("xpReward").assertDoesNotExist()
        assertEquals(120,runBlocking{store.state.first().xp})
        runBlocking{store.update{it.copy(history=listOf(PointEvent("era-test","era-test","sasang",4000,1)))}}
        ui.onNodeWithTag("tab2").performClick()
        // Returning to a tab preserves a previously viewed, already unlocked city.
        if(ui.onAllNodesWithTag("cityCurrent").fetchSemanticsNodes().isNotEmpty())
            ui.onNodeWithTag("cityCurrent").performScrollTo().performClick()
        ui.waitUntil(8_000){ui.onAllNodesWithContentDescription("사상구 Lv.11 하이테크 미래도시 · 하늘 정원 타워").fetchSemanticsNodes().isNotEmpty()}
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.waitUntil(8_000){ui.onAllNodesWithText("Lv.1 · 중세 마을").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("사상구 Lv.11 하이테크 미래도시 · 하늘 정원 타워").assertDoesNotExist()
        ui.onNodeWithTag("cityStageLabel").assertTextContains("Lv.1",substring=true)
    }
}
