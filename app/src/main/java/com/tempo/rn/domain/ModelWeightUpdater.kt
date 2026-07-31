package com.tempo.rn.domain

import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.core.model.VerificationQuality

/**
 * Spez. §6.3 / §13: transparente, schrittweise Gewichtsanpassung.
 * Max. 5 % Änderung pro Tag, Grenzen 0.40..1.20; bei geschätzten Beobachtungen
 * nur halbe Anpassungsstärke. Keine Blackbox.
 */
object ModelWeightUpdater {

    data class Update(val newWeight: Double, val applied: Boolean)

    /**
     * @param currentWeight aktuelles Gewicht des Modells
     * @param hitRate Trefferquote (0..1) der letzten Auswertungsperiode
     * @param evaluationDays Anzahl Tage mit Auswertungen (Anpassung erst ab 7)
     * @param quality Beobachtungsqualität der Referenz
     */
    fun update(
        currentWeight: Double,
        hitRate: Double,
        evaluationDays: Int,
        quality: VerificationQuality,
    ): Update {
        if (evaluationDays < ForecastConfig.WEIGHT_MIN_DAYS_BEFORE_ADAPTATION) {
            return Update(currentWeight, applied = false)
        }
        // Zielrichtung: Trefferquote über 0.5 erhöht, darunter senkt.
        val direction = (hitRate.coerceIn(0.0, 1.0) - 0.5) * 2.0 // -1..1
        var step = ForecastConfig.WEIGHT_MAX_DAILY_CHANGE * direction
        if (quality == VerificationQuality.ESTIMATED) {
            step *= ForecastConfig.WEIGHT_ESTIMATED_OBSERVATION_FACTOR
        }
        val proposed = currentWeight * (1.0 + step)
        val clamped = proposed.coerceIn(ForecastConfig.WEIGHT_MIN, ForecastConfig.WEIGHT_MAX)
        return Update(clamped, applied = clamped != currentWeight)
    }
}
