package com.gridguild.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    val resource=when(stage.era) {
        0 -> R.drawable.city_medieval_v2
        1 -> R.drawable.city_modern_v2
        else -> R.drawable.city_future_v2
    }
    val atlas=ImageBitmap.imageResource(resource)
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF18242B)).semantics {
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

/** Original geometric placeholder. It never reads or blurs an unreleased city image. */
@Composable internal fun LockedCityArtwork(modifier: Modifier=Modifier,compact: Boolean=false) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(0xFF1B3748),Color(0xFF304C5B))))) {
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
