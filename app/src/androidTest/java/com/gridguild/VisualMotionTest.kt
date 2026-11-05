package com.gridguild

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.XpVisualEvent
import com.gridguild.ui.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.zip.CRC32

@RunWith(AndroidJUnit4::class)
class VisualMotionTest {
    @get:Rule val ui=createComposeRule()
    @Test fun allTwelveBundledCityFramesHaveDifferentPixels() {
        val resources=InstrumentationRegistry.getInstrumentation().targetContext.resources
        val fingerprints=mutableSetOf<Long>()
        listOf(R.drawable.city_medieval_v4,R.drawable.city_modern_v4,R.drawable.city_future_v4).forEach { id ->
            val bitmap=BitmapFactory.decodeResource(resources,id)
            val w=bitmap.width/2;val h=bitmap.height/2
            assertTrue(w>=880 && h>=440)
            repeat(4){frame->
                val crc=CRC32()
                for(y in 12 until h-12 step 11) for(x in 12 until w-12 step 11) {
                    val pixel=bitmap.getPixel(frame%2*w+x,frame/2*h+y)
                    repeat(4){crc.update(pixel ushr (it*8) and 255)}
                }
                assertTrue("Each city stage must have unique artwork",fingerprints.add(crc.value))
            }
            bitmap.recycle()
        }
        assertEquals(12,fingerprints.size)
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun navigationShowsMouseHitAreaAndKeepsDestinationState() {
        ui.setContent { GridTheme {
            var tab by remember{mutableIntStateOf(0)}
            Column {
                GuildNavItem("홈",Icons.Outlined.Home,tab==0,false,"navTest"){tab=1}
                GuildPage(tab,Modifier.weight(1f)) { destination ->
                    Text("destination $destination")
                }
            }
        } }
        ui.onNodeWithTag("navTest").performMouseInput{enter(center)}
        ui.onNodeWithTag("navTest").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"가리킴"))
        ui.onNodeWithTag("navTest").performClick()
        ui.onNodeWithTag("page1").assertExists()
        ui.onNodeWithTag("page0").assertDoesNotExist()
    }
    @Test fun constructionUsesDifferentKeyframesAndStopsWhenReady() {
        var ready by mutableStateOf(false)
        ui.setContent { GridTheme { if(!ready)CityGrowthLoading() else Text("ready") } }
        ui.onNodeWithTag("cityLoading").assertExists()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        if(android.provider.Settings.Global.getFloat(context.contentResolver,android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f)
            ui.waitUntil(4_000) { ui.onAllNodesWithContentDescription("장식용 도시 조립 2/4").fetchSemanticsNodes().isNotEmpty() }
        else ui.onNodeWithContentDescription("장식용 도시 조립 4/4").assertExists()
        ui.runOnIdle{ready=true}
        ui.onNodeWithTag("cityLoading").assertDoesNotExist()
        ui.onNodeWithText("ready").assertExists()
    }
    @Test fun consumedRewardDoesNotReplayOnRecompositionOrSafetyReturn() {
        val channel=Channel<XpVisualEvent>(Channel.UNLIMITED)
        val flow=channel.receiveAsFlow()
        var allowed by mutableStateOf(true)
        ui.setContent { GridTheme { XpRewardHost(flow,allowed,120,{120}) } }
        channel.trySend(XpVisualEvent("visual-1",120,120,0,120))
        ui.waitUntil(4_000){ui.onAllNodesWithTag("xpReward").fetchSemanticsNodes().isNotEmpty()}
        ui.runOnIdle{allowed=false}
        ui.onNodeWithTag("xpReward").assertDoesNotExist()
        ui.runOnIdle{allowed=true}
        ui.onNodeWithTag("xpReward").assertDoesNotExist()
        channel.trySend(XpVisualEvent("visual-2",120,120,0,120))
        ui.waitUntil(4_000){ui.onAllNodesWithTag("xpReward").fetchSemanticsNodes().isNotEmpty()}
        channel.close()
    }
    @Test fun rewardDecorationNeverSwallowsUnderlyingButtonClicks() {
        ui.setContent { GridTheme {
            var clicked by remember{mutableStateOf(false)}
            Box {
                Button(onClick={clicked=true},modifier=Modifier.testTag("underReward")){Text(if(clicked)"clicked" else "click")}
                XpRewardCard(XpVisualEvent("overlap",120,120,0,120),.4f)
            }
        } }
        ui.onNodeWithTag("underReward").performClick()
        ui.onNodeWithText("clicked").assertExists()
    }
}
