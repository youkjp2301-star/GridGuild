@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.gridguild.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.core.*
import com.gridguild.data.DemoAvailabilityScenario
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable internal fun AvailabilityBadge(status: AvailabilityStatus,modifier: Modifier=Modifier) {
    val (foreground,background)=when(status) {
        AvailabilityStatus.FREE -> Green to SoftGreen
        AvailabilityStatus.BUSY -> Color(0xFF984A11) to Color(0xFFFFEBD6)
        AvailabilityStatus.NONE -> Color(0xFF4F5852) to Color(0xFFE2E5E1)
        AvailabilityStatus.UNKNOWN -> Subtle to Color(0xFFF0F0EB)
    }
    Surface(modifier.semantics(mergeDescendants=true){},color=background,shape=RoundedCornerShape(20.dp),border=BorderStroke(.5.dp,foreground.copy(alpha=.12f))) {
        Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(5.dp).background(foreground,CircleShape))
            Text(status.label,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=foreground)
        }
    }
}

@Composable internal fun ChargerAvailabilityInfo(candidate: NearbyStation,now: Long,mockControls: Boolean,onScenario: (DemoAvailabilityScenario)->Unit) {
    val summary=candidate.availability
    val id=candidate.station.id
    var testDialog by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            AvailabilityBadge(summary.status,Modifier.testTag("availability-$id"))
            Text(summary.countLabel,fontSize=14.sp,fontWeight=FontWeight.Medium,modifier=Modifier.testTag("chargerCount-$id"))
        }
        val compatibility=when(candidate.compatibility) {
            ConnectorCompatibility.COMPATIBLE -> "차량 커넥터 일치"+(if(mockControls)" · 데모 차량 기준" else "")
            ConnectorCompatibility.INCOMPATIBLE -> "차량 커넥터 불일치 · 새 미션 참여 제한"
            ConnectorCompatibility.UNKNOWN -> "차량 커넥터 미확인 · 이용 전 호환 확인"
        }
        Text(compatibility,color=if(candidate.compatibility==ConnectorCompatibility.INCOMPATIBLE)Color(0xFF984A11) else Subtle,fontSize=12.sp,modifier=Modifier.testTag("compatibility-$id"))
        if(candidate.compatibility==ConnectorCompatibility.COMPATIBLE && summary.compatibleAvailable!=null && summary.compatibleAvailable!=summary.available)
            Text("내 차량에 맞는 충전기 ${summary.compatibleAvailable}기 이용 가능",color=Subtle,fontSize=12.sp)
        Row(verticalAlignment=Alignment.CenterVertically) {
            val age=summary.updatedAt?.let{now-it}
            val time=summary.updatedAt?.let{DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul")).format(Instant.ofEpochMilli(it))}
            val source=if(summary.source==AvailabilitySource.PUBLIC_API)"공공 API" else "MOCK"
            Text("$source · "+when {
                time==null -> "갱신 정보 없음"
                age!=null && age<0 -> "확인 시각 검증 필요"
                age!=null && age>ChargingAvailability.maxAgeMs -> "$time 확인 · 갱신 지연"
                else -> "$time 확인"
            },Modifier.weight(1f).testTag("availabilityTime-$id"),fontSize=12.sp,color=Subtle)
            if(mockControls)TextButton(onClick={testDialog=true},modifier=Modifier.testTag("availabilityTest-$id")){Text("상태 테스트",fontSize=12.sp)}
        }
        if(summary.status==AvailabilityStatus.NONE) Text("현재 이용 가능한 충전기가 없어 새 미션은 참여할 수 없어요. 경유지 선택 전 상태를 다시 확인하세요.",color=Subtle,fontSize=13.sp,modifier=Modifier.testTag("availabilityWarning-$id"))
        else if(summary.status==AvailabilityStatus.UNKNOWN) Text("확인되지 않은 상태예요. 이용 전 충전소 정보를 확인하세요.",color=Subtle,fontSize=12.sp)
    }
    if(testDialog && mockControls) AlertDialog(onDismissRequest={testDialog=false},title={Text("충전기 상태 테스트")},
        text={Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("MOCK 전용 · 실제 충전소 정보는 바뀌지 않습니다.",fontSize=13.sp,color=Subtle)
            DemoAvailabilityScenario.entries.forEach { scenario ->
                TextButton(onClick={onScenario(scenario);testDialog=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("availability-$id-${scenario.name}")){Text(scenario.label)}
            }
        }},confirmButton={TextButton(onClick={testDialog=false}){Text("닫기")}})
}
