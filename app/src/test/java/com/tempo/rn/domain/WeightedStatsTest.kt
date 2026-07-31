package com.tempo.rn.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightedStatsTest {

    @Test
    fun `mediana ponderada com pesos iguais`() {
        val median = WeightedStats.weightedMedian(listOf(1.0, 3.0, 2.0), listOf(1.0, 1.0, 1.0))
        assertEquals(2.0, median, 1e-9)
    }

    @Test
    fun `mediana ponderada puxada pelo peso maior`() {
        // O valor 10 concentra 80% do peso -> mediana deve ser 10
        val median = WeightedStats.weightedMedian(listOf(0.0, 10.0), listOf(0.2, 0.8))
        assertEquals(10.0, median, 1e-9)
    }

    @Test
    fun `mediana ponderada de lista vazia e zero`() {
        assertEquals(0.0, WeightedStats.weightedMedian(emptyList(), emptyList()), 1e-9)
    }

    @Test
    fun `quantis lineares`() {
        val values = listOf(0.0, 1.0, 2.0, 3.0, 4.0)
        assertEquals(1.0, WeightedStats.quantile(values, 0.25), 1e-9)
        assertEquals(2.0, WeightedStats.quantile(values, 0.5), 1e-9)
        assertEquals(3.0, WeightedStats.quantile(values, 0.75), 1e-9)
        assertEquals(0.0, WeightedStats.quantile(values, 0.0), 1e-9)
        assertEquals(4.0, WeightedStats.quantile(values, 1.0), 1e-9)
    }
}
