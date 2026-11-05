package com.gridguild.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.GridViewModel
import com.gridguild.core.*

@Composable internal fun HomeChargeScreen(g: GameState,reading: VehicleReading,now: Long,model: GridViewModel) {
    val v=reading.vehicle
    val preferences=g.homeCharge
    val plan=HomeChargePolicy.plan(preferences,v.soc,now)
    val active=g.mission?.takeIf{it.kind==MissionKind.HOME}
    val done=HomeChargeEngine.completedToday(g,now)
    var register by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    val busy by model.startingMission.collectAsState()
    val correctSource=v.source==if(g.mode==AppMode.MOCK)DataSource.MOCK else DataSource.PLEOS
    val withinTarget=preferences.targetSoc?.let{target->v.soc?.let{it+2f<=target}}!=false
    val eligible=reading.fresh && correctSource && withinTarget && v.soc?.let{it<90f}==true && g.mission==null && !done && busy==null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("우리 집 충전",style=MaterialTheme.typography.headlineMedium)
                preferences.chargerName?.let{Text(it,fontSize=13.sp,color=Subtle,modifier=Modifier.testTag("homeChargerName"))}
            }
            IconButton(onClick={advanced=true},modifier=Modifier.testTag("homeAdvanced")){Icon(Icons.Outlined.Settings,"고급 설정",tint=Subtle)}
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(when {done->"● 오늘 참여 완료";active!=null && now<(active.windowStart ?: now)->"● 참여 완료 · 충전 대기";active!=null && v.chargeSwitch==ChargeSignal.ON->"● 충전 확인 중";else->"● 충전 대기"},color=Green,fontSize=14.sp)
            Text("현재 차량 배터리",fontSize=14.sp,color=Subtle)
            Text("${v.soc?.toInt()?.toString() ?: "—"}%",fontSize=48.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.testTag("homeSoc"))
            HorizontalDivider(color=Line,modifier=Modifier.padding(vertical=6.dp))
            Text("오늘의 추천 충전 시간",fontSize=14.sp,color=Subtle)
            val night=active?.let{(it.windowEnd ?: 0)-(it.windowStart ?: 0)==4*60*60*1000L} ?: plan.night
            Text(if(night)"밤 11:00 ~ 새벽 3:00" else "필요할 때 바로 충전",fontSize=26.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.testTag("homeRecommendation"))
            if(!night && active==null) Text(when(plan.advice) {
                HomeAdvice.LOW_BATTERY->"배터리가 부족해 충전을 미루지 않는 게 좋아요."
                HomeAdvice.UNKNOWN_DEPARTURE->"출발 시간을 몰라 충전 지연을 권하지 않아요."
                HomeAdvice.BELOW_TARGET->"목표 배터리를 고려해 충전을 우선해 주세요."
                HomeAdvice.DEPARTURE_SOON->"출발 전에 여유 있게 충전해 주세요."
                else->"차량 정보를 확인하고 평소처럼 충전해 주세요."
            },fontSize=13.sp,color=Subtle)
            Surface(color=SoftGreen,shape=RoundedCornerShape(12.dp)) {
                Text(if((v.soc ?: 0f)>=90f)"추가 충전을 권하지 않아요" else if(!withinTarget)"보상보다 목표 SOC를 우선해 주세요" else "조건 달성 시 +120 데모 XP",Modifier.padding(14.dp),color=Green,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
            }
            if(active!=null) LinearProgressIndicator(progress={active.progress},modifier=Modifier.fillMaxWidth(),color=Green)
            Button(onClick={if(preferences.chargerName==null)register=true else model.startHomeCharge()},
                enabled=if(preferences.chargerName==null)true else eligible,
                shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("homeJoin")) {
                Text(when {
                    preferences.chargerName==null->"우리 집 충전소 등록"
                    done->"오늘 보상 완료"
                    active!=null->"참여 완료 · 자동 확인 중"
                    g.mission!=null->"ROUTE 미션 진행 중"
                    busy!=null->"참여 처리 중…"
                    !reading.fresh || !correctSource->"차량 정보 확인 중"
                    (v.soc ?: 0f)>=90f->"배터리가 충분해요"
                    !withinTarget->"목표 SOC를 우선해 주세요"
                    else->"충전 미션 참여하기"
                })
            }
            Text(if(active!=null)"평소처럼 충전하세요. 충전 ON·SOC 2%p 상승을 자동 확인합니다." else "참여 후 충전 ON·SOC 2%p 상승 시 자동 완료됩니다.",fontSize=13.sp,color=Subtle)
        }
        Text("추천 시간은 예시용 모의 데이터입니다. 앱은 충전 시작을 제어하지 않습니다.",fontSize=12.sp,color=Subtle)
        Text("충전 장소·전력망 기여 미인증 데모 보상",fontSize=12.sp,color=Subtle)
        if(active!=null) TextButton(onClick=model::cancelMission,modifier=Modifier.testTag("homeCancel")){Text("참여 취소")}
    }
    if(register) HomeRegistrationDialog(onDismiss={register=false},onSave={model.registerHomeCharger(it);register=false})
    if(advanced) HomeAdvancedDialog(preferences,onDismiss={advanced=false},onSave={target,departure->model.saveHomePreferences(target,departure);advanced=false})
}

