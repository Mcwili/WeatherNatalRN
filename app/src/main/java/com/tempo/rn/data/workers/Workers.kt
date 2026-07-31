package com.tempo.rn.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tempo.rn.core.database.ArchiveDao
import com.tempo.rn.core.database.ForecastDao
import com.tempo.rn.core.database.ModelWeightEntity
import com.tempo.rn.core.database.WeightDao
import com.tempo.rn.core.model.Locations
import com.tempo.rn.core.model.VerificationQuality
import com.tempo.rn.core.model.WeatherModel
import com.tempo.rn.data.LocationRepository
import com.tempo.rn.data.MarineRepository
import com.tempo.rn.data.METRIC_RAIN
import com.tempo.rn.data.TimeUtil
import com.tempo.rn.data.WeatherRepository
import com.tempo.rn.domain.ModelWeightUpdater
import com.tempo.rn.domain.RainClassifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Stündliche Prognose-Aktualisierung (Spez. §20). */
@HiltWorker
class ForecastSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val location = locationRepository.selectedLocation.first()
        val result = weatherRepository.refresh(location)
        return when {
            result.isSuccess -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}

/** Marine-/Gezeitendaten alle 3 Stunden (Spez. §20). */
@HiltWorker
class MarineSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val marineRepository: MarineRepository,
    private val locationRepository: LocationRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val location = locationRepository.selectedLocation.first()
        val result = marineRepository.refresh(location)
        return when {
            result.isSuccess -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}

/**
 * Tägliche Verifikation und Gewichtsanpassung (Spez. §12/§13).
 * Referenz ist der Best-Match-Kurzfristwert (ESTIMATED) — Anpassung daher
 * nur mit halber Stärke, transparent und begrenzt.
 */
@HiltWorker
class VerificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val archiveDao: ArchiveDao,
    private val forecastDao: ForecastDao,
    private val weightDao: WeightDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = TimeUtil.now()
        val nowIso = TimeUtil.format(now)
        val pending = archiveDao.unverifiedBefore(nowIso)
        if (pending.isEmpty()) return Result.success()

        val existing = weightDao.byMetric(METRIC_RAIN).associateBy { it.modelId }
        val hits = mutableMapOf<String, Pair<Int, Int>>() // modelId -> (hits, evals)

        for (row in pending) {
            val observedRow = forecastDao
                .bestMatchBetween(row.locationId, row.targetTimeIso, row.targetTimeIso)
                .firstOrNull()
            if (observedRow?.precipitationMm == null) continue
            val forecastRain = RainClassifier.indicatesRain(row.precipitationMm)
            val observedRain = RainClassifier.indicatesRain(observedRow.precipitationMm)
            val hit = forecastRain == observedRain
            val (h, e) = hits.getOrDefault(row.modelId, 0 to 0)
            hits[row.modelId] = (if (hit) h + 1 else h) to (e + 1)
            archiveDao.markVerified(row.locationId, row.modelId, row.targetTimeIso, row.leadHours)
        }

        val today = TimeUtil.today()
        val updated = hits.mapNotNull { (modelId, pair) ->
            val model = WeatherModel.byId(modelId) ?: return@mapNotNull null
            val prev = existing[modelId]
            val newHitCount = (prev?.hitCount ?: 0) + pair.first
            val newEvalCount = (prev?.evalCount ?: 0) + pair.second
            val firstEval = prev?.firstEvalDateIso ?: today.toString()
            val evaluationDays = runCatching {
                ChronoUnit.DAYS.between(LocalDate.parse(firstEval), today).toInt()
            }.getOrDefault(0)
            val hitRate = if (newEvalCount > 0) newHitCount.toDouble() / newEvalCount else 0.5
            val update = ModelWeightUpdater.update(
                currentWeight = prev?.weight ?: model.startWeight,
                hitRate = hitRate,
                evaluationDays = evaluationDays,
                quality = VerificationQuality.ESTIMATED,
            )
            ModelWeightEntity(
                modelId = modelId,
                metric = METRIC_RAIN,
                weight = update.newWeight,
                hitCount = newHitCount,
                evalCount = newEvalCount,
                firstEvalDateIso = firstEval,
                updatedAtIso = nowIso,
            )
        }
        if (updated.isNotEmpty()) weightDao.upsert(updated)
        return Result.success()
    }
}
