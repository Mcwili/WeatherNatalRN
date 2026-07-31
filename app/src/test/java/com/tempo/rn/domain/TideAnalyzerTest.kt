package com.tempo.rn.domain

import com.tempo.rn.core.model.TideDataType
import com.tempo.rn.core.model.TideDirection
import com.tempo.rn.core.model.TideEventType
import com.tempo.rn.core.model.TidePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.PI

class TideAnalyzerTest {

    private val start: LocalDateTime = LocalDateTime.of(2026, 7, 30, 0, 0)

    /** Halbtägige Gezeit: Periode ~12.4 h, Hoch bei t=0. */
    private fun semidiurnalCurve(hours: Int, stepMinutes: Long = 30): List<TidePoint> {
        val points = mutableListOf<TidePoint>()
        var t = 0.0
        while (t <= hours) {
            val height = 1.3 + 0.8 * cos(2 * PI * t / 12.42)
            points += TidePoint(start.plusMinutes((t * 60).toLong()), height)
            t += stepMinutes / 60.0
        }
        return points
    }

    @Test
    fun `encontra minimos e maximos alternados`() {
        val state = TideAnalyzer.analyze(semidiurnalCurve(48), start.plusHours(5))
        assertTrue(state.events.size >= 6)
        // Typen alternieren
        state.events.zipWithNext().forEach { (a, b) ->
            assertTrue(a.type != b.type)
        }
        // Abstand zwischen Extremen ~6.2 h
        state.events.zipWithNext().forEach { (a, b) ->
            val gapMinutes = java.time.Duration.between(a.time, b.time).toMinutes()
            assertTrue("gap=$gapMinutes", gapMinutes in 300..450)
        }
    }

    @Test
    fun `direcao descendo depois da preia-mar`() {
        // t=1.5h: kurz nach Hochwasser (t=0) -> fallend
        val state = TideAnalyzer.analyze(semidiurnalCurve(48), start.plusHours(2))
        assertEquals(TideDirection.FALLING, state.direction)
    }

    @Test
    fun `direcao subindo antes da preia-mar`() {
        // t=9h: zwischen Tief (~6.2h) und Hoch (~12.4h) -> steigend
        val state = TideAnalyzer.analyze(semidiurnalCurve(48), start.plusHours(9))
        assertEquals(TideDirection.RISING, state.direction)
    }

    @Test
    fun `proxima baixa e alta sao futuras`() {
        val now = start.plusHours(5)
        val state = TideAnalyzer.analyze(semidiurnalCurve(48), now)
        assertNotNull(state.nextLow)
        assertNotNull(state.nextHigh)
        assertTrue(state.nextLow!!.time.isAfter(now))
        assertTrue(state.nextHigh!!.time.isAfter(now))
        assertEquals(TideEventType.LOW, state.nextLow!!.type)
        assertEquals(TideEventType.HIGH, state.nextHigh!!.type)
    }

    @Test
    fun `dados insuficientes resultam em UNKNOWN`() {
        val state = TideAnalyzer.analyze(semidiurnalCurve(1), start)
        assertEquals(TideDirection.UNKNOWN, state.direction)
        assertTrue(state.events.isEmpty())
    }

    @Test
    fun `tipo de dado modelado gera confianca moderada`() {
        val state = TideAnalyzer.analyze(
            semidiurnalCurve(48), start.plusHours(5),
            dataType = TideDataType.MODELLED_SEA_LEVEL,
        )
        assertEquals(com.tempo.rn.core.model.TideConfidence.MODERATE, state.confidence)
    }

    @Test
    fun `interpolacao entre pontos`() {
        val points = listOf(
            TidePoint(start, 1.0),
            TidePoint(start.plusHours(2), 2.0),
        )
        val mid = TideAnalyzer.interpolateAt(points, start.plusHours(1))
        assertEquals(1.5, mid!!, 1e-9)
    }
}
