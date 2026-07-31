package com.tempo.rn.data

import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.core.database.ArchiveDao
import com.tempo.rn.core.database.BestMatchHourlyEntity
import com.tempo.rn.core.database.DailyForecastEntity
import com.tempo.rn.core.database.EnsembleHourlyEntity
import com.tempo.rn.core.database.ForecastArchiveEntity
import com.tempo.rn.core.database.ForecastDao
import com.tempo.rn.core.database.MetaDao
import com.tempo.rn.core.database.MetaEntity
import com.tempo.rn.core.database.ModelForecastEntity
import com.tempo.rn.core.database.WeightDao
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.BestMatchHour
import com.tempo.rn.core.model.ConsolidatedForecast
import com.tempo.rn.core.model.DailySummary
import com.tempo.rn.core.model.EnsembleHourStat
import com.tempo.rn.core.model.ModelHourValue
import com.tempo.rn.core.model.WeatherModel
import com.tempo.rn.core.network.OpenMeteoEnsembleApi
import com.tempo.rn.core.network.OpenMeteoForecastApi
import com.tempo.rn.domain.ForecastConsolidator
import com.tempo.rn.domain.RainClassifier
import com.tempo.rn.domain.WeightedStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

const val METRIC_RAIN = "rain_yes_no"

private const val HOURLY_MODEL_VARS =
    "temperature_2m,precipitation,wind_speed_10m,wind_gusts_10m,wind_direction_10m,cloud_cover"
private const val HOURLY_BEST_VARS =
    "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,precipitation_probability," +
        "weather_code,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index"
private const val DAILY_VARS =
    "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max," +
        "uv_index_max,wind_speed_10m_max,wind_gusts_10m_max,sunrise,sunset"
private const val ENSEMBLE_MODELS = "ecmwf_ifs025,gfs_seamless,icon_seamless"

