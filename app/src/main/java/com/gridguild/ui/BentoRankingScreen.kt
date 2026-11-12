@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gridguild.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gridguild.core.CityLevels
import com.gridguild.core.GameState
import com.gridguild.core.RankingRow
import com.gridguild.core.Rankings

private data class RankObservation(val rank: Int,val previous: Int?=null)

@Composable internal fun BentoRankingScreen(g: GameState,now: Long,initialCities: Boolean=false,requestId: Int=0) {
    var cities by rememberSaveable { mutableStateOf(initialCities) }
    var appliedRequest by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(requestId){if(appliedRequest!=requestId){cities=initialCities;appliedRequest=requestId}}
    var monthly by rememberSaveable { mutableStateOf(true) }
    val season=GameState.season(now)
    val sorted=if(cities)Rankings.cities(g,now,monthly) else Rankings.personal(g,now,monthly)
    val myIndex=sorted.indexOfFirst{it.mine}
    val myRank=if(myIndex>=0)myIndex+1 else 0
    val mine=sorted.getOrNull(myIndex)
    val period=if(monthly)season else "all"

    // Only a committed observation in this exact scope can become the previous rank.
    // Switching filters, source mode, season, or district starts a fresh observation.
    var observed by remember(g.mode,cities,monthly,period,g.districtId) {
        mutableStateOf(RankObservation(myRank))
    }
    val previousRank=if(observed.rank!=myRank) {
        observed.rank.takeIf{it>0 && myRank>0}
    } else observed.previous
    SideEffect {
        if(observed.rank!=myRank) {
            observed=RankObservation(myRank,observed.rank.takeIf{it>0 && myRank>0})
        }
    }

    val topXp=(sorted.maxOfOrNull{it.xp} ?: 1).coerceAtLeast(1)
    LazyColumn(
        verticalArrangement=Arrangement.spacedBy(10.dp),
        contentPadding=PaddingValues(bottom=8.dp),
        modifier=Modifier.fillMaxSize().testTag("rankingList")
    ) {
        item(key="header") {
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                SectionHeader(title="함께 만드는 순위",kicker="GUILD RANKING")
                Text(
                    "${if(monthly)"$season 시즌" else "누적 성과"} · 샘플 랭킹 · 내 성과 반영",
                    color=Subtle,fontSize=12.sp,lineHeight=18.sp
                )
            }
        }
        item(key="filters") {
            FlowRow(
                horizontalArrangement=Arrangement.spacedBy(8.dp),
                verticalArrangement=Arrangement.spacedBy(2.dp)
            ) {
                FilterChip(
                    selected=!cities,onClick={cities=false},
                    label={Text("개인 랭킹")},modifier=Modifier.heightIn(min=48.dp)
                )
                FilterChip(
                    selected=cities,onClick={cities=true},
                    label={Text("지역 랭킹")},modifier=Modifier.heightIn(min=48.dp)
                )
                FilterChip(
                    selected=monthly,onClick={monthly=!monthly},
                    label={Text(if(monthly)"월간 시즌" else "누적 성과")},
                    modifier=Modifier.heightIn(min=48.dp)
                )
            }
        }
        item(key="myRank") {
            MyRankingCard(mine,myRank,previousRank,sorted.getOrNull(myIndex-1))
        }
        item(key="listHeading") {
            Row(
                Modifier.padding(top=8.dp,bottom=3.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if(cities)"부산 16개 지역" else "개인 순위",
                    style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f)
                )
                Text("샘플 기준",color=Subtle,fontSize=11.sp)
            }
        }
        itemsIndexed(sorted,key={_,row->"$cities-${row.name}"}) { index,row ->
            RankingEntry(row,index+1,cities,topXp)
        }
    }
}

