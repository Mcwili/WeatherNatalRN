package com.tempo.rn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempo.rn.core.model.ConfidenceLevel
import com.tempo.rn.core.model.ConsolidatedHour
import com.tempo.rn.core.model.TideEventType
import com.tempo.rn.core.model.TidePoint
import com.tempo.rn.core.model.TideState
import com.tempo.rn.ui.theme.LocalTempoColors
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.max

/** Catmull-Rom-Glättung zu kubischen Bezier-Segmenten. */
private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (i in 0 until points.size - 1) {
        val p0 = points[max(0, i - 1)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[minOf(points.size - 1, i + 2)]
        val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

/** Mini-Balkenreihe der Regenwahrscheinlichkeit (Startseite). */
@Composable
fun RainSparkline(
    probabilities: List<Int>,
    highlightIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTempoColors.current
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxWidth().height(44.dp)) {
        if (probabilities.isEmpty()) return@Canvas
        val w = size.width / probabilities.size
        probabilities.forEachIndexed { i, p ->
            val h = max(3f, p / 100f * (size.height - 4f))
            drawRoundRect(
                color = if (i == highlightIndex) primary else colors.chartBand,
                topLeft = Offset(i * w + 2f, size.height - h),
                size = Size(w - 4f, h),
                cornerRadius = CornerRadius(6f, 6f),
            )
        }
    }
}

/** Fläche + Linie der Regenwahrscheinlichkeit über den Horizont (Previsão). */
@Composable
fun ProbabilityAreaChart(
    hours: List<ConsolidatedHour>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTempoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(130.dp)) {
        if (hours.size < 2) return@Canvas
        val h = size.height
        val w = size.width
        for (frac in listOf(0.25f, 0.5f, 0.75f)) {
            drawLine(grid, Offset(0f, h * frac), Offset(w, h * frac), strokeWidth = 1f)
        }
        val pts = hours.mapIndexed { i, hour ->
            Offset(i / (hours.size - 1f) * w, h - hour.rainProbabilityPercent / 100f * (h - 14f))
        }
        val line = smoothPath(pts)
        val area = Path().apply {
            addPath(line)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(area, colors.chartBand)
        drawPath(line, primary, style = Stroke(width = 4f))

        val peakIndex = hours.indices.maxByOrNull { hours[it].rainProbabilityPercent } ?: 0
        val peak = pts[peakIndex]
        drawCircle(primary, radius = 7f, center = peak)
        drawText(
            textMeasurer = measurer,
            text = "${hours[peakIndex].rainProbabilityPercent}%",
            topLeft = Offset((peak.x + 10f).coerceAtMost(w - 70f), (peak.y - 34f).coerceAtLeast(0f)),
            style = TextStyle(color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/** Regenmenge: Faixa (p75) hinter Median-Balken (Previsão). */
@Composable
fun PrecipBarsChart(
    hours: List<ConsolidatedHour>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTempoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.fillMaxWidth().height(110.dp)) {
        if (hours.isEmpty()) return@Canvas
        val h = size.height
        val w = size.width
        val maxVal = max(1.0, hours.maxOf { max(it.precipP75Mm, it.precipMedianMm) }) * 1.15
        for (frac in listOf(0.33f, 0.66f)) {
            drawLine(grid, Offset(0f, h * frac), Offset(w, h * frac), strokeWidth = 1f)
        }
        val bw = w / hours.size
        hours.forEachIndexed { i, hour ->
            val bandH = (hour.precipP75Mm / maxVal * h).toFloat()
            if (bandH > 1f) {
                drawRoundRect(
                    color = colors.chartBand,
                    topLeft = Offset(i * bw + 1f, h - bandH),
                    size = Size(bw - 2f, bandH),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
            val medianH = (hour.precipMedianMm / maxVal * h).toFloat()
            if (medianH > 1f) {
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(i * bw + 2.5f, h - medianH),
                    size = Size(max(1f, bw - 5f), medianH),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
    }
}

/** Confidence-Streifen unter den Diagrammen. */
@Composable
fun ConfidenceStrip(
    levels: List<ConfidenceLevel>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTempoColors.current
    Row(
        modifier = modifier.fillMaxWidth().height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        levels.forEach { level ->
            val c = when (level) {
                ConfidenceLevel.HIGH -> colors.confHigh
                ConfidenceLevel.MODERATE -> colors.confModerate
                ConfidenceLevel.LOW -> colors.confLow
                ConfidenceLevel.VERY_LOW -> colors.confVeryLow
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(c, RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Gezeitenkurve (Spez. §17): Nachtbänder, Gitter, Fläche + Linie,
 * markierte Extreme, Sonnenzeiten, "Agora"-Marker.
 */
@Composable
fun TideCurveChart(
    state: TideState,
    windowStart: LocalDateTime,
    windowHours: Long,
    now: LocalDateTime,
    sunrise: LocalDateTime?,
    sunset: LocalDateTime?,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTempoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val measurer = rememberTextMeasurer()

    val windowEnd = windowStart.plusHours(windowHours)
    val curve = state.curve.filter { !it.time.isBefore(windowStart) && !it.time.isAfter(windowEnd) }

    Canvas(modifier = modifier.fillMaxWidth().height(if (compact) 70.dp else 150.dp)) {
        if (curve.size < 3) return@Canvas
        val w = size.width
        val h = size.height
        val minH = curve.minOf { it.heightM }
        val maxH = curve.maxOf { it.heightM }
        val span = max(0.2, maxH - minH)
        val topPad = if (compact) 8f else 22f
        val bottomPad = 8f

        fun x(t: LocalDateTime): Float =
            (Duration.between(windowStart, t).toMinutes().toFloat() / (windowHours * 60f)) * w

        fun y(height: Double): Float =
            (h - bottomPad) - ((height - minH) / span * (h - topPad - bottomPad)).toFloat()

        // Nachtbänder
        if (sunrise != null) {
            val xr = x(sunrise).coerceIn(0f, w)
            if (xr > 0f) drawRect(colors.nightBand, topLeft = Offset(0f, 0f), size = Size(xr, h))
        }
        if (sunset != null) {
            val xs = x(sunset).coerceIn(0f, w)
            if (xs < w) drawRect(colors.nightBand, topLeft = Offset(xs, 0f), size = Size(w - xs, h))
        }

        // Gitter
        for (frac in listOf(0.25f, 0.5f, 0.75f)) {
            drawLine(grid, Offset(0f, h * frac), Offset(w, h * frac), strokeWidth = 1f)
        }

        // Kurve
        val pts = curve.map { Offset(x(it.time), y(it.heightM)) }
        val line = smoothPath(pts)
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, h)
            lineTo(pts.first().x, h)
            close()
        }
        drawPath(area, colors.chartBand)
        drawPath(line, primary, style = Stroke(width = if (compact) 3.5f else 4.5f))

        // Extreme markieren
        if (!compact) {
            state.events
                .filter { !it.time.isBefore(windowStart) && !it.time.isAfter(windowEnd) }
                .forEach { event ->
                    val cx = x(event.time)
                    val cy = y(event.heightM)
                    drawCircle(surfaceHigh, radius = 9f, center = Offset(cx, cy))
                    drawCircle(primary, radius = 6f, center = Offset(cx, cy))
                    val label = "${formatMeters(event.heightM)} m"
                    val dy = if (event.type == TideEventType.HIGH) -30f else 12f
                    drawText(
                        textMeasurer = measurer,
                        text = label,
                        topLeft = Offset((cx - 22f).coerceIn(0f, w - 56f), (cy + dy).coerceIn(0f, h - 18f)),
                        style = TextStyle(color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    )
                }
        }

        // Agora
        if (!now.isBefore(windowStart) && !now.isAfter(windowEnd)) {
            val nx = x(now)
            drawLine(
                color = textColor,
                start = Offset(nx, topPad),
                end = Offset(nx, h - 2f),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
            )
            val nowHeight = state.currentHeightM
            if (nowHeight != null) {
                drawCircle(surfaceHigh, radius = 10f, center = Offset(nx, y(nowHeight)))
                drawCircle(tertiary, radius = 7f, center = Offset(nx, y(nowHeight)))
            }
            if (!compact) {
                drawText(
                    textMeasurer = measurer,
                    text = "Agora",
                    topLeft = Offset((nx + 8f).coerceAtMost(w - 60f), 2f),
                    style = TextStyle(color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

/**
 * Generische Wertlinie (Temperatur, Wind, Wolken, UV) mit optionaler
 * Zweitlinie (z. B. Böen) und Min/Max-Beschriftung.
 */
@Composable
fun ValueLineChart(
    values: List<Double?>,
    secondaryValues: List<Double?>? = null,
    unit: String = "",
    modifier: Modifier = Modifier,
) {
    val colors = LocalTempoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(130.dp)) {
        val valid = values.filterNotNull()
        if (valid.size < 2) return@Canvas
        val secValid = secondaryValues?.filterNotNull().orEmpty()
        val minV = minOf(valid.min(), secValid.minOrNull() ?: valid.min())
        val maxV = maxOf(valid.max(), secValid.maxOrNull() ?: valid.max())
        val span = max(0.1, maxV - minV)
        val w = size.width
        val h = size.height
        val topPad = 18f
        val bottomPad = 6f

        for (frac in listOf(0.25f, 0.5f, 0.75f)) {
            drawLine(grid, Offset(0f, h * frac), Offset(w, h * frac), strokeWidth = 1f)
        }

        fun toPoints(list: List<Double?>): List<Offset> = list.mapIndexedNotNull { i, v ->
            v?.let {
                Offset(
                    i / (list.size - 1f) * w,
                    (h - bottomPad) - ((it - minV) / span * (h - topPad - bottomPad)).toFloat(),
                )
            }
        }

        secondaryValues?.let { sec ->
            val pts = toPoints(sec)
            if (pts.size >= 2) {
                drawPath(
                    smoothPath(pts), tertiary,
                    style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
                )
            }
        }
        val pts = toPoints(values)
        val line = smoothPath(pts)
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, h)
            lineTo(pts.first().x, h)
            close()
        }
        drawPath(area, colors.chartBand)
        drawPath(line, primary, style = Stroke(width = 4f))

        drawText(
            textMeasurer = measurer,
            text = "${formatMm(maxV)}$unit",
            topLeft = Offset(w - 86f, 0f),
            style = TextStyle(color = textColor, fontSize = 10.sp),
        )
        drawText(
            textMeasurer = measurer,
            text = "${formatMm(minV)}$unit",
            topLeft = Offset(w - 86f, h - 16f),
            style = TextStyle(color = textColor, fontSize = 10.sp),
        )
    }
}

/** Einfache horizontale Balkenzeile für den Modellvergleich. */
@Composable
fun ModelBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.015f, 1f))
                .height(14.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}
