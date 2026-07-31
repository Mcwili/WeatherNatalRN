package com.tempo.rn.core.network

import com.tempo.rn.core.config.ForecastConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", ForecastConfig.USER_AGENT)
                    .build()
            )
        }
        .build()

    private fun retrofit(client: OkHttpClient, json: Json, baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideForecastApi(client: OkHttpClient, json: Json): OpenMeteoForecastApi =
        retrofit(client, json, "https://api.open-meteo.com/").create(OpenMeteoForecastApi::class.java)

    @Provides
    @Singleton
    fun provideEnsembleApi(client: OkHttpClient, json: Json): OpenMeteoEnsembleApi =
        retrofit(client, json, "https://ensemble-api.open-meteo.com/").create(OpenMeteoEnsembleApi::class.java)

    @Provides
    @Singleton
    fun provideMarineApi(client: OkHttpClient, json: Json): OpenMeteoMarineApi =
        retrofit(client, json, "https://marine-api.open-meteo.com/").create(OpenMeteoMarineApi::class.java)

    @Provides
    @Singleton
    fun provideRainViewerApi(client: OkHttpClient, json: Json): RainViewerApi =
        retrofit(client, json, "https://api.rainviewer.com/").create(RainViewerApi::class.java)
}
