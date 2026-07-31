package com.tempo.rn.data

import com.tempo.rn.core.database.TideDao
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.TideDataType
import com.tempo.rn.core.model.TideEvent
import com.tempo.rn.core.model.TideEventType
import com.tempo.rn.core.model.TidePoint
import com.tempo.rn.core.model.TideState
import com.tempo.rn.domain.TideAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

const val TIDE_STATION_ID = "porto_natal"
const val TIDE_STATION_NAME = "Porto de Natal"

/**
 * Gezeitenlogik (Spez. §14–§18).
 * Primär: offizielle DHN-Ereignisse aus der Datenbank (Import via JSON, Spez. §3.8).
 * Fallback: modellierter Meeresspiegel (sea_level_height_msl) mit klarer Kennzeichnung.
 * Es werden niemals Gezeitenzeiten erfunden.
 */
@Singleton
class TideRepository @Inject constructor(
    private val tideDao: TideDao,
    private val marineRepository: MarineRepository,
) {

    fun observeTideState(location: AppLocation): Flow<TideState> {
        val fromIso = TimeUtil.format(TimeUtil.now().minusDays(1))
        return combine(
            tideDao.observeEvents(TIDE_STATION_ID, fromIso),
            marineRepository.observe(location),
            marineRepository.observeLastUpdate(location),
        ) { officialEvents, marineHours, lastUpdateIso ->
            val now = TimeUtil.now()
            val updatedAt = lastUpdateIso?.let { TimeUtil.parse(it) }
            val curve = marineHours.mapNotNull { h ->
                h.seaLevelM?.let { TidePoint(h.time, it) }
            }

            if (officialEvents.isNotEmpty()) {
                // Offizielle DHN-Tabelle vorhanden: Ereignisse direkt verwenden,
                // Kurve (falls vorhanden) nur zur Darstellung.
                val events = officialEvents.mapNotNull { e ->
                    TimeUtil.parse(e.timestampIso)?.let { t ->
                        TideEvent(
                            time = t,
                            type = if (e.type == "HIGH") TideEventType.HIGH else TideEventType.LOW,
                            heightM = e.heightM,
                            dataType = TideDataType.OFFICIAL_TIDE_TABLE,
                        )
                    }
                }.sortedBy { it.time }
                val modelled = TideAnalyzer.analyze(curve, now, TideDataType.OFFICIAL_TIDE_TABLE,
                    TIDE_STATION_NAME, updatedAt)
                modelled.copy(
                    events = events,
                    nextLow = events.firstOrNull { it.type == TideEventType.LOW && it.time.isAfter(now) },
                    nextHigh = events.firstOrNull { it.type == TideEventType.HIGH && it.time.isAfter(now) },
                    dataType = TideDataType.OFFICIAL_TIDE_TABLE,
                    confidence = com.tempo.rn.core.model.TideConfidence.HIGH,
                )
            } else {
                TideAnalyzer.analyze(
                    rawCurve = curve,
                    now = now,
                    dataType = TideDataType.MODELLED_SEA_LEVEL,
                    referenceStationPt = TIDE_STATION_NAME,
                    updatedAt = updatedAt,
                )
            }
        }
    }
}
