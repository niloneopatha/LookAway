package com.example.lookaway.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val WORK_NAME = "look_away_periodic_notification"
    const val KEY_INTERVAL_MINUTES = "interval_minutes"

    fun scheduleNotification(context: Context, intervalInMinutes: Long) {
        cancelNotification(context)

        if (intervalInMinutes >= 15L) {
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                intervalInMinutes,
                TimeUnit.MINUTES,
            )
                .setInputData(workDataOf(KEY_INTERVAL_MINUTES to intervalInMinutes))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest,
            )
        } else {
            scheduleOneTimeWork(context, intervalInMinutes)
        }
    }

    fun scheduleOneTimeWork(context: Context, intervalInMinutes: Long) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(intervalInMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_INTERVAL_MINUTES to intervalInMinutes))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
    }

    fun cancelNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun isNotificationScheduled(context: Context): Flow<Boolean> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(WORK_NAME)
            .map { workInfoList ->
                workInfoList.any { workInfo ->
                    (workInfo.state == WorkInfo.State.ENQUEUED) || (workInfo.state == WorkInfo.State.RUNNING)
                }
            }
    }
}
