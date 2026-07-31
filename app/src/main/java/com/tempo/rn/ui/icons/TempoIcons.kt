package com.tempo.rn.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Navigations- und UI-Icons (einfarbig, Tint über Icon-Composable). */
object TempoIcons {

    private fun stroked(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            for (d in paths) {
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            for (d in paths) {
                addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
            }
        }.build()

    val Home: ImageVector by lazy {
        stroked("Home", "M4.5 11.5 L12 5 L19.5 11.5 L19.5 19 A1 1 0 0 1 18.5 20 L14 20 L14 15 L10 15 L10 20 L5.5 20 A1 1 0 0 1 4.5 19 Z")
    }
    val Forecast: ImageVector by lazy {
        stroked("Forecast", "M4 17.5 L9 11 L12.6 14.5 L20 6.5", "M15.5 6.5 L20 6.5 L20 11")
    }
    val Map: ImageVector by lazy {
        stroked("Map", "M9 4 L4 6 L4 20 L9 18 L15 20 L20 18 L20 4 L15 6 L9 4 Z", "M9 4 L9 18", "M15 6 L15 20")
    }
    val Wave: ImageVector by lazy {
        stroked(
            "Wave",
            "M3 9 C5.5 9 5.5 7 8 7 C10.5 7 10.5 9 13 9 C15.5 9 15.5 7 18 7 C19.5 7 20 7.7 21 8.4",
            "M3 15 C5.5 15 5.5 13 8 13 C10.5 13 10.5 15 13 15 C15.5 15 15.5 13 18 13 C19.5 13 20 13.7 21 14.4",
        )
    }
    val Layers: ImageVector by lazy {
        stroked("Layers", "M12 4 L20 8.5 L12 13 L4 8.5 Z", "M5.5 13 L12 16.6 L18.5 13", "M5.5 16.5 L12 20 L18.5 16.5")
    }
    val Play: ImageVector by lazy { filled("Play", "M8.5 6.5 L8.5 17.5 L17.5 12 Z") }
    val Pause: ImageVector by lazy { filled("Pause", "M7 6 L10 6 L10 18 L7 18 Z", "M14 6 L17 6 L17 18 L14 18 Z") }
    val Offline: ImageVector by lazy {
        stroked(
            "Offline",
            "M8.6 16.2 A5.2 5.2 0 0 1 15.4 16.2",
            "M5.6 13 A9.5 9.5 0 0 1 13.5 10.5",
            "M18.4 13 A9.4 9.4 0 0 0 16.5 11.6",
            "M3 5.5 L21 20",
            "M12 19.6 L12.01 19.6",
        )
    }
    val Refresh: ImageVector by lazy {
        stroked("Refresh", "M4.5 12 A7.5 7.5 0 1 1 6.7 17.3", "M4.5 17.5 L4.5 12.5 L9.5 12.5")
    }
    val ChevronDown: ImageVector by lazy { stroked("ChevronDown", "M7 10 L12 15 L17 10") }
    val ArrowDown: ImageVector by lazy { stroked("ArrowDown", "M12 5 L12 19", "M6 13 L12 19 L18 13") }
    val ArrowUp: ImageVector by lazy { stroked("ArrowUp", "M12 19 L12 5", "M6 11 L12 5 L18 11") }
}
