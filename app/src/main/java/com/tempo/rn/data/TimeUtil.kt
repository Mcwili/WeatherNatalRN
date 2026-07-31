package com.tempo.rn.data

import com.tempo.rn.core.config.ForecastConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtil {
    val zone: ZoneId = ZoneId.of(ForecastConfig.TIMEZONE)
    private val isoMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    fun now(): LocalDateTime = LocalDateTime.now(zone)

    fun today(): LocalDate = LocalDate.now(zone)

    /** Open-Meteo liefert lokale Zeiten als "2026-07-31T14:00". */
    fun parse(iso: String): LocalDateTime? = try {
        LocalDateTime.parse(iso, isoMinute)
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(iso)
        } catch (_: Exception) {
            null
        }
    }

    fun format(time: LocalDateTime): String = time.format(isoMinute)
}
