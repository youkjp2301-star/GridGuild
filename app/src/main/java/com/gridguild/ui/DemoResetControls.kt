@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.core.*

@Composable internal fun DemoResetControls(game: GameState,onReset: (DemoResetTarget,String?)->Unit,onLevelStep: (Int,String?)->Unit) {
    var pending by remember(game.districtId,game.mode) { mutableStateOf<DemoResetTarget?>(null) }
    val district=Busan.find(game.districtId)
    val cityXp=game.cityXp(game.districtId)
    Column(Modifier.fillMaxWidth().testTag("demoResetControls"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color=Line,modifier=Modifier.padding(vertical=6.dp))
        Text("테스트 데이터 초기화",fontWeight=FontWeight.SemiBold,color=Ink)
        Text("도시 Lv.${CityLevels.level(cityXp)} · 도시 $cityXp XP · 내 포인트 ${game.xp} XP",fontSize=13.sp,color=Subtle)
        val level=CityLevels.level(cityXp)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf(-1 to "도시 레벨 −1",1 to "도시 레벨 +1").forEach { (delta,label) ->
                OutlinedButton(onClick={onLevelStep(delta,game.districtId)},
                    enabled=game.mode==AppMode.MOCK && district!=null && (if(delta<0)level>1 else level<12),
                    shape=RoundedCornerShape(12.dp),modifier=Modifier.heightIn(min=48.dp)
                        .testTag(if(delta<0)"cityLevelMinus" else "cityLevelPlus")) { Text(label) }
            }
        }
        Text("Lv.1–12 · 도시 XP만 테스트 값으로 조정해요. 내 XP·충전 미션은 유지됩니다.",fontSize=12.sp,color=Subtle)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            listOf(DemoResetTarget.CITY_LEVEL,DemoResetTarget.CHARGING_MISSIONS,DemoResetTarget.PERSONAL_POINTS).forEach { target ->
                OutlinedButton(onClick={pending=target},enabled=target!=DemoResetTarget.CITY_LEVEL || district!=null,
                    shape=RoundedCornerShape(12.dp),modifier=Modifier.heightIn(min=48.dp).testTag(target.testTag)) {
                    Icon(Icons.Outlined.Refresh,null,Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text(target.label)
                }
            }
        }
        TextButton(onClick={pending=DemoResetTarget.ALL},modifier=Modifier.testTag(DemoResetTarget.ALL.testTag)) {
            Text(DemoResetTarget.ALL.label,color=MaterialTheme.colorScheme.error)
        }
    }
    pending?.let { target ->
        AlertDialog(onDismissRequest={pending=null},
            title={Text("${target.label}할까요?")},
            text={Text(when(target) {
                DemoResetTarget.CITY_LEVEL -> "${district?.name ?: "선택 지역"}의 도시 XP를 0으로, 레벨을 Lv.1로 되돌립니다. Lv.2 이상 도시는 다시 잠깁니다.\n\n내 포인트·미션 기록과 다른 지역의 도시 XP는 유지됩니다. 진행 중인 미션을 완료하면 도시가 다시 성장합니다."
                DemoResetTarget.CHARGING_MISSIONS -> "진행 중인 미션을 취소하고 충전소 완료 상태·일일 보너스 판정을 초기화합니다. 오늘 완료한 충전소에도 다시 도전할 수 있습니다.\n\n이미 획득한 내 포인트와 도시 XP·레벨은 유지됩니다."
                DemoResetTarget.PERSONAL_POINTS -> "내 누적·이번 시즌 데모 포인트를 0 XP로 되돌립니다.\n\n도시 XP·레벨, 진행 중인 미션과 오늘 완료 상태는 유지됩니다."
                DemoResetTarget.ALL -> "지역 선택, 모든 도시 XP·레벨, 내 포인트와 미션 기록을 지우고 처음부터 시작합니다. 데모 배터리는 42%, 충전은 OFF가 됩니다."
            })},
            confirmButton={Button(onClick={onReset(target,game.districtId);pending=null},shape=RoundedCornerShape(12.dp),modifier=Modifier.testTag("confirmDemoReset")) { Text("초기화") }},
            dismissButton={TextButton(onClick={pending=null},modifier=Modifier.testTag("cancelDemoReset")) { Text("취소") }})
    }
}

private val DemoResetTarget.label get()=when(this) {
    DemoResetTarget.CITY_LEVEL -> "도시 Lv 초기화"
    DemoResetTarget.CHARGING_MISSIONS -> "충전 미션 초기화"
    DemoResetTarget.PERSONAL_POINTS -> "내 포인트 초기화"
    DemoResetTarget.ALL -> "전체 데모 데이터 초기화"
}
private val DemoResetTarget.testTag get()=when(this) {
    DemoResetTarget.CITY_LEVEL -> "resetCityLevel"
    DemoResetTarget.CHARGING_MISSIONS -> "resetMissions"
    DemoResetTarget.PERSONAL_POINTS -> "resetPoints"
    DemoResetTarget.ALL -> "resetAll"
}
