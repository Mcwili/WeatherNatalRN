package com.tempo.rn.data

import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.core.database.MarineDao
import com.tempo.rn.core.database.MarineHourlyEntity
import com.tempo.rn.core.database.MetaDao
import com.tempo.rn.core.database.MetaEntity
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.MarineHour
import com.tempo.rn.core.network.OpenMeteoMarineApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val MARINE_VARS =
    "sea_level_height_msl,wave_height,wave_direction,wave_period,wind_wave_height," +
        "swell_wave_height,swell_wave_direction,swell_wave_period,sea_surface_temperature"

@Singleton
class MarineRepository @Inject constructor(
    private val marineApi: OpenMeteoMarineApi,
    private val marineDao: MarineDao,
    private val metaDao: MetaDao,
) {

    suspend fun refresh(location: AppLocation): Result<Unit> = runCatching {
        val nowIso = TimeUtil.format(TimeUtil.now())
        val resp = marineApi.marine(
            latitude = location.latitude,
            longitude = location.longitude,
            hourly = MARINE_VARS,
            forecastDays = 7,
            timezone = ForecastConfig.TIMEZONE,
        )
        val hourly = resp.hourly ?: error("Resposta marine vazia")
        val rows = hourly.time.mapIndexedNotNull { i, t ->
            TimeUtil.parse(t)?.let { time ->
                MarineHourlyEntity(
                    locationId = location.id,
                    timeIso = TimeUtil.format(time),
                    seaLevelM = hourly.seaLevelHeightMsl?.getOrNull(i),
                    waveHeightM = hourly.waveHeight?.getOrNull(i),
                    waveDirectionDeg = hourly.waveDirection?.getOrNull(i),
                    wavePeriodS = hourly.wavePeriod?.getOrNull(i),
                    swellHeightM = hourly.swellWaveHeight?.getOrNull(i),
                    swellDirectionDeg = hourly.swellWaveDirection?.getOrNull(i),
                    swellPeriodS = hourly.swellWavePeriod?.getOrNull(i),
                    windWaveHeightM = hourly.windWaveHeight?.getOrNull(i),
                    seaSurfaceTemperatureC = hourly.seaSurfaceTemperature?.getOrNull(i),
                    fetchedAtIso = nowIso,
                )
            }
        }
        if (rows.isEmpty()) error("Sem dados marine")
        marineDao.upsert(rows)
        marineDao.prune(TimeUtil.format(TimeUtil.now().minusDays(2)))
        metaDao.put(MetaEntity("last_update_marine_${location.id}", nowIso))
    }

    fun observe(location: AppLocation): Flow<List<MarineHour>> {
        val fromIso = TimeUtil.format(TimeUtil.now().minusHours(14))
        return marineDao.observe(location.id, fromIso).map { rows ->
            rows.mapNotNull { row ->
                TimeUtil.parse(row.timeIso)?.let { time ->
                    MarineHour(
                        time = time,
                        seaLevelM = row.seaLevelM,
                        waveHeightM = row.waveHeightM,
                        waveDirectionDeg = row.waveDirectionDeg,
                        wavePeriodS = row.wavePeriodS,
                        swellHeightM = row.swellHeightM,
                        swellDirectionDeg = row.swellDirectionDeg,
                        swellPeriodS = row.swellPeriodS,
                        windWaveHeightM = row.windWaveHeightM,
                        seaSurfaceTemperatureC = row.seaSurfaceTemperatureC,
                    )
                }
            }
        }
    }

    fun observeLastUpdate(location: AppLocation): Flow<String?> =
        metaDao.observe("last_update_marine_${location.id}")
}
