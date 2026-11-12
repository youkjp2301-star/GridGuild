package com.gridguild.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
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

@Composable internal fun HomeChargeScreen(
    g: GameState,
    reading: VehicleReading,
    now: Long,
    model: GridViewModel,
    onCity: () -> Unit = {},
) {
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
    val testWithinTarget=active?.let{mission->
        val goal=maxOf(v.soc ?: 100f,(mission.windowBaselineSoc ?: mission.baselineSoc)+mission.requiredGain)
        goal<=90f && preferences.targetSoc?.let{goal<=it}!=false
    } ?: withinTarget
    val eligible=reading.fresh && correctSource && withinTarget && v.soc?.let{it<90f}==true && g.mission==null && !done && busy==null
    val waiting=active!=null && now<(active.windowStart ?: now)
    val todayAwards=g.completionHistory.filter{it.kind=="HOME" && GameState.day(it.at)==GameState.day(now)}
    val personalAward=todayAwards.filter{it.personalCredit}.sumOf{it.xp}
    val cityAward=todayAwards.filter{it.cityCredit}.sumOf{it.xp}
    val cityXp=g.cityXp(g.districtId)
    val joinText=when {
        preferences.chargerName==null->"우리 집 충전소 등록"
        done->"오늘 보상 완료"
        active!=null->"참여 완료 · 자동 확인 중"
        g.mission!=null->"ROUTE 미션 진행 중"
        busy!=null->"참여 처리 중…"
        !reading.fresh || !correctSource->"차량 정보 확인 중"
        (v.soc ?: 0f)>=90f->"배터리가 충분해요"
        !withinTarget->"목표 SOC를 우선해 주세요"
        else->"충전 미션 참여하기"
    }
    val missionCard: @Composable (Modifier) -> Unit = { cardModifier ->
        BentoCard(modifier=cardModifier,tone=BentoTone.DARK,index=0) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Text("TODAY'S MISSION",fontSize=11.sp,letterSpacing=1.5.sp,color=Color.White.copy(alpha=.60f))
                Surface(color=Green,shape=RoundedCornerShape(8.dp)) {
                    Text(when {done->"오늘 완료";active!=null->"진행 중";else->"HOME CHARGE"},modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp),fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=Ink)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(when {
                done->"오늘의 충전,\n도시의 힘이 되었어요."
                active!=null && (!reading.fresh || !correctSource)->"차량 정보를\n확인하고 있어요."
                waiting->"참여 완료!\n추천 시간에 만나요."
                active!=null && v.chargeSwitch==ChargeSignal.ON->"충전하는 동안\n도시가 자라고 있어요."
                active!=null->"참여했어요.\n충전을 연결해 주세요."
                preferences.chargerName==null->"우리 집 충전으로\n시작하는 도시 성장."
                else->"평소처럼 충전하고\n도시를 키워보세요."
            },fontSize=27.sp,lineHeight=36.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
            Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                AnimatedNumber(if(done)personalAward else HomeChargeEngine.reward,prefix="+",suffix=" XP",color=if(done)Green else Color.White,fontSize=42.sp)
                Icon(if(done)Icons.Outlined.CheckCircle else Icons.Outlined.Bolt,contentDescription=null,tint=Green,modifier=Modifier.padding(bottom=8.dp).size(23.dp))
            }
            Text(if(done)"개인 $personalAward · 도시 $cityAward XP 반영" else "개인과 소속 도시에 함께 쌓이는 보상",fontSize=13.sp,color=Color.White.copy(alpha=.68f))
            if(active!=null) {
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(if(waiting)"충전 대기" else "SOC 상승 확인",fontSize=13.sp,color=Color.White.copy(alpha=.78f))
                    Text("${(active.progress.coerceIn(0f,1f)*100).toInt()}%",fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=Green)
                }
                BentoProgress(active.progress,Modifier.fillMaxWidth(),color=Green)
            }
            if((v.soc ?: 0f)>=90f || !withinTarget) {
                Text(if((v.soc ?: 0f)>=90f)"추가 충전을 권하지 않아요" else "보상보다 목표 SOC를 우선해 주세요",fontSize=13.sp,color=Green)
            }
            Spacer(Modifier.height(2.dp))
            PrimaryButton(
                text=joinText,
                onClick={if(preferences.chargerName==null)register=true else model.startHomeCharge()},
                enabled=if(preferences.chargerName==null)true else eligible,
                loading=preferences.chargerName!=null && busy!=null,
                modifier=Modifier.fillMaxWidth().testTag("homeJoin"),
            )
            if(g.mode==AppMode.MOCK && preferences.chargerName!=null) {
                OutlinedButton(
                    onClick=model::simulateHomeCharge,
                    enabled=reading.fresh && correctSource && testWithinTarget && (v.soc ?: 100f)<90f && !done && busy==null && (g.mission==null || active!=null),
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("homeSimulate"),
                    shape=RoundedCornerShape(14.dp),
                    border=BorderStroke(1.dp,Color.White.copy(alpha=.22f)),
                    colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White,disabledContentColor=Color.White.copy(alpha=.38f)),
                ) { Text("충전 테스트 · +120 XP",fontSize=14.sp) }
            }
            Text(if(g.mode==AppMode.MOCK)"테스트는 추천 시간 충전으로 자동 판정해요." else "충전 ON · SOC 2%p 상승을 자동 확인해요.",fontSize=12.sp,lineHeight=18.sp,color=Color.White.copy(alpha=.60f))
            if(active!=null) {
                TextButton(onClick=model::cancelMission,modifier=Modifier.align(Alignment.CenterHorizontally).heightIn(min=48.dp).testTag("homeCancel"),colors=ButtonDefaults.textButtonColors(contentColor=Color.White.copy(alpha=.78f))) { Text("참여 취소") }
            }
            if(done) {
                HorizontalDivider(color=Color.White.copy(alpha=.12f),modifier=Modifier.padding(vertical=4.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("내 도시  Lv.${CityLevels.level(cityXp)}",fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=Color.White)
                    Text("$cityXp XP",fontSize=12.sp,color=Color.White.copy(alpha=.68f))
                }
                BentoProgress(CityLevels.progress(cityXp),Modifier.fillMaxWidth(),color=Green)
                TextButton(onClick=onCity,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),colors=ButtonDefaults.textButtonColors(contentColor=Green)) {
                    Text("내 도시 보기")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward,contentDescription=null,modifier=Modifier.size(18.dp))
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide=maxWidth>=720.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom=4.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    SectionHeader("우리 집 충전",kicker="HOME CHARGE")
                    preferences.chargerName?.let{Text(it,fontSize=13.sp,color=Subtle,modifier=Modifier.testTag("homeChargerName"))}
                }
                IconButton(onClick={advanced=true},modifier=Modifier.size(48.dp).testTag("homeAdvanced")) { Icon(Icons.Outlined.Settings,"고급 설정",tint=Subtle,modifier=Modifier.size(22.dp)) }
            }
            if(wide) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top) {
                    missionCard(Modifier.weight(1.05f))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        HomeBatteryBento(reading,correctSource,preferences,onPreferences={advanced=true})
                        HomeRecommendationBento(showPriority=(v.soc ?: 100f)<30f || (g.mode==AppMode.PLEOS && !plan.scheduled))
                    }
                }
            } else {
                missionCard(Modifier.fillMaxWidth())
                HomeBatteryBento(reading,correctSource,preferences,onPreferences={advanced=true})
                HomeRecommendationBento(showPriority=(v.soc ?: 100f)<30f || (g.mode==AppMode.PLEOS && !plan.scheduled))
            }
        }
    }
    if(register) HomeRegistrationDialog(onDismiss={register=false},onSave={model.registerHomeCharger(it);register=false})
    if(advanced) HomeAdvancedDialog(preferences,onDismiss={advanced=false},onSave={target,departure->model.saveHomePreferences(target,departure);advanced=false})
}

