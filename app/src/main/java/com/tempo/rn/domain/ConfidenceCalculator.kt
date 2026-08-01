package com.tempo.rn.domain

import com.tempo.rn.core.config.ForecastConfig
import kotlin.math.roundToInt

object ConfidenceCalculator {

    /**
     * Spez. §10: confidence = 0.40*modelAgreement + 0.30*ensembleStability
     * + 0.20*historicalSkill + 0.10*freshness. Alle Inputs 0..1, Resultat 0..100.
     */
    fun score(
        modelAgreement: Double,
        ensembleStability: Double?,
        historicalSkill: Double,
        freshness: Double,
    ): Int {
        val agreement = modelAgreement.coerceIn(0.0, 1.0)
        val skill = historicalSkill.coerceIn(0.0, 1.0)
        val fresh = freshness.coerceIn(0.0, 1.0)
        // Ohne Ensemble-Daten wird deren Anteil auf Modellübereinstimmung umgelegt,
        // damit fehlende Daten die Skala nicht künstlich drücken.
        val stability = ensembleStability?.coerceIn(0.0, 1.0)
        val raw = if (stability != null) {
            ForecastConfig.CONF_MODEL_AGREEMENT_WEIGHT * agreement +
                ForecastConfig.CONF_ENSEMBLE_STABILITY_WEIGHT * stability +
                ForecastConfig.CONF_HISTORICAL_SKILL_WEIGHT * skill +
                ForecastConfig.CONF_FRESHNESS_WEIGHT * fresh
        } else {
            (ForecastConfig.CONF_MODEL_AGREEMENT_WEIGHT + ForecastConfig.CONF_ENSEMBLE_STABILITY_WEIGHT) * agreement +
                ForecastConfig.CONF_HISTORICAL_SKILL_WEIGHT * skill +
                ForecastConfig.CONF_FRESHNESS_WEIGHT * fresh
        }
        return (raw * 100.0).roundToInt().coerceIn(0, 100)
    }

    /** Frische der Daten: 1.0 bis FRESHNESS_FULL_MINUTES, dann linear auf 0 bei FRESHNESS_ZERO_MINUTES. */
    fun freshness(ageMinutes: Double): Double = when {
        ageMinutes <= ForecastConfig.FRESHNESS_FULL_MINUTES -> 1.0
        ageMinutes >= ForecastConfig.FRESHNESS_ZERO_MINUTES -> 0.0
        else -> 1.0 - (ageMinutes - ForecastConfig.FRESHNESS_FULL_MINUTES) /
            (ForecastConfig.FRESHNESS_ZERO_MINUTES - ForecastConfig.FRESHNESS_FULL_MINUTES)
    }

    /** Ensemble-Stabilität aus Niederschlags-Streuung: enge Spanne = stabil. */
    fun ensembleStability(precipP25: Double, precipP75: Double, precipP50: Double): Double {
        val spread = (precipP75 - precipP25).coerceAtLeast(0.0)
        // Normierung: Spanne relativ zu (Median + 1 mm); 0 mm Spanne -> 1.0, grosse Spanne -> gegen 0.
        val rel = spread / (precipP50 + 1.0)
        return (1.0 / (1.0 + rel * 2.0)).coerceIn(0.0, 1.0)
    }
}
