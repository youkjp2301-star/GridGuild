package com.gridguild.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gridguild.core.*
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.gridStore by preferencesDataStore("gridguild_v1")
class GameStore(context: Context) {
    private val store = context.applicationContext.gridStore
    private val key = stringPreferencesKey("state")
    val state = store.data.map { GameStateCodec.decode(it[key]) }
    suspend fun update(transform: (GameState) -> GameState) {
        store.edit { preferences -> preferences[key] = GameStateCodec.encode(transform(GameStateCodec.decode(preferences[key]))) }
    }
}

internal object GameStateCodec {
    fun encode(s: GameState): String = JSONObject().apply {
        put("schema",2); put("district",s.districtId); put("mode",s.mode.name)
        s.mission?.let { m -> put("mission",JSONObject().apply {
            put("id",m.id);put("station",m.stationId);put("district",m.districtId);put("reward",m.reward)
            put("baseline",m.baselineSoc);put("source",m.source.name);put("started",m.startedAt)
            put("observed",m.chargingObserved);put("progress",m.progress)
        }) }
        put("history",JSONArray().apply { s.history.forEach { e -> put(JSONObject().apply {
            put("id",e.id);put("mission",e.missionId);put("district",e.districtId)
            put("xp",e.xp);put("at",e.at);put("kind",e.kind);put("demo",e.demo)
            put("personalCredit",e.personalCredit);put("cityCredit",e.cityCredit);put("countsForMission",e.countsForMission)
        }) } })
    }.toString()
    fun decode(raw: String?): GameState {
        if (raw == null) return GameState()
        // Invalid storage is surfaced as an error, never silently discarded.
        val j = JSONObject(raw)
        val schema=j.getInt("schema")
        require(schema in 1..2) { "지원하지 않는 저장 버전" }
        val m=j.optJSONObject("mission")?.let {
            Mission(it.getString("id"),it.getString("station"),it.getString("district"),it.getInt("reward"),
                it.getDouble("baseline").toFloat(),DataSource.valueOf(it.getString("source")),it.getLong("started"),
                it.getBoolean("observed"),it.getDouble("progress").toFloat())
        }
        val events=j.getJSONArray("history")
        return GameState(j.optString("district").takeIf { Busan.find(it)!=null },AppMode.valueOf(j.getString("mode")),m,
            (0 until events.length()).map { i -> events.getJSONObject(i).let {
                PointEvent(it.getString("id"),it.getString("mission"),it.getString("district"),it.getInt("xp"),it.getLong("at"),it.getString("kind"),it.getBoolean("demo"),
                    personalCredit=if(schema==1) true else it.getBoolean("personalCredit"),
                    cityCredit=if(schema==1) true else it.getBoolean("cityCredit"),
                    countsForMission=if(schema==1) true else it.getBoolean("countsForMission"))
            } })
    }
}
