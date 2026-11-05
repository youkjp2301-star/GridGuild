package com.gridguild

import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.gridguild.core.*
import com.gridguild.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Test transport callbacks, never count these as real Pleos service validation. */
@RunWith(AndroidJUnit4::class)
class NavigationRepositoryTest {
    private suspend fun NavigationRepository.finished()=withTimeout(3_000){state.first{!it.pending}}
    private suspend fun main(block: ()->Unit)=withContext(Dispatchers.Main){block()}
    private fun destination(lat: Double=35.1684,lon: Double=129.0574)=DestinationInfo(latitude=lat,longitude=lon,
        displayLatitude=null,displayLongitude=null,name="SDK test fixture",address="",remainDistance=1000,remainTime=90,tollFare=0,poiId="",poiSubId=null,poiProvider="")

    @Test fun mockRefreshReallyEmitsOnEveryPressAndRerouteIsExplicitlySimulated()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
        val repo=MockNavigationRepository(scope,150)
        try {
            val original=repo.state.value.destination
            repeat(2) { i ->
                main { repo.refresh();repo.refresh() }
                assertTrue(repo.state.value.pending)
                val result=repo.finished()
                assertEquals((i+1).toLong(),result.requestId);assertTrue(result.status.contains("갱신 완료"))
                assertEquals(original,result.destination);assertNotNull(result.destinationUpdatedAt)
            }
            main{repo.reroute()}
            assertTrue(repo.finished().status.contains("실제 경로 계산"))
            val station=SampleData.stations[1]
            main{repo.setDestination(station)};repo.finished()
            main{repo.refresh()};val refreshed=repo.finished()
            assertEquals(station.lat,refreshed.lat!!,0.00001);assertTrue(refreshed.destination.contains(station.name))
            main{repo.refresh();repo.close()};val closed=repo.state.value
            delay(200);assertEquals(closed,repo.state.value)
        } finally { repo.close();scope.cancel() }
    }
    @Test fun sdkRerouteWaitsForDedicatedEndEventAndPreventsDuplicateRequests()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
        val client=FakeNaviClient();val repo=PleosNavigationRepository(client,scope,true)
        try {
            main{repo.connect();client.listener.onDestinationInfo(destination())};repo.finished()
            main{repo.reroute();repo.reroute()}
            assertEquals(1,client.reroutes);assertTrue(repo.state.value.pending);assertFalse(repo.state.value.confirmed)
            main{client.listener.onRoutingCompleted();client.listener.onDestinationInfo(destination())}
            assertTrue(repo.state.value.pending)
            main{client.listener.onReRouteEnded();client.listener.onDestinationInfo(destination())}
            val result=repo.finished()
            assertTrue(result.confirmed);assertTrue(result.status.contains("종료 알림 수신"))
            assertFalse(result.error)
        } finally { main{repo.close()};scope.cancel() }
    }
    @Test fun missingCallbacksAndExceptionsLeaveAnExplicitFailureAndPermitRetry()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
        val client=FakeNaviClient();val repo=PleosNavigationRepository(client,scope,true,80)
        try {
            main{repo.connect()}
            val timeout=repo.finished();assertTrue(timeout.error);assertTrue(timeout.status.contains("시간 초과"));assertFalse(timeout.confirmed)
            client.throwOnRead=true
            main{repo.refresh()}
            assertTrue(repo.state.value.status.contains("fake read failure"));assertFalse(repo.state.value.pending)
            delay(150);assertTrue(repo.state.value.status.contains("fake read failure"))
            client.throwOnRead=false
            main{repo.refresh();client.listener.onDestinationInfo(destination())};repo.finished()
            main{repo.reroute();client.listener.onError(NaviErrorCode.CAUSE_SEARCH_ROUTE)}
            assertTrue(repo.finished().error);assertFalse(repo.state.value.confirmed)
        } finally { main{repo.close()};scope.cancel() }
    }
    @Test fun unrelatedCallbacksCannotConfirmAnotherOperationOrChangeClosedRepository()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
        val client=FakeNaviClient();val repo=PleosNavigationRepository(client,scope,true)
        try {
            main{repo.connect();client.listener.onReRouteEnded()}
            assertTrue(repo.state.value.pending);assertEquals(NavigationOperation.REFRESH,repo.state.value.operation)
            main{client.listener.onDestinationInfo(destination())};repo.finished()
            val station=SampleData.stations[1]
            main{repo.setDestination(station);client.listener.onDestinationInfo(destination())}
            assertTrue(repo.state.value.pending)
            main{client.listener.onDestinationInfo(destination(station.lat,station.lon))}
            assertTrue(repo.finished().status.contains("좌표 확인"))
            main{repo.close()};val closed=repo.state.value
            main{client.listener.onDestinationInfo(destination());client.listener.onError(NaviErrorCode.CAUSE_SEARCH_ROUTE)}
            assertEquals(closed,repo.state.value)
        } finally { main{repo.close()};scope.cancel() }
    }
}

private class FakeNaviClient : NaviClient {
    lateinit var listener: NaviHelperEventListener
    var reroutes=0
    var throwOnRead=false
    override fun connect(listener: NaviHelperEventListener,onConnected: ()->Unit,onFailed: (Throwable)->Unit) { this.listener=listener;onConnected() }
    override fun getDestinationInfo() { if(throwOnRead)error("fake read failure") }
    override fun getWaypointInfo()=Unit
    override fun requestReRoute() { reroutes++ }
    override fun addWaypoint(info: RequestWaypointInfo)=Unit
    override fun requestRoute(info: RouteInfo)=Unit
    override fun close()=Unit
}
