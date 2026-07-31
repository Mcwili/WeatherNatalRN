package com.tempo.rn.core.network

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

/** Basis: https://api.open-meteo.com/ */
interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String? = null,
        @Query("daily") daily: String? = null,
        @Query("models") models: String? = null,
        @Query("forecast_days") forecastDays: Int = 5,
        @Query("timezone") timezone: String,
    ): ForecastResponse
}

/** Basis: https://ensemble-api.open-meteo.com/ — dynamische Member-Spalten, daher JsonObject. */
interface OpenMeteoEnsembleApi {
    @GET("v1/ensemble")
    suspend fun ensemble(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String,
        @Query("models") models: String,
        @Query("forecast_days") forecastDays: Int = 5,
        @Query("timezone") timezone: String,
    ): JsonObject
}

/** Basis: https://marine-api.open-meteo.com/ */
interface OpenMeteoMarineApi {
    @GET("v1/marine")
    suspend fun marine(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String,
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("cell_selection") cellSelection: String = "sea",
        @Query("timezone") timezone: String,
    ): MarineResponse
}

/** Basis: https://api.rainviewer.com/ */
interface RainViewerApi {
    @GET("public/weather-maps.json")
    suspend fun weatherMaps(): RainViewerMapsResponse
}
