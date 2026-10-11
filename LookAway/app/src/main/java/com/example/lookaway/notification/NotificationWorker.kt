package com.example.lookaway.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.sendNotification(
            context = applicationContext,
            title = "Time to Look Away! 👀",
            message = "Give your eyes a rest. Look at something 20 feet away for 20 seconds.",
        )

        val intervalInMinutes = inputData.getLong(NotificationScheduler.KEY_INTERVAL_MINUTES, 0L)
        if (intervalInMinutes in 1L..14L) {
            NotificationScheduler.scheduleOneTimeWork(applicationContext, intervalInMinutes)
        }

        return Result.success()
    }
}
