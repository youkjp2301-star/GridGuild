package com.gridguild.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gridguild.R
import com.gridguild.core.CityUnlocks
import com.gridguild.core.DriverScene
import com.gridguild.core.NeutralCity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Explicit registry: the build fails if any destination/level asset is missing. */
internal fun driverSceneResource(scene: DriverScene): Int = when(scene.city) {
    NeutralCity.GWANGALLI -> gwangalliScenes[scene.level-1]
    NeutralCity.CENTUM -> centumScenes[scene.level-1]
}
private val gwangalliScenes=intArrayOf(
    R.drawable.driver_gwangalli_lv01_v2,R.drawable.driver_gwangalli_lv02_v2,
    R.drawable.driver_gwangalli_lv03_v2,R.drawable.driver_gwangalli_lv04_v2,
    R.drawable.driver_gwangalli_lv05_v2,R.drawable.driver_gwangalli_lv06_v2,
    R.drawable.driver_gwangalli_lv07_v2,R.drawable.driver_gwangalli_lv08_v2,
    R.drawable.driver_gwangalli_lv09_v2,R.drawable.driver_gwangalli_lv10_v2,
    R.drawable.driver_gwangalli_lv11_v2,R.drawable.driver_gwangalli_lv12_v2,
)
private val centumScenes=intArrayOf(
    R.drawable.driver_centum_lv01_v2,R.drawable.driver_centum_lv02_v2,
    R.drawable.driver_centum_lv03_v2,R.drawable.driver_centum_lv04_v2,
    R.drawable.driver_centum_lv05_v2,R.drawable.driver_centum_lv06_v2,
    R.drawable.driver_centum_lv07_v2,R.drawable.driver_centum_lv08_v2,
    R.drawable.driver_centum_lv09_v2,R.drawable.driver_centum_lv10_v2,
    R.drawable.driver_centum_lv11_v2,R.drawable.driver_centum_lv12_v2,
)

/** A scene is keyed by BOTH destination and exact level; no era atlas or old-photo fallback. */
private object DriverSceneCache {
    private val images=LruCache<DriverScene,ImageBitmap>(3)
    @Synchronized fun peek(scene: DriverScene): ImageBitmap?=images.get(scene)
    @Synchronized fun clear(){images.evictAll()}
    @Synchronized private fun retain(scene: DriverScene,decoded: ImageBitmap): ImageBitmap =
        images.get(scene) ?: decoded.also{images.put(scene,it)}

    suspend fun load(context: Context,scene: DriverScene): ImageBitmap=withContext(Dispatchers.IO) {
        peek(scene) ?: run {
            val resource=driverSceneResource(scene)
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeResource(context.resources,resource,bounds)
            val options=BitmapFactory.Options().apply {
                inSampleSize=1
                while(bounds.outWidth/inSampleSize>2048 || bounds.outHeight/inSampleSize>1280)inSampleSize*=2
            }
            val decoded=checkNotNull(BitmapFactory.decodeResource(context.resources,resource,options)){"도시 장면을 읽지 못했어요"}.asImageBitmap()
            retain(scene,decoded)
        }
    }
}

/** Deterministic cold-load race checks; no saved game or unlock state is modified. */
internal fun clearDriverSceneMemoryCache(){DriverSceneCache.clear()}

/** Safety/unlock checks precede decode, cache lookup and every rendered image. */
@Composable internal fun DriverSceneBackground(scene: DriverScene,unlockedThrough: Int,modifier: Modifier=Modifier) {
    if(CityUnlocks.canView(scene.level,unlockedThrough)) key(scene) {
        val context=LocalContext.current.applicationContext
        var retry by remember{mutableIntStateOf(0)}
        var bitmap by remember{mutableStateOf(DriverSceneCache.peek(scene))}
        var error by remember{mutableStateOf(false)}
        LaunchedEffect(scene,retry) {
            if(bitmap==null) {
                error=false
                val started=android.os.SystemClock.elapsedRealtime()
                try {
                    val loaded=DriverSceneCache.load(context,scene)
                    holdLoadingMinimum(started)
                    bitmap=loaded
                } catch(e: CancellationException){throw e}
                catch(e: Exception){error=true}
            }
        }
        val loaded=bitmap
        if(loaded!=null) {
            val moving=motionEnabled()
            var entered by remember{mutableStateOf(false)}
            LaunchedEffect(Unit){entered=true}
            val entrance by animateFloatAsState(if(entered || !moving)1f else 0f,
                tween(if(moving)360 else 0),label="driverSceneEntrance")
            // Only the incoming permitted scene animates. No outgoing image survives a reset or restriction.
            Image(loaded,
                "${scene.city.title} Lv.${scene.level} 도로 위 운전자 1인칭 · ${scene.roadLabel} · ${scene.landmarkLabel}",
                modifier.fillMaxSize().graphicsLayer{alpha=entrance;translationY=(1f-entrance)*12.dp.toPx()}
                    .testTag("driverScene-${scene.city.destinationId}-${scene.level}"),
                contentScale=ContentScale.Fit)
        }
        else Box(modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            if(error)Button(onClick={retry++}){Text("도시 장면 다시 불러오기")}
            else CityGrowthLoading(Modifier.widthIn(max=540.dp),label="Lv.${scene.level} ${scene.city.title} 거리를 펼치고 있어요",loop=false)
        }
    }
}
