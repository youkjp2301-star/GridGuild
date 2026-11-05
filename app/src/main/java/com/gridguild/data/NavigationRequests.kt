package com.gridguild.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/** One in-flight navigation command; terminal results remain visible and every retry has an ID. */
internal class NavigationRequests(
    private val state: MutableStateFlow<NavigationState>,private val scope: CoroutineScope,
    private val timeoutMillis: Long=12_000,
) : AutoCloseable {
    private var timeout: Job?=null
    private var closed=false
    @Synchronized fun begin(operation: NavigationOperation,status: String): Long? {
        if(closed || state.value.pending) return null
        val id=state.value.requestId+1
        state.value=state.value.copy(operation=operation,requestId=id,pending=true,confirmed=false,error=false,status=status,resultAt=null)
        timeout=scope.launch {
            delay(timeoutMillis)
            fail("${when(operation) {
                NavigationOperation.REFRESH -> "목적지 조회"
                NavigationOperation.REROUTE -> "경로 재탐색"
                NavigationOperation.WAYPOINT -> "경유지 등록"
                NavigationOperation.DESTINATION -> "목적지 설정"
            }} 응답 시간 초과 · 성공 미확인. 내비 상태를 확인한 뒤 다시 시도하세요.",id)
        }
        return id
    }
    @Synchronized fun complete(id: Long,status: String,transform: (NavigationState)->NavigationState={it}): Boolean {
        if(closed || !state.value.pending || state.value.requestId!=id) return false
        timeout?.cancel();timeout=null
        state.value=transform(state.value).copy(pending=false,confirmed=true,error=false,status=status,resultAt=System.currentTimeMillis())
        return true
    }
    @Synchronized fun fail(status: String,id: Long?=null) {
        if(closed || (id!=null && (!state.value.pending || state.value.requestId!=id))) return
        timeout?.cancel();timeout=null
        val current=state.value
        state.value=current.copy(pending=false,confirmed=false,error=true,status=status,resultAt=System.currentTimeMillis(),
            destination=if(current.operation==NavigationOperation.REFRESH && !current.hasDestination)"목적지 정보 미수신" else current.destination)
    }
    @Synchronized override fun close() {
        closed=true;timeout?.cancel();timeout=null
    }
}
