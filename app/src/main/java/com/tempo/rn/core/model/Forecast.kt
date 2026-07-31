package com.tempo.rn.core.model

import java.time.LocalDate
import java.time.LocalDateTime

/** Stündlicher Wert eines einzelnen Modells nach Normalisierung. */
data class ModelHourValue(
    val model: WeatherModel,
    val time: LocalDateTime,
    val temperatureC: Double?,
    val precipitationMm: Double?,
    val windSpeedKmh: Double?,
    val windGustKmh: Double?,
    val windDirectionDeg: Double?,
    val cloudCoverPercent: Double?,
)

/** Aggregierte Ensemble-Statistik einer Stunde (aus Mitgliedern vorverdichtet). */
data class EnsembleHourStat(
    val time: LocalDateTime,
    val membersTotal: Int,
    val membersRain: Int,
    val precipP25: Double,
    val precipP50: Double,
    val precipP75: Double,
    val precipMin: Double,
    val precipMax: Double,
    val temperatureSpreadC: Double,
    val windSpreadKmh: Double,
)

/** Zusatzwerte aus dem Best-Match-Abruf (UV, Code, Wahrscheinlichkeit des Dienstes …). */
data class BestMatchHour(
    val time: LocalDateTime,
    val weatherCode: Int?,
    val temperatureC: Double?,
    val apparentC: Double?,
    val precipitationProbability: Double?,
    val precipitationMm: Double?,
    val uvIndex: Double?,
    val cloudCoverPercent: Double?,
    val windSpeedKmh: Double?,
    val windGustKmh: Double?,
    val windDirectionDeg: Double?,
    val humidityPercent: Double?,
)

data class DivergenceWarning(val messagePt: String)

/** Konsolidierte Stunde — das Kernprodukt der App (Spez. §6–§11). */
data class ConsolidatedHour(
    val time: LocalDateTime,
    val temperatureC: Double?,
    val apparentC: Double?,
    val weatherCode: Int?,
    val rainProbabilityPercent: Int,
    val precipMedianMm: Double,
    val precipP25Mm: Double,
    val precipP75Mm: Double,
    val precipMinMm: Double,
    val precipMaxMm: Double,
    val rainClass: RainClass,
    val windSpeedKmh: Double?,
    val windGustKmh: Double?,
    val windDirectionDeg: Double?,
    val cloudCoverPercent: Double?,
    val uvIndex: Double?,
    val confidenceScore: Int,
    val confidenceLevel: ConfidenceLevel,
    val modelsIndicatingRain: Int,
    val modelsTotal: Int,
)

data class ConsolidatedForecast(
    val locationId: String,
    val hours: List<ConsolidatedHour>,
    val warnings: List<DivergenceWarning>,
    val fetchedAt: LocalDateTime?,
)

data class DailySummary(
    val date: LocalDate,
    val weatherCode: Int?,
    val tMinC: Double?,
    val tMaxC: Double?,
    val precipProbabilityMaxPercent: Double?,
    val precipSumMm: Double?,
    val uvIndexMax: Double?,
    val windMaxKmh: Double?,
    val windGustMaxKmh: Double?,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
)
