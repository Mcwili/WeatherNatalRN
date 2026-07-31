package com.tempo.rn.domain

import com.tempo.rn.core.model.RainClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RainClassifierTest {

    @Test
    fun `classifica classes de chuva conforme especificacao`() {
        assertEquals(RainClass.NONE, RainClassifier.classify(null))
        assertEquals(RainClass.NONE, RainClassifier.classify(0.0))
        assertEquals(RainClass.NONE, RainClassifier.classify(0.05))
        assertEquals(RainClass.LIGHT, RainClassifier.classify(0.1))
        assertEquals(RainClass.LIGHT, RainClassifier.classify(0.99))
        assertEquals(RainClass.MODERATE, RainClassifier.classify(1.0))
        assertEquals(RainClass.MODERATE, RainClassifier.classify(4.99))
        assertEquals(RainClass.STRONG, RainClassifier.classify(5.0))
        assertEquals(RainClass.STRONG, RainClassifier.classify(14.99))
        assertEquals(RainClass.VERY_STRONG, RainClassifier.classify(15.0))
        assertEquals(RainClass.VERY_STRONG, RainClassifier.classify(40.0))
    }

    @Test
    fun `indica chuva a partir de 0_1 mm`() {
        assertFalse(RainClassifier.indicatesRain(0.0))
        assertFalse(RainClassifier.indicatesRain(0.05))
        assertTrue(RainClassifier.indicatesRain(0.1))
        assertTrue(RainClassifier.indicatesRain(2.0))
        assertFalse(RainClassifier.indicatesRain(null))
    }

    @Test
    fun `indica chuva com probabilidade alta e precipitacao positiva`() {
        assertTrue(RainClassifier.indicatesRain(0.05, probabilityPercent = 40.0))
        assertFalse(RainClassifier.indicatesRain(0.0, probabilityPercent = 90.0))
        assertFalse(RainClassifier.indicatesRain(0.05, probabilityPercent = 39.0))
    }
}
