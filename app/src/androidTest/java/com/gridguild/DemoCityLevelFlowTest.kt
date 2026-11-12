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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoCityLevelFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var model: GridViewModel
    private fun state()=runBlocking{store.state.first()}

    @Before fun setup() {
        val time=System.currentTimeMillis()
        runBlocking{store.update{GameState(districtId="sasang",
            mission=Mission("active","gwangalli","sasang",120,42f,DataSource.MOCK,time),
            history=listOf(PointEvent("earned","sasang-river:earned","sasang",370,time),
                PointEvent("other","other","haeundae",250,time)))}}
        ui.activityRule.scenario.onActivity{model=ViewModelProvider(it)[GridViewModel::class.java]}
        ui.waitUntil(15_000){model.gameAllowed.value && model.game.value.cityXp("sasang")==370 &&
            ui.onAllNodesWithTag("settings").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("settings").performClick()
    }

    private fun step(tag: String,expectedLevel: Int) {
        ui.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performClick()
        ui.waitUntil(8_000){CityLevels.level(state().cityXp("sasang"))==expectedLevel}
    }

    @Test fun buttonsChangeOnlySelectedCityAndPersistAcrossRecreation() {
        val before=state()
        step("cityLevelPlus",4)
        assertEquals(450,state().cityXp("sasang"))
        step("cityLevelMinus",3)
        assertEquals(250,state().cityXp("sasang"))
        assertEquals(before.xp,state().xp);assertEquals(before.mission,state().mission)
        assertEquals(before.completionHistory,state().completionHistory)
        assertEquals(before.cityXp("haeundae"),state().cityXp("haeundae"))
        assertEquals(before.history,state().history.take(before.history.size))
        val saved=state()
        ui.activityRule.scenario.recreate()
        ui.waitUntil(15_000){model.gameAllowed.value && ui.onAllNodesWithTag("tab2").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(saved,state())
    }

    @Test fun boundaryButtonsDisableAndRealModeOrRestrictedActionsCannotWrite() {
        runBlocking{store.update{it.copy(history=emptyList())}}
        ui.waitUntil(8_000){model.game.value.cityXp("sasang")==0}
        ui.onNodeWithTag("cityLevelMinus").performScrollTo().assertIsNotEnabled()
        val bottom=state()
        ui.activityRule.scenario.onActivity{model.stepDemoCityLevel(-1,"sasang")}
        ui.waitForIdle();assertEquals(bottom,state())
        runBlocking{store.update{it.copy(history=listOf(PointEvent("top","top","sasang",5_000,System.currentTimeMillis(),
            kind="TEST",personalCredit=false,countsForMission=false)))}}
        ui.waitUntil(8_000){model.game.value.cityXp("sasang")==5_000}
        ui.onNodeWithTag("cityLevelPlus").performScrollTo().assertIsNotEnabled()
        val top=state()
        ui.activityRule.scenario.onActivity{model.stepDemoCityLevel(1,"sasang");model.stepDemoCityLevel(-1,"haeundae")}
        ui.waitForIdle();assertEquals(top,state())
        ui.activityRule.scenario.onActivity{model.mockUx(false)}
        ui.waitUntil(8_000){!model.gameAllowed.value}
        ui.activityRule.scenario.onActivity{model.stepDemoCityLevel(-1,"sasang")}
        ui.waitForIdle();assertEquals(top,state())
        // Persisted mode is checked again inside the atomic mutation.
        runBlocking{store.update{it.copy(mode=AppMode.PLEOS)}}
        ui.waitUntil(8_000){model.game.value.mode==AppMode.PLEOS}
        val actualMode=state()
        ui.activityRule.scenario.onActivity{model.stepDemoCityLevel(-1,"sasang")}
        ui.waitForIdle();assertEquals(actualMode,state())
    }
}
