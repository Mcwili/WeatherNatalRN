package com.tempo.rn.core.model

import java.time.Duration
import java.time.LocalDateTime

data class TideEvent(
    val time: LocalDateTime,
    val type: TideEventType,
    val heightM: Double,
    val dataType: TideDataType,
)

data class TidePoint(val time: LocalDateTime, val heightM: Double)

data class TideState(
    val direction: TideDirection,
    val currentHeightM: Double?,
    val nextLow: TideEvent?,
    val nextHigh: TideEvent?,
    val events: List<TideEvent>,
    val curve: List<TidePoint>,
    val dataType: TideDataType,
    val confidence: TideConfidence,
    val referenceStationPt: String,
    val updatedAt: LocalDateTime?,
) {
    fun timeTo(event: TideEvent?, now: LocalDateTime): Duration? =
        event?.let { Duration.between(now, it.time).takeIf { d -> !d.isNegative } }
}

data class MarineHour(
    val time: LocalDateTime,
    val seaLevelM: Double?,
    val waveHeightM: Double?,
    val waveDirectionDeg: Double?,
    val wavePeriodS: Double?,
    val swellHeightM: Double?,
    val swellDirectionDeg: Double?,
    val swellPeriodS: Double?,
    val windWaveHeightM: Double?,
    val seaSurfaceTemperatureC: Double?,
)
