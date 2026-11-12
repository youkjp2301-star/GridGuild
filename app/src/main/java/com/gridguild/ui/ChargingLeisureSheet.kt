@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class,androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.core.*
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

/** Station-local, read-only discovery. Opening, filtering and inspecting never write the game ledger. */
@Composable internal fun ChargingLeisureSheet(station: Station,game: GameState,reading: VehicleReading,now: Long,
    repository: LeisurePlaceRepository,destinationEnabled: Boolean,onDestination:(LeisurePlace)->Boolean,onDismiss:()->Unit) {
    var category by remember(station.id){mutableStateOf<LeisureCategory?>(null)}
    var selectedPlaceId by remember(station.id){mutableStateOf<String?>(null)}
    var conditions by remember(station.id,game.mode){mutableStateOf(false)}
    // Demo inputs are explicit static assumptions, not refreshed vehicle telemetry or saved preferences.
    var targetText by remember(station.id,game.mode){mutableStateOf(game.homeCharge.targetSoc?.toString().orEmpty())}
    var capacityText by remember(station.id,game.mode){mutableStateOf("60")}
    var powerText by remember(station.id,game.mode){mutableStateOf("36")}
    val mock=game.mode==AppMode.MOCK
    val vehicle=reading.vehicle
    val expectedSource=if(mock)DataSource.MOCK else DataSource.PLEOS
    val soc=vehicle.soc?.toDouble()?.takeIf{reading.fresh && vehicle.source==expectedSource}
    val target=if(mock)targetText.toDoubleOrNull()?.takeIf{it.isFinite() && it in 1.0..100.0} else game.homeCharge.targetSoc?.toDouble()
    val capacity=if(mock)positiveLeisureInput(capacityText) else null
    val power=if(mock)positiveLeisureInput(powerText) else null
    val estimate=ChargingTimeEstimator.estimate(ChargingTimeInput(soc,target,capacity,power,vehicle.source,vehicle.sampledAt,
        capacitySource=if(mock)DataSource.MOCK else DataSource.UNAVAILABLE,
        powerSource=if(mock)DataSource.MOCK else DataSource.UNAVAILABLE,
        capacitySampledAt=0,powerSampledAt=0,mockStaticAssumptions=mock),now)
    var retry by remember(station.id){mutableIntStateOf(0)}
    var loadFailed by remember(station.id){mutableStateOf(false)}
    val snapshot by produceState<LeisurePlaceSnapshot?>(null,station.id,repository,retry) {
        loadFailed=false
        try {value=repository.nearby(station)}
        catch(e: CancellationException){throw e}
        catch(e: Exception){loadFailed=true}
    }
    val allPlaces=snapshot?.places.orEmpty()
    val recommendations=ChargingLeisure.recommend(station,allPlaces,category,estimate)
    val selected=selectedPlaceId?.let{id->ChargingLeisure.recommend(station,allPlaces,estimate=estimate).firstOrNull{it.place.id==id}}
    val sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheetState,containerColor=Bg,dragHandle=null) {
        // Resolve these inside the dialog's composition so they control its input token.
        val keyboard=LocalSoftwareKeyboardController.current
        val focus=LocalFocusManager.current
        val finishInput=KeyboardActions(onDone={focus.clearFocus();keyboard?.hide()})
        fun finishEditing(){focus.clearFocus();keyboard?.hide()}
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).testTag("chargingLeisureSheet")) {
            Row(Modifier.fillMaxWidth().padding(start=18.dp,end=8.dp,top=10.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                if(selected!=null) IconButton(onClick={selectedPlaceId=null},modifier=Modifier.testTag("leisureBack")){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"장소 목록으로")}
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("충전하는 동안 뭐 하지?",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink)
                    Text(station.name,fontSize=14.sp,color=Subtle,modifier=Modifier.testTag("leisureStation"))
                }
                IconButton(onClick=onDismiss,modifier=Modifier.testTag("leisureClose")){Icon(Icons.Outlined.Close,"즐길거리 닫기")}
            }
            HorizontalDivider(color=Line)
            LazyColumn(Modifier.fillMaxWidth().weight(1f).imePadding().testTag("leisureList"),
                contentPadding=PaddingValues(horizontal=18.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                if(selected==null) {
                    item { LeisureEstimatePanel(estimate,soc,target,mock,when {
                        !mock -> "배터리 용량·평균 충전 출력 미연동"
                        soc==null -> "최신 데모 SOC 확인 필요"
                        target==null -> "목표 SOC 입력 필요"
                        soc>=target -> "현재 SOC보다 높은 목표를 확인하세요"
                        capacity==null || power==null -> "모의 용량·출력 입력 필요"
                        else -> "충전 조건 확인 필요"
                    }) }
                    if(mock) item {
                        TextButton(onClick={if(conditions)finishEditing();conditions=!conditions},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("leisureConditions")) {
                            Icon(Icons.Outlined.Tune,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("충전 조건 (데모)")
                            Spacer(Modifier.weight(1f));Icon(if(conditions)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,null)
                        }
                        if(conditions)Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text(if(game.homeCharge.targetSoc!=null)"저장한 목표를 초기값으로 사용해요. 변경은 이 화면에만 적용됩니다."
                                else "목표 SOC를 입력하면 시간에 맞춰 추천해요. 모의 입력은 저장되지 않습니다.",fontSize=14.sp,color=Subtle)
                            LeisureInput(targetText,{targetText=it},"목표 SOC (%)", "leisureTarget",finishInput,
                                targetText.isNotBlank() && target==null)
                            LeisureInput(capacityText,{capacityText=it},"배터리 용량 (kWh) · 가정", "leisureCapacity",finishInput,
                                capacityText.isNotBlank() && capacity==null)
                            LeisureInput(powerText,{powerText=it},"평균 실효 출력 (kW) · 가정", "leisurePower",finishInput,
                                powerText.isNotBlank() && power==null)
                            Text("실측 출력이 아닌 계산용 가정값입니다.",fontSize=13.sp,color=Subtle)
                        }
                    }
                    item {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            LeisureBadge("가상 장소")
                            Text("반경 300m",color=Subtle,fontSize=14.sp)
                        }
                        Text("직선거리 기준 · 도보 시간 미확인",color=Subtle,fontSize=14.sp,modifier=Modifier.padding(top=8.dp).testTag("leisureDistanceNote"))
                    }
                    item {
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected=category==null,onClick={finishEditing();category=null},label={Text("전체")},modifier=Modifier.heightIn(min=48.dp).testTag("leisureFilter-ALL"))
                            LeisureCategory.entries.forEach { filter ->
                                FilterChip(selected=category==filter,onClick={finishEditing();category=filter},label={Text(filter.label)},
                                    modifier=Modifier.heightIn(min=48.dp).testTag("leisureFilter-${filter.name}"))
                            }
                        }
                    }
                    if(loadFailed)item{TextButton(onClick={retry++}){Text("장소 다시 불러오기")}}
                    else if(snapshot==null)item{Text("가상 장소를 불러오고 있어요.",color=Subtle)}
                    else if(recommendations.isEmpty())item{Text("이 조건의 가상 장소가 아직 없어요.",color=Subtle,modifier=Modifier.testTag("leisureEmpty"))}
                    items(recommendations,key={it.place.id}) { recommendation ->
                        LeisurePlaceCard(recommendation,destinationEnabled,
                            onDestination={finishEditing();if(onDestination(recommendation.place))onDismiss()},
                            onDetail={finishEditing();selectedPlaceId=recommendation.place.id})
                    }
                    item { Text(if(estimate!=null)"활동시간을 기준으로 추천해요. 보행·복귀 시간과 실제 충전 완료는 보장하지 않습니다."
                        else "활동시간은 예시이며 실제 영업·행사 정보는 연결되지 않았어요.",fontSize=14.sp,color=Subtle) }
                } else {
                    item { LeisurePlaceDetail(selected)
                        OutlinedButton(onClick={if(onDestination(selected.place))onDismiss()},enabled=destinationEnabled,
                            modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("leisureDestinationDetail")){Text("목적지 설정")}
                    }
                }
            }
        }
    }
}