@Composable private fun MyRankingCard(mine: RankingRow?,rank: Int,previous: Int?,above: RankingRow?) {
    BentoCard(modifier=Modifier.fillMaxWidth(),tone=BentoTone.DARK,index=0) {
        Row(
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)
        ) {
            Text("내 순위",color=Color.White.copy(alpha=.72f),fontSize=12.sp,modifier=Modifier.weight(1f))
            Text(
                if(previous!=null)"${previous}→${rank}위" else "현재 순위",
                color=Green,fontSize=12.sp,fontWeight=FontWeight.SemiBold
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val rankNumber:@Composable ()->Unit = {
                if(rank>0) RankingNumber(rank,suffix="위",color=Green,fontSize=48.sp)
                else Text("집계 전",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.SemiBold)
            }
            val score:@Composable ()->Unit = {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("랭킹 점수",color=Color.White.copy(alpha=.62f),fontSize=11.sp)
                    RankingNumber(
                        mine?.xp ?: 0,suffix=" XP",color=Color.White,
                        fontSize=if((mine?.xp ?: 0).toString().length>7)24.sp else 28.sp
                    )
                }
            }
            if(maxWidth<360.dp) {
                Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    rankNumber()
                    score()
                }
            } else {
                Row(
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(24.dp)
                ) {
                    Box(Modifier.weight(1f)){rankNumber()}
                    score()
                }
            }
        }
        if(mine!=null && above!=null) {
            val gap=(above.xp.toLong()-mine.xp.toLong()).coerceAtLeast(0L)
            Text("바로 위와 $gap XP 차이",color=Color.White.copy(alpha=.72f),fontSize=12.sp)
        } else if(rank==1) {
            Text("현재 1위예요",color=Color.White.copy(alpha=.72f),fontSize=12.sp)
        }
    }
}

@Composable private fun RankingEntry(row: RankingRow,rank: Int,cities: Boolean,topXp: Int) {
    BentoCard(
        modifier=Modifier.fillMaxWidth(),
        tone=if(row.mine)BentoTone.GREEN else BentoTone.SURFACE,
        index=rank.coerceAtMost(5)
    ) {
        Row(
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "%02d".format(rank),fontSize=22.sp,fontWeight=FontWeight.SemiBold,
                color=if(row.mine)GreenDark else Subtle,modifier=Modifier.widthIn(min=36.dp)
            )
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(
                    row.name+(if(row.mine && cities)" · 내 소속" else ""),
                    fontWeight=FontWeight.SemiBold,fontSize=16.sp,color=Ink
                )
                if(cities) Text(
                    if(row.mine)"Lv.${row.cityLevel ?: "—"} · 누적 도시 ${row.cityXp ?: 0} XP"
                    else "Lv.${row.cityLevel ?: "—"} · 모의 공동 점수",
                    fontSize=11.sp,lineHeight=17.sp,color=Subtle,
                    modifier=Modifier.testTag("ranking-city-level-${row.districtId}")
                )
            }
        }
        RankingNumber(
            row.xp,suffix=" XP",color=if(row.mine)GreenDark else Ink,fontSize=22.sp,
            modifier=Modifier.align(Alignment.End).then(if(cities)Modifier.testTag("ranking-city-score-${row.districtId}") else Modifier)
        )
        if(cities) BentoProgress(row.xp.toFloat()/topXp.toFloat(),Modifier.fillMaxWidth())
    }
}

@Composable private fun RankingNumber(
    value: Int,modifier: Modifier=Modifier,suffix: String="",color: Color=Ink,fontSize: TextUnit=28.sp
) {
    val duration=if(motionEnabled())450 else 0
    AnimatedContent(
        targetState=value,modifier=modifier,
        transitionSpec={
            (fadeIn(animationSpec=tween(duration)) togetherWith fadeOut(animationSpec=tween(duration)))
                .using(SizeTransform(clip=false))
        },
        label="rankingNumber"
    ) { number ->
        Text("$number$suffix",color=color,fontSize=fontSize,fontWeight=FontWeight.SemiBold,letterSpacing=(-.7).sp)
    }
}
