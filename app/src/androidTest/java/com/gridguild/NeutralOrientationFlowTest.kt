package com.gridguild

import android.app.UiAutomation
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.GameStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NeutralOrientationFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val automation get()=InstrumentationRegistry.getInstrumentation().uiAutomation
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var model: GridViewModel
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.activityRule.scenario.onActivity{model=ViewModelProvider(it)[GridViewModel::class.java]}
        ui.waitUntil(15_000){model.gameAllowed.value && model.ready.value}
    }
    @After fun releaseRotation(){automation.setRotation(UiAutomation.ROTATION_UNFREEZE)}
    private fun shell(command: String): String=android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
        .bufferedReader().use{it.readText()}
    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500);ui.waitForIdle();android.os.SystemClock.sleep(500)
        ui.waitUntil(5_000){automation.rootInActiveWindow?.packageName?.toString()=="com.gridguild"}
        val bitmap=checkNotNull(automation.takeScreenshot())
        assertTrue(bitmap.width>bitmap.height)
        java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),name)
            .outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    @Test fun bothLandscapeDirectionsKeepTheirRotationAndRestoreTheirRequest() {
        listOf(UiAutomation.ROTATION_FREEZE_90 to NeutralCity.GWANGALLI,
            UiAutomation.ROTATION_FREEZE_270 to NeutralCity.CENTUM).forEachIndexed { index,(rotation,city) ->
            assertTrue(automation.setRotation(rotation))
            ui.waitUntil(15_000){ui.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE &&
                ui.activity.windowManager.defaultDisplay.rotation==rotation}
            val beforeRotation=ui.activity.windowManager.defaultDisplay.rotation
            val beforeRequest=ui.activity.requestedOrientation
            val level=if(index==0)2 else 12
            runBlocking{store.update{it.copy(history=listOf(PointEvent("orientation-$index","orientation-$index","sasang",
                CityLevels.thresholds[level-1],System.currentTimeMillis(),kind="TEST",personalCredit=false,countsForMission=false)))}}
            ui.waitUntil(8_000){CityLevels.level(model.game.value.cityXp("sasang"))==level}
            ui.activityRule.scenario.onActivity{model.changeDemoDestination(city.destination)}
            ui.waitUntil(8_000){!model.navigation.value.pending && NeutralCity.at(model.navigation.value.lat,model.navigation.value.lon)==city}
            val before=runBlocking{store.state.first()}
            ui.activityRule.scenario.onActivity{model.enterNeutralPreview()}
            ui.waitUntil(12_000){ui.onAllNodesWithTag("driverScene-${city.destinationId}-$level").fetchSemanticsNodes().isNotEmpty()}
            assertEquals(beforeRotation,ui.activity.windowManager.defaultDisplay.rotation)
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED,ui.activity.requestedOrientation)
            screenshot("showcase-neutral-rotation-$rotation.png")
            assertTrue(automation.setRotation(UiAutomation.ROTATION_FREEZE_0))
            ui.mainClock.advanceTimeBy(500);ui.waitForIdle();android.os.SystemClock.sleep(500)
            assertEquals(Configuration.ORIENTATION_LANDSCAPE,ui.activity.resources.configuration.orientation)
            assertEquals(beforeRotation,ui.activity.windowManager.defaultDisplay.rotation)
            assertEquals(before,runBlocking{store.state.first()})
            assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
            ui.waitUntil(8_000){!model.neutralPreview.value && ui.onAllNodesWithTag("neutralScene").fetchSemanticsNodes().isEmpty()}
            assertEquals(beforeRequest,ui.activity.requestedOrientation)
        }
    }
    @Test fun portraitEntryCanChooseEitherLandscapeAndExitingRestoresTheOriginalRequest() {
        automation.setRotation(UiAutomation.ROTATION_FREEZE_0)
        ui.waitUntil(15_000){ui.activity.resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT}
        val original=ui.activity.requestedOrientation
        ui.activityRule.scenario.onActivity{model.enterNeutralPreview()}
        ui.waitUntil(15_000){ui.activity.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE}
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,ui.activity.requestedOrientation)
        ui.onNodeWithTag("neutralExit").performClick()
        ui.waitUntil(8_000){ui.activity.requestedOrientation==original}
        assertFalse(model.neutralPreview.value)
    }
    @Test fun resizedAvdKeepsPhysicalLandscapeWithoutChangingItsDisplaySettings() {
        val size=Regex("Override size: (\\d+x\\d+)").find(shell("wm size"))?.groupValues?.get(1) ?: "reset"
        val density=Regex("Override density: (\\d+)").find(shell("wm density"))?.groupValues?.get(1) ?: "reset"
        try {
            shell("wm size 1280x800");shell("wm density 160")
            assertTrue(automation.setRotation(UiAutomation.ROTATION_FREEZE_90))
            ui.waitUntil(15_000){ui.activity.resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT &&
                ui.activity.windowManager.defaultDisplay.rotation==android.view.Surface.ROTATION_90}
            val original=ui.activity.requestedOrientation
            ui.activityRule.scenario.onActivity{model.changeDemoDestination(NeutralCity.GWANGALLI.destination)}
            ui.waitUntil(8_000){!model.navigation.value.pending && NeutralCity.at(model.navigation.value.lat,model.navigation.value.lon)==NeutralCity.GWANGALLI}
            ui.activityRule.scenario.onActivity{model.enterNeutralPreview()}
            ui.waitUntil(12_000){ui.onAllNodesWithTag("driverScene-gwangalli-1").fetchSemanticsNodes().isNotEmpty()}
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED,ui.activity.requestedOrientation)
            ui.mainClock.advanceTimeBy(500);ui.waitForIdle();android.os.SystemClock.sleep(500)
            assertEquals(android.view.Surface.ROTATION_90,ui.activity.windowManager.defaultDisplay.rotation)
            assertEquals(Configuration.ORIENTATION_PORTRAIT,ui.activity.resources.configuration.orientation)
            assertTrue(shell("wm size").contains("Override size: 1280x800"))
            ui.onNodeWithTag("neutralExit").performClick()
            ui.waitUntil(8_000){!model.neutralPreview.value && ui.activity.requestedOrientation==original}
        } finally {
            shell("wm size $size");shell("wm density $density")
        }
    }
}
