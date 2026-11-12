package com.gridguild

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import java.io.File
import java.time.Instant
import java.time.ZoneId

/** Replaces saved progress with fixtures. Run only on the designated QA device. */
@RunWith(AndroidJUnit4::class)
class RankingConsistencyFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private val store get()=GameStore(context)
    private lateinit var model: GridViewModel
    private lateinit var seeded: GameState
    private fun state()=runBlocking { store.state.first() }

    @Before fun setup() {
        val now=System.currentTimeMillis()
        val previousMonth=Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul"))
            .minusMonths(1).toInstant().toEpochMilli()
        seeded=GameState(
            districtId="sasang",
            // A waiting mission proves browsing and a city-only reset preserve mission state.
            mission=Mission("qa-ranking-active","sasang-river","sasang",120,98f,DataSource.MOCK,now),
            history=listOf(
                PointEvent("qa-ranking-old","qa-ranking-old","sasang",1400,previousMonth,
                    kind="QA_RANK",countsForMission=false),
                PointEvent("qa-ranking-current","qa-ranking-current","sasang",120,now,
                    kind="QA_RANK",countsForMission=false)
            )
        )
        runBlocking { store.update { seeded } }
        ui.activityRule.scenario.onActivity { model=ViewModelProvider(it)[GridViewModel::class.java] }
        ui.waitUntil(15_000) {
            model.gameAllowed.value && model.vehicleReading.value.fresh && model.game.value==seeded &&
                ui.onAllNodesWithTag("tab2").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1520,state().xp)
        assertEquals(1520,state().cityXp("sasang"))
        assertEquals(120,state().seasonXp(now,"sasang"))
        assertEquals(7,CityLevels.level(state().cityXp("sasang")))
    }

    private fun assertCityLevel(level: Int) {
        ui.onNodeWithTag("tab2").performClick()
        ui.waitUntil(10_000) {
            ui.onAllNodes(hasTestTag("cityStageLabel") and hasText("Lv.$level ·",substring=true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithTag("cityStageLabel").performScrollTo()
            .assertIsDisplayed().assertTextContains("Lv.$level ·",substring=true)
    }

    private fun openRegionalRanking() {
        ui.onNodeWithTag("tab3").performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("rankingList").fetchSemanticsNodes().isNotEmpty() }
        rankingFilter("지역 랭킹")
        ui.onNodeWithText("지역 랭킹",substring=false).assertIsSelected()
    }

    private fun rankingFilter(text: String) {
        ui.onNodeWithTag("rankingList").performScrollToNode(hasText(text,substring=false))
        ui.onNodeWithText(text,substring=false).assertIsDisplayed().performClick()
        ui.waitForIdle()
    }

    private fun setMonthly(monthly: Boolean) {
        // Bring the chips back into composition before inspecting their current label.
        ui.onNodeWithTag("rankingList").performScrollToNode(hasText("지역 랭킹",substring=false))
        val desired=if(monthly)"월간 시즌" else "누적 성과"
        if(ui.onAllNodesWithText(desired,substring=false).fetchSemanticsNodes().isEmpty()) {
            rankingFilter(if(monthly)"누적 성과" else "월간 시즌")
        }
        ui.waitUntil(8_000) {
            ui.onAllNodesWithText(desired,substring=false).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertRankingText(tag: String,text: String) {
        ui.onNodeWithTag("rankingList").performScrollToNode(hasTestTag(tag))
        // Scores tag the AnimatedContent container; levels tag the Text itself.
        val label=hasText(text,substring=false) and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag)))
        ui.waitUntil(8_000) {
            ui.onAllNodes(label,useUnmergedTree=true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNode(label,useUnmergedTree=true).assertIsDisplayed().assertTextEquals(text)
    }

    private fun assertRegionalScore(level: Int,cityXp: Int,rankingXp: Int) {
        assertRankingText("ranking-city-level-sasang","Lv.$level · 누적 도시 $cityXp XP")
        assertRankingText("ranking-city-score-sasang","$rankingXp XP")
    }

    private fun assertHomeLevel(level: Int) {
        ui.onNodeWithTag("tab0").performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("homeDashboard").fetchSemanticsNodes().isNotEmpty() }
        val label=hasText("$level / 12",substring=false) and hasAnyAncestor(hasTestTag("homeDashboard"))
        ui.waitUntil(8_000) { ui.onAllNodes(label,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
        ui.onNode(label,useUnmergedTree=true).performScrollTo().assertIsDisplayed()
            .assertTextEquals("$level / 12")
    }

    private fun capture(name: String) {
        ui.waitUntil(10_000) {
            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()=="com.gridguild"
        }
        ui.waitForIdle()
        Thread.sleep(550)
        ui.waitForIdle()
        assertEquals("com.gridguild",instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
        val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "App screenshot unavailable" }
        try {
            val target=File(context.getExternalFilesDir(null) ?: context.filesDir,
                "ranking-consistency-$name-${bitmap.width}x${bitmap.height}.png")
            target.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        } finally { bitmap.recycle() }
    }

    @Test fun monthlyAndCumulativeRankingsKeepTheSameSavedCityLevel() {
        assertCityLevel(7)
        openRegionalRanking()
        setMonthly(true)
        assertRegionalScore(level=7,cityXp=1520,rankingXp=120)
        capture("monthly")
        setMonthly(false)
        assertRegionalScore(level=7,cityXp=1520,rankingXp=1520)
        capture("cumulative")
        assertHomeLevel(7)
        assertEquals(seeded,model.game.value)
        assertEquals(seeded,state())
    }

    @Test fun cityOnlyResetUpdatesBothRankingPeriodsWithoutChangingPersonalOrMissionRecords() {
        assertCityLevel(7)
        openRegionalRanking()
        setMonthly(true)
        assertRegionalScore(level=7,cityXp=1520,rankingXp=120)
        val before=state()
        val expected=before.copy(history=before.history.map { it.copy(cityCredit=false) })
        ui.activityRule.scenario.onActivity { model.resetDemo(DemoResetTarget.CITY_LEVEL,"sasang") }
        ui.waitUntil(10_000) { model.game.value==expected && state()==expected }
        assertRegionalScore(level=1,cityXp=0,rankingXp=0)
        capture("reset-monthly")
        setMonthly(false)
        assertRegionalScore(level=1,cityXp=0,rankingXp=0)
        capture("reset-cumulative")
        assertCityLevel(1)
        assertHomeLevel(1)
        val after=state()
        assertEquals(1520,after.xp)
        assertEquals(before.mission,after.mission)
        assertEquals(before.completionHistory,after.completionHistory)
        assertEquals(before.history.map { it.copy(cityCredit=false) },after.history)
        assertEquals(expected,after)
    }
}
