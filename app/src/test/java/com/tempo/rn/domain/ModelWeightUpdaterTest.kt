package com.tempo.rn.domain

import com.tempo.rn.core.model.VerificationQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelWeightUpdaterTest {

    @Test
    fun `sem adaptacao antes de 7 dias`() {
        val update = ModelWeightUpdater.update(1.0, 0.9, evaluationDays = 6, quality = VerificationQuality.OBSERVED)
        assertFalse(update.applied)
        assertEquals(1.0, update.newWeight, 1e-9)
    }

    @Test
    fun `maxima mudanca diaria de 5 por cento`() {
        val up = ModelWeightUpdater.update(1.0, 1.0, evaluationDays = 30, quality = VerificationQuality.OBSERVED)
        assertEquals(1.05, up.newWeight, 1e-9)
        val down = ModelWeightUpdater.update(1.0, 0.0, evaluationDays = 30, quality = VerificationQuality.OBSERVED)
        assertEquals(0.95, down.newWeight, 1e-9)
    }

    @Test
    fun `observacao estimada ajusta apenas metade`() {
        val up = ModelWeightUpdater.update(1.0, 1.0, evaluationDays = 30, quality = VerificationQuality.ESTIMATED)
        assertEquals(1.025, up.newWeight, 1e-9)
    }

    @Test
    fun `limites 0_40 e 1_20 sao respeitados`() {
        val floor = ModelWeightUpdater.update(0.41, 0.0, evaluationDays = 30, quality = VerificationQuality.OBSERVED)
        assertEquals(0.40, floor.newWeight, 1e-9)
        val ceil = ModelWeightUpdater.update(1.19, 1.0, evaluationDays = 30, quality = VerificationQuality.OBSERVED)
        assertEquals(1.20, ceil.newWeight, 1e-9)
    }

    @Test
    fun `trefferquote neutra nao muda o peso`() {
        val update = ModelWeightUpdater.update(0.9, 0.5, evaluationDays = 30, quality = VerificationQuality.OBSERVED)
        assertEquals(0.9, update.newWeight, 1e-9)
        assertFalse(update.applied)
    }
}
