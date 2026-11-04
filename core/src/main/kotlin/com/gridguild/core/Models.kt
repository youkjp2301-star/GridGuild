package com.gridguild.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DataSource { MOCK, PLEOS, UNAVAILABLE }
enum class AppMode { MOCK, PLEOS }
enum class ChargeSignal { ON, OFF, UNKNOWN }

data class VehicleState(
    val soc: Float? = null,
    val chargeSwitch: ChargeSignal = ChargeSignal.UNKNOWN,
    val source: DataSource = DataSource.UNAVAILABLE,
    val sampledAt: Long = 0,
    val detail: String = "차량 데이터 대기 중",
) {
    fun usable(now: Long) = source != DataSource.UNAVAILABLE && soc?.let { it.isFinite() && it in 0f..100f } == true &&
        chargeSwitch != ChargeSignal.UNKNOWN && now - sampledAt in 0..10_000
}

data class SafetyState(
    val parked: Boolean = false,
    val uxAllowed: Boolean = false,
    val sampledAt: Long = 0,
    val source: DataSource = DataSource.UNAVAILABLE,
    val detail: String = "주차·플랫폼 제한 확인 중",
) {
    fun allowsGame(now: Long) = parked && uxAllowed && source != DataSource.UNAVAILABLE && now - sampledAt in 0..10_000
}

data class District(val id: String, val name: String, val motif: String)
object Busan {
    val districts = listOf(
        District("jung", "중구", "항구와 오래된 골목"), District("seo", "서구", "송도 해안"),
        District("dong", "동구", "부산항과 산복도로"), District("yeongdo", "영도구", "대교와 바다"),
        District("busanjin", "부산진구", "도심의 빛"), District("dongnae", "동래구", "동래읍성"),
        District("nam", "남구", "항만과 캠퍼스"), District("buk", "북구", "낙동강과 숲"),
        District("haeundae", "해운대구", "바다와 스카이라인"), District("saha", "사하구", "을숙도와 갈대"),
        District("geumjeong", "금정구", "산성과 산"), District("gangseo", "강서구", "낙동강 삼각주"),
        District("yeonje", "연제구", "시민의 도시"), District("suyeong", "수영구", "광안대교"),
        District("sasang", "사상구", "강변과 산업의 도시"), District("gijang", "기장군", "해안과 녹색 마을"),
    )
    fun find(id: String?) = districts.firstOrNull { it.id == id }
}

data class Station(
    val id: String, val name: String, val districtId: String,
    val lat: Double, val lon: Double, val xp: Int, val hourStart: Int, val hourEnd: Int,
    val green: Boolean = true,
) {
    val windowLabel get() = "%02d:00–%02d:00".format(hourStart, hourEnd)
    fun inWindow(now: Long): Boolean {
        val hour = Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul")).hour
        return if (hourStart < hourEnd) hour in hourStart until hourEnd else hour >= hourStart || hour < hourEnd
    }
}
object SampleData {
    val stations = listOf(
        Station("sasang-river", "삼락 강변 충전소", "sasang", 35.1694, 128.9734, 120, 0, 24),
        Station("centum", "센텀 녹색 충전소", "haeundae", 35.1690, 129.1360, 100, 22, 7),
        Station("gwangalli", "광안 해변 충전소", "suyeong", 35.1530, 129.1180, 120, 0, 24),
    )
    // Fictional fixtures, never presented as real charging availability or real grid telemetry.
    fun recommend(lat: Double?, lon: Double?, soc: Float?): List<Station> =
        if (soc != null && soc >= 90f) emptyList() else stations.sortedBy { s ->
            if (lat == null || lon == null) stations.indexOf(s).toDouble()
            else (s.lat - lat) * (s.lat - lat) + (s.lon - lon) * (s.lon - lon)
        }
}

data class Mission(
    val id: String,
    val stationId: String,
    val districtId: String,
    val reward: Int,
    val baselineSoc: Float,
    val source: DataSource,
    val startedAt: Long,
    val chargingObserved: Boolean = false,
    val progress: Float = 0f,
    val requiredGain: Float = 2f,
)
data class PointEvent(
    val id: String, val missionId: String, val districtId: String,
    val xp: Int, val at: Long, val kind: String = "MISSION",
    val demo: Boolean = true,
)
data class GameState(
    val districtId: String? = null,
    val mode: AppMode = AppMode.MOCK,
    val mission: Mission? = null,
    val history: List<PointEvent> = emptyList(),
) {
    val xp get() = history.sumOf { it.xp }
    fun cityXp(id: String?) = history.filter { it.districtId == id }.sumOf { it.xp }
    fun seasonXp(now: Long, district: String? = null) = history.filter {
        season(it.at) == season(now) && (district == null || it.districtId == district)
    }.sumOf { it.xp }
    companion object {
        fun season(at: Long): String = DateTimeFormatter.ofPattern("yyyy-MM")
            .format(Instant.ofEpochMilli(at).atZone(ZoneId.of("Asia/Seoul")))
        fun day(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString()
    }
}

object CityLevels {
    val thresholds = listOf(0, 100, 250, 450, 700, 1_000, 1_400, 1_900, 2_500, 3_200, 4_000, 5_000)
    val names = listOf("중세 마을", "성벽 도시", "르네상스 도시", "산업혁명 도시", "근대 도시", "현대 도시", "첨단 산업도시", "스마트시티", "친환경 도시", "초연결 도시", "하이테크 미래도시", "초진보 미래도시")
    fun level(xp: Int) = thresholds.indexOfLast { xp >= it }.coerceAtLeast(0) + 1
    fun progress(xp: Int): Float {
        val i = level(xp) - 1
        if (i == 11) return 1f
        return (xp - thresholds[i]).toFloat() / (thresholds[i+1] - thresholds[i])
    }
}