private fun positiveLeisureInput(value: String)=value.toDoubleOrNull()?.takeIf{it.isFinite() && it>0}
private fun leisureNumber(value: Double?)=value?.let{if(it%1.0==0.0)it.toInt().toString() else "%.1f".format(Locale.KOREA,it)} ?: "—"
private val LeisureCategory.icon: ImageVector get()=when(this) {
    LeisureCategory.CAFE->Icons.Outlined.LocalCafe
    LeisureCategory.WALK_PARK->Icons.AutoMirrored.Outlined.DirectionsWalk
    LeisureCategory.RESTAURANT->Icons.Outlined.Restaurant
    LeisureCategory.POPUP_EVENT->Icons.Outlined.Event
}

@Composable private fun LeisureEstimatePanel(estimate: ChargingTimeEstimate?,soc: Double?,target: Double?,mock: Boolean,missing: String) {
    Panel(Modifier.fillMaxWidth()) {
        Text("목표까지 예상 충전시간",fontSize=15.sp,color=Subtle)
        Text(estimate?.let{"약 ${it.minutes}분"} ?: "시간 정보 없음",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.testTag("leisureEstimate"))
        Text("현재 SOC ${leisureNumber(soc)}% → 목표 ${leisureNumber(target)}%",fontSize=16.sp,color=Ink,modifier=Modifier.testTag("leisureSocTarget"))
        if(estimate==null)Text(missing,fontSize=14.sp,color=Subtle,modifier=Modifier.testTag("leisureEstimateReason"))
        else {
            Text("활동 ${(kotlin.math.floor(estimate.preciseMinutes).toInt()-ChargingLeisure.returnReserveMinutes).coerceAtLeast(0)}분 이하부터 추천해요.",
                fontSize=15.sp,color=GreenDark,modifier=Modifier.testTag("leisureTimeAdvice"))
            Text("복귀 여유 5분을 제외한 활동 기준 · 도보 시간 미확인",fontSize=14.sp,color=Subtle)
        }
    }
}

