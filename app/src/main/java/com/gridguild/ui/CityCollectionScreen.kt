@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.core.*

@Composable internal fun CityCollectionScreen(game: GameState) {
    val district=Busan.find(game.districtId) ?: return
    val xp=game.cityXp(district.id)
    val current=CityLevels.level(xp)
    var selection by rememberSaveable(district.id,current){mutableIntStateOf(current)}
    val shown=CityStage.at(CityUnlocks.selection(selection,xp))
    var nextInfo by rememberSaveable(current){mutableStateOf(false)}
    var details by remember{mutableStateOf(false)}
    var zoom by rememberSaveable{mutableFloatStateOf(1f)}
    val previous=shown.level!=current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(title="${district.name}의 도시",kicker="CITY COLLECTION")

        BentoCard(modifier=Modifier.fillMaxWidth(),index=0) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                StatusBadge(
                    text=if(previous)"해금 기록 · Lv.${shown.level}" else "지금의 도시 · Lv.$current",
                    color=if(previous)Blue else GreenDark
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick={details=true},modifier=Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Info,"도시 정보",Modifier.size(20.dp),tint=Subtle)
                }
            }
            Text(
                "Lv.${shown.level} · ${shown.name}",
                style=MaterialTheme.typography.titleLarge,
                modifier=Modifier.testTag("cityStageLabel")
            )
            Text(shown.landmark,color=Subtle,fontSize=12.sp)

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val sceneRatio=if(maxWidth<440.dp)1.45f else 1.9f
                Crossfade(
                    targetState=shown.level,
                    animationSpec=tween(if(motionEnabled())400 else 0),
                    label="unlockedCity"
                ) { displayed ->
                    CityCanvas(
                        displayed,district.id,current,
                        Modifier.fillMaxWidth().aspectRatio(sceneRatio).testTag("cityScene"),zoom
                    )
                }
            }

            Row(
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)
            ) {
                Text(shown.description,Modifier.weight(1f),color=Subtle,fontSize=12.sp,lineHeight=19.sp)
                Row {
                    IconButton(onClick={zoom=(zoom-.15f).coerceAtLeast(.7f)},modifier=Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Remove,"축소",Modifier.size(20.dp),tint=Ink)
                    }
                    IconButton(onClick={zoom=(zoom+.15f).coerceAtMost(1.5f)},modifier=Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Add,"확대",Modifier.size(20.dp),tint=Ink)
                    }
                }
            }
            if(previous) TextButton(
                onClick={selection=current},
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("cityCurrent")
            ) {
                Icon(Icons.AutoMirrored.Outlined.Undo,null,Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("내 도시로 돌아가기")
            }
        }

        CityGrowthSummary(xp,current){nextInfo=true}

        Row(
            Modifier.padding(top=8.dp,bottom=2.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text("도시 컬렉션",style=MaterialTheme.typography.titleMedium)
                Text("해금한 도시는 다시 방문할 수 있어요.",fontSize=12.sp,color=Subtle)
            }
            StatusBadge("${current}개 해금",color=GreenDark)
        }
        LazyRow(
            horizontalArrangement=Arrangement.spacedBy(10.dp),
            contentPadding=PaddingValues(bottom=8.dp),
            modifier=Modifier.fillMaxWidth().testTag("cityCollection")
        ) {
            items((1..12).toList(),key={it}) { cityLevel ->
                val unlocked=CityUnlocks.canView(cityLevel,current)
                val selectedCity=shown.level==cityLevel
                Surface(
                    onClick={if(unlocked)selection=cityLevel},enabled=unlocked,
                    modifier=Modifier.width(if(selectedCity)190.dp else 160.dp)
                        .testTag("city-stage-$cityLevel").semantics {
                            selected=selectedCity
                            contentDescription=if(unlocked)"해금된 도시 Lv.$cityLevel" else "잠긴 도시 Lv.$cityLevel"
                        },
                    color=if(selectedCity)SoftGreen else Paper,
                    shape=RoundedCornerShape(20.dp),
                    border=BorderStroke(if(selectedCity)1.5.dp else 1.dp,if(selectedCity)GreenDark else Line)
                ) {
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(unlocked) CityCanvas(cityLevel,district.id,current,Modifier.fillMaxWidth().height(104.dp))
                        else LockedCityArtwork(Modifier.fillMaxWidth().height(104.dp),compact=true)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("Lv.$cityLevel",fontSize=13.sp,fontWeight=FontWeight.SemiBold,color=if(unlocked)Ink else Subtle)
                            Spacer(Modifier.weight(1f))
                            if(unlocked) Icon(Icons.Outlined.CheckCircle,null,Modifier.size(15.dp),tint=GreenDark)
                            else Icon(Icons.Outlined.Lock,null,Modifier.size(14.dp),tint=Subtle)
                        }
                        Text(
                            if(unlocked)CityLevels.names[cityLevel-1] else "아직 열리지 않은 도시",
                            Modifier.padding(bottom=3.dp),fontSize=11.sp,color=Subtle
                        )
                    }
                }
            }
        }

        if(nextInfo && current<12) AlertDialog(
            onDismissRequest={nextInfo=false},
            title={Text("다음 도시를 향해",style=MaterialTheme.typography.titleLarge)},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(16.dp),modifier=Modifier.testTag("cityLockedDialog")) {
                    LockedCityArtwork(Modifier.fillMaxWidth().aspectRatio(2f).testTag("nextLockedImage"))
                    Text("Lv.${current+1} 해금까지 ${CityUnlocks.remainingXp(xp)} XP",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
                    Text("충전 미션을 완료하면 도시 XP가 쌓입니다. 다음 도시의 모습은 해금 후 공개돼요.",color=Subtle)
                    Text("현재 $xp XP / 해금 기준 ${CityLevels.thresholds[current]} XP",fontSize=12.sp,color=GreenDark)
                }
            },
            confirmButton={PrimaryButton(text="성장 계속하기",onClick={nextInfo=false})}
        )
        if(details) AlertDialog(
            onDismissRequest={details=false},
            title={Text("${district.name} · ${shown.landmark}")},
            text={Text("${shown.description}\n\n${district.motif} 테마의 가상 도시입니다. 실제 주변 건물·지형을 재현한 화면은 아닙니다.")},
            confirmButton={TextButton(onClick={details=false},modifier=Modifier.heightIn(min=48.dp)){Text("확인")}}
        )
    }
}

