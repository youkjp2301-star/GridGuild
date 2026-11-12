package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.GameStore
import com.gridguild.data.DemoAvailabilityScenario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChargingLeisureFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var model: GridViewModel
    private val station=SampleData.stations.first{it.id=="sasang-river"}
    private fun state()=runBlocking{store.state.first()}

    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.activityRule.scenario.onActivity{model=ViewModelProvider(it)[GridViewModel::class.java]}
        ui.waitUntil(15_000){model.gameAllowed.value && model.game.value.districtId=="sasang" &&
            model.game.value.mode==AppMode.MOCK && ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick();ui.onNodeWithTag("routeCharge").performClick()
    }
    private fun routeClick(tag: String) {
        ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag(tag))
        ui.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performClick()
    }
    private fun open(id: String=station.id) {
        routeClick("leisure-$id")
        ui.waitUntil(8_000){ui.onAllNodesWithTag("chargingLeisureSheet").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun show(tag: String) {
        ui.onNodeWithTag("leisureList").performScrollToNode(hasTestTag(tag))
        ui.onNodeWithTag(tag).performScrollTo()
    }
    private fun filter(category: LeisureCategory?) {
        val tag="leisureFilter-${category?.name ?: "ALL"}"
        show(tag);ui.onNodeWithTag(tag).performClick().assertIsSelected()
    }
    private fun screenshot(name: String) {
        ui.mainClock.advanceTimeBy(500);ui.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync();android.os.SystemClock.sleep(300)
        val automation=instrumentation.uiAutomation
        // IME dismissal can briefly detach the accessibility root of the sheet's window.
        ui.waitUntil(5_000){automation.rootInActiveWindow?.packageName?.toString()=="com.gridguild"}
        assertEquals("Screenshots must not be obscured by a system modal","com.gridguild",automation.rootInActiveWindow?.packageName?.toString())
        val bitmap=checkNotNull(automation.takeScreenshot())
        java.io.File(instrumentation.targetContext.getExternalFilesDir(null),name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        bitmap.recycle()
    }

    @Test fun allFiltersDetailsAndAdvertisementRemainFictionalAndReadOnly() {
        val before=state();open()
        ui.onNodeWithTag("leisureEstimate").assertTextEquals("시간 정보 없음")
        ui.onNodeWithTag("leisureStation").assertTextEquals(station.name)
        show("leisureDistanceNote");ui.onNodeWithTag("leisureDistanceNote").assertTextContains("도보 시간 미확인",substring=true)
        LeisureCategory.entries.forEach { category ->
            filter(category)
            val place=MockChargingLeisurePlaces.forStation(station).first{it.category==category}
            show("leisureCard-${place.id}")
            ui.onNodeWithTag("leisureCard-${place.id}").assertExists()
        }
        filter(LeisureCategory.CAFE)
        val cafe=MockChargingLeisurePlaces.forStation(station).first{it.category==LeisureCategory.CAFE && !it.isAdvertisement}
        show("leisureDetail-${cafe.id}");ui.onNodeWithTag("leisureDetail-${cafe.id}").performClick()
        ui.onNodeWithTag("leisurePlaceName").assertTextEquals(cafe.name)
        show("leisureCoordinates");ui.onNodeWithTag("leisureCoordinates").assertTextContains("가상 위치",substring=true)
        screenshot("leisure-place-detail.png")
        ui.onNodeWithTag("leisureBack").performClick()
        val ad=MockChargingLeisurePlaces.forStation(station).first{it.isAdvertisement}
        show("leisureDetail-${ad.id}");ui.onNodeWithTag("leisureDetail-${ad.id}").performClick()
        ui.onNodeWithText("광고").assertExists()
        show("leisureBenefit");ui.onNodeWithText("예시 혜택 · 발급 불가").assertExists()
        screenshot("leisure-advertisement.png")
        assertEquals(before,state())
        ui.onNodeWithTag("leisureClose").performClick()
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000){model.gameAllowed.value}
        assertEquals(before,state())
    }

    @Test fun explicitDemoInputsProduceTwentyMinutesAndMissingPowerFallsBackWithoutWriting() {
        val before=state();open()
        show("leisureConditions");ui.onNodeWithTag("leisureConditions").performClick()
        show("leisureTarget");ui.onNodeWithTag("leisureTarget").performTextReplacement("62")
        ui.onNodeWithTag("leisureTarget").performImeAction()
        show("leisureEstimate");ui.onNodeWithTag("leisureEstimate").assertTextEquals("약 20분")
        ui.onNodeWithTag("leisureTimeAdvice").assertTextContains("15분 이하",substring=true)
        show("leisureConditions");ui.onNodeWithTag("leisureConditions").performClick()
        show("leisureEstimate");screenshot("leisure-estimate-20.png")
        filter(null)
        val cafe=MockChargingLeisurePlaces.forStation(station).first{it.category==LeisureCategory.CAFE}
        show("leisureCard-${cafe.id}");screenshot("leisure-twenty-minute-recommendations.png")
        show("leisureConditions");ui.onNodeWithTag("leisureConditions").performClick()
        show("leisurePower");ui.onNodeWithTag("leisurePower").performTextClearance()
        ui.onNodeWithTag("leisurePower").performImeAction()
        show("leisureEstimate");ui.onNodeWithTag("leisureEstimate").assertTextEquals("시간 정보 없음")
        show("leisureCard-${cafe.id}");ui.onNodeWithTag("leisureCard-${cafe.id}").assertExists()
        assertEquals(before,state())
        ui.onNodeWithTag("leisureClose").performClick()
    }

    @Test fun explorationKeepsTheRouteMissionAndItsOriginalXpAward() {
        ui.activityRule.scenario.onActivity{model.setDemoAvailability(station.id,DemoAvailabilityScenario.FREE)}
        routeClick("mission-${station.id}")
        ui.waitUntil(8_000){state().mission?.stationId==station.id}
        ui.waitForIdle()
        val started=state()
        open();filter(LeisureCategory.WALK_PARK)
        val park=MockChargingLeisurePlaces.forStation(station).first{it.category==LeisureCategory.WALK_PARK}
        show("leisureDetail-${park.id}");ui.onNodeWithTag("leisureDetail-${park.id}").performClick()
        assertEquals(started,state())
        ui.onNodeWithTag("leisureClose").performClick()
        ui.onNodeWithTag("settings").performClick()
        ui.onNodeWithTag("charge").performScrollTo().performClick()
        ui.onNodeWithTag("socPlus").performScrollTo().performClick()
        ui.waitUntil(8_000){state().xp==120 && state().mission==null}
        assertEquals(120,state().cityXp("sasang"));assertEquals(2,CityLevels.level(state().cityXp("sasang")))
        assertEquals(1,state().completionHistory.size)
    }

    @Test fun changingDestinationUsesTheNewStationAndUxRestrictionClosesTheSheet() {
        open()
        ui.activityRule.scenario.onActivity{model.changeDemoDestination(DemoDestinations.places.first{it.id=="centum"})}
        ui.waitUntil(8_000){!model.navigation.value.pending && model.navigation.value.destination.contains("센텀")}
        ui.waitUntil(8_000){ui.onAllNodesWithTag("chargingLeisureSheet").fetchSemanticsNodes().isEmpty()}
        open("centum")
        ui.onNodeWithTag("leisureStation").assertTextEquals("센텀 녹색 충전소")
        val before=state()
        ui.activityRule.scenario.onActivity{model.mockUx(false)}
        ui.waitUntil(8_000){ui.onAllNodesWithTag("chargingLeisureSheet").fetchSemanticsNodes().isEmpty()}
        ui.onNodeWithTag("restricted").assertExists()
        assertEquals(before,state())
    }
}
