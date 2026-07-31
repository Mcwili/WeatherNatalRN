package com.tempo.rn.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Mehrfarbige Wetter-Glyphen (Sonne, Wolken, Regen …), fest eingefärbt wie im Design. */
object WeatherIcons {

    private val sunColor = Color(0xFFF2A900)
    private val cloudColor = Color(0xFF9FB3BC)
    private val cloudDark = Color(0xFF7D919B)
    private val rainColor = Color(0xFF2A78D6)

    private class Part(
        val d: String,
        val fill: Color? = null,
        val stroke: Color? = null,
        val strokeWidth: Float = 1.8f,
    )

    private fun vector(name: String, parts: List<Part>): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            for (p in parts) {
                addPath(
                    pathData = addPathNodes(p.d),
                    fill = p.fill?.let { SolidColor(it) },
                    stroke = p.stroke?.let { SolidColor(it) },
                    strokeLineWidth = p.strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                )
            }
        }.build()

    private val sunCore = "M12 12 m-4.4 0 a4.4 4.4 0 1 0 8.8 0 a4.4 4.4 0 1 0 -8.8 0"
    private val sunRays = listOf(
        "M12 2.4 L12 5", "M12 19 L12 21.6", "M2.4 12 L5 12", "M19 12 L21.6 12",
        "M5 5 L6.8 6.8", "M17.2 17.2 L19 19", "M19 5 L17.2 6.8", "M6.8 17.2 L5 19",
    )
    private const val CLOUD_MID = "M8 18.4 L16.6 18.4 A3.4 3.4 0 0 0 17.1 11.63 A5 5 0 0 0 7.5 12.65 A3 3 0 0 0 8 18.4 Z"
    private const val CLOUD_FULL = "M7 17.4 L16.6 17.4 A3.6 3.6 0 0 0 17.13 10.24 A5.3 5.3 0 0 0 6.95 11.32 A3.2 3.2 0 0 0 7 17.4 Z"
    private const val CLOUD_RAIN = "M7 14.4 L16.6 14.4 A3.6 3.6 0 0 0 17.13 7.24 A5.3 5.3 0 0 0 6.95 8.32 A3.2 3.2 0 0 0 7 14.4 Z"

    val Sun: ImageVector by lazy {
        vector("Sun", buildList {
            add(Part(sunCore, fill = sunColor))
            sunRays.forEach { add(Part(it, stroke = sunColor)) }
        })
    }

    val PartlyCloudy: ImageVector by lazy {
        vector("PartlyCloudy", listOf(
            Part("M9 9 m-3.6 0 a3.6 3.6 0 1 0 7.2 0 a3.6 3.6 0 1 0 -7.2 0", fill = sunColor),
            Part(CLOUD_MID, fill = cloudColor),
        ))
    }

    val Cloud: ImageVector by lazy {
        vector("Cloud", listOf(Part(CLOUD_FULL, fill = cloudColor)))
    }

    val Rain: ImageVector by lazy {
        vector("Rain", listOf(
            Part(CLOUD_RAIN, fill = cloudColor),
            Part("M9 17 L8 20", stroke = rainColor),
            Part("M13 17 L12 20", stroke = rainColor),
            Part("M17 17 L16 20", stroke = rainColor),
        ))
    }

    val Drizzle: ImageVector by lazy {
        vector("Drizzle", listOf(
            Part(CLOUD_RAIN, fill = cloudColor),
            Part("M10 17 L9.5 18.6", stroke = rainColor),
            Part("M14 17 L13.5 18.6", stroke = rainColor),
        ))
    }

    val Storm: ImageVector by lazy {
        vector("Storm", listOf(
            Part("M7 13.4 L16.6 13.4 A3.6 3.6 0 0 0 17.13 6.24 A5.3 5.3 0 0 0 6.95 7.32 A3.2 3.2 0 0 0 7 13.4 Z", fill = cloudDark),
            Part("M12.6 14.5 L10 18.6 L12.2 18.6 L11 22 L15 17 L12.6 17 L14.2 14.5 Z", fill = sunColor),
        ))
    }

    val Fog: ImageVector by lazy {
        vector("Fog", listOf(
            Part(CLOUD_RAIN, fill = cloudColor),
            Part("M6.5 17 L17.5 17", stroke = cloudDark),
            Part("M8 19.5 L16 19.5", stroke = cloudDark),
        ))
    }

    /** WMO-Wettercode → Icon + portugiesische Beschreibung. */
    fun forCode(code: Int?): Pair<ImageVector, String> = when (code) {
        0 -> Sun to "Céu limpo"
        1 -> Sun to "Predominantemente limpo"
        2 -> PartlyCloudy to "Parcialmente nublado"
        3 -> Cloud to "Nublado"
        45, 48 -> Fog to "Neblina"
        51, 53, 55, 56, 57 -> Drizzle to "Garoa"
        61, 63, 66, 80 -> Rain to "Chuva"
        65, 67, 81, 82 -> Rain to "Chuva forte"
        71, 73, 75, 77, 85, 86 -> Rain to "Precipitação"
        95, 96, 99 -> Storm to "Trovoada"
        else -> PartlyCloudy to "Sem dados"
    }
}
