package com.gridguild.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.core.*
import kotlin.math.cos

/** Local coordinate plot of sample locations; deliberately has no road tiles or routing lines. */
@Composable internal fun NearbyStationMap(lat: Double,lon: Double,nearby: List<NearbyStation>) {
    Text("주변 샘플 위치 · 목적지 반경 5 km",fontSize=14.sp,color=Ink)
    Canvas(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp)).background(Pale)
        .testTag("nearbyMap").semantics {
            contentDescription="목적지 중심 반경 5 km, 가상 충전소 ${nearby.size}곳. "+nearby.mapIndexed { i,it -> "${i+1}번 ${it.station.name} 직선 %.1f km".format(java.util.Locale.KOREA,it.distanceKm) }.joinToString(", ")+". 도로 지도 및 실제 시설 아님"
        }) {
        val center=Offset(size.width/2,size.height/2)
        val radius=minOf(size.width,size.height)*.40f
        val scale=radius/ChargingRecommendations.radiusKm.toFloat()
        repeat(9) { i ->
            val x=size.width*i/8;val y=size.height*i/8
            drawLine(Color(0xFFDDE8E7),Offset(x,0f),Offset(x,size.height),1f)
            drawLine(Color(0xFFDDE8E7),Offset(0f,y),Offset(size.width,y),1f)
        }
        drawCircle(Green.copy(alpha=.07f),radius,center)
        drawCircle(Color.White,10.dp.toPx(),center);drawCircle(Blue,7.dp.toPx(),center)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=12.sp.toPx();textAlign=Paint.Align.CENTER;isFakeBoldText=true }
        nearby.forEachIndexed { index,item ->
            val east=(item.station.lon-lon)*111.32*cos(Math.toRadians(lat))
            val north=(item.station.lat-lat)*110.57
            val actual=Offset(center.x+(east*scale).toFloat(),center.y-(north*scale).toFloat())
            val delta=actual-center
            val point=if(delta.getDistance()<32.dp.toPx()) {
                val direction=if(delta.getDistance()<1f)Offset(-.707f,-.707f) else delta/delta.getDistance()
                center+direction*36.dp.toPx()
            } else actual
            if(point!=actual) { drawLine(Green.copy(alpha=.45f),actual,point,1.dp.toPx());drawCircle(Green,3.dp.toPx(),actual) }
            drawCircle(Color.White,16.dp.toPx(),point);drawCircle(Green,13.dp.toPx(),point)
            paint.color=Ink.toArgb();drawContext.canvas.nativeCanvas.drawText("${index+1}",point.x,point.y+4.dp.toPx(),paint)
        }
        paint.color=Ink.toArgb();drawContext.canvas.nativeCanvas.drawText("목적지",center.x,center.y+30.dp.toPx(),paint)
        paint.textAlign=Paint.Align.LEFT;drawContext.canvas.nativeCanvas.drawText("N ↑",16.dp.toPx(),25.dp.toPx(),paint)
    }
    Text("가상 충전소 · 실제 시설 아님. 위치 관계도는 직선거리 기준이며 도로·경로를 제공하지 않습니다.",fontSize=12.sp,color=Subtle)
}
