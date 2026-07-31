package com.tempo.rn.domain

import com.tempo.rn.core.model.EnsembleHourStat
import com.tempo.rn.core.model.ModelHourValue
import com.tempo.rn.core.model.WeatherModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ForecastConsolidatorTest {

    private val t0: LocalDateTime = LocalDateTime.of(2026, 7, 31, 15, 0)

    private fun hour(model: WeatherModel, precip: Double?, temp: Double? = 27.0) = ModelHourValue(
        model = model,
        time = t0,
        temperatureC = temp,
        precipitationMm = precip,
        windSpeedKmh = 15.0,
        windGustKmh = 25.0,
        windDirectionDeg = 90.0,
        cloudCoverPercent = 50.0,
    )

    @Test
    fun `probabilidade ponderada dos modelos`() {
        // Zwei Modelle mit Regen (Gewichte 1.0, 0.9), zwei ohne (0.85, 0.8)
        val models = listOf(
            hour(WeatherModel.ECMWF_IFS, 1.0),
            hour(WeatherModel.ICON, 0.5),
            hour(WeatherModel.GFS, 0.0),
            hour(WeatherModel.GEM, 0.0),
        )
        val weights = ForecastConsolidator.Weights(
            rainWeight = mapOf(
                WeatherModel.ECMWF_IFS to 1.0,
                WeatherModel.ICON to 0.9,
                WeatherModel.GFS to 0.85,
                WeatherModel.GEM to 0.8,
            ),
            rainSkill = emptyMap(),
        )
        val (hours, _) = ForecastConsolidator.consolidate(
            modelHours = models,
            ensembleStats = emptyList(),
            bestMatch = emptyList(),
            weights = weights,
            fetchedAt = t0,
            now = t0,
        )
        assertEquals(1, hours.size)
        // P = (1.0+0.9)/(1.0+0.9+0.85+0.8) = 1.9/3.55 = 0.535 -> 54%
        assertEquals(54, hours[0].rainProbabilityPercent)
        assertEquals(2, hours[0].modelsIndicatingRain)
        assertEquals(4, hours[0].modelsTotal)
    }

    @Test
    fun `combinacao 60-40 com ensemble`() {
        val models = listOf(
            hour(WeatherModel.ECMWF_IFS, 1.0),
            hour(WeatherModel.ICON, 1.0),
        )
        val ensemble = EnsembleHourStat(
            time = t0,
            membersTotal = 10,
            membersRain = 5,
            precipP25 = 0.2,
            precipP50 = 0.8,
            precipP75 = 1.5,
            precipMin = 0.0,
            precipMax = 3.0,
            temperatureSpreadC = 1.0,
            windSpreadKmh = 5.0,
        )
        val (hours, _) = ForecastConsolidator.consolidate(
            modelHours = models,
            ensembleStats = listOf(ensemble),
            bestMatch = emptyList(),
            weights = ForecastConsolidator.Weights.defaults(),
            fetchedAt = t0,
            now = t0,
        )
        // P_models = 1.0, P_ensemble = 0.5 -> 0.6*1.0 + 0.4*0.5 = 0.8
        assertEquals(80, hours[0].rainProbabilityPercent)
        // Bandbreite kommt aus dem Ensemble
        assertEquals(0.2, hours[0].precipP25Mm, 1e-9)
        assertEquals(1.5, hours[0].precipP75Mm, 1e-9)
    }

    @Test
    fun `mediana ponderada nao e media simples`() {
        val models = listOf(
            hour(WeatherModel.ECMWF_IFS, 0.0),
            hour(WeatherModel.ECMWF_AIFS, 0.0),
            hour(WeatherModel.ICON, 6.0),
        )
        val (hours, _) = ForecastConsolidator.consolidate(
            modelHours = models,
            ensembleStats = emptyList(),
            bestMatch = emptyList(),
            weights = ForecastConsolidator.Weights.defaults(),
            fetchedAt = t0,
            now = t0,
        )
        // Median (gewichtet) liegt bei 0.0 — ein Ausreisser zieht die Menge nicht hoch
        assertEquals(0.0, hours[0].precipMedianMm, 1e-9)
    }

    @Test
    fun `modelos ausentes sao tolerados`() {
        val models = listOf(hour(WeatherModel.ECMWF_IFS, 2.0))
        val (hours, _) = ForecastConsolidator.consolidate(
            modelHours = models,
            ensembleStats = emptyList(),
            bestMatch = emptyList(),
            weights = ForecastConsolidator.Weights.defaults(),
            fetchedAt = t0,
            now = t0,
        )
        assertEquals(1, hours.size)
        assertEquals(100, hours[0].rainProbabilityPercent)
    }

    @Test
    fun `aviso de divergencia quando modelos discordam`() {
        val models = listOf(
            hour(WeatherModel.ECMWF_IFS, 6.0),
            hour(WeatherModel.ICON, 0.0),
            hour(WeatherModel.GFS, 0.0),
            hour(WeatherModel.GEM, 5.5),
        )
        val (_, warnings) = ForecastConsolidator.consolidate(
            modelHours = models,
            ensembleStats = emptyList(),
            bestMatch = emptyList(),
            weights = ForecastConsolidator.Weights.defaults(),
            fetchedAt = t0,
            now = t0,
        )
        assertTrue(warnings.isNotEmpty())
    }

    @Test
    fun `confianca cai com dados velhos`() {
        val models = listOf(
            hour(WeatherModel.ECMWF_IFS, 1.0),
            hour(WeatherModel.ICON, 1.0),
        )
        val fresh = ForecastConsolidator.consolidate(
            models, emptyList(), emptyList(),
            ForecastConsolidator.Weights.defaults(), fetchedAt = t0, now = t0,
        ).first[0].confidenceScore
        val stale = ForecastConsolidator.consolidate(
            models, emptyList(), emptyList(),
            ForecastConsolidator.Weights.defaults(), fetchedAt = t0.minusHours(8), now = t0,
        ).first[0].confidenceScore
        assertTrue(fresh > stale)
    }
}
