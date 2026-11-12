package com.gridguild.core

/** A navigation coordinate, independent of charging-station and mission identity. */
data class NavigationTarget(val name: String,val lat: Double,val lon: Double) {
    val isValid get()=name.isNotBlank() && ChargingRecommendations.validCoordinates(lat,lon)
    companion object {
        fun fromLeisure(station: Station,place: LeisurePlace): NavigationTarget? {
            if(place.source!=LeisureDataSource.MOCK || place.stationId!=station.id || place.id.isBlank() ||
                !ChargingRecommendations.validCoordinates(station.lat,station.lon))return null
            val target=NavigationTarget(place.name.trim(),place.lat,place.lon)
            if(!target.isValid || ChargingRecommendations.distanceKm(station.lat,station.lon,target.lat,target.lon)*1000>
                ChargingLeisure.radiusMeters+0.000001)return null
            return target
        }
    }
}
