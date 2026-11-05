package com.gridguild

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.DataSource
import com.gridguild.core.ChargeSignal
import com.gridguild.data.PleosVehicleRepository
import com.gridguild.data.PleosNavigationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit runtime probes: never count a plain Android device as SDK validation. */
@RunWith(AndroidJUnit4::class)
class PleosSdkProbeTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun vehicleSdkReturnsRealBatteryAndGearData() = runBlocking {
        assumeTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
        val repo=PleosVehicleRepository(context)
        val job=launch(Dispatchers.IO) { repo.run() }
        try {
            val sample=withTimeoutOrNull(45_000) { repo.state.first {
                it.source==DataSource.PLEOS && it.soc!=null && it.chargeSwitch!=ChargeSignal.UNKNOWN
            } }
            assertNotNull("Vehicle SDK response missing: ${repo.state.value.detail}",sample)
            println("GRIDGUILD_VEHICLE_PROBE source=${sample!!.source} soc=${sample.soc} chargeSwitch=${sample.chargeSwitch}")
            assertTrue(sample.usable(System.currentTimeMillis()))
            val gear=withTimeoutOrNull(15_000) { repo.safety.first{it.source==DataSource.PLEOS} }
            assertNotNull("Gear SDK response missing: ${repo.safety.value.detail}",gear)
            println("GRIDGUILD_SAFETY_PROBE ${gear!!.detail}")
        } finally { job.cancelAndJoin();repo.close() }
    }
    @Test fun naviSdkReturnsDestinationCallback() = runBlocking {
        assumeTrue(context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
        val repo=PleosNavigationRepository(context,scope)
        try {
            withContext(Dispatchers.Main) { repo.connect() }
            val response=withTimeoutOrNull(30_000) { repo.state.first {it.source==DataSource.PLEOS} }
            assertNotNull("Navi destination callback missing: ${repo.state.value.status}",response)
            println("GRIDGUILD_NAVI_PROBE source=${response!!.source} destination=${response.destination} lat=${response.lat} lon=${response.lon}")
        } finally { withContext(Dispatchers.Main){repo.close()};scope.cancel() }
    }
}
