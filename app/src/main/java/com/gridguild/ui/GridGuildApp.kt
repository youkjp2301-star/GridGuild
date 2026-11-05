@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gridguild.GridViewModel
import com.gridguild.core.*
import com.gridguild.data.NavigationState

private val Blue=Color(0xFF4D91C9)
private val Ink=Color(0xFF182C40)
private val Subtle=Color(0xFF526779)
private val Pale=Color(0xFFDCEFFD)
private val Bg=Color(0xFFF7FAFD)
private val Green=Color(0xFF26866E)
private val tabs=listOf("홈","충전 미션","가상 도시","랭킹")
private val tabIcons=listOf(Icons.Outlined.Home,Icons.Outlined.EvStation,Icons.Outlined.LocationCity,Icons.Outlined.Leaderboard)

@Composable fun GridGuildApp(model: GridViewModel) {
    val game by model.game.collectAsStateWithLifecycle()
    val reading by model.vehicleReading.collectAsStateWithLifecycle()
    val vehicle=reading.vehicle
    val safety by model.safety.collectAsStateWithLifecycle()
    val navi by model.navigation.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val allowed by model.gameAllowed.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val reward by model.reward.collectAsStateWithLifecycle()
    val tick by model.now.collectAsStateWithLifecycle()
    val now=System.currentTimeMillis().coerceAtLeast(tick)
    val platformAllowed by model.platformAllowed.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var controls by rememberSaveable { mutableStateOf(false) }
    var modeInfo by rememberSaveable { mutableStateOf(false) }
    var wasAllowed by remember { mutableStateOf(false) }
    LaunchedEffect(allowed,ready) {
        if (allowed && !wasAllowed && ready && game.districtId!=null) tab=2
        if (!allowed) { tab=0;controls=false;modeInfo=false }
        wasAllowed=allowed
    }
    MaterialTheme(colorScheme=lightColorScheme(primary=Blue,onPrimary=Color.White,background=Bg,
        surface=Color.White,onSurface=Ink,secondary=Green,onBackground=Ink)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Bg).safeDrawingPadding()) {
            val wide=maxWidth>=840.dp
            val compact=maxWidth<600.dp
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal=if(compact)16.dp else 24.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(!compact) Icon(Icons.Outlined.Bolt,"",tint=Blue,modifier=Modifier.size(32.dp))
                    Text("GRIDGUILD",fontSize=if(compact)18.sp else 23.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=if(compact)1.sp else 2.sp)
                    Spacer(Modifier.weight(1f))
                    AssistChip(onClick={modeInfo=true},enabled=platformAllowed,
                        label={Text(if(game.mode==AppMode.MOCK) "데모 (Mock)" else "Pleos SDK")},
                        leadingIcon={Icon(Icons.Outlined.Info,null,Modifier.size(18.dp))},
                        modifier=Modifier.heightIn(min=48.dp).testTag("modeInfo"))
                    Spacer(Modifier.width(16.dp))
                    if(platformAllowed) {
                        if(compact) IconButton(onClick={controls=!controls},modifier=Modifier.testTag("settings")) { Icon(Icons.Outlined.Tune,"연결 · 테스트") }
                        else TextButton(onClick={controls=!controls},modifier=Modifier.testTag("settings")) { Icon(Icons.Outlined.Tune,null);Spacer(Modifier.width(6.dp));Text("연결 · 테스트") }
                    }
                }
                HorizontalDivider(color=Pale)
                if (controls && platformAllowed) ConnectionPanel(model,game,vehicle,safety,allowed)
                if (!ready) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Text(message.ifBlank { "로컬 진행 상황 불러오는 중…" }) }
                else if(!platformAllowed) Box(Modifier.fillMaxSize().padding(32.dp),contentAlignment=Alignment.Center) { Text("플랫폼 주차 UX 확인 대기 · 주행 중에는 게임을 이용할 수 없습니다.") }
                else if(game.districtId==null) Onboarding(model)
                else if(!allowed) RestrictedScreen(safety,vehicle,game,model)
                else Row(Modifier.weight(1f)) {
                    if(wide) NavigationRail(containerColor=Bg) {
                        Spacer(Modifier.height(24.dp))
                        tabs.forEachIndexed { i,label -> NavigationRailItem(selected=tab==i,onClick={tab=i},icon={Icon(tabIcons[i],label)},label={Text(label)},modifier=Modifier.padding(vertical=8.dp).testTag("tab$i")) }
                    }
                    Column(Modifier.weight(1f)) {
                        if(!wide) NavigationBar(containerColor=Bg) { tabs.forEachIndexed { i,label -> NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(tabIcons[i],label)},label={Text(label)},modifier=Modifier.testTag("tab$i")) } }
                        Box(Modifier.weight(1f).padding(if(wide)24.dp else 16.dp)) {
                            when(tab) {
                                0 -> HomeScreen(game,reading,navi,now) { tab=1 }
                                1 -> MissionScreen(game,reading,navi,now,model)
                                2 -> CityScreen(game)
                                3 -> RankingScreen(game,now)
                            }
                        }
                    }
                }
                if(message.isNotBlank()) Row(Modifier.fillMaxWidth().background(Pale).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(message,Modifier.weight(1f),fontSize=14.sp)
                    TextButton(onClick={model.message.value=""}){Text("닫기")}
                }
                Text("MVP · 충전소 / 전력망 / 경쟁자는 MOCK · 모든 XP는 위치 미인증 데모 성과",fontSize=12.sp,color=Subtle,modifier=Modifier.padding(horizontal=24.dp,vertical=8.dp))
            }
            AnimatedVisibility(reward>0 && allowed,modifier=Modifier.align(Alignment.TopCenter).padding(top=75.dp)) {
                Surface(color=Green,shape=RoundedCornerShape(20.dp),shadowElevation=12.dp) {
                    Text("+$reward XP · 개인과 도시가 함께 성장했어요",Modifier.padding(24.dp),color=Color.White,fontWeight=FontWeight.Bold)
                }
            }
            if(modeInfo && platformAllowed) AlertDialog(onDismissRequest={modeInfo=false},
                title={Text(if(game.mode==AppMode.MOCK) "데모 모드란?" else "Pleos SDK 모드")},
                text={Text(if(game.mode==AppMode.MOCK)
                    "Mock은 실제 차량 없이 체험하는 데모 모드입니다.\n\n배터리 SOC, 충전 ON/OFF, P·D 상태를 직접 바꿔 미션을 테스트할 수 있어요. 충전소·전력망·경쟁자는 샘플이며, XP는 데모 성과입니다."
                    else "배터리·충전 스위치·기어는 연결된 Pleos 차량 서비스에서 확인합니다.\n\n연결되지 않은 값은 미확인으로 표시합니다. 충전소·전력망·경쟁자는 여전히 샘플이며 XP는 위치 미인증 데모 성과입니다.")},
                confirmButton={TextButton(onClick={modeInfo=false;controls=true},modifier=Modifier.testTag("openModeControls")) { Text(if(game.mode==AppMode.MOCK) "데모 조작 열기" else "연결 설정 열기") }},
                dismissButton={TextButton(onClick={modeInfo=false}){Text("닫기")}})
        }
    }
}

