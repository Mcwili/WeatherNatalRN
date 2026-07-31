package com.tempo.rn.domain

import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.core.model.BestMatchHour
import com.tempo.rn.core.model.ConfidenceLevel
import com.tempo.rn.core.model.ConsolidatedHour
import com.tempo.rn.core.model.DivergenceWarning
import com.tempo.rn.core.model.EnsembleHourStat
import com.tempo.rn.core.model.ModelHourValue
import com.tempo.rn.core.model.WeatherModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Kern der Prognose (Spez. §5–§11): Modellvergleich, gewichteter Median,
 * Regenwahrscheinlichkeit, Confidence und Divergenzwarnungen.
 * Kein einfacher Durchschnitt.
 */
object ForecastConsolidator {

    data class Weights(
        val rainWeight: Map<WeatherModel, Double>,
        val rainSkill: Map<WeatherModel, Double>,
    ) {
        companion object {
            fun defaults(): Weights = Weights(
                rainWeight = WeatherModel.entries.associateWith { it.startWeight },
                rainSkill = emptyMap(),
            )
        }
    }

    fun consolidate(
        modelHours: List<ModelHourValue>,
        ensembleStats: List<EnsembleHourStat>,
        bestMatch: List<BestMatchHour>,
        weights: Weights,
        fetchedAt: LocalDateTime?,
        now: LocalDateTime,
    ): Pair<List<ConsolidatedHour>, List<DivergenceWarning>> {
        if (modelHours.isEmpty() && bestMatch.isEmpty()) return emptyList<ConsolidatedHour>() to emptyList()

        val byTime = modelHours.groupBy { it.time }
        val ensembleByTime = ensembleStats.associateBy { it.time }
        val bestByTime = bestMatch.associateBy { it.time }
        val times = (byTime.keys + bestByTime.keys).toSortedSet()

        val ageMinutes = fetchedAt?.let { java.time.Duration.between(it, now).toMinutes().toDouble() } ?: Double.MAX_VALUE
        val freshness = ConfidenceCalculator.freshness(ageMinutes)
        val historicalSkill = if (weights.rainSkill.isEmpty()) 0.7
        else weights.rainSkill.values.average().coerceIn(0.0, 1.0)

        val hours = ArrayList<ConsolidatedHour>(times.size)
        for (time in times) {
            val models = byTime[time].orEmpty()
            val ensemble = ensembleByTime[time]
            val best = bestByTime[time]
            hours += consolidateHour(models, ensemble, best, weights, historicalSkill, freshness, time)
        }

        val warnings = divergenceWarnings(hours, byTime, now)
        return hours to warnings
    }

    private fun consolidateHour(
        models: List<ModelHourValue>,
        ensemble: EnsembleHourStat?,
        best: BestMatchHour?,
        weights: Weights,
        historicalSkill: Double,
        freshness: Double,
        time: LocalDateTime,
    ): ConsolidatedHour {
        val valid = models.filter { it.precipitationMm != null || it.temperatureC != null }

        // §8.1 modellbasierte Wahrscheinlichkeit
        val rainVotes = valid.mapNotNull { m ->
            val w = weights.rainWeight[m.model] ?: m.model.startWeight
            m.precipitationMm?.let { Triple(m.model, RainClassifier.indicatesRain(it), w) }
        }
        val weightSum = rainVotes.sumOf { it.third }
        val pModels = if (weightSum > 0) rainVotes.sumOf { if (it.second) it.third else 0.0 } / weightSum else null

        // §8.2 Ensemble-Wahrscheinlichkeit
        val pEnsemble = ensemble?.takeIf { it.membersTotal > 0 }
            ?.let { it.membersRain.toDouble() / it.membersTotal }

        // §8.3 finale Wahrscheinlichkeit; Fallback: Dienst-Wahrscheinlichkeit des Best-Match
        val pFinal = when {
            pModels != null && pEnsemble != null ->
                ForecastConfig.P_MODELS_WEIGHT * pModels + ForecastConfig.P_ENSEMBLE_WEIGHT * pEnsemble
            pModels != null -> pModels
            pEnsemble != null -> pEnsemble
            else -> (best?.precipitationProbability ?: 0.0) / 100.0
        }.coerceIn(0.0, 1.0)

        // §9 Regenmenge: gewichteter Median + Bandbreite (bevorzugt Ensemble-Quantile)
        val precipValues = valid.mapNotNull { m ->
            m.precipitationMm?.let { it to (weights.rainWeight[m.model] ?: m.model.startWeight) }
        }
        val median = if (precipValues.isNotEmpty()) {
            WeightedStats.weightedMedian(precipValues.map { it.first }, precipValues.map { it.second })
        } else ensemble?.precipP50 ?: best?.precipitationMm ?: 0.0
        val p25 = ensemble?.precipP25 ?: WeightedStats.quantile(precipValues.map { it.first }, 0.25)
        val p75 = ensemble?.precipP75 ?: WeightedStats.quantile(precipValues.map { it.first }, 0.75)
        val minMm = ensemble?.precipMin ?: (precipValues.minOfOrNull { it.first } ?: 0.0)
        val maxMm = ensemble?.precipMax ?: (precipValues.maxOfOrNull { it.first } ?: 0.0)

        // §6.1 Modellübereinstimmung: Anteil der (gewichteten) Mehrheitsseite
        val agreement = if (weightSum > 0) {
            val rainShare = rainVotes.sumOf { if (it.second) it.third else 0.0 } / weightSum
            maxOf(rainShare, 1.0 - rainShare)
        } else 0.5

        val stability = ensemble?.let {
            ConfidenceCalculator.ensembleStability(it.precipP25, it.precipP75, it.precipP50)
        }
        val confidence = ConfidenceCalculator.score(agreement, stability, historicalSkill, freshness)

        val temps = valid.mapNotNull { it.temperatureC }
        val winds = valid.mapNotNull { it.windSpeedKmh }
        val gusts = valid.mapNotNull { it.windGustKmh }
        val clouds = valid.mapNotNull { it.cloudCoverPercent }
        val dirs = valid.mapNotNull { it.windDirectionDeg }

        return ConsolidatedHour(
            time = time,
            temperatureC = temps.takeIf { it.isNotEmpty() }?.average() ?: best?.temperatureC,
            apparentC = best?.apparentC,
            weatherCode = best?.weatherCode,
            rainProbabilityPercent = (pFinal * 100).roundToInt().coerceIn(0, 100),
            precipMedianMm = median,
            precipP25Mm = minOf(p25, median),
            precipP75Mm = maxOf(p75, median),
            precipMinMm = minMm,
            precipMaxMm = maxOf(maxMm, median),
            rainClass = RainClassifier.classify(median),
            windSpeedKmh = winds.takeIf { it.isNotEmpty() }?.average() ?: best?.windSpeedKmh,
            windGustKmh = gusts.takeIf { it.isNotEmpty() }?.max() ?: best?.windGustKmh,
            windDirectionDeg = dirs.takeIf { it.isNotEmpty() }?.let { circularMean(it) } ?: best?.windDirectionDeg,
            cloudCoverPercent = clouds.takeIf { it.isNotEmpty() }?.average() ?: best?.cloudCoverPercent,
            uvIndex = best?.uvIndex,
            confidenceScore = confidence,
            confidenceLevel = ConfidenceLevel.fromScore(confidence),
            modelsIndicatingRain = rainVotes.count { it.second },
            modelsTotal = rainVotes.size,
        )
    }

