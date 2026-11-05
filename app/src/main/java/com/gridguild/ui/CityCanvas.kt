package com.gridguild.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gridguild.R
import com.gridguild.core.Busan
import com.gridguild.core.CityStage
import kotlin.math.roundToInt

/** Twelve authored diorama scenes, cropped at draw time from three project-owned atlases. */
@Composable fun CityCanvas(level: Int,district: String,modifier: Modifier=Modifier,zoom: Float=1f) {
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
