@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text("MY CITY COLLECTION",color=Green,fontSize=11.sp,letterSpacing=1.6.sp,fontWeight=FontWeight.Bold)
                Text("${district.name}의 도시",style=MaterialTheme.typography.headlineMedium)
                Text("충전의 성과가 쌓여, 새로운 풍경이 열립니다.",color=Subtle,fontSize=14.sp)
            }
            Surface(color=Color.White,shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,Line)) {
                Column(Modifier.padding(horizontal=18.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("$current / 12",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
                    Text("도시 해금",fontSize=12.sp,color=Subtle)
                }
            }
        }
        Surface(color=Color.White,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,Line),shadowElevation=1.dp) {
            Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Surface(color=if(previous)Pale else SoftGreen,shape=RoundedCornerShape(8.dp)) {
                        Text(if(previous)"해금 기록 · Lv.${shown.level}" else "지금의 도시 · Lv.$current",Modifier.padding(horizontal=12.dp,vertical=8.dp),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=if(previous)Blue else Green)
                    }
                    if(previous) TextButton(onClick={selection=current},modifier=Modifier.testTag("cityCurrent")) { Icon(Icons.AutoMirrored.Outlined.Undo,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("내 도시로 돌아가기") }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lv.${shown.level} · ${shown.name}",style=MaterialTheme.typography.titleLarge,modifier=Modifier.testTag("cityStageLabel"))
                        Text(shown.landmark,color=Subtle,fontSize=14.sp)
                    }
                    IconButton(onClick={details=true}) { Icon(Icons.Outlined.Info,"도시 정보",tint=Subtle) }
                }
                Crossfade(targetState=shown.level,animationSpec=tween(400),label="unlockedCity") { displayed ->
                    CityCanvas(displayed,district.id,current,Modifier.fillMaxWidth().aspectRatio(2f).testTag("cityScene"),zoom)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(shown.description,Modifier.weight(1f),color=Subtle,fontSize=14.sp)
                    IconButton(onClick={zoom=(zoom-.15f).coerceAtLeast(.7f)}){Icon(Icons.Outlined.Remove,"축소",tint=Blue)}
                    IconButton(onClick={zoom=(zoom+.15f).coerceAtMost(1.5f)}){Icon(Icons.Outlined.Add,"확대",tint=Blue)}
                }
                HorizontalDivider(color=Line)
                CityGrowthSummary(xp,current){nextInfo=true}
                LinearProgressIndicator(progress={CityLevels.progress(xp)},modifier=Modifier.fillMaxWidth().height(6.dp),color=Gold,trackColor=Color(0xFFF0F2F2))
            }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("도시 컬렉션",style=MaterialTheme.typography.titleMedium)
                Text("해금한 도시는 다시 방문할 수 있어요.",fontSize=13.sp,color=Subtle)
            }
            Text("${current}개 해금",fontSize=12.sp,color=Green,fontWeight=FontWeight.SemiBold)
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=4.dp),modifier=Modifier.fillMaxWidth().testTag("cityCollection")) {
            items((1..12).toList(),key={it}) { cityLevel ->
                val unlocked=CityUnlocks.canView(cityLevel,current)
                Surface(onClick={if(unlocked)selection=cityLevel},enabled=unlocked,
                    modifier=Modifier.width(180.dp).testTag("city-stage-$cityLevel").semantics {
                        contentDescription=if(unlocked)"해금된 도시 Lv.$cityLevel" else "잠긴 도시 Lv.$cityLevel"
                    },
                    color=Color.White,shape=RoundedCornerShape(18.dp),border=BorderStroke(if(shown.level==cityLevel)2.dp else 1.dp,if(shown.level==cityLevel)Green else Line)) {
                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        if(unlocked) CityCanvas(cityLevel,district.id,current,Modifier.fillMaxWidth().height(87.dp))
                        else LockedCityArtwork(Modifier.fillMaxWidth().height(87.dp),compact=true)
                        Row(Modifier.padding(horizontal=5.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("Lv.$cityLevel",fontSize=13.sp,fontWeight=FontWeight.Bold,color=if(unlocked)Ink else Subtle)
                            Spacer(Modifier.weight(1f))
                            if(unlocked)Icon(Icons.Outlined.CheckCircle,null,Modifier.size(15.dp),tint=Green)
                            else Icon(Icons.Outlined.Lock,null,Modifier.size(14.dp),tint=Subtle)
                        }
                        Text(if(unlocked)CityLevels.names[cityLevel-1] else "아직 열리지 않은 도시",Modifier.padding(start=5.dp,end=5.dp,bottom=6.dp),fontSize=12.sp,color=Subtle)
                    }
                }
            }
        }
        if(nextInfo && current<12) AlertDialog(onDismissRequest={nextInfo=false},
            title={Text("다음 도시를 향해",style=MaterialTheme.typography.titleLarge)},
            text={Column(verticalArrangement=Arrangement.spacedBy(16.dp),modifier=Modifier.testTag("cityLockedDialog")) {
                LockedCityArtwork(Modifier.fillMaxWidth().aspectRatio(2f).testTag("nextLockedImage"))
                Text("Lv.${current+1} 해금까지 ${CityUnlocks.remainingXp(xp)} XP",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text("충전 미션을 완료하면 도시 XP가 쌓입니다. 다음 도시의 모습은 해금 후 공개돼요.",color=Subtle)
                Text("현재 $xp XP / 해금 기준 ${CityLevels.thresholds[current]} XP",fontSize=13.sp,color=Green)
            }},
            confirmButton={Button(onClick={nextInfo=false},shape=RoundedCornerShape(14.dp)){Text("성장 계속하기")}})
        if(details) AlertDialog(onDismissRequest={details=false},title={Text("${district.name} · ${shown.landmark}")},
            text={Text("${shown.description}\n\n${district.motif} 테마의 가상 도시입니다. 실제 주변 건물·지형을 재현한 화면은 아닙니다.")},
            confirmButton={TextButton(onClick={details=false}){Text("확인")}})
    }
}

@Composable private fun CityGrowthSummary(xp: Int,current: Int,onNext:()->Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked=maxWidth<420.dp
        val summary:@Composable ()->Unit = {
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("현재 도시 Lv.$current",fontSize=12.sp,color=Subtle)
                Text("$xp XP",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text(if(current==12)"모든 도시를 해금했어요" else "다음 도시까지 ${CityUnlocks.remainingXp(xp)} XP",color=Subtle,fontSize=13.sp)
            }
        }
        val action:@Composable (Modifier)->Unit = { modifier ->
            Button(onClick=onNext,enabled=current<12,shape=RoundedCornerShape(14.dp),modifier=modifier.heightIn(min=52.dp).testTag("cityNextCheck")) {
                Icon(if(current==12)Icons.Outlined.CheckCircle else Icons.Outlined.Lock,null,Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp));Text(if(current==12)"최고 단계 달성" else "다음 단계 확인하기")
            }
        }
        if(stacked)Column(verticalArrangement=Arrangement.spacedBy(14.dp)) { summary();action(Modifier.fillMaxWidth()) }
        else Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)){summary()};action(Modifier)
        }
    }
}
