package com.tempo.rn.domain

import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.core.model.TideConfidence
import com.tempo.rn.core.model.TideDataType
import com.tempo.rn.core.model.TideDirection
import com.tempo.rn.core.model.TideEvent
import com.tempo.rn.core.model.TideEventType
import com.tempo.rn.core.model.TidePoint
import com.tempo.rn.core.model.TideState
import java.time.Duration
import java.time.LocalDateTime

/**
 * Spez. §16: Erkennung von Ebbe und Flut aus einer Wasserstandskurve.
 * Sortieren → validieren → glätten → Extrema über Vorzeichenwechsel der
 * ersten Ableitung → Mindestabstände prüfen.
 */
object TideAnalyzer {

    fun analyze(
        rawCurve: List<TidePoint>,
        now: LocalDateTime,
        dataType: TideDataType = TideDataType.MODELLED_SEA_LEVEL,
        referenceStationPt: String = "Porto de Natal",
        updatedAt: LocalDateTime? = null,
    ): TideState {
        val sorted = rawCurve
            .filter { it.heightM.isFinite() }
            .sortedBy { it.time }
            .distinctBy { it.time }
        if (sorted.size < 6) {
            return TideState(
                direction = TideDirection.UNKNOWN,
                currentHeightM = null,
                nextLow = null,
                nextHigh = null,
                events = emptyList(),
                curve = sorted,
                dataType = dataType,
                confidence = TideConfidence.UNKNOWN,
                referenceStationPt = referenceStationPt,
                updatedAt = updatedAt,
            )
        }

        val smoothed = smooth(sorted)
        val events = findExtrema(smoothed, dataType)

        val current = interpolateAt(smoothed, now)
        val direction = direction(smoothed, events, now)
        val nextLow = events.firstOrNull { it.type == TideEventType.LOW && it.time.isAfter(now) }
        val nextHigh = events.firstOrNull { it.type == TideEventType.HIGH && it.time.isAfter(now) }

        val confidence = when (dataType) {
            TideDataType.OFFICIAL_TIDE_TABLE -> TideConfidence.HIGH
            TideDataType.MODELLED_TIDE, TideDataType.MODELLED_SEA_LEVEL -> TideConfidence.MODERATE
            TideDataType.ESTIMATED_LOCAL_TIDE -> TideConfidence.LOW
        }

        return TideState(
            direction = direction,
            currentHeightM = current,
            nextLow = nextLow,
            nextHigh = nextHigh,
            events = events,
            curve = smoothed,
            dataType = dataType,
            confidence = confidence,
            referenceStationPt = referenceStationPt,
            updatedAt = updatedAt,
        )
    }

    /** Vorsichtige Glättung: gleitendes Mittel über 3 Punkte. */
    fun smooth(points: List<TidePoint>): List<TidePoint> {
        if (points.size < 3) return points
        return points.mapIndexed { i, p ->
            if (i == 0 || i == points.lastIndex) p
            else TidePoint(p.time, (points[i - 1].heightM + p.heightM + points[i + 1].heightM) / 3.0)
        }
    }

    /** Lokale Minima/Maxima über Vorzeichenwechsel der ersten Ableitung, mit Mindestabstand. */
    fun findExtrema(points: List<TidePoint>, dataType: TideDataType): List<TideEvent> {
        val events = mutableListOf<TideEvent>()
        if (points.size < 3) return events
        for (i in 1 until points.lastIndex) {
            val prev = points[i - 1].heightM
            val cur = points[i].heightM
            val next = points[i + 1].heightM
            val isMax = cur >= prev && cur > next
            val isMin = cur <= prev && cur < next
            if (!isMax && !isMin) continue
            val type = if (isMax) TideEventType.HIGH else TideEventType.LOW
            val candidate = TideEvent(points[i].time, type, cur, dataType)
            val last = events.lastOrNull()
            if (last != null) {
                val spacing = Duration.between(last.time, candidate.time)
                if (spacing < Duration.ofMinutes((ForecastConfig.TIDE_MIN_EXTREMA_SPACING_HOURS * 60).toLong())) {
                    // Zu nah: bei gleichem Typ das extremere behalten, sonst Kandidat verwerfen
                    if (last.type == type) {
                        val keepCandidate = if (type == TideEventType.HIGH) cur > last.heightM else cur < last.heightM
                        if (keepCandidate) {
                            events.removeAt(events.lastIndex)
                            events.add(candidate)
                        }
                    }
                    continue
                }
                if (last.type == type) {
                    // Zwei gleiche Typen in Folge: das extremere behalten
                    val keepCandidate = if (type == TideEventType.HIGH) cur > last.heightM else cur < last.heightM
                    if (keepCandidate) {
                        events.removeAt(events.lastIndex)
                        events.add(candidate)
                    }
                    continue
                }
            }
            events.add(candidate)
        }
        return events
    }

    fun direction(points: List<TidePoint>, events: List<TideEvent>, now: LocalDateTime): TideDirection {
        val nearEvent = events.minByOrNull { Duration.between(it.time, now).abs() }
        if (nearEvent != null &&
            Duration.between(nearEvent.time, now).abs() <=
            Duration.ofMinutes(ForecastConfig.TIDE_NEAR_EXTREME_MINUTES.toLong())
        ) {
            return if (nearEvent.type == TideEventType.HIGH) TideDirection.NEAR_HIGH else TideDirection.NEAR_LOW
        }
        val before = points.lastOrNull { !it.time.isAfter(now) } ?: return TideDirection.UNKNOWN
        val after = points.firstOrNull { it.time.isAfter(now) } ?: return TideDirection.UNKNOWN
        return when {
            after.heightM > before.heightM -> TideDirection.RISING
            after.heightM < before.heightM -> TideDirection.FALLING
            else -> TideDirection.UNKNOWN
        }
    }

    fun interpolateAt(points: List<TidePoint>, at: LocalDateTime): Double? {
        val before = points.lastOrNull { !it.time.isAfter(at) } ?: return points.firstOrNull()?.heightM
        val after = points.firstOrNull { it.time.isAfter(at) } ?: return before.heightM
        val total = Duration.between(before.time, after.time).toMillis().toDouble()
        if (total <= 0) return before.heightM
        val frac = Duration.between(before.time, at).toMillis() / total
        return before.heightM + (after.heightM - before.heightM) * frac
    }
}
