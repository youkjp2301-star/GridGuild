package com.gridguild.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/** Code-native 2.5D artwork, three eras and district-specific rivers/bridge/hill motifs. */
@Composable fun CityCanvas(level: Int,district: String,modifier: Modifier=Modifier,zoom: Float=1f) {
    Canvas(modifier) {
        val era=when { level<=3 -> 0;level<=8 -> 1;else -> 2 }
        val unit=minOf(size.width/13f,size.height/9f)*zoom
        withTransform({ translate(size.width/2,size.height*.24f);scale(unit,unit,Offset.Zero) }) {
            val land=Color(if(era==0)0xFFC8D9BF else if(era==1)0xFFDDEAF2 else 0xFFBEE6E6)
            polygon(listOf(Offset(0f,0f),Offset(5.6f,2.8f),Offset(0f,5.6f),Offset(-5.6f,2.8f)),land)
            polygon(listOf(Offset(-5.6f,2.8f),Offset(0f,5.6f),Offset(0f,5.85f),Offset(-5.6f,3.05f)),Color(0xFFA7BCC5))
            polygon(listOf(Offset(0f,5.6f),Offset(5.6f,2.8f),Offset(5.6f,3.05f),Offset(0f,5.85f)),Color(0xFF8FA9BA))
            val river=district in setOf("sasang","buk","saha","gangseo","haeundae","suyeong","yeongdo","gijang","seo","dong","nam","jung")
            if(river) polygon(listOf(Offset(-5.3f,2.85f),Offset(0f,5.5f),Offset(1.5f,4.75f),Offset(-3.8f,2.1f)),Color(0xFF83BDE7))
            for(i in -2..2) {
                drawLine(Color.White.copy(alpha=.7f),Offset(i.toFloat()-2.3f,1.3f+i*.3f),Offset(i+2.3f,3.6f+i*.3f),.10f)
            }
            val count=(4+level/2).coerceAtMost(10)
            val seed=district.hashCode().let { kotlin.math.abs(it%7) }
            val plots=listOf(Offset(-.2f,1f),Offset(1.3f,1.7f),Offset(-1.7f,1.7f),Offset(0f,2.5f),Offset(2.6f,2.4f),Offset(-2.8f,2.6f),Offset(1.5f,3.3f),Offset(-1.3f,3.3f),Offset(0f,4f),Offset(3.6f,2.9f))
            plots.take(count).sortedBy { it.y }.forEachIndexed { i,p ->
                val h=when(era) { 0 -> .45f+(i%3)*.2f;1 -> .7f+(i%3)*.6f;else -> 1.2f+(i%4)*.5f }
                building(p,.65f,h,era,i+seed)
            }
            if(district in setOf("suyeong","yeongdo","sasang")) {
                drawLine(Color(0xFFEFF7FF),Offset(-4f,3.5f),Offset(-1f,4.2f),.18f)
                drawLine(Color(0xFF4D91C9),Offset(-3.5f,3.4f),Offset(-3.5f,2.5f),.12f)
                drawLine(Color(0xFF4D91C9),Offset(-1.5f,3.95f),Offset(-1.5f,3.05f),.12f)
                drawLine(Color.White,Offset(-3.5f,2.5f),Offset(-1.5f,3.05f),.04f)
            }
            for(i in 0..6) {
                val x=-3.6f+i*.8f;val y=1.3f+kotlin.math.abs(i-3)*.4f
                drawLine(Color(0xFF647E63),Offset(x,y),Offset(x,y-.35f),.07f)
                drawCircle(Color(if(era==2)0xFF26866E else 0xFF83AB84),.20f,Offset(x,y-.42f))
            }
        }
    }
}
private fun DrawScope.polygon(points: List<Offset>,color: Color) {
    drawPath(Path().apply { moveTo(points[0].x,points[0].y);points.drop(1).forEach{lineTo(it.x,it.y)};close() },color)
}
private fun DrawScope.building(p: Offset,w: Float,h: Float,era: Int,index: Int) {
    val x=p.x;val y=p.y
    val front=Color(if(era==0)0xFFCAA982 else if(era==1)0xFF83BDE7 else 0xFF60A7C0)
    val side=Color(if(era==0)0xFFAB8968 else if(era==1)0xFF4D91C9 else 0xFF31758E)
    polygon(listOf(Offset(x-w,y-h),Offset(x,y-h+w*.5f),Offset(x,y+w*.5f),Offset(x-w,y)),front)
    polygon(listOf(Offset(x,y-h+w*.5f),Offset(x+w,y-h),Offset(x+w,y),Offset(x,y+w*.5f)),side)
    polygon(listOf(Offset(x-w,y-h),Offset(x,y-h-w*.5f),Offset(x+w,y-h),Offset(x,y-h+w*.5f)),Color(if(era==0)0xFF8C6B68 else 0xFFE5F3FC))
    if(era==0) polygon(listOf(Offset(x-w-.1f,y-h),Offset(x,y-h-.65f),Offset(x+w+.1f,y-h),Offset(x,y-h+.32f)),Color(0xFF718AA1))
    else {
        for(i in 0..(h/.35f).toInt()) drawLine(Color(0xFFC7F4FF),Offset(x+.12f,y-h+.22f+i*.27f),Offset(x+w-.1f,y-h-.02f+i*.27f),.06f)
        if(era==2) { drawLine(Color(0xFF26866E),Offset(x,y-h),Offset(x,y-h-.6f),.06f);drawCircle(Color(0xFF83BDE7),.16f,Offset(x,y-h-.65f)) }
    }
}
