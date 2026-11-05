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
class NavigationFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("routeCharge").performClick()
        show("navTools");ui.onNodeWithTag("navTools").performClick()
    }
    private fun show(tag: String)=ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag(tag))
    private fun result(text: String) { ui.waitUntil(8_000){ui.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()} }
    @Test fun bothButtonsShowProgressAndProduceVisibleResultsEveryTime() {
        show("navRefresh")
        repeat(2){i ->
            ui.onNodeWithTag("navRefresh").performClick()
            ui.onNodeWithTag("navReroute").assertIsNotEnabled()
            result("데모 목적지 갱신 완료")
            ui.onNodeWithTag("navRequest").assertTextContains("요청 #${i+1}",substring=true)
        }
        ui.onNodeWithTag("navReroute").performClick()
        ui.onNodeWithTag("navRefresh").assertIsNotEnabled()
        result("데모 재탐색 완료")
        ui.onNodeWithTag("navRequest").assertTextContains("요청 #3",substring=true)
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"navigation-fixed.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
        assertEquals(0,runBlocking{store.state.first().xp})
    }
    @Test fun refreshKeepsTheSelectedDestinationAndItsNearbyRecommendations() {
        show("changeDemoDestination");ui.onNodeWithTag("changeDemoDestination").performClick()
        ui.onNodeWithTag("demo-destination-centum").performClick()
        show("navStatus");result("데모 목적지 설정 완료")
        show("destination-centum");ui.onNodeWithTag("destination-centum").performClick()
        show("navStatus");result("데모 목적지 설정 완료")
        ui.onNodeWithTag("navDestination").assertTextContains("센텀 녹색 충전소",substring=true)
        ui.onNodeWithTag("navRefresh").performClick();result("데모 목적지 갱신 완료")
        ui.onNodeWithTag("navDestination").assertTextContains("센텀 녹색 충전소",substring=true)
        ui.onNodeWithTag("navReroute").performClick();result("데모 재탐색 완료")
        ui.onNodeWithTag("navDestination").assertTextContains("센텀 녹색 충전소",substring=true)
    }
}
