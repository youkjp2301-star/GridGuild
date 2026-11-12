@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.gridguild.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.core.*
import com.gridguild.data.DemoAvailabilityScenario

@Composable internal fun AvailabilityBadge(status: AvailabilityStatus,modifier: Modifier=Modifier) {
    val (foreground,background)=when(status) {
        AvailabilityStatus.FREE -> GreenDark to SoftGreen
        AvailabilityStatus.BUSY -> Color(0xFFA34F12) to Color(0xFFFFEBDD)
        AvailabilityStatus.NONE -> Color(0xFF505A53) to Color(0xFFE3E7E3)
        AvailabilityStatus.UNKNOWN -> Color(0xFF69736C) to Color(0xFFF0F2EF)
    }
    Surface(modifier.semantics(mergeDescendants=true){},color=background,shape=RoundedCornerShape(8.dp),border=BorderStroke(.5.dp,foreground.copy(alpha=.10f))) {
        Row(Modifier.padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(4.dp).background(foreground,CircleShape))
            Text(status.label,fontSize=11.sp,lineHeight=16.sp,fontWeight=FontWeight.SemiBold,color=foreground)
        }
    }
}

@Composable internal fun ChargerAvailabilityInfo(candidate: NearbyStation,mockControls: Boolean,onScenario: (DemoAvailabilityScenario)->Unit) {
    val summary=candidate.availability
    val id=candidate.station.id
    var testDialog by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                AvailabilityBadge(summary.status,Modifier.testTag("availability-$id"))
                Text(summary.countLabel,fontSize=15.sp,lineHeight=21.sp,fontWeight=FontWeight.Medium,color=Ink,modifier=Modifier.testTag("chargerCount-$id").semantics(mergeDescendants=true){})
            }
            if(mockControls)IconButton(onClick={testDialog=true},modifier=Modifier.size(48.dp).testTag("availabilityTest-$id")){Icon(Icons.Outlined.MoreVert,"충전기 상태 테스트",Modifier.size(20.dp),tint=Subtle)}
        }
        val compatibility=when(candidate.compatibility) {
            ConnectorCompatibility.COMPATIBLE -> ""
            ConnectorCompatibility.INCOMPATIBLE -> "차량 커넥터 불일치 · 새 미션 참여 제한"
            ConnectorCompatibility.UNKNOWN -> "차량 커넥터 미확인 · 이용 전 호환 확인"
        }
        if(compatibility.isNotEmpty())Text(compatibility,color=if(candidate.compatibility==ConnectorCompatibility.INCOMPATIBLE)Color(0xFFA34F12) else Subtle,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.testTag("compatibility-$id").semantics(mergeDescendants=true){})
        if(candidate.compatibility==ConnectorCompatibility.COMPATIBLE && summary.compatibleAvailable!=null && summary.compatibleAvailable!=summary.available)
            Text("내 차량에 맞는 충전기 ${summary.compatibleAvailable}기 이용 가능",color=Subtle,fontSize=12.sp)
        if(summary.status==AvailabilityStatus.NONE)Text("지금은 빈자리가 없어요",color=Subtle,fontSize=12.sp,modifier=Modifier.testTag("availabilityWarning-$id").semantics(mergeDescendants=true){})
    }
    if(testDialog && mockControls) AlertDialog(onDismissRequest={testDialog=false},title={Text("충전기 상태 테스트")},
        text={Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("MOCK 전용 · 실제 충전소 정보는 바뀌지 않습니다.",fontSize=13.sp,color=Subtle)
            DemoAvailabilityScenario.entries.forEach { scenario ->
                TextButton(onClick={onScenario(scenario);testDialog=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("availability-$id-${scenario.name}")){Text(scenario.label)}
            }
        }},confirmButton={TextButton(onClick={testDialog=false},modifier=Modifier.heightIn(min=48.dp)){Text("닫기")}})
}
