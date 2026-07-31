package com.tempo.rn.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val hourly: HourlyBlock? = null,
    val daily: DailyBlock? = null,
)

@Serializable
data class HourlyBlock(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?>? = null,
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?>? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity: List<Double?>? = null,
    @SerialName("precipitation") val precipitation: List<Double?>? = null,
    @SerialName("rain") val rain: List<Double?>? = null,
    @SerialName("showers") val showers: List<Double?>? = null,
    @SerialName("precipitation_probability") val precipitationProbability: List<Double?>? = null,
    @SerialName("weather_code") val weatherCode: List<Int?>? = null,
    @SerialName("cloud_cover") val cloudCover: List<Double?>? = null,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>? = null,
    @SerialName("wind_direction_10m") val windDirection: List<Double?>? = null,
    @SerialName("wind_gusts_10m") val windGusts: List<Double?>? = null,
    @SerialName("uv_index") val uvIndex: List<Double?>? = null,
    @SerialName("pressure_msl") val pressureMsl: List<Double?>? = null,
    @SerialName("visibility") val visibility: List<Double?>? = null,
    @SerialName("cape") val cape: List<Double?>? = null,
)

@Serializable
data class DailyBlock(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?>? = null,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?>? = null,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?>? = null,
    @SerialName("precipitation_sum") val precipitationSum: List<Double?>? = null,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Double?>? = null,
    @SerialName("uv_index_max") val uvIndexMax: List<Double?>? = null,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?>? = null,
    @SerialName("wind_gusts_10m_max") val windGustsMax: List<Double?>? = null,
    @SerialName("sunrise") val sunrise: List<String>? = null,
    @SerialName("sunset") val sunset: List<String>? = null,
)

@Serializable
data class MarineResponse(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val hourly: MarineHourlyBlock? = null,
)

@Serializable
data class MarineHourlyBlock(
    val time: List<String> = emptyList(),
    @SerialName("sea_level_height_msl") val seaLevelHeightMsl: List<Double?>? = null,
    @SerialName("wave_height") val waveHeight: List<Double?>? = null,
    @SerialName("wave_direction") val waveDirection: List<Double?>? = null,
    @SerialName("wave_period") val wavePeriod: List<Double?>? = null,
    @SerialName("wind_wave_height") val windWaveHeight: List<Double?>? = null,
    @SerialName("swell_wave_height") val swellWaveHeight: List<Double?>? = null,
    @SerialName("swell_wave_direction") val swellWaveDirection: List<Double?>? = null,
    @SerialName("swell_wave_period") val swellWavePeriod: List<Double?>? = null,
    @SerialName("sea_surface_temperature") val seaSurfaceTemperature: List<Double?>? = null,
)

@Serializable
data class RainViewerMapsResponse(
    val version: String? = null,
    val generated: Long? = null,
    val host: String? = null,
    val radar: RainViewerRadar? = null,
)

@Serializable
data class RainViewerRadar(
    val past: List<RainViewerFrame> = emptyList(),
    val nowcast: List<RainViewerFrame> = emptyList(),
)

@Serializable
data class RainViewerFrame(
    val time: Long = 0,
    val path: String = "",
)
