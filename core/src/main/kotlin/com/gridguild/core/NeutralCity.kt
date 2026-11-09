package com.gridguild.core

/** Curated destination panoramas, not a live location or a navigation camera. */
enum class NeutralCity(val destinationId: String,val title: String,val landmark: String) {
    GWANGALLI("gwangalli","광안리","광안대교"),
    CENTUM("centum","센텀시티","영화의전당");

    val destination get()=DemoDestinations.places.first{it.id==destinationId}
    companion object {
        fun at(lat: Double?,lon: Double?): NeutralCity? {
            if(!ChargingRecommendations.validCoordinates(lat,lon))return null
            return entries.map{it to ChargingRecommendations.distanceKm(lat!!,lon!!,it.destination.lat,it.destination.lon)}
                .filter{it.second<=1.5}.minByOrNull{it.second}?.first
        }
        // N is a visual TEST flag only. It must never authorize actual neutral-gear game use.
        fun canEnter(mode: AppMode,safety: SafetyState,platformAllowed: Boolean,now: Long)=
            mode==AppMode.MOCK && safety.source==DataSource.MOCK && platformAllowed && safety.allowsGame(now)
    }
}
