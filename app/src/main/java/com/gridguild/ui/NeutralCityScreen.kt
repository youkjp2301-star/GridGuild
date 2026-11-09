package com.gridguild.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gridguild.GridViewModel
import com.gridguild.R
import com.gridguild.core.*
import com.gridguild.data.NavigationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class LandmarkPhoto(val resource: Int,val author: String,val source: String,val license: String,val licenseUrl: String)
internal fun NeutralCity.photo()=when(this) {
    NeutralCity.GWANGALLI -> LandmarkPhoto(R.drawable.immersive_gwangalli_v1,"lumoplank","https://commons.wikimedia.org/wiki/File:Gwangan_Bridge_and_Gwangalli_Beach_-_Gwangalli2721.jpg","CC0 1.0 Universal","https://creativecommons.org/publicdomain/zero/1.0/")
    NeutralCity.CENTUM -> LandmarkPhoto(R.drawable.immersive_centum_v1,"399scout","https://commons.wikimedia.org/wiki/File:Busan_Cinema_Center.jpg","CC BY-SA 4.0 International","https://creativecommons.org/licenses/by-sa/4.0/")
}

// At most two sampled photographs (~22 MiB total), decoded off the main thread.
private val landmarkCache=mutableMapOf<NeutralCity,ImageBitmap>()
private fun cached(city: NeutralCity)=synchronized(landmarkCache){landmarkCache[city]}
private suspend fun loadLandmark(context: Context,city: NeutralCity): ImageBitmap=withContext(Dispatchers.IO) {
    cached(city) ?: run {
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        BitmapFactory.decodeResource(context.resources,city.photo().resource,bounds)
        val options=BitmapFactory.Options().apply{
            inSampleSize=1
            while(bounds.outWidth/inSampleSize>2560 || bounds.outHeight/inSampleSize>1600)inSampleSize*=2
        }
        val decoded=checkNotNull(BitmapFactory.decodeResource(context.resources,city.photo().resource,options)){"랜드마크 사진을 읽지 못했어요"}.asImageBitmap()
        synchronized(landmarkCache){landmarkCache.getOrPut(city){decoded}}
    }
}
private fun Context.activity(): Activity?=when(this){is Activity->this;is ContextWrapper->baseContext.activity();else->null}

