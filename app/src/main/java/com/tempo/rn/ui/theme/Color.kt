package com.tempo.rn.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Farbwelt "Horizontlinie": Oceano (Primär), Duna (Sekundär), Poente (Tertiär)

// Light
val OceanoLight = Color(0xFF006B7D)
val OnOceanoLight = Color(0xFFFFFFFF)
val OceanoContainerLight = Color(0xFFB2EBF8)
val OnOceanoContainerLight = Color(0xFF001F26)
val DunaLight = Color(0xFF6D5C3F)
val DunaContainerLight = Color(0xFFF6E7CB)
val OnDunaContainerLight = Color(0xFF251A04)
val PoenteLight = Color(0xFFB3502E)
val PoenteContainerLight = Color(0xFFFFDBCF)
val SurfaceLight = Color(0xFFF6FAFB)
val SurfaceContainerLight = Color(0xFFEAF1F3)
val SurfaceHighLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF171C1E)
val OnSurfaceVariantLight = Color(0xFF43555C)
val OutlineLight = Color(0xFFD3DEE1)

// Dark
val OceanoDark = Color(0xFF7AD0E2)
val OnOceanoDark = Color(0xFF00363F)
val OceanoContainerDark = Color(0xFF004E5B)
val OnOceanoContainerDark = Color(0xFFB2EBF8)
val DunaDark = Color(0xFFDCC296)
val DunaContainerDark = Color(0xFF53442A)
val OnDunaContainerDark = Color(0xFFF6E7CB)
val PoenteDark = Color(0xFFFFB59A)
val PoenteContainerDark = Color(0xFF872F10)
val SurfaceDark = Color(0xFF0E1A20)
val SurfaceContainerDark = Color(0xFF16242B)
val SurfaceHighDark = Color(0xFF1C2E36)
val OnSurfaceDark = Color(0xFFE2E9EB)
val OnSurfaceVariantDark = Color(0xFFAEBFC5)
val OutlineDark = Color(0xFF2A3B44)

/**
 * Erweiterte Semantik-Farben: Confidence-Skala, Chart-Serien (CVD-validiert),
 * Nacht-Band. Über CompositionLocal verfügbar.
 */
@Immutable
data class TempoColors(
    val confHigh: Color,
    val confModerate: Color,
    val confLow: Color,
    val confVeryLow: Color,
    val confHighBg: Color,
    val confModerateBg: Color,
    val confLowBg: Color,
    val confVeryLowBg: Color,
    val series1: Color,
    val series2: Color,
    val series3: Color,
    val series4: Color,
    val series5: Color,
    val chartBand: Color,
    val nightBand: Color,
    val sunYellow: Color,
    val cloudGray: Color,
    val rainBlue: Color,
) {
    val seriesColors: List<Color> get() = listOf(series1, series2, series3, series4, series5)
}

val TempoColorsLight = TempoColors(
    confHigh = Color(0xFF0B8A55),
    confModerate = Color(0xFFB57B00),
    confLow = Color(0xFFCF5A33),
    confVeryLow = Color(0xFFC22F3D),
    confHighBg = Color(0xFFDCF3E7),
    confModerateBg = Color(0xFFF8ECD2),
    confLowBg = Color(0xFFFBE3D8),
    confVeryLowBg = Color(0xFFFADCDF),
    series1 = Color(0xFF2A78D6),
    series2 = Color(0xFFEB6834),
    series3 = Color(0xFF1BAF7A),
    series4 = Color(0xFFEDA100),
    series5 = Color(0xFFE87BA4),
    chartBand = Color(0x24006B7D),
    nightBand = Color(0x1210222A),
    sunYellow = Color(0xFFF2A900),
    cloudGray = Color(0xFF9FB3BC),
    rainBlue = Color(0xFF2A78D6),
)

val TempoColorsDark = TempoColors(
    confHigh = Color(0xFF4FD598),
    confModerate = Color(0xFFEEC24F),
    confLow = Color(0xFFF0906C),
    confVeryLow = Color(0xFFF07A86),
    confHighBg = Color(0xFF113627),
    confModerateBg = Color(0xFF3A2F10),
    confLowBg = Color(0xFF40200F),
    confVeryLowBg = Color(0xFF401318),
    series1 = Color(0xFF3987E5),
    series2 = Color(0xFFD95926),
    series3 = Color(0xFF199E70),
    series4 = Color(0xFFC98500),
    series5 = Color(0xFFD55181),
    chartBand = Color(0x297AD0E2),
    nightBand = Color(0x4D000000),
    sunYellow = Color(0xFFF2A900),
    cloudGray = Color(0xFF9FB3BC),
    rainBlue = Color(0xFF3987E5),
)

val LocalTempoColors = staticCompositionLocalOf { TempoColorsLight }
