package com.tempo.rn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = OceanoLight,
    onPrimary = OnOceanoLight,
    primaryContainer = OceanoContainerLight,
    onPrimaryContainer = OnOceanoContainerLight,
    secondary = DunaLight,
    onSecondary = OnOceanoLight,
    secondaryContainer = DunaContainerLight,
    onSecondaryContainer = OnDunaContainerLight,
    tertiary = PoenteLight,
    onTertiary = OnOceanoLight,
    tertiaryContainer = PoenteContainerLight,
    onTertiaryContainer = Color(0xFF3A0B00),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceHighLight,
    surfaceContainerHighest = SurfaceHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = OceanoDark,
    onPrimary = OnOceanoDark,
    primaryContainer = OceanoContainerDark,
    onPrimaryContainer = OnOceanoContainerDark,
    secondary = DunaDark,
    onSecondary = Color(0xFF3A2E12),
    secondaryContainer = DunaContainerDark,
    onSecondaryContainer = OnDunaContainerDark,
    tertiary = PoenteDark,
    onTertiary = Color(0xFF561F00),
    tertiaryContainer = PoenteContainerDark,
    onTertiaryContainer = Color(0xFFFFDBCF),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceHighDark,
    surfaceContainerHighest = SurfaceHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
)

@Composable
fun TempoRNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tempoColors = if (darkTheme) TempoColorsDark else TempoColorsLight
    CompositionLocalProvider(LocalTempoColors provides tempoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TempoTypography,
            content = content,
        )
    }
}
