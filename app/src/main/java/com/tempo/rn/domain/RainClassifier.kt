package com.tempo.rn.domain

import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.core.model.RainClass

object RainClassifier {

    /** Spez. §7: Klassifikation der stündlichen Regenmenge. */
    fun classify(mmPerHour: Double?): RainClass {
        val mm = mmPerHour ?: return RainClass.NONE
        return when {
            mm < ForecastConfig.RAIN_MIN_MM_PER_HOUR -> RainClass.NONE
            mm < ForecastConfig.RAIN_CLASS_LIGHT_MAX -> RainClass.LIGHT
            mm < ForecastConfig.RAIN_CLASS_MODERATE_MAX -> RainClass.MODERATE
            mm < ForecastConfig.RAIN_CLASS_STRONG_MAX -> RainClass.STRONG
            else -> RainClass.VERY_STRONG
        }
    }

    /**
     * Spez. §7: Signalisiert ein Modell Regen?
     * Mindestens eine Bedingung: precipitation/rain/showers >= 0.1 mm oder
     * Modell-Wahrscheinlichkeit >= 40 % bei precipitation > 0.
     */
    fun indicatesRain(precipitationMm: Double?, probabilityPercent: Double? = null): Boolean {
        val precip = precipitationMm
        if (precip != null && precip >= ForecastConfig.RAIN_MIN_MM_PER_HOUR) return true
        if (probabilityPercent != null && precip != null) {
            return probabilityPercent >= ForecastConfig.RAIN_PROBABILITY_GATE_PERCENT && precip > 0.0
        }
        return false
    }
}