@Composable private fun HomeBatteryBento(reading: VehicleReading,correctSource: Boolean,preferences: HomeChargePreferences,onPreferences: () -> Unit) {
    val trusted=reading.fresh && correctSource
    val soc=reading.vehicle.soc.takeIf{trusted}
    val charge=if(trusted)reading.vehicle.chargeSwitch else ChargeSignal.UNKNOWN
    BentoCard(Modifier.fillMaxWidth(),index=1) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("차량 배터리",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            StatusBadge(when(charge){ChargeSignal.ON->"충전 ON";ChargeSignal.OFF->"충전 OFF";else->"확인 중"},color=if(charge==ChargeSignal.ON)GreenDark else Subtle)
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(122.dp),contentAlignment=Alignment.Center) {
                SocGauge(soc=soc,fresh=trusted,targetSoc=preferences.targetSoc,size=122.dp,showValue=false)
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("${soc?.toInt()?.toString() ?: "—"}%",fontSize=31.sp,fontWeight=FontWeight.SemiBold,color=Ink,modifier=Modifier.testTag("homeSoc"))
                    Text("현재 SOC",fontSize=11.sp,color=Subtle)
                }
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                HomeSavedPreference("목표 SOC",preferences.targetSoc?.let{"$it%"} ?: "미설정")
                HomeSavedPreference("저장된 출발",preferences.departureMinutes?.let{"%02d:%02d".format(it/60,it%60)} ?: "미설정")
            }
        }
        TextButton(onClick=onPreferences,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),colors=ButtonDefaults.textButtonColors(contentColor=GreenDark)) {
            Text("목표 · 출발 설정",fontSize=13.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward,contentDescription=null,modifier=Modifier.size(16.dp))
        }
    }
}

