package com.tempo.rn.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ForecastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModelHours(rows: List<ModelForecastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBestMatchHours(rows: List<BestMatchHourlyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEnsembleHours(rows: List<EnsembleHourlyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(rows: List<DailyForecastEntity>)

    @Query("SELECT * FROM model_forecast WHERE locationId = :locationId AND timeIso >= :fromIso ORDER BY timeIso")
    fun observeModelHours(locationId: String, fromIso: String): Flow<List<ModelForecastEntity>>

    @Query("SELECT * FROM best_match_hourly WHERE locationId = :locationId AND timeIso >= :fromIso ORDER BY timeIso")
    fun observeBestMatchHours(locationId: String, fromIso: String): Flow<List<BestMatchHourlyEntity>>

    @Query("SELECT * FROM ensemble_hourly WHERE locationId = :locationId AND timeIso >= :fromIso ORDER BY timeIso")
    fun observeEnsembleHours(locationId: String, fromIso: String): Flow<List<EnsembleHourlyEntity>>

    @Query("SELECT * FROM daily_forecast WHERE locationId = :locationId AND dateIso >= :fromIso ORDER BY dateIso")
    fun observeDaily(locationId: String, fromIso: String): Flow<List<DailyForecastEntity>>

    @Query("SELECT * FROM best_match_hourly WHERE locationId = :locationId AND timeIso >= :fromIso AND timeIso <= :toIso ORDER BY timeIso")
    suspend fun bestMatchBetween(locationId: String, fromIso: String, toIso: String): List<BestMatchHourlyEntity>

    @Query("DELETE FROM model_forecast WHERE timeIso < :beforeIso")
    suspend fun pruneModelHours(beforeIso: String)

    @Query("DELETE FROM best_match_hourly WHERE timeIso < :beforeIso")
    suspend fun pruneBestMatch(beforeIso: String)

    @Query("DELETE FROM ensemble_hourly WHERE timeIso < :beforeIso")
    suspend fun pruneEnsemble(beforeIso: String)

    @Query("DELETE FROM daily_forecast WHERE dateIso < :beforeIso")
    suspend fun pruneDaily(beforeIso: String)
}

@Dao
interface MarineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<MarineHourlyEntity>)

    @Query("SELECT * FROM marine_hourly WHERE locationId = :locationId AND timeIso >= :fromIso ORDER BY timeIso")
    fun observe(locationId: String, fromIso: String): Flow<List<MarineHourlyEntity>>

    @Query("DELETE FROM marine_hourly WHERE timeIso < :beforeIso")
    suspend fun prune(beforeIso: String)
}

@Dao
interface TideDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(rows: List<TideEventEntity>)

    @Query("SELECT * FROM tide_event WHERE stationId = :stationId AND timestampIso >= :fromIso ORDER BY timestampIso")
    fun observeEvents(stationId: String, fromIso: String): Flow<List<TideEventEntity>>
}

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<ModelWeightEntity>)

    @Query("SELECT * FROM model_weight WHERE metric = :metric")
    fun observeByMetric(metric: String): Flow<List<ModelWeightEntity>>

    @Query("SELECT * FROM model_weight WHERE metric = :metric")
    suspend fun byMetric(metric: String): List<ModelWeightEntity>
}

@Dao
interface ArchiveDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<ForecastArchiveEntity>)

    @Query("SELECT * FROM forecast_archive WHERE verified = 0 AND targetTimeIso < :nowIso")
    suspend fun unverifiedBefore(nowIso: String): List<ForecastArchiveEntity>

    @Query("UPDATE forecast_archive SET verified = 1 WHERE locationId = :locationId AND modelId = :modelId AND targetTimeIso = :targetTimeIso AND leadHours = :leadHours")
    suspend fun markVerified(locationId: String, modelId: String, targetTimeIso: String, leadHours: Int)

    @Query("DELETE FROM forecast_archive WHERE targetTimeIso < :beforeIso")
    suspend fun prune(beforeIso: String)
}

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(row: MetaEntity)

    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM meta WHERE `key` = :key")
    fun observe(key: String): Flow<String?>
}
