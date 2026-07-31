package com.tempo.rn.data

import com.tempo.rn.core.network.RainViewerApi
import com.tempo.rn.core.network.RainViewerMapsResponse
import javax.inject.Inject
import javax.inject.Singleton

data class RadarFrame(
    val timeEpoch: Long,
    val tileUrlTemplate: String,
    val isNowcast: Boolean,
)

@Singleton
class RadarRepository @Inject constructor(
    private val rainViewerApi: RainViewerApi,
) {

    /** RainViewer-Metadaten laden und in Tile-URL-Vorlagen umsetzen (Spez. §3.6). */
    suspend fun loadFrames(): Result<List<RadarFrame>> = runCatching {
        val resp: RainViewerMapsResponse = rainViewerApi.weatherMaps()
        val host = resp.host ?: "https://tilecache.rainviewer.com"
        val radar = resp.radar ?: return@runCatching emptyList()
        val past = radar.past.map { frame ->
            RadarFrame(frame.time, "$host${frame.path}/256/{z}/{x}/{y}/2/1_1.png", isNowcast = false)
        }
        val nowcast = radar.nowcast.map { frame ->
            RadarFrame(frame.time, "$host${frame.path}/256/{z}/{x}/{y}/2/1_1.png", isNowcast = true)
        }
        past + nowcast
    }
}
