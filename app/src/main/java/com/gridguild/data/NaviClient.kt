package com.gridguild.data

import android.content.Context
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/** Small seam for testing callbacks. Production always uses the official typed SDK below. */
internal interface NaviClient : AutoCloseable {
    fun connect(listener: NaviHelperEventListener,onConnected: ()->Unit,onFailed: (Throwable)->Unit)
    fun getDestinationInfo()
    fun getWaypointInfo()
    fun requestReRoute()
    fun addWaypoint(info: RequestWaypointInfo)
    fun requestRoute(info: RouteInfo)
}

internal class PleosNaviClient(context: Context) : NaviClient {
    private val helper=NaviHelper(context)
    private var listener: NaviHelperEventListener?=null
    override fun connect(listener: NaviHelperEventListener,onConnected: ()->Unit,onFailed: (Throwable)->Unit) {
        this.listener=listener
        helper.initialize(onConnected={runCatching { helper.addListener(listener);onConnected() }.onFailure(onFailed)},onFailed=onFailed)
    }
    override fun getDestinationInfo()=helper.getDestinationInfo()
    override fun getWaypointInfo()=helper.getWaypointInfo()
    override fun requestReRoute()=helper.requestReRoute()
    override fun addWaypoint(info: RequestWaypointInfo)=helper.addWaypoint(info)
    override fun requestRoute(info: RouteInfo)=helper.requestRoute(info)
    override fun close() { try { listener?.let{helper.removeListener(it)} } finally { helper.release();listener=null } }
}
