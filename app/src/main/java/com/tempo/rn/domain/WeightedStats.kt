package com.tempo.rn.domain

/** Gewichtete Statistik für die Konsolidierung (Spez. §9: gewichteter Median). */
object WeightedStats {

    /**
     * Gewichteter Median: Werte aufsteigend sortieren, kumulierte Gewichte,
     * erster Wert, dessen kumuliertes Gewicht >= 50 % der Gesamtsumme ist.
     */
    fun weightedMedian(values: List<Double>, weights: List<Double>): Double {
        require(values.size == weights.size) { "values und weights müssen gleich lang sein" }
        if (values.isEmpty()) return 0.0
        val sorted = values.zip(weights).sortedBy { it.first }
        val total = sorted.sumOf { it.second }
        if (total <= 0.0) return sorted[sorted.size / 2].first
        var cum = 0.0
        for ((v, w) in sorted) {
            cum += w
            if (cum >= total / 2.0) return v
        }
        return sorted.last().first
    }

    /** Lineares Quantil (0..1) über ungewichtete Werte. */
    fun quantile(values: List<Double>, q: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val pos = (sorted.size - 1) * q.coerceIn(0.0, 1.0)
        val lower = sorted[pos.toInt()]
        val upper = sorted[minOf(pos.toInt() + 1, sorted.size - 1)]
        val frac = pos - pos.toInt()
        return lower + (upper - lower) * frac
    }

    fun weightedMean(values: List<Double>, weights: List<Double>): Double {
        require(values.size == weights.size)
        val total = weights.sum()
        if (values.isEmpty() || total <= 0.0) return 0.0
        return values.zip(weights).sumOf { it.first * it.second } / total
    }
}
