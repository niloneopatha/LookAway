package com.example.lookaway.stats

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

object ScreenTimeTracker {
    fun getTodayScreenTimeMinutes(context: Context): Long {
        if (!UsageAccessHelper.hasUsageStatsPermission(context)) {
            return 0L
        }
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val statsMap = usageStatsManager.queryAndAggregateUsageStats(
            startTime,
            endTime
        )

        var totalTimeMillis = 0L
        for ((_, stats) in statsMap) {
            totalTimeMillis += stats.totalTimeInForeground
        }

        return totalTimeMillis / (1000 * 60)
    }
}
