package com.tempo.rn.core.model

enum class RainClass(val labelPt: String) {
    NONE("Sem chuva"),
    LIGHT("Chuva fraca"),
    MODERATE("Chuva moderada"),
    STRONG("Chuva forte"),
    VERY_STRONG("Chuva muito forte"),
}

enum class ConfidenceLevel(val labelPt: String) {
    HIGH("Confiança alta"),
    MODERATE("Confiança moderada"),
    LOW("Confiança baixa"),
    VERY_LOW("Confiança muito baixa");

    companion object {
        fun fromScore(score: Int): ConfidenceLevel = when {
            score >= 80 -> HIGH
            score >= 60 -> MODERATE
            score >= 40 -> LOW
            else -> VERY_LOW
        }
    }
}

enum class TideDataType {
    OFFICIAL_TIDE_TABLE,
    MODELLED_TIDE,
    MODELLED_SEA_LEVEL,
    ESTIMATED_LOCAL_TIDE,
}

enum class TideDirection(val labelPt: String) {
    RISING("Maré subindo"),
    FALLING("Maré descendo"),
    NEAR_HIGH("Próxima da maré alta"),
    NEAR_LOW("Próxima da maré baixa"),
    UNKNOWN("Situação da maré indisponível"),
}

enum class TideConfidence(val labelPt: String) {
    HIGH("Confiança alta"),
    MODERATE("Confiança moderada"),
    LOW("Confiança baixa"),
    UNKNOWN("Qualidade desconhecida"),
}

enum class TideEventType { LOW, HIGH }

enum class VerificationQuality { OBSERVED, ESTIMATED }

enum class UvCategory(val labelPt: String, val advicePt: String) {
    LOW("Baixo", ""),
    MODERATE("Moderado", "Use protetor solar."),
    HIGH("Alto", "Use protetor solar."),
    VERY_HIGH("Muito alto", "Evite exposição prolongada."),
    EXTREME("Extremo", "Índice UV extremo. Evite exposição prolongada.");

    companion object {
        fun fromIndex(uv: Double): UvCategory = when {
            uv < 3.0 -> LOW
            uv < 6.0 -> MODERATE
            uv < 8.0 -> HIGH
            uv < 11.0 -> VERY_HIGH
            else -> EXTREME
        }
    }
}