/** N is a mock presentation state; real P + platform safety checks remain active. */
@Composable internal fun NeutralCityScreen(game: GameState,navigation: NavigationState,allowed: Boolean,model: GridViewModel) {
    val context=LocalContext.current
    val activity=context.activity()
    val scene=if(navigation.hasDestination)NeutralCity.at(navigation.lat,navigation.lon) else null
    var credits by remember{mutableStateOf(false)}
    var linkUnavailable by remember{mutableStateOf(false)}
    BackHandler(onBack=model::exitNeutralPreview)
    DisposableEffect(activity) {
        if(activity!=null) {
            if(model.neutralOriginalOrientation==null)model.neutralOriginalOrientation=activity.requestedOrientation
            activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
    val xp=game.cityXp(game.districtId)
    val level=CityLevels.level(xp)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF101C25)).testTag("neutralScene")) {
        val compact=maxWidth<760.dp
        if(allowed && scene!=null) LandmarkBackground(scene)
        if(allowed && scene!=null)Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.65f),Color.Transparent,Color.Transparent,Color.Black.copy(alpha=.7f)))))
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=if(compact)20.dp else 32.dp,vertical=20.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Box(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text("GridGuild",fontSize=if(compact)24.sp else 29.sp,fontWeight=FontWeight.Bold,color=white,letterSpacing=(-.7).sp)
                    Text("가상도시 체험 · DEMO",fontSize=12.sp,color=muted)
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
                Text(if(allowed)"광안리 또는 센텀으로 데모 목적지를 바꾸면\n주요 랜드마크가 넓게 펼쳐집니다." else "허용 상태가 확인되기 전에는 도시를 표시하지 않습니다.",color=muted,fontSize=15.sp)
            }
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(allowed && scene!=null) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(scene.title,color=white,fontSize=if(compact)30.sp else 38.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.testTag("neutralLocation"))
                        Text(scene.landmark,color=muted,fontSize=17.sp,modifier=Modifier.testTag("neutralLandmark"))
                    }
                    TextButton(onClick={credits=true},modifier=Modifier.testTag("neutralCredits")) { Icon(Icons.Outlined.Info,null,tint=white,modifier=Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text("사진 출처",color=white) }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    NeutralCity.entries.forEach { city ->
                        OutlinedButton(onClick={model.changeDemoDestination(city.destination)},enabled=allowed && !navigation.pending,
                            shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(scene==city)white.copy(alpha=.8f) else white.copy(alpha=.25f)),
                            colors=ButtonDefaults.outlinedButtonColors(containerColor=if(scene==city)Color(0xFFDBE8F4).copy(alpha=.18f) else Color.Black.copy(alpha=.3f),contentColor=white,disabledContentColor=muted),
                            modifier=Modifier.heightIn(min=50.dp).testTag("neutralPlace-${city.destinationId}")) {
                            Icon(if(scene==city)Icons.Outlined.LocationOn else Icons.Outlined.Place,null,Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text(city.title)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(if(navigation.pending)"목적지 변경 중…" else "실사 파노라마 · 실시간 화면 아님",color=muted,fontSize=12.sp)
                }
            }
        }
        if(allowed) XpRewardHost(model.rewards,true,xp,{model.game.value.cityXp(model.game.value.districtId)},Modifier.align(Alignment.BottomEnd).padding(end=24.dp,bottom=140.dp))
    }
    if(credits && scene!=null && allowed) {
        val photo=scene.photo();val uri=LocalUriHandler.current
        val openLink:(String)->Unit={url ->
            try {uri.openUri(url);linkUnavailable=false}
            catch(e: Exception){linkUnavailable=true}
        }
        AlertDialog(onDismissRequest={credits=false},title={Text("${scene.landmark} · 사진 출처")},
            text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("사진: ${photo.author}\n${photo.license} · Wikimedia Commons\n원본 파일 그대로 수록, 화면에 맞춰 크기만 조절합니다. 과거에 촬영된 사진이며 현재 현장의 모습은 아닙니다.")
                TextButton(onClick={openLink(photo.source)}){Text("원본·저작자 보기")}
                TextButton(onClick={openLink(photo.licenseUrl)}){Text("사진 이용허락 보기")}
                if(linkUnavailable)Text("이 기기에서 웹 링크를 열 수 없어요.",color=Subtle)
            }},confirmButton={TextButton(onClick={credits=false}){Text("닫기")}})
    }
}

@Composable private fun HudPill(modifier: Modifier=Modifier,content: @Composable RowScope.()->Unit) {
    Row(modifier.background(Color(0xFF17232D).copy(alpha=.78f),RoundedCornerShape(24.dp))
        .border(1.dp,Color(0xFFB7CFF2).copy(alpha=.55f),RoundedCornerShape(24.dp)).padding(horizontal=21.dp,vertical=12.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp),content=content)
}

@Composable private fun LandmarkBackground(city: NeutralCity) {
    val context=LocalContext.current.applicationContext
    var retry by remember(city){mutableIntStateOf(0)}
    var bitmap by remember(city){mutableStateOf(cached(city))}
    var error by remember(city){mutableStateOf(false)}
    LaunchedEffect(city,retry) {
        if(bitmap==null) {
            error=false;val started=android.os.SystemClock.elapsedRealtime()
            try { val loaded=loadLandmark(context,city);holdLoadingMinimum(started);bitmap=loaded }
            catch(e: kotlinx.coroutines.CancellationException){throw e}
            catch(e: Exception){error=true}
        }
    }
    val loaded=bitmap
    if(loaded!=null)Image(loaded,"${city.title} ${city.landmark} 실사 파노라마",Modifier.fillMaxSize().testTag("neutralPhoto-${city.destinationId}"),contentScale=ContentScale.Fit)
    else Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
        if(error)Button(onClick={retry++}){Text("사진 다시 불러오기")}
        else CityGrowthLoading(Modifier.widthIn(max=540.dp),label="${city.landmark} 풍경을 펼치고 있어요",loop=false)
    }
}