@Composable private fun HomeRegistrationDialog(onDismiss: ()->Unit,onSave: (String)->Unit) {
    var name by remember{mutableStateOf("우리 집 충전소")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("우리 집 충전소 등록")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("한 번 등록하면 다음부터 자동으로 불러옵니다. 실제 장소 인증은 제공하지 않습니다.",fontSize=14.sp)
        OutlinedTextField(value=name,onValueChange={if(it.length<=40)name=it},label={Text("충전소 이름")},singleLine=true,modifier=Modifier.testTag("homeChargerInput"))
    }},confirmButton={Button(onClick={onSave(name.trim())},enabled=name.isNotBlank(),modifier=Modifier.testTag("homeRegisterSave")){Text("등록")}},dismissButton={TextButton(onClick=onDismiss){Text("취소")}})
}

@Composable private fun HomeAdvancedDialog(preferences: HomeChargePreferences,onDismiss: ()->Unit,onSave: (Int?,Int?)->Unit) {
    var target by remember{mutableStateOf(preferences.targetSoc?.toString() ?: "")}
    var departure by remember{mutableStateOf(preferences.departureMinutes?.let{"%02d:%02d".format(it/60,it%60)} ?: "")}
    val targetValue=target.toIntOrNull()
    val parts=departure.split(':')
    val hour=parts.getOrNull(0)?.toIntOrNull();val minute=parts.getOrNull(1)?.toIntOrNull()
    val departureValue=if(parts.size==2 && hour!=null && hour in 0..23 && minute!=null && minute in 0..59)hour*60+minute else null
    val valid=(target.isBlank() || targetValue in 1..100) && (departure.isBlank() || departureValue!=null)
    AlertDialog(onDismissRequest=onDismiss,title={Text("고급 설정 · 선택 사항")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("필요할 때 한 번 저장하면 재사용합니다. 비워 두어도 미션에 참여할 수 있습니다.",fontSize=14.sp)
        OutlinedTextField(value=target,onValueChange={target=it},label={Text("목표 SOC (%) · 선택")},singleLine=true,isError=target.isNotBlank() && targetValue !in 1..100,modifier=Modifier.testTag("homeTargetSoc"))
        OutlinedTextField(value=departure,onValueChange={departure=it},label={Text("출발 시각 HH:mm · 선택")},singleLine=true,isError=departure.isNotBlank() && departureValue==null,modifier=Modifier.testTag("homeDeparture"))
        Text("배터리가 부족하거나 출발 시간을 모르면 충전을 미루지 않습니다. 밤 시간은 모의 추천이며 목표 SOC 도달이나 완료 시각을 보장하지 않습니다. 변경한 설정은 다음 참여부터 적용됩니다.",fontSize=12.sp,color=Subtle)
        if(!valid)Text("SOC는 1~100, 출발 시각은 07:30 형식으로 입력하세요.",color=MaterialTheme.colorScheme.error,fontSize=12.sp)
    }},confirmButton={Button(onClick={onSave(if(target.isBlank())null else targetValue,if(departure.isBlank())null else departureValue)},enabled=valid,modifier=Modifier.testTag("homeAdvancedSave")){Text("저장")}},dismissButton={TextButton(onClick=onDismiss){Text("닫기")}})
}
