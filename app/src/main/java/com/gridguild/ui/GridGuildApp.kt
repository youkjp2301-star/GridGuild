@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gridguild.GridViewModel
import com.gridguild.core.*
import com.gridguild.data.NavigationState

private val tabs=listOf("홈","미션","내 도시","랭킹")
private val tabIcons=listOf(Icons.Outlined.Home,Icons.Outlined.EvStation,Icons.Outlined.LocationCity,Icons.Outlined.Leaderboard)

@Composable fun GridGuildApp(model: GridViewModel) {
    val game by model.game.collectAsStateWithLifecycle()
    val reading by model.vehicleReading.collectAsStateWithLifecycle()
    val vehicle=reading.vehicle
    val safety by model.safety.collectAsStateWithLifecycle()
    val navi by model.navigation.collectAsStateWithLifecycle()
    val ready by model.ready.collectAsStateWithLifecycle()
    val resources=LocalContext.current.resources
    val allowed by model.gameAllowed.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val tick by model.now.collectAsStateWithLifecycle()
    val now=System.currentTimeMillis().coerceAtLeast(tick)
    val platformAllowed by model.platformAllowed.collectAsStateWithLifecycle()
    val introSeen by model.introSeen.collectAsStateWithLifecycle()
    val introFinished=rememberIntroFinished(ready && platformAllowed && (allowed || game.districtId==null),introSeen)
    LaunchedEffect(introFinished){if(introFinished)model.introSeen.value=true}
    val neutralPreview by model.neutralPreview.collectAsStateWithLifecycle()
    val normalState=rememberSaveableStateHolder()
    val unlocked=CityLevels.level(game.cityXp(game.districtId))
    LaunchedEffect(ready,allowed,game.districtId,unlocked) {
        if(ready && allowed && game.districtId!=null)preloadCityArt(resources,unlocked,unlocked)
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var controls by rememberSaveable { mutableStateOf(false) }
    var modeInfo by rememberSaveable { mutableStateOf(false) }
    var parkingScreenShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(allowed,ready,game.districtId,safety.source,safety.parked,safety.uxAllowed) {
        if(allowed && !parkingScreenShown && ready && game.districtId!=null) { tab=2;parkingScreenShown=true }
        if(!allowed) {
            // A lifecycle reconnect is unknown, not a new driving-to-parking arrival.
            if(safety.source!=DataSource.UNAVAILABLE && (!safety.parked || !safety.uxAllowed)) {tab=0;parkingScreenShown=false}
            controls=false;modeInfo=false
        }
    }
    GridTheme {
        if(neutralPreview && game.mode==AppMode.MOCK && game.districtId!=null) {
            NeutralCityScreen(game,navi,allowed && platformAllowed,model)
        } else normalState.SaveableStateProvider("normal") {
        BoxWithConstraints(Modifier.fillMaxSize().background(Bg).safeDrawingPadding()) {
            val wide=maxWidth>=840.dp
            val compact=maxWidth<600.dp
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().background(Paper.copy(alpha=.95f)).padding(horizontal=if(compact)16.dp else 26.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(!compact) {
                        Surface(color=Ink,shape=RoundedCornerShape(12.dp)) { Icon(Icons.Outlined.Bolt,null,tint=Color.White,modifier=Modifier.padding(8.dp).size(24.dp)) }
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        Text("GRIDGUILD",fontSize=if(compact)18.sp else 21.sp,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp)
                        if(!compact)Text("충전으로 함께 자라는 도시",fontSize=11.sp,color=Subtle)
                    }
                    Spacer(Modifier.weight(1f))
                    AssistChip(onClick={modeInfo=true},enabled=platformAllowed,
                        label={Text(if(game.mode==AppMode.MOCK) "체험 모드" else "Pleos SDK")},
                        leadingIcon={Icon(Icons.Outlined.Info,null,Modifier.size(18.dp))},
                        shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,Line),
                        colors=AssistChipDefaults.assistChipColors(containerColor=Bg,labelColor=Blue,leadingIconContentColor=Blue),
                        modifier=Modifier.heightIn(min=48.dp).testTag("modeInfo"))
                    Spacer(Modifier.width(16.dp))
                    if(platformAllowed) {
                        if(compact) IconButton(onClick={controls=!controls},modifier=Modifier.testTag("settings")) { Icon(Icons.Outlined.Tune,"연결 · 테스트") }
                        else TextButton(onClick={controls=!controls},modifier=Modifier.testTag("settings")) { Icon(Icons.Outlined.Tune,null);Spacer(Modifier.width(6.dp));Text("연결 · 테스트") }
                    }
                }
                HorizontalDivider(color=Line)
                if (controls && platformAllowed) ConnectionPanel(model,game,vehicle,safety,allowed)
                if(!platformAllowed) Box(Modifier.fillMaxSize().padding(32.dp),contentAlignment=Alignment.Center) { Text("플랫폼 주차 UX 확인 대기 · 주행 중에는 게임을 이용할 수 없습니다.") }
                else if(!ready) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("진행 상황 확인 중")}
                else if(game.districtId!=null && !allowed)RestrictedScreen(safety,vehicle,game,model)
                else if (!introFinished) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                    CityGrowthLoading(Modifier.widthIn(max=700.dp),animate=allowed,label=message.ifBlank{"도시를 준비하고 있어요"},loop=false)
                }
                else if(game.districtId==null) Onboarding(model)
                else Row(Modifier.weight(1f)) {
                    if(wide) Column(Modifier.fillMaxHeight().width(130.dp).background(Paper.copy(alpha=.92f)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                        Spacer(Modifier.height(12.dp))
                        tabs.forEachIndexed { i,label -> GuildNavItem(label,tabIcons[i],tab==i,true,"tab$i"){tab=i} }
                    }
                    Column(Modifier.weight(1f)) {
                        if(!wide) Row(Modifier.fillMaxWidth().background(Paper.copy(alpha=.94f)).padding(8.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                            tabs.forEachIndexed { i,label -> GuildNavItem(label,tabIcons[i],tab==i,false,"tab$i"){tab=i} }
                        }
                        GuildPage(tab,Modifier.weight(1f).padding(if(wide)24.dp else 16.dp)) { destination ->
                            when(destination) {
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
            }
            XpRewardHost(model.rewards,allowed && ready && introFinished,game.cityXp(game.districtId),
                {model.game.value.cityXp(model.game.value.districtId)},
                Modifier.align(Alignment.BottomEnd).padding(bottom=76.dp,start=20.dp,end=20.dp))
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
}

@Composable private fun BadgeText(text: String,color: Color=Blue) {
    Surface(color=color.copy(alpha=.09f),shape=RoundedCornerShape(8.dp)) { Text(text,Modifier.padding(horizontal=11.dp,vertical=7.dp),color=if(color==Gold)Color(0xFF80602E) else color,fontSize=12.sp,fontWeight=FontWeight.SemiBold) }
}
@Composable private fun Section(title: String,subtitle: String) {
    Text(title,style=MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(6.dp));Text(subtitle,color=Subtle,fontSize=14.sp);Spacer(Modifier.height(24.dp))
}
@Composable internal fun Panel(modifier: Modifier=Modifier,content: @Composable ColumnScope.()->Unit) {
    Surface(modifier,shape=RoundedCornerShape(24.dp),color=Paper,border=BorderStroke(1.dp,Line),shadowElevation=1.dp) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable private fun Metric(label: String,value: String,icon: ImageVector) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Surface(color=if(icon==Icons.Outlined.Bolt)Gold.copy(alpha=.12f) else Pale,shape=RoundedCornerShape(14.dp)) {
            Icon(icon,null,tint=if(icon==Icons.Outlined.Bolt)Gold else Blue,modifier=Modifier.padding(11.dp).size(25.dp))
        }
        Column(verticalArrangement=Arrangement.spacedBy(3.dp)) { Text(label,color=Subtle,fontSize=13.sp);Text(value,fontWeight=FontWeight.Bold,fontSize=30.sp,letterSpacing=(-.5).sp) }
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
    var loadingPreview by remember{mutableStateOf(false)}
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
            if(allowed) {
                Button(onClick=model::enterNeutralPreview,modifier=Modifier.heightIn(min=52.dp).testTag("neutralTest")) {
                    Icon(Icons.Outlined.Panorama,null);Spacer(Modifier.width(8.dp));Text("N단 도시 체험 · TEST")
                }
                Text("실제 기어는 바꾸지 않는 가상 체험 · P단·UX 제한 유지",fontSize=12.sp,color=Subtle)
                TextButton(onClick={loadingPreview=true},modifier=Modifier.testTag("loadingPreview")){Text("도시 로딩 연출 미리보기")}
                DemoResetControls(game,model::resetDemo,model::stepDemoCityLevel)
            }
        }
    }
    if(loadingPreview && allowed) AlertDialog(onDismissRequest={loadingPreview=false},
        title={Text("도시 로딩 · 연출 미리보기")},
        text={CityGrowthLoading(label="충전으로 자라는 도시")},
        confirmButton={TextButton(onClick={loadingPreview=false}){Text("닫기")}})
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
        Text("CHARGE THE FUTURE",fontSize=11.sp,letterSpacing=2.sp,color=Green,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Section("좋은 충전이 만드는, ${d.name}의 내일","오늘도 내 도시가 한 뼘 자랍니다.")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val city: @Composable ()->Unit = {
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) { BadgeText("Lv.$level · ${CityLevels.names[level-1]}");Spacer(Modifier.weight(1f));Text(d.name,fontWeight=FontWeight.Bold) }
                CityCanvas(level,d.id,level,Modifier.fillMaxWidth().aspectRatio(2f).testTag("homeCityHero"))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("지역 성장 · $xp XP",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    Text("MY CITY",fontSize=10.sp,letterSpacing=1.8.sp,color=Green)
                }
                LinearProgressIndicator(progress={CityLevels.progress(xp)},modifier=Modifier.fillMaxWidth().height(7.dp),color=Green,trackColor=SoftGreen)
                Text(if(level==12) "최고 레벨 달성" else "다음 레벨까지 ${CityLevels.thresholds[level]-xp} XP",color=Subtle,fontSize=14.sp)
            } }
            val metrics: @Composable ()->Unit = {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val battery: @Composable (Modifier)->Unit = { card -> Panel(card) {
                    Metric("EV 배터리",if(reading.fresh)"${v.soc?.toInt() ?: "—"}%" else "—%",Icons.Outlined.BatteryChargingFull)
                    LinearProgressIndicator(progress={if(reading.fresh)(v.soc ?: 0f)/100f else 0f},modifier=Modifier.fillMaxWidth().height(5.dp),color=Green,trackColor=Bg)
                    Text(if(reading.fresh)"충전 스위치 ${v.chargeSwitch}" else "충전 상태 확인 중",fontSize=14.sp,
                        color=if(reading.fresh)Green else Subtle,modifier=Modifier.testTag("batteryStatus"))
                } }
                val points: @Composable (Modifier)->Unit = { card -> Panel(card) {
                    Metric("내 누적 XP","${g.xp} XP",Icons.Outlined.Bolt)
                    Text("이번 시즌 ${g.seasonXp(now)} XP",color=Subtle)
                    Button(onClick=onMission,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)) { Icon(Icons.Outlined.EvStation,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("충전 미션 참여하기") }
                } }
                if(maxWidth>=600.dp) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {battery(Modifier.weight(1f));points(Modifier.weight(1f))}
                else Column(verticalArrangement=Arrangement.spacedBy(16.dp)){battery(Modifier.fillMaxWidth());points(Modifier.fillMaxWidth())}
            } }
            if(maxWidth>=1040.dp) Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(2.6f)){city()};Box(Modifier.weight(1f)){metrics()}
            } else Column(verticalArrangement=Arrangement.spacedBy(18.dp)) { city();metrics() }
        }
        Spacer(Modifier.height(16.dp));Text(if(n.hasDestination)"목적지 ${destinationName(n.destination)}" else "목적지를 선택하세요",
            color=Ink,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun MissionScreen(g: GameState,reading: VehicleReading,n: NavigationState,now: Long,model: GridViewModel) {
    var home by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FilterChip(selected=home,onClick={home=true},label={Text("HOME CHARGE")},modifier=Modifier.heightIn(min=48.dp).testTag("homeCharge"))
            FilterChip(selected=!home,onClick={home=false},label={Text("ROUTE CHARGE")},modifier=Modifier.heightIn(min=48.dp).testTag("routeCharge"))
        }
        Box(Modifier.weight(1f)) {
            if(home) HomeChargeScreen(g,reading,now,model)
            else RouteChargeScreen(g,reading,n,now,model)
        }
    }
}
@Composable private fun RouteChargeScreen(g: GameState,reading: VehicleReading,n: NavigationState,now: Long,model: GridViewModel) {
    val v=reading.vehicle
    val starting by model.startingMission.collectAsStateWithLifecycle()
    val availability by model.chargerAvailability.collectAsStateWithLifecycle()
    val connector by model.demoConnector.collectAsStateWithLifecycle()
    val compatible=if(g.mode==AppMode.MOCK)setOf(connector) else null
    var connectorDialog by remember{mutableStateOf(false)}
    var stationOptions by rememberSaveable{mutableStateOf(false)}
    val allowed by model.gameAllowed.collectAsStateWithLifecycle()
    var leisureStationId by remember(g.mode){mutableStateOf<String?>(null)}
    val availabilityNow=System.currentTimeMillis().coerceAtLeast(now)
    val listState=rememberLazyListState()
    LaunchedEffect(g.mission?.id) { if(g.mission!=null) listState.animateScrollToItem(0) }
    val recommendations=ChargingRecommendations.around(n.lat,n.lon,v.soc,availability=availability,observedAt=availabilityNow,compatible=compatible)
    val stations=recommendations.nearby.map{it.station}
    val leisureStation=stations.firstOrNull{it.id==leisureStationId}
    LaunchedEffect(allowed,stations.map{it.id}) {
        if(!allowed || leisureStation==null)leisureStationId=null
    }
    LazyColumn(state=listState,modifier=Modifier.testTag("missionList"),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Section("충전 미션","내 이동에 맞는 충전으로, 도시의 다음 단계를 준비해요.") }
        item { Panel(Modifier.fillMaxWidth()) {
            Text(if(reading.fresh)"SOC ${v.soc?.toInt() ?: "—"}% · 충전 스위치 ${v.chargeSwitch}" else "SOC — · 충전 상태 확인 중",
                fontWeight=FontWeight.Bold,fontSize=20.sp)
            NavigationControls(n,g.mode==AppMode.MOCK,model::refreshNavigation,model::reroute,model::changeDemoDestination,model::randomDemoDestination)
        } }
        item { Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(if(recommendations.status==RecommendationStatus.HIGH_SOC)"충전 추천 중지" else "목적지 주변 충전소 · ${stations.size}곳",fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f).testTag("nearbySummary"))
                IconButton(onClick=model::refreshChargerAvailability,modifier=Modifier.testTag("refreshAvailability")){Icon(Icons.Outlined.Refresh,"데모 상태 새로고침")}
                IconButton(onClick={stationOptions=!stationOptions},modifier=Modifier.testTag("availabilitySettings")){Icon(Icons.Outlined.Tune,"추천 안내·데모 설정")}
            }
            if(recommendations.status!=RecommendationStatus.READY)Text(when(recommendations.status) {
                RecommendationStatus.READY -> ""
                RecommendationStatus.NO_DESTINATION -> "목적지 좌표를 확인하면 주변 후보가 표시됩니다."
                RecommendationStatus.HIGH_SOC -> "SOC 90% 이상으로 추가 충전을 권장하지 않습니다."
                RecommendationStatus.NO_NEARBY -> if(g.mode==AppMode.MOCK)"목적지 반경 5 km 안에 샘플 충전소가 없습니다. 다른 테스트 목적지를 선택해 보세요."
                    else "목적지 반경 5 km 안에 샘플 충전소가 없습니다. 실제 시설의 부재를 뜻하지 않습니다."
            },fontSize=15.sp,color=Subtle,modifier=Modifier.testTag("nearbyReason"))
            if(stationOptions) {
                Text("데모 현황은 도착 전 바뀔 수 있어요. 혼잡은 대기시간이 아닌 이용 비율입니다. 경로 이탈 거리는 미연동이며 목적지 직선거리를 표시합니다.",fontSize=14.sp,color=Subtle)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(g.mode==AppMode.MOCK)TextButton(onClick={connectorDialog=true},modifier=Modifier.testTag("demoConnector")){Text("내 커넥터 · ${connector.label} (테스트)")}
                if(g.mode==AppMode.MOCK)TextButton(onClick=model::shuffleDemoAvailability,modifier=Modifier.testTag("shuffleAvailability")){Text("상태 섞기")}
                }
            }
        } }
        g.mission?.let { m -> item { Panel(Modifier.fillMaxWidth().testTag("activeMission")) {
            Text(if(m.kind==MissionKind.HOME)"우리 집 충전 참여 중" else "미션 진행 중",fontWeight=FontWeight.Bold,color=Blue)
            Text(if(m.kind==MissionKind.HOME)g.homeCharge.chargerName ?: "우리 집 충전" else SampleData.stations.firstOrNull{it.id==m.stationId}?.name ?: "충전 미션",fontWeight=FontWeight.SemiBold)
            Text("목적지를 변경해도 이미 시작한 미션은 유지됩니다.",fontSize=13.sp,color=Subtle)
            val baseline=if(m.kind==MissionKind.HOME)m.windowBaselineSoc else m.baselineSoc
            Text(if(baseline==null)"미션 시간 시작 후 배터리를 자동 확인합니다." else "시작 SOC ${baseline.toInt()}% → 완료 조건 ${(baseline+m.requiredGain).toInt()}% · 충전 ON + SOC 2%p 증가")
            LinearProgressIndicator(progress={m.progress},modifier=Modifier.fillMaxWidth(),trackColor=Pale)
            Text("${(m.progress*100).toInt()}% · 조건 달성 시 자동 완료",fontSize=14.sp,color=Subtle)
            TextButton(onClick=model::cancelMission){Text("미션 취소")}
        } } }
        items(recommendations.nearby,key={it.station.id}) { candidate ->
            val station=candidate.station
            val done=g.completedToday(station.id,now)
            val correctSource=v.source==if(g.mode==AppMode.MOCK)DataSource.MOCK else DataSource.PLEOS
            val hasCharger=candidate.availability.status!=AvailabilityStatus.NONE && candidate.compatibility!=ConnectorCompatibility.INCOMPATIBLE && candidate.availability.compatibleAvailable!=0
            val canStart=g.mission==null && starting==null && reading.fresh && correctSource && station.inWindow(now) && !done && hasCharger
            val buttonLabel=when {
                done -> "오늘 완료"
                g.mission?.stationId==station.id -> "미션 진행 중"
                g.mission!=null -> "다른 미션 진행 중"
                starting==station.id -> "미션 시작 중…"
                starting!=null -> "시작 요청 처리 중"
                candidate.availability.status==AvailabilityStatus.NONE -> "이용 가능한 충전기 없음"
                candidate.compatibility==ConnectorCompatibility.INCOMPATIBLE -> "커넥터 불일치"
                candidate.availability.compatibleAvailable==0 -> "호환 충전기 빈자리 없음"
                !station.inWindow(now) -> "미션 참여 시간 아님"
                !reading.fresh || !correctSource -> "차량 정보 확인 필요"
                else -> "미션 시작"
            }
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=SoftGreen,shape=RoundedCornerShape(14.dp)){Icon(Icons.Outlined.EvStation,null,tint=Green,modifier=Modifier.padding(12.dp).size(26.dp))};Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${stations.indexOf(station)+1}. ${station.name}",fontSize=20.sp,fontWeight=FontWeight.Bold)
                        Text("${Busan.find(station.districtId)?.name} · "+if(station.hourStart==0 && station.hourEnd==24)"미션 상시 참여" else "미션 ${station.windowLabel}",color=Subtle)
                        Text(station.connectors.joinToString(" / "){it.label}+" · "+if(candidate.detourKm!=null)"이탈 %.1f km".format(java.util.Locale.KOREA,candidate.detourKm) else "목적지 직선 %.1f km".format(java.util.Locale.KOREA,candidate.distanceKm),fontSize=14.sp,color=Subtle)
                    }
                    BadgeText("+${station.xp} XP",Gold)
                }
                ChargerAvailabilityInfo(candidate,g.mode==AppMode.MOCK){scenario->model.setDemoAvailability(station.id,scenario)}
                FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Button(onClick={model.startMission(station)},enabled=canStart,shape=RoundedCornerShape(14.dp),modifier=Modifier.width(200.dp).heightIn(min=56.dp).testTag("mission-${station.id}")) {
                        if(starting==station.id) { CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp)) }
                        Text(buttonLabel)
                    }
                    OutlinedButton(onClick={model.addWaypoint(station)},enabled=!n.pending,modifier=Modifier.heightIn(min=52.dp).testTag("waypoint-${station.id}")) { Text("경유지 추가") }
                    TextButton(onClick={model.setDestination(station)},enabled=!n.pending,modifier=Modifier.testTag("destination-${station.id}")){Text("목적지 설정")}
                }
                TextButton(onClick={leisureStationId=station.id},enabled=allowed,
                    modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("leisure-${station.id}")) {
                    Icon(Icons.Outlined.Explore,null,Modifier.size(20.dp));Spacer(Modifier.width(9.dp));Text("충전하는 동안 뭐 하지?")
                }
                val footnote=when {
                    done -> ""
                    g.mission?.stationId==station.id -> "위의 진행 카드에서 목표 SOC와 자동 완료 조건을 확인하세요."
                    g.mission!=null -> "진행 중인 미션을 완료하거나 취소한 후 시작할 수 있어요."
                    !hasCharger -> "이용 가능한 호환 충전기가 있는 다른 후보를 확인하세요."
                    !reading.fresh || !correctSource -> "차량 정보를 기다리고 있어요. 데모에서는 상단 조작 메뉴를 사용할 수 있어요."
                    !station.inWindow(now) -> "보상 미션은 ${station.windowLabel}에 참여할 수 있어요. 충전소가 닫혔다는 뜻은 아닙니다."
                    else -> ""
                }
                if(footnote.isNotBlank())Text(footnote,fontSize=15.sp,color=Subtle)
            }
        }
        if(n.hasDestination && recommendations.status!=RecommendationStatus.HIGH_SOC) item {
            Panel(Modifier.fillMaxWidth()) { NearbyStationMap(n.lat!!,n.lon!!,recommendations.nearby) }
        }
        items(g.completionHistory.takeLast(5).reversed()) { e ->
            Text("개인 +${if(e.personalCredit)e.xp else 0} · 도시 +${if(e.cityCredit)e.xp else 0} XP · ${GameState.day(e.at)} · ${if(e.kind=="DAILY") "일일 보너스" else "충전 성과"}",color=Green)
        }
    }
    if(connectorDialog && g.mode==AppMode.MOCK)AlertDialog(onDismissRequest={connectorDialog=false},title={Text("데모 차량 커넥터")},
        text={Column { ChargerConnector.entries.forEach{type->TextButton(onClick={model.setDemoConnector(type);connectorDialog=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("connector-${type.name}")){Text(type.label)} } }},
        confirmButton={TextButton(onClick={connectorDialog=false}){Text("닫기")}})
    if(allowed && leisureStation!=null)key(leisureStation.id,g.mode) {
        ChargingLeisureSheet(leisureStation,g,reading,availabilityNow,model.leisurePlaceRepository,
            destinationEnabled=g.mode==AppMode.MOCK && !n.pending,
            onDestination={place->model.setLeisureDestination(leisureStation,place)}){leisureStationId=null}
    }
}
@Composable private fun CityScreen(g: GameState) {
    CityCollectionScreen(g)
}
@Composable private fun RankingScreen(g: GameState,now: Long) {
    var cities by rememberSaveable { mutableStateOf(false) }
    var monthly by rememberSaveable { mutableStateOf(true) }
    val sorted=if(cities)Rankings.cities(g,now,monthly) else Rankings.personal(g,now,monthly)
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.testTag("rankingList")) {
        item { Section("함께 만드는 순위","${GameState.season(now)} 시즌 · 샘플 랭킹 · 내 성과 반영") }
        item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(selected=!cities,onClick={cities=false},label={Text("개인 랭킹")})
            FilterChip(selected=cities,onClick={cities=true},label={Text("지역 랭킹")})
            FilterChip(selected=monthly,onClick={monthly=!monthly},label={Text(if(monthly) "월간 시즌" else "누적 성과")})
        } }
        item { BadgeText("내 순위 ${sorted.indexOfFirst{it.mine}+1}위 · 데모") }
        items(sorted.size) { index -> val row=sorted[index]
            Surface(color=if(row.mine)Pale else Color.White,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(22.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("%02d".format(index+1),fontSize=24.sp,fontWeight=FontWeight.Bold,color=Blue,modifier=Modifier.width(60.dp))
                    Column(Modifier.weight(1f)) { Text(row.name+(if(row.mine && cities)" · 내 소속" else ""),fontWeight=FontWeight.Bold,fontSize=18.sp);if(cities)Text("Lv.${CityLevels.level(row.xp)} · 모의 공동 점수",fontSize=16.sp,color=Subtle) }
                    Text("${row.xp} XP",fontSize=22.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}
