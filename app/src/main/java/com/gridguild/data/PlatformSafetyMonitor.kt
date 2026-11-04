package com.gridguild.data

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder

/** Unknown, disconnected and restricted all fail closed. Speed zero never unlocks the UI. */
class PlatformSafetyMonitor(private val context: Context, private val onChanged: (Boolean)->Unit = {}) : AutoCloseable {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    @Volatile private var allowed = false
    private val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
        allowed = permits(restrictions)
        onChanged(allowed)
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            runCatching {
                manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                manager?.registerListener(listener)
                allowed = manager?.currentCarUxRestrictions?.let(::permits) == true
                onChanged(allowed)
            }.onFailure { allowed = false;onChanged(false) }
        }
        override fun onServiceDisconnected(name: ComponentName?) { manager = null; allowed = false;onChanged(false) }
    }
    private fun permits(r: CarUxRestrictions) = !r.isRequiresDistractionOptimization &&
        r.activeRestrictions == CarUxRestrictions.UX_RESTRICTIONS_BASELINE
    fun connect() { runCatching { car = Car.createCar(context,connection); car?.connect() }.onFailure { allowed = false } }
    fun readAllowed(): Boolean = runCatching {
        val current = manager?.currentCarUxRestrictions ?: return false
        allowed && permits(current)
    }.getOrDefault(false)
    override fun close() {
        allowed = false
        runCatching { manager?.unregisterListener() }; manager = null
        runCatching { car?.disconnect() }; car = null
    }
}
