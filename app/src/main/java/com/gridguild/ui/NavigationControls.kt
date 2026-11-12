@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.gridguild.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.gridguild.data.NavigationOperation
import com.gridguild.data.NavigationState
import com.gridguild.core.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable internal fun NavigationControls(n: NavigationState,mock: Boolean,onRefresh: ()->Unit,onReroute: ()->Unit,
    onDemoDestination: (DemoDestination)->Unit,onRandomDestination: ()->Unit) {
    var tools by rememberSaveable { mutableStateOf(false) }
    var chooseDestination by remember(mock) { mutableStateOf(false) }
    Text(if(n.hasDestination)"목적지 ${destinationName(n.destination)}" else "목적지를 선택하세요",
        color=Ink,fontSize=22.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.testTag("navDestination"))
    if(mock) Button(onClick={chooseDestination=true},enabled=!n.pending,modifier=Modifier.heightIn(min=52.dp).testTag("changeDemoDestination")) {
        if(n.pending && n.operation==NavigationOperation.DESTINATION) CircularProgressIndicator(Modifier.size(17.dp),strokeWidth=2.dp)
        else Icon(Icons.Outlined.Place,null,Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp));Text(if(n.pending && n.operation==NavigationOperation.DESTINATION)"변경 중…" else "목적지 변경")
    }
    val status=when {
        n.error -> n.status
        n.pending -> "처리 중…"
        n.confirmed && n.requestId>0 -> when(n.operation) {
            NavigationOperation.DESTINATION -> "변경 완료"
            NavigationOperation.WAYPOINT -> "경유지 추가 완료"
            NavigationOperation.REFRESH -> "갱신 완료"
            NavigationOperation.REROUTE -> "재탐색 완료"
            null -> ""
        }
        else -> ""
    }
    if(status.isNotBlank())Text(status,color=if(n.error)MaterialTheme.colorScheme.error else GreenDark,modifier=Modifier.testTag("navStatus"))
    TextButton(onClick={tools=!tools},modifier=Modifier.testTag("navTools")) {
        Text(if(tools)"내비 연결 도구 접기" else "내비 연결 도구")
        Icon(if(tools)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null)
    }
    if(tools || n.error) {
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
            Spacer(Modifier.width(6.dp));Text(if(n.pending && n.operation==NavigationOperation.REROUTE)"재탐색 응답 대기…" else "경로 재탐색")
        }
    }
    if(!n.hasDestination && !n.pending) Text("목적지를 먼저 설정하세요.",fontSize=13.sp,color=Subtle)
    }
    if(chooseDestination && mock) AlertDialog(onDismissRequest={chooseDestination=false},
        title={Text("목적지 변경")},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            DemoDestinations.places.forEach { place ->
                val current=n.lat?.let{lat->n.lon?.let{lon->ChargingRecommendations.distanceKm(lat,lon,place.lat,place.lon)<.05}}==true
                OutlinedButton(onClick={onDemoDestination(place);chooseDestination=false},enabled=!current && !n.pending,
                    modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).testTag("demo-destination-${place.id}")) {
                    Text(place.name+if(current)" · 현재" else "",Modifier.fillMaxWidth())
                }
            }
        }},
        confirmButton={Button(onClick={onRandomDestination();chooseDestination=false},modifier=Modifier.testTag("randomDemoDestination")){Text("랜덤 위치로 변경")}},
        dismissButton={TextButton(onClick={chooseDestination=false}){Text("닫기")}})
}

internal fun destinationName(raw: String)=raw.removeSuffix("(MOCK)").trim()

private fun time(at: Long)=DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Asia/Seoul")).format(Instant.ofEpochMilli(at))
