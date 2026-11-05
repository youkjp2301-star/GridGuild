@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.gridguild.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.data.NavigationOperation
import com.gridguild.data.NavigationState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable internal fun NavigationControls(n: NavigationState,mock: Boolean,onRefresh: ()->Unit,onReroute: ()->Unit) {
    Text("${n.destination} · ${if(mock)"데모" else n.source}",color=Subtle,modifier=Modifier.testTag("navDestination"))
    Text(n.status,color=when { n.error->MaterialTheme.colorScheme.error;n.confirmed->Green;else->Subtle },modifier=Modifier.testTag("navStatus"))
    if(n.requestId>0) Text("요청 #${n.requestId} · ${if(n.pending)"처리 중" else n.resultAt?.let{time(it)} ?: "대기"}",color=Subtle,fontSize=12.sp,modifier=Modifier.testTag("navRequest"))
    n.destinationUpdatedAt?.let { Text("최근 목적지 확인 ${time(it)}",color=Subtle,fontSize=12.sp) }
    FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick=onRefresh,enabled=!n.pending,modifier=Modifier.heightIn(min=52.dp).testTag("navRefresh")) {
            if(n.pending && n.operation==NavigationOperation.REFRESH) CircularProgressIndicator(Modifier.size(17.dp),strokeWidth=2.dp)
            else Icon(Icons.Outlined.Refresh,null,Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp));Text(if(n.pending && n.operation==NavigationOperation.REFRESH)"목적지 조회 중…" else "목적지 새로고침")
        }
        OutlinedButton(onClick=onReroute,enabled=!n.pending && n.hasDestination,modifier=Modifier.heightIn(min=52.dp).testTag("navReroute")) {
            if(n.pending && n.operation==NavigationOperation.REROUTE) CircularProgressIndicator(Modifier.size(17.dp),strokeWidth=2.dp)
            else Icon(Icons.Outlined.Route,null,Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp));Text(if(n.pending && n.operation==NavigationOperation.REROUTE)"재탐색 응답 대기…" else if(mock)"경로 재탐색 (데모)" else "경로 재탐색 요청")
        }
    }
    if(mock) Text("데모는 요청 흐름을 시뮬레이션하며 실제 경로 계산·내비 안내는 하지 않습니다.",fontSize=12.sp,color=Subtle)
    else if(!n.hasDestination && !n.pending) Text("재탐색하려면 내비 목적지를 설정하고 새로고침해 좌표를 확인하세요.",fontSize=13.sp,color=Subtle)
}

private fun time(at: Long)=DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Asia/Seoul")).format(Instant.ofEpochMilli(at))
