package com.tempo.rn.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        ModelForecastEntity::class,
        BestMatchHourlyEntity::class,
        EnsembleHourlyEntity::class,
        DailyForecastEntity::class,
        MarineHourlyEntity::class,
        TideEventEntity::class,
        ModelWeightEntity::class,
        ForecastArchiveEntity::class,
        MetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TempoDatabase : RoomDatabase() {
    abstract fun forecastDao(): ForecastDao
    abstract fun marineDao(): MarineDao
    abstract fun tideDao(): TideDao
    abstract fun weightDao(): WeightDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun metaDao(): MetaDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TempoDatabase =
        Room.databaseBuilder(context, TempoDatabase::class.java, "tempo_rn.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun forecastDao(db: TempoDatabase): ForecastDao = db.forecastDao()
    @Provides fun marineDao(db: TempoDatabase): MarineDao = db.marineDao()
    @Provides fun tideDao(db: TempoDatabase): TideDao = db.tideDao()
    @Provides fun weightDao(db: TempoDatabase): WeightDao = db.weightDao()
    @Provides fun archiveDao(db: TempoDatabase): ArchiveDao = db.archiveDao()
    @Provides fun metaDao(db: TempoDatabase): MetaDao = db.metaDao()
}