@Composable private fun BadgeText(text: String,color: Color=Blue) {
    Surface(color=color.copy(alpha=.10f),shape=RoundedCornerShape(9.dp)) { Text(text,Modifier.padding(horizontal=10.dp,vertical=5.dp),color=color,fontSize=13.sp,fontWeight=FontWeight.Bold) }
}
@Composable private fun Section(title: String,subtitle: String) {
    Text(title,fontSize=27.sp,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(5.dp));Text(subtitle,color=Subtle,fontSize=15.sp);Spacer(Modifier.height(22.dp))
}
@Composable private fun Panel(modifier: Modifier=Modifier,content: @Composable ColumnScope.()->Unit) {
    Surface(modifier,shape=RoundedCornerShape(20.dp),color=Color.White,border=BorderStroke(1.dp,Color(0xFFE4EDF4))) {
        Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}
@Composable private fun Metric(label: String,value: String,icon: ImageVector) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(icon,null,tint=Blue,modifier=Modifier.size(28.dp))
        Column { Text(label,color=Subtle,fontSize=14.sp);Text(value,fontWeight=FontWeight.Bold,fontSize=27.sp) }
    }
}
@Composable private fun Onboarding(model: GridViewModel) {
    var selected by rememberSaveable { mutableStateOf("sasang") }
    var mode by rememberSaveable { mutableStateOf(AppMode.MOCK) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("충전으로, 우리 도시의 내일을.",fontSize=32.sp,fontWeight=FontWeight.Bold)
        Text("Charge the Future, Build Your City.",color=Blue,fontSize=19.sp)
        Text("부산의 16개 구·군에서 소속 지역을 선택하세요.\n충전 미션의 데모 XP가 내 도시의 성장을 만듭니다.",color=Subtle)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Busan.districts.forEach { d -> FilterChip(selected=selected==d.id,onClick={selected=d.id},label={Text(d.name)},modifier=Modifier.heightIn(min=48.dp)) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(selected=mode==AppMode.MOCK,onClick={mode=AppMode.MOCK},label={Text("데모 체험 (Mock)")})
            FilterChip(selected=mode==AppMode.PLEOS,onClick={mode=AppMode.PLEOS},label={Text("Pleos SDK · 실제 서비스 필요")})
        }
        Text("SDK 모드는 Pleos Vehicle/Navi Service와 승인된 권한이 필요합니다. 연결 실패 시 원인을 표시하며, Mock은 직접 선택합니다.",color=Subtle,fontSize=14.sp)
        Button(onClick={model.setup(selected,mode)},modifier=Modifier.heightIn(min=56.dp).testTag("start")) { Text("${Busan.find(selected)?.name}에서 시작하기") }
    }
}
@Composable private fun ConnectionPanel(model: GridViewModel,game: GameState,v: VehicleState,s: SafetyState,allowed: Boolean) {
    var requestedMode by remember { mutableStateOf<AppMode?>(null) }
    val selectMode:(AppMode)->Unit = { target ->
        if(game.mission!=null && target!=game.mode) requestedMode=target else model.setMode(target)
    }
    Column(Modifier.fillMaxWidth().heightIn(max=310.dp).verticalScroll(rememberScrollState()).background(Pale.copy(alpha=.45f)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("데이터 소스",fontWeight=FontWeight.Bold)
            FilterChip(selected=game.mode==AppMode.MOCK,onClick={selectMode(AppMode.MOCK)},label={Text("데모 (Mock)")},modifier=Modifier.testTag("modeMock"))
            FilterChip(selected=game.mode==AppMode.PLEOS,onClick={selectMode(AppMode.PLEOS)},label={Text("Pleos SDK")},modifier=Modifier.testTag("modePleos"))
        }
        Text(v.detail,fontSize=14.sp);Text(s.detail,fontSize=14.sp)
        Text("모드 변경 시 진행 중 미션은 취소됩니다. 이미 획득한 데모 XP는 보존됩니다.",fontSize=12.sp,color=Subtle)
        if(game.mode==AppMode.MOCK) {
            Text("데모 조작 · 실제 차량을 제어하지 않습니다",fontWeight=FontWeight.Bold,color=Blue)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick={model.mockCharge(v.chargeSwitch!=ChargeSignal.ON)},modifier=Modifier.testTag("charge")) { Text(if(v.chargeSwitch==ChargeSignal.ON) "충전 OFF" else "충전 ON") }
                OutlinedButton(onClick={model.mockSoc((v.soc ?: 42f)+2f)},modifier=Modifier.testTag("socPlus")) { Text("SOC +2%") }
                OutlinedButton(onClick={model.mockSoc((v.soc ?: 42f)-2f)}) { Text("SOC −2%") }
                OutlinedButton(onClick={model.mockPark(!s.parked)},modifier=Modifier.testTag("park")) { Text(if(s.parked) "D단 시뮬레이션" else "P단 시뮬레이션") }
                OutlinedButton(onClick={model.mockUx(!s.uxAllowed)},modifier=Modifier.testTag("ux")) { Text(if(s.uxAllowed) "UX 제한" else "UX 허용") }
            }
            var confirmReset by remember { mutableStateOf(false) }
            if(allowed) TextButton(onClick={confirmReset=true}){Text("데모 데이터 초기화")}
            if(confirmReset && allowed) AlertDialog(onDismissRequest={confirmReset=false},title={Text("데모 기록을 초기화할까요?")},text={Text("지역 선택, 모든 데모 XP와 미션 기록을 지웁니다.")},confirmButton={TextButton(onClick={model.reset();confirmReset=false}){Text("초기화")}},dismissButton={TextButton(onClick={confirmReset=false}){Text("취소")}})
        }
    }
    requestedMode?.let { target ->
        AlertDialog(onDismissRequest={requestedMode=null},title={Text("진행 중인 미션을 취소하고 전환할까요?")},
            text={Text("모드를 바꾸면 진행 중인 미션은 취소됩니다. 이미 획득한 데모 XP는 유지됩니다.")},
            confirmButton={TextButton(onClick={model.setMode(target);requestedMode=null}){Text("미션 취소 후 전환")}},
            dismissButton={TextButton(onClick={requestedMode=null}){Text("계속 진행")}})
    }
}
@Composable private fun RestrictedScreen(s: SafetyState,v: VehicleState,game: GameState,model: GridViewModel) {
    Column(Modifier.fillMaxSize().padding(36.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Lock,null,tint=Blue,modifier=Modifier.size(64.dp))
        Spacer(Modifier.height(20.dp));Text("안전하게 주차한 후 만나요",fontSize=28.sp,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("restricted"))
        Text("P단과 플랫폼 UX 허용이 모두 확인되면 도시가 열립니다.",color=Subtle,modifier=Modifier.padding(12.dp))
        Text(s.detail,color=Subtle)
        Text("배터리 ${v.soc?.toInt()?.toString() ?: "—"}% · 개인 ${game.xp} 데모 XP",modifier=Modifier.padding(16.dp))
        if(game.mode==AppMode.MOCK) TextButton(onClick={model.mockPark(true)}){Text("MOCK · P단으로 전환")}
        else TextButton(onClick={model.setMode(AppMode.MOCK)}){Text("SDK 사용 불가 · MOCK 모드 선택")}
    }
}
@Composable private fun HomeScreen(g: GameState,reading: VehicleReading,n: NavigationState,now: Long,onMission:()->Unit) {
    val v=reading.vehicle
    val d=Busan.find(g.districtId)!!;val xp=g.cityXp(d.id);val level=CityLevels.level(xp)
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Section("${d.name}, 함께 만드는 미래","좋은 충전 습관이 도시의 새로운 풍경이 됩니다.")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val city: @Composable ()->Unit = {
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) { BadgeText("Lv.$level · ${CityLevels.names[level-1]}");Spacer(Modifier.weight(1f));Text(d.name,fontWeight=FontWeight.Bold) }
                CityCanvas(level,d.id,Modifier.fillMaxWidth().aspectRatio(2f))
                Text("지역 성장 · $xp XP",fontWeight=FontWeight.Bold)
                LinearProgressIndicator(progress={CityLevels.progress(xp)},modifier=Modifier.fillMaxWidth(),color=Blue,trackColor=Pale)
                Text(if(level==12) "최고 레벨 달성" else "다음 레벨까지 ${CityLevels.thresholds[level]-xp} XP",color=Subtle,fontSize=14.sp)
            } }
            val metrics: @Composable ()->Unit = {
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Panel(Modifier.fillMaxWidth()) {
                    Metric("EV 배터리 · ${v.source}","${v.soc?.toInt() ?: "—"}%",Icons.Outlined.BatteryChargingFull)
                    Text("충전 스위치 ${v.chargeSwitch} · ${reading.statusLabel}",fontSize=14.sp,color=Subtle,modifier=Modifier.testTag("batteryStatus"))
                    Text("스위치 ON은 전력량·장소 인증이 아닙니다.",fontSize=12.sp,color=Subtle)
                }
                Panel(Modifier.fillMaxWidth()) {
                    Metric("내 누적 데모 성과","${g.xp} XP",Icons.Outlined.Bolt)
                    Text("이번 시즌 ${g.seasonXp(now)} XP",color=Subtle)
                    Button(onClick=onMission,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("충전 미션 찾기") }
                }
            } }
            if(maxWidth<620.dp) Column(verticalArrangement=Arrangement.spacedBy(18.dp)) { city();metrics() }
            else Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) { Box(Modifier.weight(1.4f)){city()};Box(Modifier.weight(1f)){metrics()} }
        }
        Spacer(Modifier.height(16.dp));Text("목적지 · ${n.destination}  |  ${n.source}",color=Subtle)
        Text("월간 개인 ${Rankings.personal(g,now).indexOfFirst{it.mine}+1}위 · 소속 지역 ${Rankings.cities(g,now).indexOfFirst{it.mine}+1}위 · 경쟁자 MOCK",color=Subtle,fontSize=14.sp)
    }
}
@Composable private fun MissionScreen(g: GameState,reading: VehicleReading,n: NavigationState,now: Long,model: GridViewModel) {
    val v=reading.vehicle
    val starting by model.startingMission.collectAsStateWithLifecycle()
    val listState=rememberLazyListState()
    LaunchedEffect(g.mission?.id) { if(g.mission!=null) listState.animateScrollToItem(0) }
    val stations=SampleData.recommend(n.lat,n.lon,v.soc)
    LazyColumn(state=listState,modifier=Modifier.testTag("missionList"),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Section("충전이 도시의 에너지가 되도록","목적지 주변 후보 · MOCK 충전소 / 모의 전력망 · 실제 경로 형상 미사용") }
        item { Panel(Modifier.fillMaxWidth()) {
            Text("SOC ${v.soc?.toInt() ?: "—"}% · 충전 스위치 ${v.chargeSwitch}",fontWeight=FontWeight.Bold,fontSize=20.sp)
            Text(reading.statusLabel,fontSize=14.sp,color=if(reading.fresh)Green else Subtle,modifier=Modifier.testTag("missionBatteryStatus"))
            Text("${n.destination} · ${n.source}",color=Subtle)
            Text(n.status,color=if(n.confirmed)Green else Subtle,modifier=Modifier.testTag("navStatus"))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick=model::refreshNavigation){Text("목적지 새로고침")}
                OutlinedButton(onClick=model::reroute){Text("경로 재탐색 요청")}
            }
        } }
        g.mission?.let { m -> item { Panel(Modifier.fillMaxWidth().testTag("activeMission")) {
            Text("미션 진행 중 · ${m.source}",fontWeight=FontWeight.Bold,color=Blue)
            Text("시작 SOC ${m.baselineSoc.toInt()}% → 목표 ${(m.baselineSoc+m.requiredGain).toInt()}% · 충전 ON + SOC 2%p 증가")
            LinearProgressIndicator(progress={m.progress},modifier=Modifier.fillMaxWidth(),trackColor=Pale)
            Text("${(m.progress*100).toInt()}% · 권장 시간 내 조건 충족 시 자동 완료 · 위치 미인증 데모 XP",fontSize=14.sp,color=Subtle)
            TextButton(onClick=model::cancelMission){Text("미션 취소")}
        } } }
        if(stations.isEmpty()) item { Text("SOC 90% 이상으로 추가 충전을 권장하지 않습니다.") }
        items(stations,key={it.id}) { station ->
            val done=g.history.any { it.missionId.startsWith("${station.id}:") && GameState.day(it.at)==GameState.day(now) }
            val correctSource=v.source==if(g.mode==AppMode.MOCK)DataSource.MOCK else DataSource.PLEOS
            val canStart=g.mission==null && starting==null && reading.fresh && correctSource && station.inWindow(now) && !done
            val buttonLabel=when {
                done -> "오늘 완료"
                g.mission?.stationId==station.id -> "미션 진행 중"
                g.mission!=null -> "다른 미션 진행 중"
                starting==station.id -> "미션 시작 중…"
                starting!=null -> "시작 요청 처리 중"
                !station.inWindow(now) -> "권장 시간 외"
                !reading.fresh || !correctSource -> "차량 정보 확인 필요"
                else -> "미션 시작"
            }
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Outlined.EvStation,null,tint=Green,modifier=Modifier.size(34.dp));Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(station.name,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("${Busan.find(station.districtId)?.name} · ${station.windowLabel}",color=Subtle) }
                    BadgeText("+${station.xp} 데모 XP",Green)
                }
                Text("MOCK · 녹색 전력망 여유 / 충전소 운영 정보 미연동",fontSize=14.sp,color=Subtle)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Button(onClick={model.startMission(station)},enabled=canStart,modifier=Modifier.width(200.dp).heightIn(min=56.dp).testTag("mission-${station.id}")) {
                        if(starting==station.id) { CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp)) }
                        Text(buttonLabel)
                    }
                    OutlinedButton(onClick={model.addWaypoint(station)},enabled=!n.pending,modifier=Modifier.heightIn(min=52.dp).testTag("waypoint-${station.id}")) { Text(if(g.mode==AppMode.MOCK) "경유지 추가 (MOCK)" else "샘플 좌표 경유지 요청") }
                    TextButton(onClick={model.setDestination(station)}){Text("목적지로 설정")}
                }
                Text(when {
                    done -> "오늘의 보상은 이미 받았어요. 다음 충전소에 참여해 보세요."
                    g.mission?.stationId==station.id -> "위의 진행 카드에서 목표 SOC와 자동 완료 조건을 확인하세요."
                    g.mission!=null -> "진행 중인 미션을 완료하거나 취소한 후 시작할 수 있어요."
                    !reading.fresh || !correctSource -> "차량 정보를 기다리고 있어요. 데모에서는 상단 조작 메뉴를 사용할 수 있어요."
                    !station.inWindow(now) -> "${station.windowLabel}에 시작할 수 있어요."
                    else -> "충전 ON 상태에서 SOC가 2%p 증가하면 자동으로 완료됩니다."
                },fontSize=13.sp,color=Subtle,modifier=Modifier.heightIn(min=24.dp))
            }
        }
        item { Text("SOC만으로 실제 충전 장소나 전력망 기여를 인증하지 않습니다. 하루에 샘플 충전소 3곳을 모두 완료하면 데모 50 XP 추가.",fontSize=14.sp,color=Subtle) }
        items(g.history.takeLast(5).reversed()) { e -> Text("+${e.xp} XP · ${GameState.day(e.at)} · ${if(e.kind=="DAILY") "일일 보너스" else "데모 충전 성과"}",color=Green) }
    }
}
@Composable private fun CityScreen(g: GameState) {
    val d=Busan.find(g.districtId)!!;val xp=g.cityXp(d.id);val level=CityLevels.level(xp)
    var previewLevel by rememberSaveable(g.districtId,level) { mutableStateOf<Int?>(null) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var building by remember { mutableStateOf(false) }
    val shown=CityStage.at(previewLevel ?: level)
    val preview=shown.level!=level
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Section("${d.name} · ${shown.name}",if(preview)"미리보기 Lv.${shown.level} · 실제 도시 레벨과 XP는 그대로 유지됩니다." else "${d.motif} 테마 · 중세에서 미래로 성장하는 가상 도시")
        Panel(Modifier.fillMaxWidth()) {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                BadgeText(if(preview)"미리보기 · Lv.${shown.level}" else "내 도시 · Lv.$level",if(preview)Green else Blue)
                OutlinedButton(onClick={previewLevel=null},enabled=preview,modifier=Modifier.testTag("cityCurrent")){Text("현재 도시로 돌아가기")}
                Button(onClick={previewLevel=level+1},enabled=level<12,modifier=Modifier.testTag("cityPreview")){Text(if(level==12)"최고 레벨 달성" else "다음 레벨 미리보기 · Lv.${level+1}")}
            }
            Text("Lv.${shown.level} · ${shown.landmark}",fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("cityStageLabel"))
            Crossfade(targetState=shown.level,animationSpec=tween(450),label="cityEvolution") { displayed ->
                CityCanvas(displayed,d.id,Modifier.fillMaxWidth().aspectRatio(2f).testTag("cityScene").clickable{building=true},zoom)
            }
            Text(shown.description,fontSize=15.sp,color=Subtle)
            Text("내 도시 Lv.$level · $xp 지역 XP",fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={CityLevels.progress(xp)},modifier=Modifier.fillMaxWidth(),trackColor=Pale)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                OutlinedButton(onClick={zoom=(zoom+.15f).coerceAtMost(1.5f)}){Text("확대 +")}
                OutlinedButton(onClick={zoom=(zoom-.15f).coerceAtLeast(.7f)}){Text("축소 −")}
                TextButton(onClick={building=true}){Text("건물 정보")}
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("성장 단계 둘러보기 · 선택하면 미리보기로 표시됩니다",fontSize=14.sp,color=Subtle)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            CityLevels.names.forEachIndexed { i,name -> FilterChip(selected=shown.level==i+1,onClick={previewLevel=if(i+1==level)null else i+1},label={Text("Lv.${i+1} $name")},modifier=Modifier.testTag("city-stage-${i+1}")) }
        }
        if(building) AlertDialog(onDismissRequest={building=false},title={Text("${d.name} · ${shown.landmark}")},text={Text("${shown.description}\n\n${if(preview) "Lv.${shown.level} 미리보기" else "내 도시 Lv.$level"} · 실제 건물·계통 데이터와 관계없는 가상 도시입니다.")},confirmButton={TextButton(onClick={building=false}){Text("확인")}})
    }
}
@Composable private fun RankingScreen(g: GameState,now: Long) {
    var cities by rememberSaveable { mutableStateOf(false) }
    var monthly by rememberSaveable { mutableStateOf(true) }
    val sorted=if(cities)Rankings.cities(g,now,monthly) else Rankings.personal(g,now,monthly)
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Section("함께 만드는 순위","${GameState.season(now)} 시즌 · 경쟁자와 지역 기본 점수 MOCK · 내 기여는 로컬 데모 XP") }
        item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(selected=!cities,onClick={cities=false},label={Text("개인 랭킹")})
            FilterChip(selected=cities,onClick={cities=true},label={Text("지역 랭킹")})
            FilterChip(selected=monthly,onClick={monthly=!monthly},label={Text(if(monthly) "월간 시즌" else "누적 성과")})
        } }
        item { BadgeText("내 순위 ${sorted.indexOfFirst{it.mine}+1}위 · 실제 온라인 순위 아님") }
        items(sorted.size) { index -> val row=sorted[index]
            Surface(color=if(row.mine)Pale else Color.White,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(22.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("%02d".format(index+1),fontSize=24.sp,fontWeight=FontWeight.Bold,color=Blue,modifier=Modifier.width(60.dp))
                    Column(Modifier.weight(1f)) { Text(row.name+(if(row.mine)" · 내 소속" else ""),fontWeight=FontWeight.Bold,fontSize=18.sp);if(cities)Text("Lv.${CityLevels.level(row.xp)} · 모의 공동 점수",fontSize=14.sp,color=Subtle) }
                    Text("${row.xp} XP",fontSize=22.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}