@Composable private fun CityGrowthSummary(xp: Int,current: Int,onNext:()->Unit) {
    BentoCard(modifier=Modifier.fillMaxWidth(),tone=BentoTone.SUBTLE,index=1) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked=maxWidth<420.dp
            val summary:@Composable ()->Unit = {
                Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text("현재 도시 Lv.$current",fontSize=12.sp,color=Subtle)
                    AnimatedNumber(
                        value=xp,suffix=" XP",color=Ink,
                        fontSize=if(xp.toString().length>6)24.sp else if(stacked)32.sp else 36.sp
                    )
                }
            }
            val action:@Composable (Modifier)->Unit = { modifier ->
                PrimaryButton(
                    text=if(current==12)"최고 단계 달성" else "다음 단계 확인하기",
                    onClick=onNext,enabled=current<12,
                    icon=if(current==12)Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                    modifier=modifier.heightIn(min=48.dp).testTag("cityNextCheck")
                )
            }
            if(stacked) {
                Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    summary()
                    BentoProgress(CityLevels.progress(xp),Modifier.fillMaxWidth())
                    Text(
                        if(current==12)"모든 도시를 해금했어요" else "다음 도시까지 ${CityUnlocks.remainingXp(xp)} XP",
                        color=Subtle,fontSize=12.sp
                    )
                    action(Modifier.fillMaxWidth())
                }
            } else {
                Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.weight(1f)){summary()}
                        action(Modifier)
                    }
                    BentoProgress(CityLevels.progress(xp),Modifier.fillMaxWidth())
                    Text(
                        if(current==12)"모든 도시를 해금했어요" else "다음 도시까지 ${CityUnlocks.remainingXp(xp)} XP",
                        color=Subtle,fontSize=12.sp
                    )
                }
            }
        }
    }
}