    /** §11 Divergenzwarnungen für die nächsten 24 h. */
    private fun divergenceWarnings(
        hours: List<ConsolidatedHour>,
        byTime: Map<LocalDateTime, List<ModelHourValue>>,
        now: LocalDateTime,
    ): List<DivergenceWarning> {
        val warnings = mutableListOf<DivergenceWarning>()
        val window = hours.filter { !it.time.isBefore(now) && it.time.isBefore(now.plusHours(24)) }
        if (window.isEmpty()) return warnings
        val hourFmt = DateTimeFormatter.ofPattern("H'h'")

        // Uneinigkeit über Regen ja/nein
        val split = window.filter {
            it.modelsTotal >= 3 && it.modelsIndicatingRain > 0 && it.modelsIndicatingRain < it.modelsTotal &&
                (maxOf(it.modelsIndicatingRain, it.modelsTotal - it.modelsIndicatingRain).toDouble() / it.modelsTotal) * 100 <
                ForecastConfig.DIVERGENCE_AGREEMENT_PERCENT
        }
        if (split.isNotEmpty()) {
            val first = split.first()
            val last = split.last()
            warnings += DivergenceWarning(
                "Os modelos divergem sobre a chuva entre ${first.time.format(hourFmt)} e ${last.time.plusHours(1).format(hourFmt)}."
            )
            warnings += DivergenceWarning(
                "${first.modelsIndicatingRain} de ${first.modelsTotal} modelos indicam chuva."
            )
        }

        // Spannen prüfen
        for (h in window) {
            val models = byTime[h.time].orEmpty()
            val temps = models.mapNotNull { it.temperatureC }
            val precs = models.mapNotNull { it.precipitationMm }
            val winds = models.mapNotNull { it.windSpeedKmh }
            if (precs.size >= 2 && (precs.max() - precs.min()) > ForecastConfig.DIVERGENCE_RAIN_SPAN_MM) {
                warnings += DivergenceWarning("Há grande variação na quantidade prevista.")
                break
            }
            if (temps.size >= 2 && (temps.max() - temps.min()) > ForecastConfig.DIVERGENCE_TEMP_SPAN_C) {
                warnings += DivergenceWarning("Os modelos divergem sobre a temperatura.")
                break
            }
            if (winds.size >= 2 && (winds.max() - winds.min()) > ForecastConfig.DIVERGENCE_WIND_SPAN_KMH) {
                warnings += DivergenceWarning("Os modelos divergem sobre o vento.")
                break
            }
        }
        return warnings.distinctBy { it.messagePt }
    }

    private fun circularMean(degrees: List<Double>): Double {
        val rad = degrees.map { Math.toRadians(it) }
        val x = rad.sumOf { Math.cos(it) }
        val y = rad.sumOf { Math.sin(it) }
        val mean = Math.toDegrees(Math.atan2(y, x))
        return (mean + 360.0) % 360.0
    }
}
