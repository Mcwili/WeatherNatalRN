package com.tempo.rn.core.database

import androidx.room.Entity

/** Stündliche Prognose eines einzelnen Modells (Spez. §34 ModelForecastEntity). */
@Entity(tableName = "model_forecast", primaryKeys = ["locationId", "modelId", "timeIso"])
data class ModelForecastEntity(
    val locationId: String,
    val modelId: String,
    val timeIso: String,
    val temperatureC: Double?,
    val precipitationMm: Double?,
    val windSpeedKmh: Double?,
    val windGustKmh: Double?,
    val windDirectionDeg: Double?,
    val cloudCoverPercent: Double?,
    val fetchedAtIso: String,
)

/** Best-Match-Stunde (UV, Code, Dienst-Wahrscheinlichkeit …). */
@Entity(tableName = "best_match_hourly", primaryKeys = ["locationId", "timeIso"])
data class BestMatchHourlyEntity(
    val locationId: String,
    val timeIso: String,
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
    val fetchedAtIso: String,
)

/** Aggregierte Ensemble-Statistik pro Stunde. */
@Entity(tableName = "ensemble_hourly", primaryKeys = ["locationId", "timeIso"])
data class EnsembleHourlyEntity(
    val locationId: String,
    val timeIso: String,
    val membersTotal: Int,
    val membersRain: Int,
    val precipP25: Double,
    val precipP50: Double,
    val precipP75: Double,
    val precipMin: Double,
    val precipMax: Double,
    val temperatureSpreadC: Double,
    val windSpreadKmh: Double,
    val fetchedAtIso: String,
)

/** Tageswerte. */
@Entity(tableName = "daily_forecast", primaryKeys = ["locationId", "dateIso"])
data class DailyForecastEntity(
    val locationId: String,
    val dateIso: String,
    val weatherCode: Int?,
    val tMinC: Double?,
    val tMaxC: Double?,
    val precipProbabilityMax: Double?,
    val precipSumMm: Double?,
    val uvIndexMax: Double?,
    val windMaxKmh: Double?,
    val windGustMaxKmh: Double?,
    val sunriseIso: String?,
    val sunsetIso: String?,
    val fetchedAtIso: String,
)

/** Marine-Stunde (Spez. §34 MarineForecastEntity). */
@Entity(tableName = "marine_hourly", primaryKeys = ["locationId", "timeIso"])
data class MarineHourlyEntity(
    val locationId: String,
    val timeIso: String,
    val seaLevelM: Double?,
    val waveHeightM: Double?,
    val waveDirectionDeg: Double?,
    val wavePeriodS: Double?,
    val swellHeightM: Double?,
    val swellDirectionDeg: Double?,
    val swellPeriodS: Double?,
    val windWaveHeightM: Double?,
    val seaSurfaceTemperatureC: Double?,
    val fetchedAtIso: String,
)

/** Offizielle oder importierte Gezeitenereignisse (DHN). Leer, solange kein Import vorliegt. */
@Entity(tableName = "tide_event", primaryKeys = ["stationId", "timestampIso"])
data class TideEventEntity(
    val stationId: String,
    val timestampIso: String,
    val type: String, // LOW | HIGH
    val heightM: Double,
    val source: String, // TideDataType-Name
)

/** Modellgewichte je Metrik (Spez. §34 ModelWeightEntity). */
@Entity(tableName = "model_weight", primaryKeys = ["modelId", "metric"])
data class ModelWeightEntity(
    val modelId: String,
    val metric: String, // z. B. "rain_yes_no"
    val weight: Double,
    val hitCount: Int,
    val evalCount: Int,
    val firstEvalDateIso: String?,
    val updatedAtIso: String?,
)

/** Archivierte Prognosen für das Backtesting (Spez. §12). */
@Entity(tableName = "forecast_archive", primaryKeys = ["locationId", "modelId", "targetTimeIso", "leadHours"])
data class ForecastArchiveEntity(
    val locationId: String,
    val modelId: String,
    val targetTimeIso: String,
    val leadHours: Int,
    val precipitationMm: Double?,
    val temperatureC: Double?,
    val verified: Boolean,
)

/** Kleiner Schlüssel/Wert-Speicher (z. B. letzte Aktualisierung je Datenart). */
@Entity(tableName = "meta", primaryKeys = ["key"])
data class MetaEntity(
    val key: String,
    val value: String,
)
