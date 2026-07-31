package com.tempo.rn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfidenceCalculatorTest {

    @Test
    fun `pesos da formula conforme especificacao`() {
        // 0.4*1 + 0.3*1 + 0.2*1 + 0.1*1 = 100
        assertEquals(100, ConfidenceCalculator.score(1.0, 1.0, 1.0, 1.0))
        // 0.4*0.5 + 0.3*0.5 + 0.2*0.5 + 0.1*0.5 = 50
        assertEquals(50, ConfidenceCalculator.score(0.5, 0.5, 0.5, 0.5))
        assertEquals(0, ConfidenceCalculator.score(0.0, 0.0, 0.0, 0.0))
    }

    @Test
    fun `sem ensemble o peso vai para concordancia`() {
        // (0.4+0.3)*1 + 0.2*0.5 + 0.1*1 = 0.9 -> 90
        assertEquals(90, ConfidenceCalculator.score(1.0, null, 0.5, 1.0))
    }

    @Test
    fun `frescor decai linearmente`() {
        assertEquals(1.0, ConfidenceCalculator.freshness(0.0), 1e-9)
        assertEquals(1.0, ConfidenceCalculator.freshness(90.0), 1e-9)
        assertEquals(0.0, ConfidenceCalculator.freshness(360.0), 1e-9)
        val mid = ConfidenceCalculator.freshness(225.0)
        assertEquals(0.5, mid, 1e-9)
    }

    @Test
    fun `estabilidade do ensemble diminui com dispersao`() {
        val tight = ConfidenceCalculator.ensembleStability(1.0, 1.2, 1.1)
        val wide = ConfidenceCalculator.ensembleStability(0.0, 8.0, 1.0)
        assertTrue(tight > wide)
        assertEquals(1.0, ConfidenceCalculator.ensembleStability(0.5, 0.5, 0.5), 1e-9)
    }
}
