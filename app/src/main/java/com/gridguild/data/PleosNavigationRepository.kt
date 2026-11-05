package com.gridguild.data

import android.content.Context
import android.content.pm.PackageManager
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.constants.*
import com.gridguild.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class PleosNavigationRepository internal constructor(
    private val client: NaviClient,private val scope: CoroutineScope,private val automotive: Boolean,
    timeoutMillis: Long=12_000,
) : NavigationRepository {
    constructor(context: Context,scope: CoroutineScope) : this(PleosNaviClient(context),scope,
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
    override val state = MutableStateFlow(NavigationState())
    private val requests=NavigationRequests(state,scope,timeoutMillis)
    private var connected = false
    @Volatile private var closed = false
    private var expected: Station? = null
    private fun receive(block: ()->Unit) { scope.launch(Dispatchers.Main.immediate) { if(!closed)block() } }
    private val listener = object : NaviHelperEventListener {
        override fun onDestinationInfo(info: DestinationInfo) = receive {
            val valid=info.latitude.isFinite() && info.longitude.isFinite() && info.latitude in -90.0..90.0 &&
                info.longitude in -180.0..180.0 && (info.latitude!=0.0 || info.longitude!=0.0)
            state.update { it.copy(destination=if(valid)info.name.ifBlank{"목적지 이름 없음"} else "유효한 목적지 좌표 미확인",
                lat=if(valid)info.latitude else null,lon=if(valid)info.longitude else null,source=DataSource.PLEOS,
                destinationUpdatedAt=System.currentTimeMillis()) }
            val current=state.value
            if(current.pending && current.operation==NavigationOperation.REFRESH)
                requests.complete(current.requestId,if(valid)"Pleos 목적지 갱신 완료 · 콜백 수신" else "Pleos 목적지 콜백 수신 · 유효한 목적지 좌표 없음")
            else if(current.pending && current.operation==NavigationOperation.DESTINATION && expected?.let {
                valid && abs(info.latitude-it.lat)<0.0001 && abs(info.longitude-it.lon)<0.0001
            }==true) {
                expected=null;requests.complete(current.requestId,"Pleos 응답으로 목적지 좌표 확인")
            }
        }
        override fun onDestinationChanged() = receive {
            if(state.value.pending)send { client.getDestinationInfo() } else refresh()
        }
        override fun onWaypointChanged(info: WaypointChangedInfo) = receive { send { client.getWaypointInfo() } }
        override fun onWayPointInfo(info: List<WaypointInfo>) = receive {
            val requested=expected
            val current=state.value
            if(current.pending && current.operation==NavigationOperation.WAYPOINT && requested!=null &&
                info.any { abs(it.latitude-requested.lat)<0.0001 && abs(it.longitude-requested.lon)<0.0001 }) {
                expected=null;requests.complete(current.requestId,"Pleos 응답으로 경유지 좌표 확인: ${requested.name}")
            }
        }
        override fun onRoutingCompleted() = receive { send { client.getDestinationInfo();client.getWaypointInfo() } }
        override fun onReRouteEnded() = receive {
            val current=state.value
            if(current.pending && current.operation==NavigationOperation.REROUTE)
                requests.complete(current.requestId,"Pleos 재탐색 종료 알림 수신 · 실제 경로 형상 미제공")
            send { client.getDestinationInfo() }
        }
        override fun onRouteCancelled() = receive {
            if(state.value.pending && state.value.operation!=NavigationOperation.REFRESH) requests.fail("내비 경로가 취소되었습니다 · 요청 성공 미확인")
        }
        override fun onError(code: NaviErrorCode) = receive { expected=null;requests.fail("NaviHelper 오류: $code · 요청 성공 미확인") }
    }
    override fun connect() {
        if(!automotive) {
            requests.fail("Pleos Navi Service가 있는 AAOS가 필요합니다")
            return
        }
        runCatching {
            client.connect(listener,onConnected={ receive { connected=true;refresh() } },
                onFailed={ error -> receive { connected=false;requests.fail("내비 연결 실패: ${error.message}") } })
        }.onFailure { requests.fail("내비 초기화 실패: ${it.message}") }
    }
    private fun send(action: ()->Unit) {
        if(closed) return
        if(!connected) { requests.fail("Pleos Navi Service 연결이 필요합니다");return }
        runCatching(action).onFailure { expected=null;requests.fail("내비 요청 실패: ${it.message}") }
    }
    private fun begin(operation: NavigationOperation,status: String): Long? {
        if(closed || state.value.pending) return null
        if(!connected) { requests.fail("Pleos Navi Service 연결이 필요합니다");return null }
        return requests.begin(operation,status)
    }
    override fun refresh() {
        begin(NavigationOperation.REFRESH,"Pleos 목적지 조회 중…") ?: return
        expected=null
        state.update { it.copy(destination="목적지 조회 중",lat=null,lon=null,source=DataSource.UNAVAILABLE) }
        send { client.getDestinationInfo() }
    }
    override fun addWaypoint(station: Station) {
        begin(NavigationOperation.WAYPOINT,"경유지 요청 전송 · 응답 확인 중…") ?: return
        expected=station
        send {
            client.addWaypoint(RequestWaypointInfo(latitude=station.lat,longitude=station.lon,poiName=station.name,waypointIndex=WaypointIndex.FIRST,poiId="",address="",poiSubId=""))
            client.getWaypointInfo()
        }
    }
    override fun setDestination(station: Station) {
        begin(NavigationOperation.DESTINATION,"목적지 설정 요청 전송 · 좌표 응답 확인 중…") ?: return
        expected=station
        send {
            client.requestRoute(RouteInfo(latitude=station.lat,longitude=station.lon,poiName=station.name,poiId="",address="",poiSubId=""))
            client.getDestinationInfo()
        }
    }
    override fun reroute() {
        if(closed || state.value.pending) return
        if(!state.value.hasDestination) { requests.fail("목적지 좌표가 미확인입니다. 내비에서 목적지를 설정한 뒤 새로고침하세요.");return }
        begin(NavigationOperation.REROUTE,"Pleos 재탐색 요청 전송 · 종료 알림 확인 중…") ?: return
        expected=null
        send { client.requestReRoute() }
    }
    override fun close() {
        closed=true;requests.close();expected=null;connected=false
        runCatching { client.close() }
    }
}
