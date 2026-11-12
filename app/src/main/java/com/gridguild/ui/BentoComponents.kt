package com.gridguild.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

internal enum class BentoTone { SURFACE, SUBTLE, DARK, GREEN }

/** Presentation-only motion. Recomposition and vehicle heartbeats never restart entry. */
@Composable internal fun BentoCard(modifier: Modifier=Modifier,tone: BentoTone=BentoTone.SURFACE,index: Int=0,
    onClick:(()->Unit)?=null,content:@Composable ColumnScope.()->Unit) {
    val motion=motionEnabled()
    var entered by remember{mutableStateOf(!motion)}
    LaunchedEffect(Unit){if(motion)delay(index.coerceIn(0,5)*35L);entered=true}
    val entry by animateFloatAsState(if(entered)1f else 0f,tween(if(motion)220 else 0),label="bentoEntry")
    val interactions=remember{MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed && onClick!=null).985f else 1f,tween(if(motion)130 else 0),label="cardPress")
    val dark=tone==BentoTone.DARK
    val fill=when(tone){BentoTone.DARK->Charcoal;BentoTone.SUBTLE->Pale;BentoTone.GREEN->SoftGreen;else->Paper}
    val action=if(onClick!=null)Modifier.clickable(interactionSource=interactions,indication=LocalIndication.current,onClick=onClick) else Modifier
    Surface(modifier.graphicsLayer{alpha=entry;translationY=(1f-entry)*10.dp.toPx();scaleX=scale;scaleY=scale}
        .animateContentSize(tween(if(motion)240 else 0)).then(action),
        color=fill,contentColor=if(dark)Color.White else Ink,shape=RoundedCornerShape(24.dp),
        border=if(tone==BentoTone.SURFACE)BorderStroke(1.dp,Line) else null) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}

@Composable internal fun SectionHeader(title: String,kicker: String?=null) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        kicker?.let{Text(it,color=Subtle,fontSize=11.sp,lineHeight=16.sp,fontWeight=FontWeight.Medium,letterSpacing=1.2.sp)}
        Text(title,style=MaterialTheme.typography.headlineMedium,color=Ink)
    }
}

@Composable internal fun AnimatedNumber(value: Int,modifier: Modifier=Modifier,suffix: String="",prefix: String="",
    color: Color=Ink,fontSize: TextUnit=32.sp) {
    val number by animateIntAsState(value,tween(if(motionEnabled())450 else 0,easing=FastOutSlowInEasing),label="stateNumber")
    Text("$prefix$number$suffix",modifier,color=color,fontSize=fontSize,fontWeight=FontWeight.SemiBold,letterSpacing=(-.8).sp)
}

@Composable internal fun BentoProgress(progress: Float,modifier: Modifier=Modifier,color: Color=Green) {
    val displayed by animateFloatAsState(progress.coerceIn(0f,1f),tween(if(motionEnabled())450 else 0),label="stateProgress")
    LinearProgressIndicator(progress={displayed},modifier=modifier.fillMaxWidth().height(7.dp),color=color,
        trackColor=if(color==Green)Line else color.copy(alpha=.12f),strokeCap=StrokeCap.Round)
}

@Composable internal fun SocGauge(soc: Float?,fresh: Boolean,targetSoc: Int?,modifier: Modifier=Modifier,
    size: Dp=112.dp,dark: Boolean=false,showValue: Boolean=true) {
    val valid=if(fresh)soc?.takeIf{it.isFinite() && it in 0f..100f} else null
    val progress by animateFloatAsState((valid ?: 0f)/100f,tween(if(motionEnabled())450 else 0),label="socArc")
    Box(modifier.size(size).semantics{stateDescription=if(valid==null)"차량 배터리 확인 중" else "배터리 ${valid.toInt()}%, 목표 ${targetSoc?.let{"$it%"} ?: "미설정"}"},contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            val width=8.dp.toPx()
            drawArc(if(dark)Color.White.copy(alpha=.10f) else Pale,-90f,360f,false,style=Stroke(width,cap=StrokeCap.Round))
            if(valid!=null)drawArc(Green,-90f,360f*progress,false,style=Stroke(width,cap=StrokeCap.Round))
            if(targetSoc!=null && targetSoc in 1..100) {
                val angle=Math.toRadians(targetSoc*3.6-90)
                val radius=this.size.minDimension/2
                val point=center+Offset(cos(angle).toFloat()*radius,sin(angle).toFloat()*radius)
                drawCircle(if(dark)Color.White else Ink,3.dp.toPx(),point)
            }
        }
        if(showValue)if(valid!=null)AnimatedNumber(valid.toInt(),suffix="%",fontSize=if(size>=120.dp)34.sp else 28.sp,color=if(dark)Color.White else Ink)
            else Text("—%",fontSize=30.sp,color=if(dark)Color.White else Ink)
    }
}

@Composable internal fun StatusBadge(text: String,color: Color=Green,modifier: Modifier=Modifier) {
    Surface(modifier,color=color.copy(alpha=.12f),shape=RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal=9.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(5.dp).background(color,RoundedCornerShape(3.dp)))
            Text(text,color=if(color==Green)GreenDark else color,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable internal fun PrimaryButton(text: String,onClick:()->Unit,modifier: Modifier=Modifier,
    enabled: Boolean=true,loading: Boolean=false,icon: ImageVector?=null) {
    val interactions=remember{MutableInteractionSource()}
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).975f else 1f,tween(if(motionEnabled())130 else 0),label="buttonPress")
    Button(onClick=onClick,enabled=enabled && !loading,interactionSource=interactions,
        shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Ink,
            disabledContainerColor=Pale,disabledContentColor=Subtle),
        modifier=modifier.heightIn(min=52.dp).graphicsLayer{scaleX=scale;scaleY=scale}) {
        if(loading){CircularProgressIndicator(Modifier.size(18.dp),color=Ink,strokeWidth=2.dp);Spacer(Modifier.width(8.dp))}
        else icon?.let{Icon(it,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp))}
        Text(text,fontWeight=FontWeight.SemiBold)
    }
}

@Composable internal fun StatCard(label: String,value: Int,modifier: Modifier=Modifier,suffix: String="",hint: String?=null,
    tone: BentoTone=BentoTone.SURFACE,index: Int=0,onClick:(()->Unit)?=null) {
    BentoCard(modifier,tone,index,onClick) {
        Text(label,fontSize=13.sp,color=if(tone==BentoTone.DARK)OnDarkMuted else Subtle)
        AnimatedNumber(value,suffix=suffix,color=if(tone==BentoTone.DARK)Color.White else Ink)
        hint?.let{Text(it,fontSize=12.sp,color=if(tone==BentoTone.DARK)OnDarkMuted else Subtle)}
    }
}
