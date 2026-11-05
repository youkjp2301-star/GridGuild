package com.gridguild.data

import android.content.Context
import android.content.pm.PackageManager
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.constants.*
import com.gridguild.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class PleosNavigationRepository(private val context: Context, private val scope: CoroutineScope) : NavigationRepository {
    override val state = MutableStateFlow(NavigationState())
    private val helper = NaviHelper(context)
    private var connected = false
    private var closed = false
    private var expected: Station? = null
    private var timeout: Job? = null
    private var destinationTimeout: Job? = null
    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(info: DestinationInfo) {
            if (closed) return
            destinationTimeout?.cancel()
            state.update { it.copy(destination = info.name.ifBlank { "목적지 이름 없음" },lat=info.latitude,lon=info.longitude,source=DataSource.PLEOS,
                status=if(it.pending)it.status else "Pleos 목적지 콜백 수신") }
        }
        override fun onDestinationChanged() = refresh()
        override fun onWaypointChanged(info: WaypointChangedInfo) { safe { helper.getWaypointInfo() } }
        override fun onWayPointInfo(info: List<WaypointInfo>) {
            val requested = expected ?: return
            if (closed) return
            if (info.any { abs(it.latitude-requested.lat)<0.0001 && abs(it.longitude-requested.lon)<0.0001 }) {
                timeout?.cancel(); expected = null
                state.update { it.copy(pending=false,confirmed=true,status="Pleos 응답으로 경유지 좌표 확인: ${requested.name}") }
            }
        }
        override fun onRoutingCompleted() { safe { helper.getWaypointInfo(); helper.getDestinationInfo() } }
        override fun onError(code: NaviErrorCode) { fail("NaviHelper 오류: $code") }
    }
    override fun connect() {
        if(!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            fail("Pleos Navi Service가 있는 AAOS가 필요합니다")
            return
        }
        runCatching {
            helper.initialize(onConnected = {
                if (!closed) {
                    connected=true; helper.addListener(listener)
                    state.update { it.copy(status="Pleos Navi Service 연결됨 · 목적지 조회 중") }; refresh()
                }
            },onFailed = { fail("내비 연결 실패: ${it.message}") })
        }.onFailure { fail("내비 초기화 실패: ${it.message}") }
    }
    private fun safe(action: () -> Unit) {
        if (closed) return
        if (!connected) { fail("Pleos Navi Service 연결이 필요합니다"); return }
        runCatching(action).onFailure { fail("내비 요청 실패: ${it.message}") }
    }
    private fun fail(message: String) {
        if (closed) return
        timeout?.cancel(); expected=null
        state.update { it.copy(status=message,pending=false,confirmed=false) }
    }
    override fun refresh() = safe {
        destinationTimeout?.cancel()
        state.update { it.copy(destination="목적지 조회 중",lat=null,lon=null,source=DataSource.UNAVAILABLE) }
        helper.getDestinationInfo();helper.getWaypointInfo()
        destinationTimeout=scope.launch {
            delay(10_000)
            if(!closed && state.value.source==DataSource.UNAVAILABLE) state.update {
                it.copy(destination="목적지 정보 미수신",status=if(it.pending)it.status else "Pleos 연결됨 · 목적지 응답 시간 초과. 내비 목적지/서비스 상태를 확인하거나 MOCK을 선택하세요.")
            }
        }
    }
    override fun addWaypoint(station: Station) = safe {
        if (state.value.pending) return@safe
        expected=station
        state.update { it.copy(pending=true,confirmed=false,status="경유지 요청 전송 · 응답 확인 중") }
        helper.addWaypoint(RequestWaypointInfo(latitude=station.lat,longitude=station.lon,poiName=station.name,waypointIndex=WaypointIndex.FIRST,poiId="",address="",poiSubId=""))
        helper.getWaypointInfo()
        timeout=scope.launch { delay(12_000); if (expected != null) fail("경유지 응답 시간 초과 · 등록 성공 미확인") }
    }
    override fun setDestination(station: Station) = safe {
        helper.requestRoute(RouteInfo(latitude=station.lat,longitude=station.lon,poiName=station.name,poiId="",address="",poiSubId=""))
        state.update { it.copy(status="목적지 설정 요청 전송 · 내비 응답을 확인하세요",confirmed=false) }
    }
    override fun reroute() = safe { helper.requestReRoute(); state.update { it.copy(status="재탐색 요청 전송 · 완료 미확인") } }
    override fun close() {
        closed=true; timeout?.cancel(); destinationTimeout?.cancel(); expected=null
        runCatching { helper.removeListener(listener); helper.release() }; connected=false
    }
}
