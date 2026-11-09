package com.gridguild

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.gridguild.ui.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoadingDurationTest {
    @get:Rule val ui=createComposeRule()
    @Test fun startupConstructionStaysVisibleForAtLeastThreeSeconds() {
        val start=SystemClock.elapsedRealtime()
        ui.setContent{GridTheme{if(rememberIntroFinished())Text("ready") else CityGrowthLoading(loop=false)}}
        ui.onNodeWithTag("cityLoading").assertExists()
        ui.waitUntil(8_000){ui.onAllNodesWithText("ready").fetchSemanticsNodes().isNotEmpty()}
        assertTrue("Startup loader left early",SystemClock.elapsedRealtime()-start>=3000)
    }
    @Test fun coldCityLoadingLastsThreeSecondsAndWarmCacheNeverFlashesLoading() {
        clearCityArtMemoryCache()
        var visible by mutableStateOf(true)
        val start=SystemClock.elapsedRealtime()
        ui.setContent{GridTheme{if(visible)CityCanvas(5,"sasang",5,Modifier.fillMaxWidth().aspectRatio(2f))}}
        ui.onNodeWithTag("cityLoading").assertExists()
        ui.waitUntil(9_000){ui.onAllNodesWithContentDescription("사상구 Lv.5 근대 도시 · 도심 광장").fetchSemanticsNodes().isNotEmpty()}
        assertTrue("Cold image loader left early",SystemClock.elapsedRealtime()-start>=3000)
        ui.runOnIdle{visible=false};ui.waitForIdle();ui.runOnIdle{visible=true}
        ui.onNodeWithTag("cityLoading").assertDoesNotExist()
        ui.onNodeWithContentDescription("사상구 Lv.5 근대 도시 · 도심 광장").assertExists()
    }
}
