package com.gridguild.core

import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor

enum class LeisureCategory(val label: String) {
    CAFE("카페"), WALK_PARK("산책·공원"), RESTAURANT("음식점"), POPUP_EVENT("팝업·행사")
}

/** POIs are independent of the Pleos vehicle source; no external place API is connected yet. */
enum class LeisureDataSource { MOCK, EXTERNAL, UNKNOWN }

/** Display-only extension metadata. It is not an issued coupon, spending balance or XP award. */
data class LeisureBenefit(val title: String,val description: String,val partnerReference: String?=null)

data class LeisurePlace(
    val id: String,val stationId: String,val name: String,val category: LeisureCategory,
    val lat: Double,val lon: Double,val activityMinutes: Int,val description: String,
    val isAdvertisement: Boolean=false,val benefit: LeisureBenefit?=null,
    val source: LeisureDataSource=LeisureDataSource.MOCK,val updatedAt: Long?=null,
    val providerReference: String?=null,
    val activityTitle: String=category.label,
)

/** A future API adapter maps its response into this contract without changing recommendation rules. */
data class LeisurePlaceSnapshot(
    val stationId: String,val places: List<LeisurePlace>,val source: LeisureDataSource,
    val observedAt: Long?=null,val attribution: String?=null,
)
interface LeisurePlaceRepository {
    suspend fun nearby(station: Station,radiusMeters: Double=ChargingLeisure.radiusMeters): LeisurePlaceSnapshot
}

class MockLeisurePlaceRepository : LeisurePlaceRepository {
    override suspend fun nearby(station: Station,radiusMeters: Double): LeisurePlaceSnapshot {
        val radius=radiusMeters.takeIf{it.isFinite() && it>=0}?.coerceAtMost(ChargingLeisure.radiusMeters)
        val places=if(radius==null)emptyList() else MockChargingLeisurePlaces.forStation(station).filter {
            ChargingRecommendations.validCoordinates(station.lat,station.lon) &&
                ChargingRecommendations.distanceKm(station.lat,station.lon,it.lat,it.lon)*1000<=radius+0.000001
        }
        return LeisurePlaceSnapshot(station.id,places,LeisureDataSource.MOCK,
            attribution="GridGuild 가상 장소 예시 · 실제 운영 정보 미연동")
    }
}

data class ChargingTimeInput(
    val currentSoc: Double?,val targetSoc: Double?,val capacityKwh: Double?,val effectivePowerKw: Double?,
    val source: DataSource,val sampledAt: Long,
    val capacitySource: DataSource=source,val powerSource: DataSource=source,
    val capacitySampledAt: Long=sampledAt,val powerSampledAt: Long=sampledAt,
    // Explicit static calculation inputs, never a synthetic fresh telemetry observation.
    val mockStaticAssumptions: Boolean=false,
)

data class ChargingTimeEstimate(
    val minutes: Int,val preciseMinutes: Double,val energyKwh: Double,
    val source: DataSource,val sampledAt: Long,val usesDemoAssumptions: Boolean,
)

object ChargingTimeEstimator {
    const val maximumAgeMillis=10_000L

    /** Energy-to-target / average effective power, not a charger-rating or completion guarantee. */
    fun estimate(input: ChargingTimeInput,now: Long): ChargingTimeEstimate? {
        fun fresh(at: Long)=at>0 && now>=at && now-at<=maximumAgeMillis
        val soc=input.currentSoc?.takeIf{it.isFinite() && it in 0.0..100.0} ?: return null
        val target=input.targetSoc?.takeIf{it.isFinite() && it in 0.0..100.0} ?: return null
        val capacity=input.capacityKwh?.takeIf{it.isFinite() && it>0} ?: return null
        val power=input.effectivePowerKw?.takeIf{it.isFinite() && it>0} ?: return null
        if(soc>=target || input.source==DataSource.UNAVAILABLE || !fresh(input.sampledAt))return null
        if(input.capacitySource!=input.source || input.powerSource!=input.source)return null
        if(input.mockStaticAssumptions) {
            if(input.source!=DataSource.MOCK)return null
        } else if(!fresh(input.capacitySampledAt) || !fresh(input.powerSampledAt))return null
        val energy=(target-soc)/100.0*capacity
        val precise=energy/power*60.0
        if(!energy.isFinite() || energy<=0 || !precise.isFinite() || precise<=0 || precise>Int.MAX_VALUE.toDouble())return null
        val rounded=ceil(precise-1e-9).toInt().coerceAtLeast(1)
        return ChargingTimeEstimate(rounded,precise,energy,input.source,input.sampledAt,input.mockStaticAssumptions)
    }
}

