package com.gridguild.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gridguild.GridViewModel
import com.gridguild.core.*
import com.gridguild.data.NavigationState

@Composable internal fun HomeDashboard(g: GameState,reading: VehicleReading,n: NavigationState,now: Long,
    model: GridViewModel,onMission:()->Unit,onRoute:()->Unit,onCity:()->Unit,onRanking:()->Unit) {
    val district=Busan.find(g.districtId) ?: return
    val cityXp=g.cityXp(district.id)
    val level=CityLevels.level(cityXp)
    val ranking=Rankings.cities(g,now)
    val rank=ranking.indexOfFirst{it.mine}+1
    val trusted=reading.fresh && reading.vehicle.source==if(g.mode==AppMode.MOCK)DataSource.MOCK else DataSource.PLEOS
    val availability by model.chargerAvailability.collectAsStateWithLifecycle()
    val connector by model.demoConnector.collectAsStateWithLifecycle()
    val recommendations=ChargingRecommendations.around(n.lat,n.lon,reading.vehicle.soc.takeIf{trusted},availability=availability,
        observedAt=now,compatible=if(g.mode==AppMode.MOCK)setOf(connector) else null)
    val recommended=recommendations.nearby.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("homeDashboard"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionHeader("좋은 충전으로 시작하는 오늘",district.name+" · GRIDGUILD")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val dashboardWide=maxWidth>=720.dp
            val battery:@Composable (Modifier)->Unit={modifier->BentoCard(modifier,index=0) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("EV 배터리",color=Subtle,fontSize=13.sp)
                        Text(g.homeCharge.targetSoc?.let{"목표 $it%"} ?: "목표 미설정",fontWeight=FontWeight.SemiBold,fontSize=19.sp,
                            modifier=Modifier.testTag("dashboardTarget"))
                        Text(if(trusted)"충전 스위치 ${reading.vehicle.chargeSwitch}" else "충전 상태 확인 중",fontSize=12.sp,
                            color=GreenDark,modifier=Modifier.testTag("batteryStatus"))
                    }
                    SocGauge(reading.vehicle.soc,trusted,g.homeCharge.targetSoc,size=if(dashboardWide)116.dp else 96.dp)
                }
            }}
            val mission:@Composable (Modifier)->Unit={modifier->BentoCard(modifier,BentoTone.DARK,index=1) {
                val active=g.mission
                val completed=g.completionHistory.lastOrNull{GameState.day(it.at)==GameState.day(now)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("오늘의 충전 미션",color=OnDarkMuted,fontSize=13.sp)
                    Icon(if(active!=null)Icons.Outlined.Bolt else if(completed!=null)Icons.Outlined.CheckCircle else Icons.Outlined.EvStation,
                        null,tint=Green,modifier=Modifier.size(21.dp))
                }
                Text(when{active!=null->"충전이 도시를 키우고 있어요";completed!=null->"오늘도 도시가 자랐어요";else->"충전하고 도시를 키워요"},
                    color=Color.White,fontSize=22.sp,fontWeight=FontWeight.SemiBold)
                if(active!=null) {
                    Text("${if(active.kind==MissionKind.HOME)"우리 집" else "이동 중"} 충전 · 완료 시 +${active.reward} XP",fontSize=13.sp,color=OnDarkMuted)
                    BentoProgress(active.progress)
                } else Text(if(completed!=null)"개인 +${if(completed.personalCredit)completed.xp else 0} · 도시 +${if(completed.cityCredit)completed.xp else 0} XP 반영"
                    else "16–17시 / 22–23시 · +${HomeChargeEngine.reward} XP",color=Green,fontSize=15.sp,fontWeight=FontWeight.Medium)
                PrimaryButton(if(active!=null)"진행 중인 미션 보기" else "충전 미션 참여하기",onMission,Modifier.fillMaxWidth().testTag("dashboardMission"),icon=Icons.AutoMirrored.Outlined.ArrowForward)
                if(active==null && completed==null)Text("모의 추천 시간",fontSize=12.sp,color=OnDarkMuted)
            }}
            if(maxWidth>=720.dp)Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Top) {
                battery(Modifier.weight(.9f));mission(Modifier.weight(1.4f))
            } else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){battery(Modifier.fillMaxWidth());mission(Modifier.fillMaxWidth())}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            StatCard("내 누적 XP",g.xp,Modifier.weight(1f),suffix=" XP",hint="이번 시즌 ${g.seasonXp(now)} XP",index=2)
            StatCard("도시 레벨",level,Modifier.weight(1f),suffix=" / 12",hint=district.name,tone=BentoTone.GREEN,index=3,onClick=onCity)
        }
        BentoCard(Modifier.fillMaxWidth().testTag("dashboardCity"),index=3) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("내 도시",fontSize=13.sp,color=Subtle)
                    Text("${district.name}의 다음 풍경",fontSize=21.sp,fontWeight=FontWeight.SemiBold)
                }
                IconButton(onCity,Modifier.size(48.dp)){Icon(Icons.AutoMirrored.Outlined.ArrowForward,"내 도시 보기",tint=Ink)}
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                CityCanvas(level,district.id,level,Modifier.fillMaxWidth().aspectRatio(if(maxWidth<600.dp)1.65f else 2.5f).testTag("homeCityHero"))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("지역 성장 · $cityXp XP",fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                Text("Lv.$level",fontSize=14.sp,color=GreenDark)
            }
            BentoProgress(CityLevels.progress(cityXp))
            Text(if(level==12)"최고 레벨 달성" else "다음 레벨까지 ${CityLevels.thresholds[level]-cityXp} XP",color=Subtle,fontSize=12.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val station:@Composable (Modifier)->Unit={modifier->BentoCard(modifier,index=4,onClick=onRoute) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("목적지 주변 충전소",color=Subtle,fontSize=13.sp)
                    StatusBadge("샘플",Subtle)
                }
                Text(if(n.hasDestination)destinationName(n.destination) else "목적지를 선택하세요",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                Text(recommended?.station?.name ?: when(recommendations.status){RecommendationStatus.HIGH_SOC->"배터리가 충분해요";RecommendationStatus.NO_NEARBY->"주변 후보 없음";else->"목적지 설정 후 확인"},
                    fontSize=19.sp,fontWeight=FontWeight.SemiBold)
                recommended?.let {
                    StatusBadge(when(it.availability.status){AvailabilityStatus.FREE->"여유";AvailabilityStatus.BUSY->"혼잡";AvailabilityStatus.NONE->"이용 가능 없음";else->"정보 없음"},
                        when(it.availability.status){AvailabilityStatus.FREE->Green;AvailabilityStatus.BUSY->Gold;else->Subtle})
                    Text("목적지 직선 %.1f km · +%d XP".format(java.util.Locale.KOREA,it.distanceKm,it.station.xp),fontSize=13.sp,color=Subtle)
                }
                TextButton(onRoute,Modifier.heightIn(min=48.dp)){Text("추천 충전소 보기");Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(16.dp))}
            }}
            val region:@Composable (Modifier)->Unit={modifier->BentoCard(modifier,BentoTone.SUBTLE,index=5,onClick=onRanking) {
                Text("지역 랭킹 · 샘플",color=Subtle,fontSize=13.sp)
                AnimatedNumber(rank,suffix="위",fontSize=38.sp)
                Text("${ranking.size}개 지역 중 ${district.name}",fontSize=15.sp,fontWeight=FontWeight.Medium)
                Text("충전 성과가 우리 지역의 순위를 바꿔요",fontSize=12.sp,color=Subtle)
                TextButton(onRanking,Modifier.heightIn(min=48.dp)){Text("지역 랭킹 보기");Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,Modifier.size(16.dp))}
            }}
            if(maxWidth>=600.dp)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){station(Modifier.weight(1.4f));region(Modifier.weight(1f))}
            else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){station(Modifier.fillMaxWidth());region(Modifier.fillMaxWidth())}
        }
        Spacer(Modifier.height(8.dp))
    }
}
