package com.gridguild

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.*
import com.gridguild.data.GameStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val store get()=GameStore(InstrumentationRegistry.getInstrumentation().targetContext)
    private lateinit var model: GridViewModel
    @Before fun setup() {
        runBlocking{store.update{GameState(districtId="sasang")}}
        ui.activityRule.scenario.onActivity{model=ViewModelProvider(it)[GridViewModel::class.java]}
        ui.waitUntil(15_000){model.gameAllowed.value && model.game.value.mode==AppMode.MOCK &&
            ui.onAllNodesWithTag("tab1").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithTag("tab1").performClick()
        ui.onNodeWithTag("routeCharge").performClick()
        show("navTools");ui.onNodeWithTag("navTools").performClick()
    }
    private fun show(tag: String)=ui.onNodeWithTag("missionList").performScrollToNode(hasTestTag(tag))
    private fun completedAfter(previousId: Long,text: String): Long {
        ui.waitUntil(8_000){model.navigation.value.let{it.requestId>previousId && !it.pending && it.status.contains(text)}}
        return model.navigation.value.requestId
    }
    private fun request(tag: String,text: String): Long {
        show(tag)
        val before=model.navigation.value.requestId
        ui.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performClick()
        val id=completedAfter(before,text)
        ui.onNodeWithTag(tag).assertIsEnabled()
        show("navRequest");ui.onNodeWithTag("navRequest").assertTextContains("요청 #$id",substring=true)
        return id
    }
    @Test fun bothButtonsShowProgressAndProduceVisibleResultsEveryTime() {
        val pendingIds=ConcurrentHashMap.newKeySet<Long>()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
        val observation=scope.launch(start=CoroutineStart.UNDISPATCHED){model.navigation.collect{if(it.pending)pendingIds.add(it.requestId)}}
        try {
            // Observe the actual request transition rather than racing a 650ms UI snapshot.
            val ids=listOf(request("navRefresh","데모 목적지 갱신 완료"),
                request("navRefresh","데모 목적지 갱신 완료"),request("navReroute","데모 재탐색 완료"))
            assertEquals(listOf(1L,2L,3L),ids)
            org.junit.Assert.assertTrue("Every clicked request must emit pending before completing",pendingIds.containsAll(ids))
        } finally {observation.cancel();scope.cancel()}
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"navigation-fixed.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
        assertEquals(0,runBlocking{store.state.first().xp})
    }
    @Test fun refreshKeepsTheSelectedDestinationAndItsNearbyRecommendations() {
        show("changeDemoDestination");ui.onNodeWithTag("changeDemoDestination").performClick()
        val beforeCity=model.navigation.value.requestId
        ui.onNodeWithTag("demo-destination-centum").performScrollTo().performClick()
        completedAfter(beforeCity,"데모 목적지 설정 완료")
        show("destination-centum")
        val beforeStation=model.navigation.value.requestId
        ui.onNodeWithTag("destination-centum").performScrollTo().assertIsEnabled().performClick()
        completedAfter(beforeStation,"데모 목적지 설정 완료")
        show("navDestination")
        ui.onNodeWithTag("navDestination").assertTextContains("센텀 녹색 충전소",substring=true)
        request("navRefresh","데모 목적지 갱신 완료")
        show("navDestination")
        ui.onNodeWithTag("navDestination").assertTextContains("센텀 녹색 충전소",substring=true)
        request("navReroute","데모 재탐색 완료")
        show("navDestination")
        ui.onNodeWithTag("navDestination").assertTextContains("센텀 녹색 충전소",substring=true)
        val station=SampleData.stations.first{it.id=="centum"}
        assertEquals(station.lat,model.navigation.value.lat!!,0.00001)
        assertEquals(station.lon,model.navigation.value.lon!!,0.00001)
    }
}
