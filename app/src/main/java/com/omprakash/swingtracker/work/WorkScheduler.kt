package com.omprakash.swingtracker.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val WORK_NAME = "screener_periodic_work"

    /**
     * Schedules the screener to run every 30 minutes when there's network access.
     * 30 minutes is WorkManager's minimum interval for periodic work - it also
     * skips runs automatically outside constraints (e.g. no internet), and the
     * OS may shift the exact timing slightly to save battery. This is normal.
     */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ScreenerWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Triggers one immediate run, useful for a manual "refresh now" button. */
    fun runOnce(context: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<ScreenerWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