/** ACTIVITY_FITS is an activity-duration reference; the walking route remains unverified. */
enum class LeisureTimeFit { ACTIVITY_FITS, TOO_LONG, UNKNOWN }
data class LeisureRecommendation(
    val place: LeisurePlace,val straightLineMeters: Double,val timeFit: LeisureTimeFit,
    val activityBudgetMinutes: Int?,val returnReserveMinutes: Int,
    // Never infer a walking route/time from the straight-line distance in this MVP.
    val walkingMinutes: Int?=null,
)

object ChargingLeisure {
    const val radiusMeters=300.0
    const val returnReserveMinutes=5
    private const val distanceToleranceMeters=0.000001

    fun recommend(
        station: Station,places: List<LeisurePlace> = MockChargingLeisurePlaces.forStation(station),
        category: LeisureCategory?=null,estimate: ChargingTimeEstimate?=null,
    ): List<LeisureRecommendation> {
        if(!ChargingRecommendations.validCoordinates(station.lat,station.lon))return emptyList()
        val budget=estimate?.takeIf {
            it.source!=DataSource.UNAVAILABLE && it.preciseMinutes.isFinite() && it.preciseMinutes>0 &&
                it.preciseMinutes<=Int.MAX_VALUE.toDouble()
        }?.let{(floor(it.preciseMinutes+1e-9).toInt()-returnReserveMinutes).coerceAtLeast(0)}
        return places.asSequence().filter {
            it.stationId==station.id && it.id.isNotBlank() && it.name.isNotBlank() && it.description.isNotBlank() &&
                it.activityMinutes>0 && (category==null || it.category==category) &&
                ChargingRecommendations.validCoordinates(it.lat,it.lon)
        }.distinctBy{it.id}.map { place ->
            val distance=ChargingRecommendations.distanceKm(station.lat,station.lon,place.lat,place.lon)*1000.0
            val fit=when {
                budget==null -> LeisureTimeFit.UNKNOWN
                place.activityMinutes<=budget -> LeisureTimeFit.ACTIVITY_FITS
                else -> LeisureTimeFit.TOO_LONG
            }
            LeisureRecommendation(place,distance,fit,budget,returnReserveMinutes)
        }.filter{it.straightLineMeters<=radiusMeters+distanceToleranceMeters}
            .sortedWith(compareBy<LeisureRecommendation>{when(it.timeFit) {
                LeisureTimeFit.ACTIVITY_FITS->0;LeisureTimeFit.UNKNOWN->1;LeisureTimeFit.TOO_LONG->2
            }}.thenBy{it.straightLineMeters}.thenBy{it.place.id})
            .toList()
    }
}

/** Five fictional places per fixed sample station. No real business, opening-hours or route claims. */
object MockChargingLeisurePlaces {
    private const val earthRadiusMeters=6_371_000.0
    private val fixtures by lazy { SampleData.stations.associate { station ->
        val label=station.name.removeSuffix(" 충전소")
        fun place(suffix: String,name: String,category: LeisureCategory,east: Double,north: Double,
            minutes: Int,description: String,advertisement: Boolean=false,benefit: LeisureBenefit?=null): LeisurePlace {
            val lat=station.lat+Math.toDegrees(north/earthRadiusMeters)
            val lon=station.lon+Math.toDegrees(east/(earthRadiusMeters*cos(Math.toRadians(station.lat))))
            return LeisurePlace("${station.id}-leisure-$suffix",station.id,"가상 $label $name",category,
                lat,lon,minutes,description,advertisement,benefit,LeisureDataSource.MOCK,
                activityTitle=when(suffix){"cafe"->"커피 한 잔";"cafe-ad"->"잠깐 쉬어가기";"park"->"가볍게 산책";
                    "food"->"간단한 식사";else->"전시 둘러보기"})
        }
        station.id to listOf(
            place("cafe","커피방",LeisureCategory.CAFE,85.0,0.0,12,"잠시 앉아 쉬는 활동을 가정한 가상 카페입니다."),
            place("cafe-ad","라운지",LeisureCategory.CAFE,190.0,0.0,12,"제휴 노출을 가정한 가상 라운지입니다.",true,
                LeisureBenefit("제휴 혜택 예시","향후 할인·제휴 연계를 위한 정보 예시이며 지금 발급·지급·사용할 수 없습니다.","demo-${station.id}")),
            place("park","작은 공원",LeisureCategory.WALK_PARK,-60.0,120.0,10,"짧은 산책 활동을 가정한 가상 녹지입니다."),
            place("food","한 끼 식당",LeisureCategory.RESTAURANT,55.0,-130.0,25,"간단한 식사를 가정한 가상 음식점입니다."),
            place("popup","문화 팝업",LeisureCategory.POPUP_EVENT,90.0,170.0,15,"짧게 둘러보는 전시 활동을 가정한 가상 행사입니다."),
        )
    } }
    fun forStation(station: Station): List<LeisurePlace> = fixtures[station.id].orEmpty()
}
