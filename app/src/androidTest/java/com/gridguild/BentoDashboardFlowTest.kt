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

/** These fixtures replace the saved game: run this class only on the designated QA device. */
@RunWith(AndroidJUnit4::class)
class BentoDashboardFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private val store get()=GameStore(context)
    private lateinit var model: GridViewModel
    private fun state()=runBlocking{store.state.first()}

    @Before fun setup() {
        runBlocking { store.update { GameState(districtId="sasang",
            homeCharge=HomeChargePreferences(targetSoc=80,departureMinutes=450)) } }
        bindModel()
        ui.waitUntil(15_000) {
            model.gameAllowed.value && model.vehicleReading.value.fresh &&
                model.game.value.homeCharge.targetSoc==80 &&
                ui.onAllNodesWithTag("tab0").fetchSemanticsNodes().isNotEmpty()
        }
        home()
    }

    private fun bindModel() {
        ui.activityRule.scenario.onActivity { model=ViewModelProvider(it)[GridViewModel::class.java] }
    }

    private fun home() {
        ui.onNodeWithTag("tab0").performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("homeDashboard").fetchSemanticsNodes().isNotEmpty() }
    }

    /** The same scroll ancestor supports the compact single column and the wide dashboard. */
    private fun click(tag: String) {
        ui.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().performClick()
        ui.waitForIdle()
    }

    private fun dashboardText(text: String)=ui.onNode(
        hasText(text,substring=false) and hasAnyAncestor(hasTestTag("homeDashboard")))

    private fun awaitDashboardText(text: String) {
        ui.waitUntil(8_000) {
            ui.onAllNodes(hasText(text,substring=false) and hasAnyAncestor(hasTestTag("homeDashboard")))
                .fetchSemanticsNodes().isNotEmpty()
        }
        dashboardText(text).performScrollTo().assertIsDisplayed()
    }

    private fun registerHome() {
        click("homeJoin")
        ui.onNodeWithTag("homeRegisterSave").performClick()
        ui.waitUntil(8_000) { state().homeCharge.chargerName!=null }
    }

    private fun rankingText(text: String) {
        ui.onNodeWithTag("rankingList").performScrollToNode(hasText(text,substring=false))
        ui.onNodeWithText(text,substring=false).assertIsDisplayed()
    }

    private fun rankingFilter(text: String) {
        rankingText(text)
        ui.onNodeWithText(text,substring=false).performClick()
        ui.waitForIdle()
    }

    private fun assertCurrentRankingHasNoMovement() {
        rankingText("현재 순위")
        ui.onAllNodesWithText("→",substring=true).assertCountEquals(0)
    }

    private fun capture(name: String) {
        // A system dialog may temporarily own focus; never save it as an app screenshot.
        ui.waitUntil(10_000) {
            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()==context.packageName
        }
        ui.waitForIdle()
        Thread.sleep(550)
        ui.waitForIdle()
        assertEquals(context.packageName,
            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
        val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "App screenshot unavailable" }
        try {
            val file=File(context.getExternalFilesDir(null) ?: context.filesDir,
                "bento-$name-${bitmap.width}x${bitmap.height}.png")
            file.outputStream().use { output -> assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output)) }
        } finally { bitmap.recycle() }
    }

    @Test fun savedGoalAndVehicleSourceRenderAndDashboardActionsKeepExistingGuards() {
        ui.onNodeWithTag("dashboardTarget").performScrollTo().assertTextEquals("목표 80%")
        awaitDashboardText("42%")
        ui.onNodeWithTag("batteryStatus").assertTextEquals("충전 스위치 OFF")
        assertEquals(450,state().homeCharge.departureMinutes)
        capture("saved-target")

        click("dashboardMission")
        ui.onNodeWithTag("homeCharge").assertIsSelected()
        registerHome()
        ui.activityRule.scenario.onActivity { model.mockSoc(90f) }
        ui.waitUntil(8_000) { model.vehicleReading.value.fresh && model.vehicle.value.soc==90f }
        ui.onNodeWithTag("homeJoin").performScrollTo().assertIsNotEnabled()
        assertNull(state().mission)
        assertEquals(0,state().xp)

        home()
        awaitDashboardText("90%")
        ui.onNodeWithText("추천 충전소 보기").performScrollTo().performClick()
        ui.onNodeWithTag("routeCharge").assertIsSelected()
        ui.onNodeWithTag("missionList").assertExists()
        ui.onNodeWithTag("nearbySummary").performScrollTo().assertTextEquals("충전 추천 중지")

        home()
        ui.onNodeWithTag("dashboardCity").performScrollTo()
        ui.onNodeWithContentDescription("내 도시 보기").performClick()
        ui.onNodeWithTag("cityStageLabel").performScrollTo().assertTextContains("Lv.1",substring=true)
        home()
        ui.onNodeWithText("지역 랭킹 보기").performScrollTo().performClick()
        ui.onNodeWithTag("rankingList").assertExists()
        ui.onNodeWithText("지역 랭킹").assertIsSelected()
        assertTrue(state().history.isEmpty())

        // Clearing the optional goal must not invent an 80% default in the gauge or label.
        ui.activityRule.scenario.onActivity { model.saveHomePreferences(null,450) }
        ui.waitUntil(8_000) { state().homeCharge.targetSoc==null }
        home()
        ui.onNodeWithTag("dashboardTarget").performScrollTo().assertTextEquals("목표 미설정")
        assertEquals(450,state().homeCharge.departureMinutes)
    }

    @Test fun automaticHomeAwardReachesAnimatedBalancesAndRecreationNeverReissuesIt() {
        click("dashboardMission")
        registerHome()
        ui.onNodeWithTag("homeJoin").performScrollTo().assertIsEnabled()
        ui.onNodeWithText("TODAY'S MISSION").performScrollTo()
        capture("mission-ready")
        click("homeJoin")
        ui.waitUntil(8_000) { state().mission?.kind==MissionKind.HOME }
        ui.activityRule.scenario.onActivity { model.mockCharge(true);model.mockSoc(43f) }
        ui.waitUntil(8_000) {
            state().mission?.let { it.kind==MissionKind.HOME && it.chargingObserved && it.progress==.5f }==true
        }
        assertEquals(0,state().xp)
        assertEquals(0,state().cityXp("sasang"))
        ui.onNodeWithText("50%",substring=false).performScrollTo().assertIsDisplayed()
        capture("mission-progress")
        ui.activityRule.scenario.onActivity { model.mockSoc(44f) }
        ui.waitUntil(10_000) { state().xp==120 && state().mission==null }
        val earned=state()
        assertEquals(120,earned.cityXp("sasang"))
        assertEquals(2,CityLevels.level(earned.cityXp("sasang")))
        assertEquals("HOME",earned.history.single().kind)
        ui.onNodeWithTag("homeJoin").performScrollTo().assertIsNotEnabled().assertTextContains("오늘 보상 완료")
        ui.onNodeWithText("TODAY'S MISSION").performScrollTo()
        capture("mission-success")

        home()
        awaitDashboardText("120 XP")
        awaitDashboardText("지역 성장 · 120 XP")
        awaitDashboardText("Lv.2")
        capture("earned-city")
        ui.activityRule.scenario.recreate()
        bindModel()
        ui.waitUntil(15_000) { model.gameAllowed.value &&
            ui.onAllNodesWithTag("tab0").fetchSemanticsNodes().isNotEmpty() }
        home()
        awaitDashboardText("120 XP")
        awaitDashboardText("Lv.2")
        assertEquals(earned,state())
        click("dashboardMission")
        ui.onNodeWithTag("homeJoin").performScrollTo().assertIsNotEnabled().assertTextContains("오늘 보상 완료")
        ui.onNodeWithTag("homeSimulate").performScrollTo().assertIsNotEnabled()
        assertEquals(earned,state())
    }

    @Test fun drivingAndUxRestrictionsHideDashboardAndPreserveTheLedger() {
        val before=state()
        ui.activityRule.scenario.onActivity { model.mockPark(false) }
        ui.waitUntil(8_000) { ui.onAllNodesWithTag("restricted").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("homeDashboard").assertDoesNotExist()
        ui.onNodeWithTag("dashboardMission").assertDoesNotExist()
        ui.onNodeWithTag("tab0").assertDoesNotExist()
        capture("driving-restricted")
        assertEquals(before,state())

        ui.activityRule.scenario.onActivity { model.mockPark(true) }
        ui.waitUntil(8_000) { model.gameAllowed.value &&
            ui.onAllNodesWithTag("tab0").fetchSemanticsNodes().isNotEmpty() }
        home()
        ui.activityRule.scenario.onActivity { model.mockUx(false) }
        ui.waitUntil(8_000) { ui.onAllNodesWithTag("restricted").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("homeDashboard").assertDoesNotExist()
        ui.onNodeWithTag("dashboardMission").assertDoesNotExist()
        assertEquals(before,state())

        ui.activityRule.scenario.onActivity { model.mockUx(true) }
        ui.waitUntil(8_000) { model.gameAllowed.value &&
            ui.onAllNodesWithTag("tab0").fetchSemanticsNodes().isNotEmpty() }
        home()
        ui.onNodeWithTag("dashboardTarget").performScrollTo().assertTextEquals("목표 80%")
        assertEquals(before,state())
    }

    @Test fun rankingMovementUsesCommittedScoresAndFilterChangesResetTheObservation() {
        ui.onNodeWithText("지역 랭킹 보기").performScrollTo().performClick()
        ui.onNodeWithText("지역 랭킹").assertIsSelected()
        assertCurrentRankingHasNoMovement()
        val regionBefore=state()
        assertEquals(0,regionBefore.cityXp("sasang"))
        val now=System.currentTimeMillis()
        val regionStart=Rankings.cities(regionBefore,now).indexOfFirst{it.mine}+1
        // QA-only committed fixture: isolate city credit so the personal scope still starts at zero.
        val cityEvent=PointEvent("qa-rank-city","qa-rank-city","sasang",20_000,now,
            kind="QA_RANK",personalCredit=false,cityCredit=true,countsForMission=false)
        val cityExpected=regionBefore.copy(history=regionBefore.history+cityEvent)
        val regionEnd=Rankings.cities(cityExpected,now).indexOfFirst{it.mine}+1
        assertTrue(regionEnd<regionStart)
        runBlocking { store.update { current ->
            assertEquals(regionBefore,current)
            current.copy(history=current.history+cityEvent)
        } }
        val cityMovement="$regionStart→${regionEnd}위"
        ui.waitUntil(8_000) { model.game.value==cityExpected &&
            ui.onAllNodesWithText(cityMovement,substring=false).fetchSemanticsNodes().isNotEmpty() }
        rankingText(cityMovement)
        ui.onNodeWithText("현재 순위").assertDoesNotExist()
        ui.waitUntil(8_000) { ui.onAllNodesWithTag("xpReward").fetchSemanticsNodes().isEmpty() }
        capture("ranking-regional-change")

        rankingFilter("월간 시즌")
        assertCurrentRankingHasNoMovement()
        rankingFilter("누적 성과")
        assertCurrentRankingHasNoMovement()
        rankingFilter("개인 랭킹")
        ui.onNodeWithText("개인 랭킹").assertIsSelected()
        assertCurrentRankingHasNoMovement()
        val personalBefore=state()
        assertEquals(0,personalBefore.xp)
        val personalAt=System.currentTimeMillis()
        val personalStart=Rankings.personal(personalBefore,personalAt).indexOfFirst{it.mine}+1
        val personalEvent=PointEvent("qa-rank-personal","qa-rank-personal","sasang",20_000,personalAt,
            kind="QA_RANK",personalCredit=true,cityCredit=false,countsForMission=false)
        val personalExpected=personalBefore.copy(history=personalBefore.history+personalEvent)
        val personalEnd=Rankings.personal(personalExpected,personalAt).indexOfFirst{it.mine}+1
        assertTrue(personalEnd<personalStart)
        runBlocking { store.update { current ->
            assertEquals(personalBefore,current)
            current.copy(history=current.history+personalEvent)
        } }
        val personalMovement="$personalStart→${personalEnd}위"
        ui.waitUntil(8_000) { model.game.value==personalExpected &&
            ui.onAllNodesWithText(personalMovement,substring=false).fetchSemanticsNodes().isNotEmpty() }
        rankingText(personalMovement)
        ui.onNodeWithText("현재 순위").assertDoesNotExist()
        ui.waitUntil(8_000) { ui.onAllNodesWithTag("xpReward").fetchSemanticsNodes().isEmpty() }
        capture("ranking-personal-change")

        rankingFilter("지역 랭킹")
        assertCurrentRankingHasNoMovement()
        rankingFilter("개인 랭킹")
        assertCurrentRankingHasNoMovement()
        assertEquals(personalExpected,state())
        assertEquals(20_000,state().xp)
        assertEquals(20_000,state().cityXp("sasang"))
        assertTrue(state().completionHistory.isEmpty())
    }
}
