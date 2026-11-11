package com.gridguild

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gridguild.core.DriverScene
import com.gridguild.core.NeutralCity
import com.gridguild.ui.driverSceneResource
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.zip.CRC32

/** Resource identity/decoding checks complement human camera/material/landmark visual review. */
@RunWith(AndroidJUnit4::class)
class DriverSceneResourcesTest {
    @Test fun allTwentyFourDestinationLevelResourcesAreWideDecodableAndHaveDifferentPixels() {
        val resources=InstrumentationRegistry.getInstrumentation().targetContext.resources
        val ids=mutableSetOf<Int>()
        val fingerprints=mutableSetOf<Long>()
        NeutralCity.entries.forEach { city -> (1..12).forEach { level ->
            val scene=DriverScene(city,level)
            val resource=driverSceneResource(scene)
            assertTrue("Distinct resource for ${scene.assetKey}",ids.add(resource))
            assertEquals(scene.assetKey,resources.getResourceEntryName(resource))
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeResource(resources,resource,bounds)
            assertTrue("Wide driver scene width for ${scene.assetKey}",bounds.outWidth>=1280)
            assertTrue("Driver scene height for ${scene.assetKey}",bounds.outHeight>=720)
            assertTrue("Landscape 16:9 composition for ${scene.assetKey}",bounds.outWidth.toFloat()/bounds.outHeight in 1.7f..1.9f)
            val bitmap=checkNotNull(BitmapFactory.decodeResource(resources,resource,BitmapFactory.Options().apply{inSampleSize=8}))
            try {
                val crc=CRC32()
                for(y in 0 until bitmap.height step 5)for(x in 0 until bitmap.width step 5) {
                    val pixel=bitmap.getPixel(x,y)
                    repeat(4){crc.update(pixel ushr (it*8) and 255)}
                }
                assertTrue("Each city/level needs its own artwork: ${scene.assetKey}",fingerprints.add(crc.value))
            } finally {bitmap.recycle()}
        } }
        assertEquals(24,ids.size)
        assertEquals(24,fingerprints.size)
    }
}