@Composable private fun HomeSavedPreference(label: String,value: String) {
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text(label,fontSize=12.sp,color=Subtle)
        Text(value,fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Ink)
    }
}

@Composable private fun HomeRecommendationBento(showPriority: Boolean) {
    BentoCard(Modifier.fillMaxWidth(),tone=BentoTone.SUBTLE,index=2) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule,contentDescription=null,tint=Subtle,modifier=Modifier.size(18.dp))
            Text("추천 충전 시간",fontSize=15.sp,fontWeight=FontWeight.SemiBold,color=Ink,modifier=Modifier.weight(1f))
            Text("모의 추천",fontSize=11.sp,color=Subtle)
        }
        Row(Modifier.fillMaxWidth().testTag("homeRecommendation"),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            HomeRecommendationSlot("16:00","17:00",Modifier.weight(1f))
            HomeRecommendationSlot("22:00","23:00",Modifier.weight(1f))
        }
        if(showPriority) {
            Surface(color=SoftGreen,shape=RoundedCornerShape(12.dp)) {
                Text("충전을 미루지 말고 배터리와 출발 일정을 우선하세요.",fontSize=13.sp,lineHeight=20.sp,color=GreenDark,modifier=Modifier.padding(12.dp).testTag("homeChargePriority"))
            }
        }
        Text("모의 추천 · 충전 시작은 직접 조절합니다.",fontSize=12.sp,lineHeight=18.sp,color=Subtle)
    }
}

@Composable private fun HomeRecommendationSlot(start: String,end: String,modifier: Modifier=Modifier) {
    Surface(modifier=modifier,color=Paper,shape=RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(start,fontSize=25.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            Text("– $end",fontSize=14.sp,color=Subtle)
        }
    }
}

@Composable private fun HomeRegistrationDialog(onDismiss: ()->Unit,onSave: (String)->Unit) {
    var name by remember{mutableStateOf("우리 집 충전소")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("우리 집 충전소 등록")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("한 번 등록하면 다음부터 자동으로 불러옵니다. 실제 장소 인증은 제공하지 않습니다.",fontSize=14.sp)
        OutlinedTextField(value=name,onValueChange={if(it.length<=40)name=it},label={Text("충전소 이름")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("homeChargerInput"))
    }},confirmButton={Button(onClick={onSave(name.trim())},enabled=name.isNotBlank(),modifier=Modifier.heightIn(min=48.dp).testTag("homeRegisterSave")){Text("등록")}},dismissButton={TextButton(onClick=onDismiss,modifier=Modifier.heightIn(min=48.dp)){Text("취소")}})
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
        Text("한 번 저장하면 다음 참여에도 사용해요. 비워 두어도 참여할 수 있어요.",fontSize=14.sp)
        OutlinedTextField(value=target,onValueChange={target=it},label={Text("목표 SOC (%) · 선택")},singleLine=true,isError=target.isNotBlank() && targetValue !in 1..100,modifier=Modifier.fillMaxWidth().testTag("homeTargetSoc"))
        OutlinedTextField(value=departure,onValueChange={departure=it},label={Text("출발 시각 HH:mm · 선택")},singleLine=true,isError=departure.isNotBlank() && departureValue==null,modifier=Modifier.fillMaxWidth().testTag("homeDeparture"))
        Text("배터리와 출발 일정을 우선하세요. 시간은 모의 추천이며 충전 완료를 보장하지 않아요. 변경은 다음 참여부터 적용돼요.",fontSize=13.sp,lineHeight=20.sp,color=Subtle)
        if(!valid)Text("SOC는 1~100, 출발 시각은 07:30 형식으로 입력하세요.",color=MaterialTheme.colorScheme.error,fontSize=12.sp)
    }},confirmButton={Button(onClick={onSave(if(target.isBlank())null else targetValue,if(departure.isBlank())null else departureValue)},enabled=valid,modifier=Modifier.heightIn(min=48.dp).testTag("homeAdvancedSave")){Text("저장")}},dismissButton={TextButton(onClick=onDismiss,modifier=Modifier.heightIn(min=48.dp)){Text("닫기")}})
}
