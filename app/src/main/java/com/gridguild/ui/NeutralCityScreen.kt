@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gridguild.GridViewModel
import com.gridguild.core.*
import com.gridguild.data.NavigationState
private fun Context.activity(): Activity?=when(this){is Activity->this;is ContextWrapper->baseContext.activity();else->null}

/** N is a mock presentation state; real P + platform safety checks remain active. */
@Composable internal fun NeutralCityScreen(game: GameState,navigation: NavigationState,allowed: Boolean,model: GridViewModel) {
    val context=LocalContext.current
    val activity=context.activity()
    val city=if(navigation.hasDestination)NeutralCity.at(navigation.lat,navigation.lon) else null
    val xp=game.cityXp(game.districtId)
    val level=CityLevels.level(xp)
    var selectionOwnerLevel by rememberSaveable(game.districtId){mutableIntStateOf(level)}
    var requestedLevel by rememberSaveable(game.districtId){mutableIntStateOf(0)}
    // Clamp synchronously before cache/decode/render. A later effect must never protect a stale frame.
    val request=requestedLevel.takeIf{selectionOwnerLevel==level && it>0}
    val scene=city?.let{DriverScene.resolve(it,xp,request)}
    LaunchedEffect(game.districtId,level) {
        if(selectionOwnerLevel!=level){requestedLevel=0;selectionOwnerLevel=level}
    }
    var credits by remember{mutableStateOf(false)}
    var levels by remember{mutableStateOf(false)}
    BackHandler(onBack=model::exitNeutralPreview)
    DisposableEffect(activity) {
        if(activity!=null) {
            if(model.neutralOriginalOrientation==null)model.neutralOriginalOrientation=activity.requestedOrientation
            // A wm size override can invert the logical orientation relative to the physical panel.
            // Preserve the direction the user is already viewing on either kind of wide display.
            @Suppress("DEPRECATION")
            val display=if(Build.VERSION.SDK_INT>=30)activity.display else activity.windowManager.defaultDisplay
            val physicallyWide=display?.let {
                val naturalWide=it.mode.physicalWidth>it.mode.physicalHeight
                if(it.rotation==Surface.ROTATION_90 || it.rotation==Surface.ROTATION_270)!naturalWide else naturalWide
            } ?: false
            activity.requestedOrientation=if(physicallyWide || activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LOCKED else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(activity.window,false)
            WindowCompat.getInsetsController(activity.window,activity.window.decorView).apply {
                systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if(activity!=null && !activity.isChangingConfigurations) {
                activity.requestedOrientation=model.neutralOriginalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                model.neutralOriginalOrientation=null
                WindowCompat.setDecorFitsSystemWindows(activity.window,true)
                WindowCompat.getInsetsController(activity.window,activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val white=Color(0xFFF5F7FA)
    val muted=Color(0xFFCFD7DF)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF101C25)).testTag("neutralScene")) {
        val compact=maxWidth<760.dp
        if(allowed && scene!=null) DriverSceneBackground(scene,level)
        if(allowed && scene!=null)Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.65f),Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.7f)))))
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=if(compact)20.dp else 32.dp,vertical=20.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Box(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text("GridGuild",fontSize=if(compact)24.sp else 29.sp,fontWeight=FontWeight.Bold,color=white,letterSpacing=(-.7).sp)
                    Text("내 도시",fontSize=12.sp,color=muted)
                }
                Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("${Busan.find(game.districtId)?.name.orEmpty()} · Lv.$level",color=white,fontSize=20.sp,fontWeight=FontWeight.Medium,modifier=Modifier.testTag("neutralGrowth"))
                    Text(if(level==12)"$xp XP · 최고 레벨" else "$xp / ${CityLevels.thresholds[level]} XP",color=muted,fontSize=13.sp)
                    LinearProgressIndicator(progress={CityLevels.progress(xp)},modifier=Modifier.width(if(compact)145.dp else 200.dp).height(4.dp),color=Color(0xFFB7CFF2),trackColor=white.copy(alpha=.22f))
                }
                IconButton(onClick=model::exitNeutralPreview,modifier=Modifier.background(Color.Black.copy(alpha=.35f),RoundedCornerShape(24.dp)).testTag("neutralExit")) { Icon(Icons.Outlined.Close,"체험 나가기",tint=white) }
            }
            HudPill(Modifier.align(Alignment.TopCenter)) { Icon(Icons.Outlined.Panorama,null,tint=Color(0xFFBFD2F8),modifier=Modifier.size(21.dp));Text("N · TEST",color=white,fontSize=17.sp,fontWeight=FontWeight.SemiBold) }
            }
            if(!allowed || scene==null) Column(Modifier.align(Alignment.CenterHorizontally).widthIn(max=650.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Icon(if(allowed)Icons.Outlined.TravelExplore else Icons.Outlined.Lock,null,tint=muted,modifier=Modifier.size(44.dp))
                Text(if(allowed)"어느 도시를 둘러볼까요?" else "주차·플랫폼 허용 상태 확인 중",fontSize=25.sp,color=white,fontWeight=FontWeight.SemiBold)
                Text(if(allowed)"광안리 또는 센텀으로 목적지를 바꾸면\n현재 Lv.$level 도시의 도로 위에 진입합니다." else "허용 상태가 확인되기 전에는 도시를 표시하지 않습니다.",color=muted,fontSize=15.sp)
            }
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(allowed && scene!=null) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(scene.city.title,color=white,fontSize=if(compact)30.sp else 38.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.testTag("neutralLocation"))
                        Text("Lv.${scene.level} · ${scene.stageName}${if(scene.level<level)" · 이전 도시 둘러보기" else ""}",color=white,fontSize=17.sp,modifier=Modifier.testTag("neutralViewingLevel"))
                        Text(scene.landmarkLabel,color=muted,fontSize=14.sp,modifier=Modifier.testTag("neutralLandmark"))
                    }
                    TextButton(onClick={levels=true},modifier=Modifier.testTag("neutralLevels")) { Icon(Icons.Outlined.Layers,null,tint=white,modifier=Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text("단계 둘러보기",color=white) }
                    TextButton(onClick={credits=true},modifier=Modifier.testTag("neutralCredits")) { Icon(Icons.Outlined.Info,null,tint=white,modifier=Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text("장면 정보",color=white) }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    NeutralCity.entries.forEach { city ->
                        OutlinedButton(onClick={model.changeDemoDestination(city.destination)},enabled=allowed && !navigation.pending,
                            shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(scene?.city==city)white.copy(alpha=.8f) else white.copy(alpha=.25f)),
                            colors=ButtonDefaults.outlinedButtonColors(containerColor=if(scene?.city==city)Color(0xFFDBE8F4).copy(alpha=.18f) else Color.Black.copy(alpha=.3f),contentColor=white,disabledContentColor=muted),
                            modifier=Modifier.heightIn(min=50.dp).testTag("neutralPlace-${city.destinationId}")) {
                            Icon(if(scene?.city==city)Icons.Outlined.LocationOn else Icons.Outlined.Place,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(city.title)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(if(navigation.pending)"목적지 변경 중…" else scene?.let{"Lv.${it.level} · 도로 위 1인칭"}.orEmpty(),color=muted,fontSize=12.sp)
                }
            }
        }
        if(allowed) XpRewardHost(model.rewards,true,xp,{model.game.value.cityXp(model.game.value.districtId)},Modifier.align(Alignment.BottomEnd).padding(end=24.dp,bottom=140.dp))
    }
    if(credits && scene!=null && allowed) {
        AlertDialog(onDismissRequest={credits=false},title={Text("${scene.city.title} · Lv.${scene.level} 장면 정보")},
            text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("${scene.stageName}\n${scene.roadLabel}\n${scene.landmarkLabel}")
                Text("도로 위 운전자 눈높이의 1인칭 장면입니다. 지역과 도시 발전 단계를 바탕으로 AI로 생성한 가상 재구성입니다. 실제 역사·현재 도로·차량 카메라 정보로 사용하지 않습니다.",color=Subtle)
            }},confirmButton={TextButton(onClick={credits=false}){Text("닫기")}})
    }
    if(levels && scene!=null && allowed) AlertDialog(onDismissRequest={levels=false},
        title={Text("해금한 도시 둘러보기")},
        text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("현재 Lv.${level}까지 해금했어요. 선택해도 XP는 바뀌지 않습니다.",color=Subtle)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                (1..12).forEach { candidate ->
                    FilterChip(selected=scene.level==candidate,enabled=candidate<=level,
                        onClick={requestedLevel=if(candidate==level)0 else candidate;levels=false},
                        label={Text("Lv.$candidate${if(candidate>level)" · 잠김" else ""}")},
                        leadingIcon=if(candidate>level){{Icon(Icons.Outlined.Lock,null,Modifier.size(15.dp))}} else null,
                        modifier=Modifier.heightIn(min=48.dp).testTag("neutral-level-$candidate"))
                }
            }
        }},confirmButton={TextButton(onClick={requestedLevel=0;levels=false},modifier=Modifier.testTag("neutralCurrentLevel")){Text("지금의 도시")}},
        dismissButton={TextButton(onClick={levels=false}){Text("닫기")}})
}

@Composable private fun HudPill(modifier: Modifier=Modifier,content: @Composable RowScope.()->Unit) {
    Row(modifier.background(Color(0xFF17232D).copy(alpha=.78f),RoundedCornerShape(24.dp))
        .border(1.dp,Color(0xFFB7CFF2).copy(alpha=.55f),RoundedCornerShape(24.dp)).padding(horizontal=21.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