@Composable private fun LeisureInput(value: String,onValueChange:(String)->Unit,label: String,tag: String,actions: KeyboardActions,error: Boolean) {
    OutlinedTextField(value=value,onValueChange={if(it.length<=10)onValueChange(it)},label={Text(label)},singleLine=true,
        isError=error,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal,imeAction=ImeAction.Done),keyboardActions=actions,
        supportingText=if(error){{Text("유효한 양수를 입력하세요.")}} else null,
        modifier=Modifier.fillMaxWidth().testTag(tag))
}

@Composable private fun LeisureBadge(text: String,advertisement: Boolean=false) {
    Surface(color=if(advertisement)Gold.copy(alpha=.12f) else SoftGreen,shape=RoundedCornerShape(8.dp)) {
        Text(text,Modifier.padding(horizontal=10.dp,vertical=6.dp),fontSize=13.sp,color=if(advertisement)Ink else Green,fontWeight=FontWeight.Medium)
    }
}

@Composable private fun LeisurePlaceCard(recommendation: LeisureRecommendation,destinationEnabled: Boolean,onDestination:()->Unit,onDetail:()->Unit) {
    val place=recommendation.place
    Surface(color=Paper,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth().testTag("leisureCard-${place.id}")) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Surface(color=SoftGreen,shape=RoundedCornerShape(12.dp)){Icon(place.category.icon,null,tint=Green,modifier=Modifier.padding(10.dp).size(23.dp))}
                Text(place.name,Modifier.weight(1f),fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                if(place.isAdvertisement)LeisureBadge("광고",true)
            }
            Text("${place.category.label} · 직선 ${recommendation.straightLineMeters.roundToInt()}m",fontSize=14.sp,color=Subtle)
            Text("${place.activityTitle} · 약 ${place.activityMinutes}분",fontSize=16.sp,fontWeight=FontWeight.Medium,color=Ink)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick=onDetail,modifier=Modifier.heightIn(min=48.dp).testTag("leisureDetail-${place.id}")){Text("상세보기");Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp))}
                OutlinedButton(onClick=onDestination,enabled=destinationEnabled,
                    modifier=Modifier.heightIn(min=48.dp).testTag("leisureDestination-${place.id}")){Text("목적지 설정")}
            }
            if(recommendation.timeFit==LeisureTimeFit.TOO_LONG)Text("활동시간 여유를 확인하세요",fontSize=14.sp,color=Subtle)
        }
    }
}

@Composable private fun LeisurePlaceDetail(recommendation: LeisureRecommendation) {
    val place=recommendation.place
    Column(Modifier.fillMaxWidth().testTag("leisurePlaceDetail"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(place.category.icon,null,tint=Green,modifier=Modifier.size(32.dp))
            Text(place.category.label,color=Subtle,fontSize=16.sp)
            if(place.isAdvertisement)LeisureBadge("광고",true)
        }
        Text(place.name,fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.testTag("leisurePlaceName"))
        Text(place.description,fontSize=16.sp,color=Ink)
        Panel(Modifier.fillMaxWidth()) {
            Text("${place.activityTitle} · 약 ${place.activityMinutes}분",fontSize=17.sp,fontWeight=FontWeight.Medium)
            Text("충전소에서 직선 ${recommendation.straightLineMeters.roundToInt()}m",fontSize=16.sp)
            Text(recommendation.walkingMinutes?.let{"도보 약 ${it}분"} ?: "도보 시간 미확인",fontSize=15.sp,color=Subtle)
            Text("가상 위치 · %.5f, %.5f".format(Locale.KOREA,place.lat,place.lon),fontSize=14.sp,color=Subtle,modifier=Modifier.testTag("leisureCoordinates"))
        }
        place.benefit?.let { benefit ->
            Panel(Modifier.fillMaxWidth().testTag("leisureBenefit")) {
                LeisureBadge("예시 혜택 · 발급 불가")
                Text(benefit.title,fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                Text(benefit.description,fontSize=15.sp,color=Subtle)
            }
        }
        Text("실제 장소·영업·행사 정보는 연결되지 않은 가상 예시입니다.",fontSize=14.sp,color=Subtle)
    }
}
