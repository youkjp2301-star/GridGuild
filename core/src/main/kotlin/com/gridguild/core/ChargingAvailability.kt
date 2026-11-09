package com.gridguild.core

enum class ChargerConnector(val label: String) { DC_COMBO("DC콤보 · 급속"), CHADEMO("차데모 · 급속"), AC_TYPE1("AC완속") }
enum class ChargerUseState { AVAILABLE, IN_USE, FAULT, MAINTENANCE, OUT_OF_SERVICE, UNKNOWN }
enum class AvailabilitySource { MOCK, PUBLIC_API }
enum class AvailabilityStatus(val label: String) { FREE("여유"), BUSY("혼잡"), NONE("이용 가능 없음"), UNKNOWN("정보 없음") }
enum class ConnectorCompatibility { COMPATIBLE, INCOMPATIBLE, UNKNOWN }

/** observedAt is the time this status was verified, never just the last state-change time. */
data class ChargerObservation(val id: String,val connector: ChargerConnector?,val state: ChargerUseState,val observedAt: Long?,
    val rawStatus: String?=null,val statusChangedAt: Long?=null)
data class StationAvailability(
    val stationId: String,val chargers: List<ChargerObservation>,val totalChargers: Int?,
    val collectedAt: Long?,val source: AvailabilitySource,
)
data class AvailabilitySummary(
    val status: AvailabilityStatus,val total: Int?,val available: Int,
    val compatibleAvailable: Int?,val complete: Boolean,val updatedAt: Long?,val source: AvailabilitySource?,
) {
    val countLabel get()=when {
        total==null -> "수량 미확인"
        complete -> "${total}기 중 ${available}기 이용 가능"
        available>0 -> "${total}기 중 ${available}기 확인 · 일부 미확인"
        else -> "총 ${total}기"
    }
}

object ChargingAvailability {
    // Product policy for this MVP, not an API/provider freshness guarantee.
    const val maxAgeMs=10*60*1000L
    fun summarize(snapshot: StationAvailability?,now: Long,compatible: Set<ChargerConnector>?=null): AvailabilitySummary {
        if(snapshot==null)return AvailabilitySummary(AvailabilityStatus.UNKNOWN,null,0,null,false,null,null)
        val total=snapshot.totalChargers?.takeIf{it>0}
        val distinct=snapshot.chargers.distinctBy{it.id}
        val valid=total!=null && distinct.size==snapshot.chargers.size && distinct.size<=total && distinct.all{it.id.isNotBlank()}
        val collectionFresh=snapshot.collectedAt?.let{now-it in 0..maxAgeMs}==true
        val fresh=if(valid && collectionFresh)distinct.filter{it.observedAt?.let{time->now-time in 0..maxAgeMs}==true} else emptyList()
        val available=fresh.filter{it.state==ChargerUseState.AVAILABLE}
        val complete=valid && collectionFresh && distinct.size==total && fresh.size==total && fresh.none{it.state==ChargerUseState.UNKNOWN}
        val status=when {
            !complete -> AvailabilityStatus.UNKNOWN
            available.isEmpty() -> AvailabilityStatus.NONE
            available.size*2>=total!! -> AvailabilityStatus.FREE
            else -> AvailabilityStatus.BUSY
        }
        // Oldest observation is the effective freshness time; fetching old observations again
        // must not make a partially stale station look current.
        val times=distinct.mapNotNull{it.observedAt}+listOfNotNull(snapshot.collectedAt)
        return AvailabilitySummary(status,total,available.size,
            compatible?.takeIf{complete && fresh.all{it.connector!=null}}?.let{types->available.count{it.connector in types}},complete,times.minOrNull(),snapshot.source)
    }
    fun compatibility(station: Station,compatible: Set<ChargerConnector>?): ConnectorCompatibility = when {
        compatible==null || station.connectors.isEmpty() -> ConnectorCompatibility.UNKNOWN
        station.connectors.any{it in compatible} -> ConnectorCompatibility.COMPATIBLE
        else -> ConnectorCompatibility.INCOMPATIBLE
    }
}
