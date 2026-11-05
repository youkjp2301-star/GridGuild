package com.gridguild.core

import kotlin.math.*
import kotlin.random.Random

/** Fixed illustrative test destinations, never live POIs from a map provider. */
data class DemoDestination(val id: String,val name: String,val lat: Double,val lon: Double,val hint: String)
object DemoDestinations {
    val places=listOf(
        DemoDestination("samnak","삼락생태공원",35.1694,128.9780,"서부 · 삼락 충전소 2곳"),
        DemoDestination("centum","센텀시티",35.1690,129.1300,"동부 · 센텀·광안 후보"),
        DemoDestination("gwangalli","광안리 해변",35.1530,129.1180,"해변 · 가까운 순서 변화"),
        DemoDestination("gijang","기장 해안",35.2440,129.2220,"주변 샘플 충전소 없는 예시"),
    )
    fun randomOther(lat: Double?,lon: Double?,random: Random=Random.Default)=places.filter {
        !ChargingRecommendations.validCoordinates(lat,lon) || ChargingRecommendations.distanceKm(lat!!,lon!!,it.lat,it.lon)>.05
    }.random(random)
}

data class NearbyStation(val station: Station,val distanceKm: Double)
enum class RecommendationStatus { READY, NO_DESTINATION, HIGH_SOC, NO_NEARBY }
data class RecommendationResult(val status: RecommendationStatus,val nearby: List<NearbyStation>)

object ChargingRecommendations {
    const val radiusKm=5.0
    fun validCoordinates(lat: Double?,lon: Double?)=lat!=null && lon!=null && lat.isFinite() && lon.isFinite() &&
        lat in -90.0..90.0 && lon in -180.0..180.0 && (lat!=0.0 || lon!=0.0)
    fun distanceKm(lat1: Double,lon1: Double,lat2: Double,lon2: Double): Double {
        val latitude=Math.toRadians(lat2-lat1);val longitude=Math.toRadians(lon2-lon1)
        val a=sin(latitude/2).pow(2)+cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(longitude/2).pow(2)
        return 6371.0*2*asin(sqrt(a.coerceIn(0.0,1.0)))
    }
    fun around(lat: Double?,lon: Double?,soc: Float?,stations: List<Station> = SampleData.stations): RecommendationResult {
        if(!validCoordinates(lat,lon))return RecommendationResult(RecommendationStatus.NO_DESTINATION,emptyList())
        if(soc!=null && soc>=90f)return RecommendationResult(RecommendationStatus.HIGH_SOC,emptyList())
        val nearby=stations.map { NearbyStation(it,distanceKm(lat!!,lon!!,it.lat,it.lon)) }
            .filter { it.distanceKm<=radiusKm }.sortedWith(compareBy<NearbyStation>{it.distanceKm}.thenBy{it.station.id})
        return RecommendationResult(if(nearby.isEmpty())RecommendationStatus.NO_NEARBY else RecommendationStatus.READY,nearby)
    }
}
