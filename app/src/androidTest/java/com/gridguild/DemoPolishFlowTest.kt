package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class DemoPolishFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private fun state()=runBlocking{store.state.first()}
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang",homeCharge=HomeChargePreferences("우리 집 충전소"))}}
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun capture(name: String) {
        ui.waitForIdle();Thread.sleep(350)
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let{bitmap->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"polish-$name.png")
                .outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun twoTimeSlotsAndOneClickMockChargeAwardExactlyOnce() {
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("homeRecommendation").performScrollTo()
        ui.onNodeWithText("16:00").assertIsDisplayed();ui.onNodeWithText("– 17:00").assertIsDisplayed()
        ui.onNodeWithText("22:00").assertIsDisplayed();ui.onNodeWithText("– 23:00").assertIsDisplayed()
        ui.onNodeWithText("필요할 때 바로 충전").assertDoesNotExist()
        capture("home-times")
        ui.onNodeWithTag("homeSimulate").performScrollTo().performClick()
        ui.waitUntil(10_000){state().xp==120}
        assertEquals(120,state().cityXp("sasang"));assertNull(state().mission)
        ui.onNodeWithTag("homeSimulate").assertIsNotEnabled()
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000){ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("homeSimulate").performScrollTo().assertIsNotEnabled()
        assertEquals(120,state().xp)
    }
    @Test fun regionalLevelsAndLargerPersonalListRenderInsideOriginalRankingTab() {
        ui.onNodeWithTag("tab3").performClick();ui.onNodeWithText("지역 랭킹").performClick()
        capture("city-ranking")
        ui.onNodeWithTag("rankingList").performScrollToNode(hasText("Lv.3 · 모의 공동 점수"))
        ui.onAllNodesWithText("Lv.3 · 모의 공동 점수").onFirst().assertExists()
        ui.onNodeWithTag("rankingList").performScrollToNode(hasText("개인 랭킹"))
        ui.onNodeWithText("개인 랭킹").performClick();capture("personal-ranking")
        ui.onNodeWithTag("rankingList").performScrollToNode(hasText("나",substring=false))
        ui.onNodeWithText("나").assertExists()
        assertEquals(33,Rankings.personal(state(),System.currentTimeMillis()).size)
        assertEquals(0,state().xp)
    }
    @Test fun mockSamplesRemainMixedAfterTenMinutesAndKeepIntentionalUnknowns() {
        for(seed in 0..6) {
            var clock=1_000_000L
            val repo=MockChargingAvailabilityRepository(clock={clock},random=Random(seed))
            fun statuses()=repo.snapshots.value.values.map{ChargingAvailability.summarize(it,clock).status}.toSet()
            assertEquals(AvailabilityStatus.entries.toSet(),statuses())
            repo.setScenario("gwangalli-marina",DemoAvailabilityScenario.STALE)
            clock+=11*60*1000
            repo.refreshIfDue()
            assertEquals(AvailabilityStatus.FREE,ChargingAvailability.summarize(repo.snapshots.value["sasang-river"],clock).status)
            assertEquals(AvailabilityStatus.UNKNOWN,ChargingAvailability.summarize(repo.snapshots.value["gwangalli-marina"],clock).status)
            assertTrue(statuses().contains(AvailabilityStatus.BUSY));assertTrue(statuses().size>=3)
            repo.shuffle(keepFirstUsable=false)
            assertEquals(AvailabilityStatus.entries.toSet(),statuses())
        }
    }
}