@Singleton
class WeatherRepository @Inject constructor(
    private val forecastApi: OpenMeteoForecastApi,
    private val ensembleApi: OpenMeteoEnsembleApi,
    private val forecastDao: ForecastDao,
    private val weightDao: WeightDao,
    private val archiveDao: ArchiveDao,
    private val metaDao: MetaDao,
) {

    /** Kompletter Abruf: Best-Match, alle Einzelmodelle, Ensemble. Fehler pro Quelle werden toleriert. */
    suspend fun refresh(location: AppLocation): Result<Unit> {
        val now = TimeUtil.now()
        val nowIso = TimeUtil.format(now)
        var anySuccess = false

        // 1) Best-Match (Tageswerte, UV, Codes, Dienst-Wahrscheinlichkeit)
        runCatching {
            val resp = forecastApi.forecast(
                latitude = location.latitude,
                longitude = location.longitude,
                hourly = HOURLY_BEST_VARS,
                daily = DAILY_VARS,
                forecastDays = ForecastConfig.CACHE_DAYS,
                timezone = ForecastConfig.TIMEZONE,
            )
            val hourly = resp.hourly
            if (hourly != null) {
                val rows = hourly.time.mapIndexedNotNull { i, t ->
                    TimeUtil.parse(t)?.let { time ->
                        BestMatchHourlyEntity(
                            locationId = location.id,
                            timeIso = TimeUtil.format(time),
                            weatherCode = hourly.weatherCode?.getOrNull(i),
                            temperatureC = hourly.temperature?.getOrNull(i),
                            apparentC = hourly.apparentTemperature?.getOrNull(i),
                            precipitationProbability = hourly.precipitationProbability?.getOrNull(i),
                            precipitationMm = hourly.precipitation?.getOrNull(i),
                            uvIndex = hourly.uvIndex?.getOrNull(i),
                            cloudCoverPercent = hourly.cloudCover?.getOrNull(i),
                            windSpeedKmh = hourly.windSpeed?.getOrNull(i),
                            windGustKmh = hourly.windGusts?.getOrNull(i),
                            windDirectionDeg = hourly.windDirection?.getOrNull(i),
                            humidityPercent = hourly.relativeHumidity?.getOrNull(i),
                            fetchedAtIso = nowIso,
                        )
                    }
                }
                forecastDao.upsertBestMatchHours(rows)
            }
            val daily = resp.daily
            if (daily != null) {
                val rows = daily.time.mapIndexed { i, d ->
                    DailyForecastEntity(
                        locationId = location.id,
                        dateIso = d,
                        weatherCode = daily.weatherCode?.getOrNull(i),
                        tMinC = daily.temperatureMin?.getOrNull(i),
                        tMaxC = daily.temperatureMax?.getOrNull(i),
                        precipProbabilityMax = daily.precipitationProbabilityMax?.getOrNull(i),
                        precipSumMm = daily.precipitationSum?.getOrNull(i),
                        uvIndexMax = daily.uvIndexMax?.getOrNull(i),
                        windMaxKmh = daily.windSpeedMax?.getOrNull(i),
                        windGustMaxKmh = daily.windGustsMax?.getOrNull(i),
                        sunriseIso = daily.sunrise?.getOrNull(i),
                        sunsetIso = daily.sunset?.getOrNull(i),
                        fetchedAtIso = nowIso,
                    )
                }
                forecastDao.upsertDaily(rows)
            }
            anySuccess = true
        }

        // 2) Einzelmodelle — jedes Modell isoliert, Ausfälle werden toleriert (Spez. §3.1, §33)
        for (model in WeatherModel.entries) {
            runCatching {
                val resp = forecastApi.forecast(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    hourly = HOURLY_MODEL_VARS,
                    models = model.apiParam,
                    forecastDays = 5,
                    timezone = ForecastConfig.TIMEZONE,
                )
                val hourly = resp.hourly ?: return@runCatching
                val rows = hourly.time.mapIndexedNotNull { i, t ->
                    TimeUtil.parse(t)?.let { time ->
                        ModelForecastEntity(
                            locationId = location.id,
                            modelId = model.id,
                            timeIso = TimeUtil.format(time),
                            temperatureC = hourly.temperature?.getOrNull(i),
                            precipitationMm = hourly.precipitation?.getOrNull(i),
                            windSpeedKmh = hourly.windSpeed?.getOrNull(i),
                            windGustKmh = hourly.windGusts?.getOrNull(i),
                            windDirectionDeg = hourly.windDirection?.getOrNull(i),
                            cloudCoverPercent = hourly.cloudCover?.getOrNull(i),
                            fetchedAtIso = nowIso,
                        )
                    }
                }
                if (rows.isNotEmpty()) {
                    forecastDao.upsertModelHours(rows)
                    archiveForBacktesting(location.id, model.id, rows, now)
                    anySuccess = true
                }
            }
        }

        // 3) Ensemble
        runCatching {
            val obj = ensembleApi.ensemble(
                latitude = location.latitude,
                longitude = location.longitude,
                hourly = "precipitation,temperature_2m,wind_speed_10m",
                models = ENSEMBLE_MODELS,
                forecastDays = 5,
                timezone = ForecastConfig.TIMEZONE,
            )
            val rows = parseEnsemble(obj, location.id, nowIso)
            if (rows.isNotEmpty()) {
                forecastDao.upsertEnsembleHours(rows)
                anySuccess = true
            }
        }

        return if (anySuccess) {
            metaDao.put(MetaEntity("last_update_forecast_${location.id}", nowIso))
            prune(now)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Nenhuma fonte de dados respondeu"))
        }
    }

    /** Ensemble-Antwort mit dynamischen Member-Spalten aggregieren. */
    internal fun parseEnsemble(obj: JsonObject, locationId: String, nowIso: String): List<EnsembleHourlyEntity> {
        val hourly = (obj["hourly"] as? JsonObject) ?: return emptyList()
        val times = (hourly["time"] as? JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
            ?: return emptyList()

        fun memberArrays(prefix: String): List<List<Double?>> =
            hourly.entries
                .filter { it.key == prefix || it.key.startsWith("${prefix}_") }
                .mapNotNull { entry ->
                    (entry.value as? JsonArray)?.map { el ->
                        if (el is JsonNull) null else el.jsonPrimitive.doubleOrNull
                    }
                }

        val precipMembers = memberArrays("precipitation")
        val tempMembers = memberArrays("temperature_2m")
        val windMembers = memberArrays("wind_speed_10m")
        if (precipMembers.isEmpty()) return emptyList()

        return times.mapIndexedNotNull { i, t ->
            val time = TimeUtil.parse(t) ?: return@mapIndexedNotNull null
            val precVals = precipMembers.mapNotNull { it.getOrNull(i) }
            if (precVals.isEmpty()) return@mapIndexedNotNull null
            val tempVals = tempMembers.mapNotNull { it.getOrNull(i) }
            val windVals = windMembers.mapNotNull { it.getOrNull(i) }
            EnsembleHourlyEntity(
                locationId = locationId,
                timeIso = TimeUtil.format(time),
                membersTotal = precVals.size,
                membersRain = precVals.count { RainClassifier.indicatesRain(it) },
                precipP25 = WeightedStats.quantile(precVals, 0.25),
                precipP50 = WeightedStats.quantile(precVals, 0.50),
                precipP75 = WeightedStats.quantile(precVals, 0.75),
                precipMin = precVals.min(),
                precipMax = precVals.max(),
                temperatureSpreadC = if (tempVals.size >= 2) tempVals.max() - tempVals.min() else 0.0,
                windSpreadKmh = if (windVals.size >= 2) windVals.max() - windVals.min() else 0.0,
                fetchedAtIso = nowIso,
            )
        }
    }

    /** Prognosen für spätere Verifikation archivieren (Leads 6/12/24/48 h). */
    private suspend fun archiveForBacktesting(
        locationId: String,
        modelId: String,
        rows: List<ModelForecastEntity>,
        now: LocalDateTime,
    ) {
        val leads = listOf(6, 12, 24, 48)
        val archive = mutableListOf<ForecastArchiveEntity>()
        for (lead in leads) {
            val target = now.plusHours(lead.toLong()).withMinute(0).withSecond(0).withNano(0)
            val targetIso = TimeUtil.format(target)
            rows.firstOrNull { it.timeIso == targetIso }?.let { row ->
                archive += ForecastArchiveEntity(
                    locationId = locationId,
                    modelId = modelId,
                    targetTimeIso = targetIso,
                    leadHours = lead,
                    precipitationMm = row.precipitationMm,
                    temperatureC = row.temperatureC,
                    verified = false,
                )
            }
        }
        if (archive.isNotEmpty()) archiveDao.insertAll(archive)
    }

    private suspend fun prune(now: LocalDateTime) {
        val cutoff = TimeUtil.format(now.minusDays(2))
        forecastDao.pruneModelHours(cutoff)
        forecastDao.pruneBestMatch(cutoff)
        forecastDao.pruneEnsemble(cutoff)
        forecastDao.pruneDaily(now.toLocalDate().minusDays(2).toString())
        archiveDao.prune(TimeUtil.format(now.minusDays(90)))
    }

    /** Konsolidierte Prognose als Flow — reagiert auf DB-Änderungen und Gewichts-Updates. */
    fun observeConsolidated(location: AppLocation): Flow<ConsolidatedForecast> {
        val fromIso = TimeUtil.format(TimeUtil.now().minusHours(1))
        return combine(
            forecastDao.observeModelHours(location.id, fromIso),
            forecastDao.observeBestMatchHours(location.id, fromIso),
            forecastDao.observeEnsembleHours(location.id, fromIso),
            weightDao.observeByMetric(METRIC_RAIN),
            metaDao.observe("last_update_forecast_${location.id}"),
        ) { modelRows, bestRows, ensembleRows, weightRows, lastUpdate ->
            val modelHours = modelRows.mapNotNull { row ->
                val model = WeatherModel.byId(row.modelId) ?: return@mapNotNull null
                val time = TimeUtil.parse(row.timeIso) ?: return@mapNotNull null
                ModelHourValue(
                    model = model,
                    time = time,
                    temperatureC = row.temperatureC,
                    precipitationMm = row.precipitationMm,
                    windSpeedKmh = row.windSpeedKmh,
                    windGustKmh = row.windGustKmh,
                    windDirectionDeg = row.windDirectionDeg,
                    cloudCoverPercent = row.cloudCoverPercent,
                )
            }
            val best = bestRows.mapNotNull { row ->
                TimeUtil.parse(row.timeIso)?.let { time ->
                    BestMatchHour(
                        time = time,
                        weatherCode = row.weatherCode,
                        temperatureC = row.temperatureC,
                        apparentC = row.apparentC,
                        precipitationProbability = row.precipitationProbability,
                        precipitationMm = row.precipitationMm,
                        uvIndex = row.uvIndex,
                        cloudCoverPercent = row.cloudCoverPercent,
                        windSpeedKmh = row.windSpeedKmh,
                        windGustKmh = row.windGustKmh,
                        windDirectionDeg = row.windDirectionDeg,
                        humidityPercent = row.humidityPercent,
                    )
                }
            }
            val ensemble = ensembleRows.mapNotNull { row ->
                TimeUtil.parse(row.timeIso)?.let { time ->
                    EnsembleHourStat(
                        time = time,
                        membersTotal = row.membersTotal,
                        membersRain = row.membersRain,
                        precipP25 = row.precipP25,
                        precipP50 = row.precipP50,
                        precipP75 = row.precipP75,
                        precipMin = row.precipMin,
                        precipMax = row.precipMax,
                        temperatureSpreadC = row.temperatureSpreadC,
                        windSpreadKmh = row.windSpreadKmh,
                    )
                }
            }
            val weights = ForecastConsolidator.Weights(
                rainWeight = WeatherModel.entries.associateWith { m ->
                    weightRows.firstOrNull { it.modelId == m.id }?.weight ?: m.startWeight
                },
                rainSkill = weightRows.mapNotNull { row ->
                    val model = WeatherModel.byId(row.modelId) ?: return@mapNotNull null
                    if (row.evalCount > 0) model to row.hitCount.toDouble() / row.evalCount else null
                }.toMap(),
            )
            val fetchedAt = lastUpdate?.let { TimeUtil.parse(it) }
            val now = TimeUtil.now()
            val (hours, warnings) = ForecastConsolidator.consolidate(
                modelHours = modelHours,
                ensembleStats = ensemble,
                bestMatch = best,
                weights = weights,
                fetchedAt = fetchedAt,
                now = now,
            )
            ConsolidatedForecast(
                locationId = location.id,
                hours = hours.filter { !it.time.isBefore(now.minusHours(1)) }
                    .take(ForecastConfig.DETAIL_HORIZON_HOURS),
                warnings = warnings,
                fetchedAt = fetchedAt,
            )
        }
    }

    fun observeDaily(location: AppLocation): Flow<List<DailySummary>> =
        forecastDao.observeDaily(location.id, TimeUtil.today().toString()).let { flow ->
            flow.map { rows ->
                rows.mapNotNull { row ->
                    runCatching { LocalDate.parse(row.dateIso) }.getOrNull()?.let { date ->
                        DailySummary(
                            date = date,
                            weatherCode = row.weatherCode,
                            tMinC = row.tMinC,
                            tMaxC = row.tMaxC,
                            precipProbabilityMaxPercent = row.precipProbabilityMax,
                            precipSumMm = row.precipSumMm,
                            uvIndexMax = row.uvIndexMax,
                            windMaxKmh = row.windMaxKmh,
                            windGustMaxKmh = row.windGustMaxKmh,
                            sunrise = row.sunriseIso?.let { TimeUtil.parse(it) },
                            sunset = row.sunsetIso?.let { TimeUtil.parse(it) },
                        )
                    }
                }
            }
        }

    fun observeModelHours(location: AppLocation): Flow<List<ModelHourValue>> {
        val fromIso = TimeUtil.format(TimeUtil.now().minusHours(1))
        return forecastDao.observeModelHours(location.id, fromIso).map { rows ->
            rows.mapNotNull { row ->
                val model = WeatherModel.byId(row.modelId) ?: return@mapNotNull null
                val time = TimeUtil.parse(row.timeIso) ?: return@mapNotNull null
                ModelHourValue(model, time, row.temperatureC, row.precipitationMm,
                    row.windSpeedKmh, row.windGustKmh, row.windDirectionDeg, row.cloudCoverPercent)
            }
        }
    }

    fun observeWeights() = weightDao.observeByMetric(METRIC_RAIN)

    fun observeLastUpdate(location: AppLocation): Flow<String?> =
        metaDao.observe("last_update_forecast_${location.id}")
}
