package com.tempo.rn.core.config

/**
 * Zentrale, konfigurierbare Schwellenwerte der Prognose- und Gezeitenlogik (Spez. §6–§11).
 * Alle fachlichen Grenzwerte der App leben hier — nirgendwo sonst.
 */
object ForecastConfig {

    // §7 Regendefinition
    const val RAIN_MIN_MM_PER_HOUR = 0.1
    const val RAIN_PROBABILITY_GATE_PERCENT = 40.0
    const val RAIN_CLASS_LIGHT_MAX = 1.0
    const val RAIN_CLASS_MODERATE_MAX = 5.0
    const val RAIN_CLASS_STRONG_MAX = 15.0

    // §8 Finale Wahrscheinlichkeit
    const val P_MODELS_WEIGHT = 0.60
    const val P_ENSEMBLE_WEIGHT = 0.40

    // §10 Confidence Score
    const val CONF_MODEL_AGREEMENT_WEIGHT = 0.40
    const val CONF_ENSEMBLE_STABILITY_WEIGHT = 0.30
    const val CONF_HISTORICAL_SKILL_WEIGHT = 0.20
    const val CONF_FRESHNESS_WEIGHT = 0.10

    // §11 Divergenz-Schwellen
    const val DIVERGENCE_TEMP_SPAN_C = 3.0
    const val DIVERGENCE_RAIN_SPAN_MM = 5.0
    const val DIVERGENCE_WIND_SPAN_KMH = 15.0
    const val DIVERGENCE_ONSET_HOURS = 2
    const val DIVERGENCE_AGREEMENT_PERCENT = 65.0

    // §6.3 Modellgewichtung
    const val WEIGHT_MIN = 0.40
    const val WEIGHT_MAX = 1.20
    const val WEIGHT_MAX_DAILY_CHANGE = 0.05
    const val WEIGHT_ESTIMATED_OBSERVATION_FACTOR = 0.5
    const val WEIGHT_MIN_DAYS_BEFORE_ADAPTATION = 7

    // Horizonte & Cache
    const val DETAIL_HORIZON_HOURS = 96
    const val DAILY_VIEW_DAYS = 4
    const val CACHE_DAYS = 7

    // Gezeitenerkennung
    const val TIDE_MIN_EXTREMA_SPACING_HOURS = 3.0
    const val TIDE_NEAR_EXTREME_MINUTES = 45

    // Aktualisierung
    const val FORECAST_REFRESH_MINUTES = 60L
    const val MARINE_REFRESH_HOURS = 3L
    const val RADAR_REFRESH_MINUTES = 10L
    const val FRESHNESS_FULL_MINUTES = 90.0
    const val FRESHNESS_ZERO_MINUTES = 360.0

    const val TIMEZONE = "America/Fortaleza"
    const val USER_AGENT = "TempoRN/1.0.0 (Android; previsao local RN Brasil)"
}
