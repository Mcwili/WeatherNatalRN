package com.tempo.rn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tempo.rn.core.model.ConfidenceLevel
import com.tempo.rn.core.model.TideConfidence
import com.tempo.rn.ui.icons.TempoIcons
import com.tempo.rn.ui.theme.LocalTempoColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val PtBr: Locale = Locale.forLanguageTag("pt-BR")

fun formatMm(value: Double): String = String.format(PtBr, "%.1f", value)
fun formatMeters(value: Double): String = String.format(PtBr, "%.1f", value)
fun formatTime(time: LocalDateTime): String = time.format(DateTimeFormatter.ofPattern("HH:mm"))
fun formatHour(time: LocalDateTime): String = "${time.hour}h"

/** Karte im Stil des Designs: 20dp-Radius, Hairline, Surface-High oder tonal. */
@Composable
fun TempoCard(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val bg = if (tonal) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(16.dp),
        content = content,
    )
}

/** Versal-Sektionstitel wie im Mockup. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(PtBr),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Confidence-Chip: Punkt + Text, feste Semantikfarben, nie Farbe allein. */
@Composable
fun ConfidenceChip(level: ConfidenceLevel, modifier: Modifier = Modifier, textOverride: String? = null) {
    val colors = LocalTempoColors.current
    val (fg, bg) = when (level) {
        ConfidenceLevel.HIGH -> colors.confHigh to colors.confHighBg
        ConfidenceLevel.MODERATE -> colors.confModerate to colors.confModerateBg
        ConfidenceLevel.LOW -> colors.confLow to colors.confLowBg
        ConfidenceLevel.VERY_LOW -> colors.confVeryLow to colors.confVeryLowBg
    }
    ChipRow(fg, bg, textOverride ?: level.labelPt, modifier)
}

@Composable
fun TideConfidenceChip(confidence: TideConfidence, modifier: Modifier = Modifier) {
    val colors = LocalTempoColors.current
    val (fg, bg) = when (confidence) {
        TideConfidence.HIGH -> colors.confHigh to colors.confHighBg
        TideConfidence.MODERATE -> colors.confModerate to colors.confModerateBg
        TideConfidence.LOW -> colors.confLow to colors.confLowBg
        TideConfidence.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceContainer
    }
    ChipRow(fg, bg, confidence.labelPt, modifier)
}

@Composable
private fun ChipRow(fg: Color, bg: Color, text: String, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(7.dp).background(fg, CircleShape))
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

/** Offline-/Hinweisbanner in Duna (Spez. §21). */
@Composable
fun OfflineBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = TempoIcons.Offline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** "Atualizado às 13:42" / "Dados de 2 horas atrás" (Spez. §20). */
fun updatedAtText(fetchedAt: LocalDateTime?, now: LocalDateTime): String {
    if (fetchedAt == null) return "Sem dados carregados"
    val minutes = java.time.Duration.between(fetchedAt, now).toMinutes()
    return when {
        minutes < 90 -> "Atualizado às ${formatTime(fetchedAt)}"
        minutes < 60 * 24 -> "Dados de ${minutes / 60} horas atrás"
        else -> "Dados de ${minutes / (60 * 24)} dias atrás"
    }
}
