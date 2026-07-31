package com.tempo.rn.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tempo.rn.core.config.ForecastConfig
import com.tempo.rn.data.workers.ForecastSyncWorker
import com.tempo.rn.data.workers.MarineSyncWorker
import com.tempo.rn.data.workers.VerificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val network = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic() {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            "forecast_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ForecastSyncWorker>(
                ForecastConfig.FORECAST_REFRESH_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            "marine_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MarineSyncWorker>(
                ForecastConfig.MARINE_REFRESH_HOURS, TimeUnit.HOURS
            )
                .setConstraints(network)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            "verification",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<VerificationWorker>(24, TimeUnit.HOURS).build()
        )
    }

    /** Sofort-Aktualisierung, z. B. bei App-Start oder Ortswechsel. */
    fun refreshNow() {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork(
            "forecast_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ForecastSyncWorker>().setConstraints(network).build()
        )
        wm.enqueueUniqueWork(
            "marine_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MarineSyncWorker>().setConstraints(network).build()
        )
    }
}
