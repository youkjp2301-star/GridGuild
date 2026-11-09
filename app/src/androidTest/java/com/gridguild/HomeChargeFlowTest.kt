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
class HomeChargeFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private fun state()=runBlocking{store.state.first()}
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick()
    }
    private fun register() {
        ui.onNodeWithTag("homeJoin").performScrollTo().performClick()
        ui.onNodeWithTag("homeRegisterSave").performClick()
        ui.waitUntil(8_000){state().homeCharge.chargerName!=null}
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun registerOnceJoinAndChargeAutomaticallyRewardsBothBalancesAndCity() {
        register()
        ui.onNodeWithTag("homeSoc").assertTextContains("42%")
        screenshot("home-charge-main.png")
        ui.onNodeWithTag("homeJoin").performScrollTo().assertTextContains("충전 미션 참여하기").performClick()
        ui.waitUntil(8_000){state().mission?.kind==MissionKind.HOME}
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("charge").performClick();ui.onNodeWithTag("socPlus").performClick()
        ui.waitUntil(8_000){state().xp==120}
        assertEquals(120,state().cityXp("sasang"));assertEquals(2,CityLevels.level(state().cityXp("sasang")))
        assertEquals("HOME",state().history.single().kind);assertNull(state().mission)
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("homeChargerName").assertTextContains("우리 집 충전소")
        ui.onNodeWithTag("homeJoin").performScrollTo().assertIsNotEnabled().assertTextContains("오늘 보상 완료")
        assertEquals(120,state().xp)
    }
    @Test fun optionalAdvancedPreferencesAreSavedAndReused() {
        register()
        ui.onNodeWithTag("homeAdvanced").performScrollTo().performClick()
        ui.onNodeWithTag("homeTargetSoc").performTextInput("80")
        ui.onNodeWithTag("homeDeparture").performTextInput("07:30")
        screenshot("home-charge-advanced.png")
        ui.onNodeWithTag("homeAdvancedSave").performClick()
        ui.waitUntil(8_000){state().homeCharge.targetSoc==80}
        assertEquals(450,state().homeCharge.departureMinutes)
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("homeAdvanced").performClick()
        ui.onNodeWithTag("homeTargetSoc").assertTextContains("80")
        ui.onNodeWithTag("homeDeparture").assertTextContains("07:30")
    }
    @Test fun lowBatteryUsesChargeNowAndHighSocDoesNotPromoteExtraCharging() {
        register()
        ui.activityRule.scenario.onActivity{androidx.lifecycle.ViewModelProvider(it)[GridViewModel::class.java].mockSoc(20f)}
        ui.waitUntil(8_000){ui.onAllNodesWithText("20%").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("homeChargePriority").assertTextContains("충전을 미루지",substring=true)
        ui.onNodeWithTag("homeJoin").performScrollTo().assertIsEnabled()
        ui.activityRule.scenario.onActivity{androidx.lifecycle.ViewModelProvider(it)[GridViewModel::class.java].mockSoc(90f)}
        ui.waitUntil(8_000){ui.onAllNodesWithText("90%").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("homeJoin").assertIsNotEnabled().assertTextContains("배터리가 충분해요")
    }
}
