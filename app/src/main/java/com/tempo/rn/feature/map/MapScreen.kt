package com.tempo.rn.feature.map

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tempo.rn.core.model.Locations
import com.tempo.rn.data.RadarFrame
import com.tempo.rn.ui.components.SectionLabel
import com.tempo.rn.ui.components.TempoCard
import com.tempo.rn.ui.icons.TempoIcons
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val OSM_STYLE = """
{
  "version": 8,
  "name": "osm-raster",
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    { "id": "osm", "type": "raster", "source": "osm" }
  ]
}
"""

private const val RADAR_SOURCE = "radar-src"
private const val RADAR_LAYER = "radar-layer"

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            mapRef = map
            map.setStyle(Style.Builder().fromJson(OSM_STYLE))
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(Locations.default.latitude, Locations.default.longitude))
                .zoom(9.0)
                .build()
        }
    }

    // Radar-Layer bei Frame-Wechsel austauschen
    LaunchedEffect(state.selectedFrame, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        val frame = state.selectedFrame
        map.getStyle { style -> updateRadarLayer(style, frame) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Mapas",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Radar · RainViewer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            state.selectedFrame?.let { frame ->
                Text(
                    text = frameTimeText(frame),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(10.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .align(Alignment.TopStart),
                )
            }
            Text(
                "© OpenStreetMap contributors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        TempoCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        .clickable { viewModel.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.playing) TempoIcons.Pause else TempoIcons.Play,
                        contentDescription = if (state.playing) "Pausar" else "Reproduzir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Slider(
                    value = state.selectedIndex.toFloat(),
                    onValueChange = { viewModel.select(it.toInt()) },
                    valueRange = 0f..(state.frames.size - 1).coerceAtLeast(1).toFloat(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "histórico",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "agora",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "previsão curta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.error || state.frames.isEmpty()) {
            TempoCard(tonal = true) {
                SectionLabel("Cobertura")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Radar meteorológico indisponível ou com cobertura limitada para esta região.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            TempoCard(tonal = true) {
                SectionLabel("Cobertura")
                Spacer(Modifier.height(4.dp))
                Text(
                    "A cobertura de radar no litoral do RN é limitada. Áreas sem eco podem significar ausência de radar, não ausência de chuva. O radar não substitui a previsão dos modelos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun updateRadarLayer(style: Style, frame: RadarFrame?) {
    try {
        style.getLayer(RADAR_LAYER)?.let { style.removeLayer(RADAR_LAYER) }
        style.getSource(RADAR_SOURCE)?.let { style.removeSource(RADAR_SOURCE) }
        if (frame != null) {
            val tileSet = TileSet("2.1.0", frame.tileUrlTemplate)
            style.addSource(RasterSource(RADAR_SOURCE, tileSet, 256))
            style.addLayer(
                RasterLayer(RADAR_LAYER, RADAR_SOURCE)
                    .withProperties(PropertyFactory.rasterOpacity(0.75f))
            )
        }
    } catch (_: Exception) {
        // Style noch nicht bereit oder Layer-Konflikt — nächster Frame-Wechsel versucht es erneut.
    }
}

private fun frameTimeText(frame: RadarFrame): String {
    val time = Instant.ofEpochSecond(frame.timeEpoch)
        .atZone(ZoneId.of("America/Fortaleza"))
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    return if (frame.isNowcast) "$time · previsão curta" else time
}
