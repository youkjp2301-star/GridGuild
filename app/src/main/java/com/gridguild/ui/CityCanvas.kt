package com.gridguild.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.R
import com.gridguild.core.Busan
import com.gridguild.core.CityStage
import com.gridguild.core.CityUnlocks
import kotlin.math.roundToInt

/** Twelve authored diorama scenes, cropped at draw time from three project-owned atlases. */
@Composable fun CityCanvas(level: Int,district: String,unlockedThrough: Int,modifier: Modifier=Modifier,zoom: Float=1f) {
    // Check before loading an atlas, including outgoing Crossfade frames after an XP/region reset.
    if(!CityUnlocks.canView(level,unlockedThrough)) {
        LockedCityArtwork(modifier)
        return
    }
    val stage=CityStage.at(level)
    val resource=cityArtResource(stage.era)
    val context=LocalContext.current
    var retry by remember(resource){mutableIntStateOf(0)}
    val cached=remember(resource){CityArtCache.peek(resource)}
    val loaded by produceState<Pair<Int,Result<ImageBitmap>>?>(cached?.let{resource to Result.success(it)},resource,retry) {
        val started=android.os.SystemClock.elapsedRealtime()
        val result=withContext(Dispatchers.IO){runCatching{CityArtCache.load(context.resources,resource)}}
        if(cached==null && result.isSuccess)holdLoadingMinimum(started)
        value=resource to result
    }
    // Keyed result prevents an older era from being rendered after an XP reset.
    val art=loaded?.takeIf{it.first==resource}?.second
    val atlas=cached ?: art?.getOrNull()
    if(atlas==null) {
        if(art?.isFailure==true) Box(modifier,contentAlignment=Alignment.Center) {
            TextButton(onClick={retry++}){Text("도시 이미지 다시 불러오기")}
        } else CityGrowthLoading(modifier,label="도시 풍경 불러오는 중",loop=false,caption=false)
        return
    }
    Canvas(modifier.clip(RoundedCornerShape(20.dp)).background(Bg).semantics {
        contentDescription="${Busan.find(district)?.name ?: "도시"} Lv.${stage.level} ${stage.name} · ${stage.landmark}"
    }) {
        val frameWidth=atlas.width/2
        val frameHeight=atlas.height/2
        val src=IntSize(frameWidth-4,frameHeight-4)
        val scale=minOf(size.width/src.width,size.height/src.height)*zoom
        val dst=IntSize((src.width*scale).roundToInt(),(src.height*scale).roundToInt())
        drawImage(atlas,srcOffset=IntOffset((stage.frame%2)*frameWidth+2,(stage.frame/2)*frameHeight+2),
            srcSize=src,dstOffset=IntOffset(((size.width-dst.width)/2).roundToInt(),((size.height-dst.height)/2).roundToInt()),
            dstSize=dst,filterQuality=FilterQuality.High)
    }
}

private fun cityArtResource(era: Int)=when(era) {
        0 -> R.drawable.city_medieval_v4
        1 -> R.drawable.city_modern_v4
        else -> R.drawable.city_future_v4
    }
/** Cold scenes honor the user's minimum duration; warm cache hits do not flash a loader. */
private object CityArtCache {
    private val images=android.util.LruCache<Int,ImageBitmap>(3)
    @Synchronized fun peek(id: Int): ImageBitmap? =images.get(id)
    @Synchronized fun clear(){images.evictAll()}
    @Synchronized fun load(resources: android.content.res.Resources,id: Int): ImageBitmap =
        images.get(id) ?: requireNotNull(BitmapFactory.decodeResource(resources,id)).asImageBitmap().also{images.put(id,it)}
}
/** Deterministic cold/warm rendering checks, without touching the saved game. */
internal fun clearCityArtMemoryCache(){CityArtCache.clear()}
internal suspend fun preloadCityArt(resources: android.content.res.Resources,level: Int,unlockedThrough: Int) {
    if(CityUnlocks.canView(level,unlockedThrough))withContext(Dispatchers.IO){runCatching{CityArtCache.load(resources,cityArtResource(CityStage.at(level).era))}}
}

/** Original geometric placeholder. It never reads or blurs an unreleased city image. */
@Composable internal fun LockedCityArtwork(modifier: Modifier=Modifier,compact: Boolean=false) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF263B2D),Color(0xFF405B48))))) {
        Canvas(Modifier.matchParentSize()) {
            fun point(x: Float,y: Float)=Offset(size.width*x,size.height*y)
            fun shape(points: List<Offset>,color: Color) {
                drawPath(Path().apply{moveTo(points[0].x,points[0].y);points.drop(1).forEach{lineTo(it.x,it.y)};close()},color)
            }
            shape(listOf(point(.16f,.60f),point(.49f,.36f),point(.85f,.59f),point(.51f,.86f)),Color(0xFF718792).copy(alpha=.23f))
            for(i in 0..5) {
                val x=.26f+i*.07f
                drawLine(Color.White.copy(alpha=.045f),point(x,.48f),point(x+.31f,.70f),1f)
                drawLine(Color.White.copy(alpha=.045f),point(.30f+i*.07f,.74f),point(.62f+i*.035f,.49f),1f)
            }
        }
        Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(if(compact)6.dp else 12.dp)) {
            Surface(color=Color.White.copy(alpha=.12f),shape=CircleShape) {
                Icon(Icons.Outlined.Lock,null,tint=Color(0xFFF3E5C9),modifier=Modifier.padding(if(compact)10.dp else 16.dp).size(if(compact)22.dp else 34.dp))
            }
            Text(if(compact)"LOCKED" else "해금 후 공개됩니다",fontSize=if(compact)10.sp else 15.sp,color=Color(0xFFE5EDF0),letterSpacing=if(compact)1.5.sp else 0.sp)
        }
    }
}
