package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChargingAvailabilityFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private fun state()=runBlocking{store.state.first()}
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick();ui.onNodeWithTag("routeCharge").performClick()
    }
    private fun show(tag: String) { ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag(tag)) }
    private fun choose(scenario: DemoAvailabilityScenario) {
        show("availabilityTest-sasang-river");ui.onNodeWithTag("availabilityTest-sasang-river").performClick()
        ui.onNodeWithTag("availability-sasang-river-${scenario.name}").performClick()
    }
    private fun picture(name: String) {
        ui.waitForIdle()
        // Compose idleness does not wait for the native dialog window to finish fading out.
        Thread.sleep(350)
        show("mission-sasang-river")
        ui.waitForIdle()
        Thread.sleep(200)
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let{bitmap->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"availability-$name.png")
                .outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun fourStatesCountsAndFreshnessAreVisibleInsideExistingCards() {
        for((scenario,label,count) in listOf(
            Triple(DemoAvailabilityScenario.FREE,"여유","5기 중 3기 이용 가능"),
            Triple(DemoAvailabilityScenario.BUSY,"혼잡","5기 중 1기 이용 가능"),
            Triple(DemoAvailabilityScenario.NONE,"이용 가능 없음","5기 중 0기 이용 가능"),
            Triple(DemoAvailabilityScenario.UNKNOWN,"정보 없음","총 5기"))) {
            choose(scenario);show("availability-sasang-river")
            ui.onNodeWithTag("availability-sasang-river").assertTextContains(label)
            ui.onNodeWithTag("chargerCount-sasang-river").assertTextContains(count)
            ui.onNodeWithTag("availabilityTime-sasang-river").assertDoesNotExist()
            picture(scenario.name.lowercase())
        }
        choose(DemoAvailabilityScenario.STALE);show("availability-sasang-river")
        ui.onNodeWithTag("availability-sasang-river").assertTextContains("정보 없음")
        ui.onNodeWithTag("availabilityTime-sasang-river").assertDoesNotExist()
        choose(DemoAvailabilityScenario.MISSING);show("chargerCount-sasang-river")
        ui.onNodeWithTag("chargerCount-sasang-river").assertTextContains("수량 미확인")
        assertEquals(0,state().xp)
    }
    @Test fun noSlotsBlocksNewMissionButNeverCancelsAnExistingMissionOrAward() {
        choose(DemoAvailabilityScenario.NONE);show("mission-sasang-river")
        ui.onNodeWithTag("mission-sasang-river").assertIsNotEnabled()
        ui.activityRule.scenario.onActivity{ViewModelProvider(it)[GridViewModel::class.java].startMission(SampleData.stations.first())}
        ui.waitUntil(8_000){ui.onAllNodesWithText("이용 가능한 충전기가 없습니다. 다른 후보를 확인하세요").fetchSemanticsNodes().isNotEmpty()}
        assertNull(state().mission);assertEquals(0,state().xp)
        ui.onNodeWithText("닫기").performClick()
        choose(DemoAvailabilityScenario.FREE);show("mission-sasang-river")
        ui.onNodeWithTag("mission-sasang-river").performClick()
        ui.waitUntil(8_000){state().mission!=null}
        val missionId=state().mission!!.id
        choose(DemoAvailabilityScenario.NONE)
        assertEquals(missionId,state().mission?.id)
        ui.activityRule.scenario.onActivity{ViewModelProvider(it)[GridViewModel::class.java].let{model->model.mockCharge(true);model.mockSoc(44f)}}
        ui.waitUntil(8_000){state().xp==120}
        assertEquals(120,state().cityXp("sasang"));assertEquals(2,CityLevels.level(state().cityXp("sasang")))
        assertNull(state().mission)
    }
    @Test fun connectorMismatchAndDemoRefreshAreExplicitWithoutChangingNaviContract() {
        show("availabilitySettings");ui.onNodeWithTag("availabilitySettings").performClick()
        show("demoConnector");ui.onNodeWithTag("demoConnector").performClick()
        ui.onNodeWithTag("connector-AC_TYPE1").performClick()
        show("compatibility-sasang-river")
        ui.onNodeWithTag("compatibility-sasang-river").assertTextContains("불일치",substring=true)
        show("mission-sasang-river");ui.onNodeWithTag("mission-sasang-river").assertIsNotEnabled()
        show("demoConnector");ui.onNodeWithTag("demoConnector").performClick()
        ui.onNodeWithTag("connector-DC_COMBO").performClick()
        show("refreshAvailability");ui.onNodeWithTag("refreshAvailability").performClick()
        show("waypoint-sasang-river");ui.onNodeWithTag("waypoint-sasang-river").performClick()
        show("navStatus")
        ui.waitUntil(8_000){ui.onAllNodesWithText("데모 경유지 반영 완료",substring=true).fetchSemanticsNodes().isNotEmpty()}
        assertEquals(0,state().xp)
    }
    @Test fun updatingOneMockStationDoesNotReTimestampAnother() {
        val repository=MockChargingAvailabilityRepository()
        val before=repository.snapshots.value["centum"]
        repository.setScenario("sasang-river",DemoAvailabilityScenario.NONE)
        assertSame(before,repository.snapshots.value["centum"])
        assertEquals(AvailabilityStatus.NONE,ChargingAvailability.summarize(repository.snapshots.value["sasang-river"],System.currentTimeMillis()).status)
    }
}
