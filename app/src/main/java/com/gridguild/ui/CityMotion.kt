package com.gridguild.ui

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.gridguild.R
import com.gridguild.core.CityLevels
import com.gridguild.core.XpVisualEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/** OS motion preference is respected; vehicle safety is still gated by the caller. */
@Composable internal fun motionEnabled(): Boolean {
    val context=LocalContext.current
    return Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f
}

@Composable internal fun GuildNavItem(label: String,icon: ImageVector,selected: Boolean,wide: Boolean,tag: String,onClick:()->Unit) {
    val interactions=remember{MutableInteractionSource()}
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val motion=motionEnabled()
    val fill by animateColorAsState(if(selected)Charcoal else if(hovered || focused)Pale else Color.Transparent,tween(if(motion)160 else 0),label="navigationFill")
    val scale by animateFloatAsState(if(pressed).97f else 1f,tween(if(motion)130 else 0),label="navigationPress")
    Column(Modifier.then(if(wide)Modifier.fillMaxWidth() else Modifier.widthIn(min=64.dp))
        .heightIn(min=64.dp).graphicsLayer{scaleX=scale;scaleY=scale}
        .clip(RoundedCornerShape(18.dp)).background(fill)
        .border(if(focused)2.dp else 1.dp,if(focused)GreenDark else Color.Transparent,RoundedCornerShape(18.dp))
        .hoverable(interactions).selectable(selected=selected,interactionSource=interactions,indication=LocalIndication.current,role=Role.Tab,onClick=onClick)
        .testTag(tag).semantics{stateDescription=if(pressed)"누름" else if(hovered)"가리킴" else if(selected)"선택됨" else "대기"}
        .padding(horizontal=16.dp,vertical=10.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Icon(icon,null,tint=if(selected)Green else Subtle,modifier=Modifier.size(23.dp))
        Text(label,fontSize=12.sp,color=if(selected)Color.White else Subtle,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable internal fun GuildPage(tab: Int,modifier: Modifier=Modifier,content: @Composable (Int)->Unit) {
    val holder=rememberSaveableStateHolder()
    val motion=motionEnabled()
    AnimatedContent(tab,modifier.clip(RoundedCornerShape(20.dp)),transitionSpec={
        val direction=if(targetState>initialState)1 else -1
        if(motion) (slideInHorizontally(tween(300,easing=FastOutSlowInEasing)){it*direction/7}+fadeIn(tween(210))) togetherWith
            (slideOutHorizontally(tween(240,easing=FastOutSlowInEasing)){-it*direction/9}+fadeOut(tween(160)))
        else EnterTransition.None togetherWith ExitTransition.None
    },label="directionalPage") { destination ->
        holder.SaveableStateProvider(destination) { Box(Modifier.fillMaxSize().testTag("page$destination")){content(destination)} }
    }
}

/** Four dedicated construction keyframes. Cosmetic assembly, never a preview of a locked level. */
const val minimumLoadingMs=3000L
internal suspend fun holdLoadingMinimum(started: Long) = withContext(Dispatchers.Default) {
    // Elapsed real time, independent of animation scale and Compose's virtual test clock.
    val remaining=minimumLoadingMs-(android.os.SystemClock.elapsedRealtime()-started)
    if(remaining>0)delay(remaining)
}
@Composable internal fun rememberIntroFinished(enabled: Boolean=true,initialDone: Boolean=false): Boolean {
    var done by remember{mutableStateOf(initialDone)}
    LaunchedEffect(enabled){if(enabled && !done){holdLoadingMinimum(android.os.SystemClock.elapsedRealtime());done=true}}
    return done
}
@Composable internal fun CityGrowthLoading(modifier: Modifier=Modifier,animate: Boolean=true,label: String="도시를 준비하고 있어요",loop: Boolean=true,caption: Boolean=true) {
    val atlas=ImageBitmap.imageResource(R.drawable.city_loading_v2)
    var phase by remember{mutableIntStateOf(0)}
    val moving=animate && motionEnabled()
    LaunchedEffect(moving,loop) {
        if(moving)while(true){delay(650);if(phase<3)phase++ else if(loop)phase=0 else break} else phase=3
    }
    Column(modifier.padding(if(caption)20.dp else 0.dp).testTag("cityLoading"),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Crossfade(phase,animationSpec=tween(if(moving)260 else 0),label="constructionFrame") { frame ->
            Canvas(Modifier.fillMaxWidth().aspectRatio(2f).semantics{contentDescription="장식용 도시 조립 ${frame+1}/4"}) {
                val w=atlas.width/2;val h=atlas.height/2
                drawImage(atlas,IntOffset((frame%2)*w+2,(frame/2)*h+2),IntSize(w-4,h-4),
                    dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()),filterQuality=FilterQuality.High)
            }
        }
        if(caption)Text(label,color=Ink,fontWeight=FontWeight.SemiBold,fontSize=20.sp)
        if(caption)Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            repeat(4){i->Box(Modifier.size(if(i==phase)9.dp else 6.dp).background(if(i==phase)Green else Line,RoundedCornerShape(4.dp)))}
        }
    }
}

/** Channel delivery consumes an award once, including across activity recreation. */
@Composable internal fun XpRewardHost(events: Flow<XpVisualEvent>,allowed: Boolean,cityXp: Int,currentXp: ()->Int,modifier: Modifier=Modifier) {
    var event by remember{mutableStateOf<XpVisualEvent?>(null)}
    val progress=remember{Animatable(0f)}
    val latestXp by rememberUpdatedState(currentXp)
    val moving=motionEnabled()
    LaunchedEffect(events,allowed,moving) {
        event=null
        events.collect { award ->
            if(allowed && award.cityTotal<=latestXp()) {
                progress.snapTo(0f);event=award
                try {
                    if(moving)progress.animateTo(1f,tween(600,easing=LinearOutSlowInEasing)) else progress.snapTo(1f)
                    delay(if(moving)1300 else 900)
                } finally { event=null }
            }
        }
    }
    // A reset invalidates the currently displayed total without emitting a new award.
    LaunchedEffect(cityXp){if((event?.cityTotal ?: 0)>cityXp)event=null}
    if(allowed)event?.let { award -> XpRewardCard(award,progress.value,modifier) }
}

@Composable internal fun XpRewardCard(award: XpVisualEvent,progress: Float,modifier: Modifier=Modifier) {
    val displayed=award.cityBefore+(award.cityGain*progress).roundToInt()
    // A non-clickable Material Surface still blocks pointer propagation. This decoration must
    // never swallow navigation or a mission click underneath it.
    Box(modifier.widthIn(max=470.dp).fillMaxWidth().testTag("xpReward")
        .clip(RoundedCornerShape(20.dp))
        .background(Charcoal).border(1.dp,Charcoal,RoundedCornerShape(20.dp))) {
        Column(Modifier.padding(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("충전 미션 완료",color=OnDarkMuted,fontSize=12.sp)
                    Text("+${award.personalGain} XP",fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=Green)
                }
                Text("도시 $displayed XP",fontWeight=FontWeight.Medium,fontSize=14.sp,color=Color.White)
            }
            LinearProgressIndicator(progress={CityLevels.progress(displayed)},color=Green,trackColor=Color.White.copy(alpha=.12f),
                modifier=Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)))
        }
    }
}
