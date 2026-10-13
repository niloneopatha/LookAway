package com.example.lookaway.stats

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalBreaks: Int = 0,
    val dailyGoalMinutes: Int = 240, // Default 4 hours
    val todayScreenTimeMinutes: Int = 0,
    val weeklyScreenTimeMinutes: Int = 0,
    val neckStretchingCount: Int = 0,
    val eyeTrackingEnabled: Boolean = false,
    val eyeTrackingLookAways: Int = 0,
    val weeklyBreakCounts: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val lastBreakDate: String = "",
) {
    val healthScore: Int
        get() {
            var score = 70
            score += minOf(currentStreak * 3, 20)
            score += minOf(neckStretchingCount * 2, 15)
            score += minOf(totalBreaks * 1, 10)
            if (todayScreenTimeMinutes > dailyGoalMinutes && dailyGoalMinutes > 0) {
                score -= 15
            } else {
                score += 10
            }
            return score.coerceIn(0, 100)
        }
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean
)

object AchievementManager {
    fun getAchievements(stats: UserStats): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_break",
                title = "First Break",
                description = "Log your very first eye break",
                icon = "🥇",
                unlocked = stats.totalBreaks >= 1
            ),
            Achievement(
                id = "10_breaks",
                title = "10 Breaks",
                description = "Complete 10 total eye breaks",
                icon = "🥈",
                unlocked = stats.totalBreaks >= 10
            ),
            Achievement(
                id = "50_breaks",
                title = "50 Breaks",
                description = "Complete 50 total eye breaks",
                icon = "🥉",
                unlocked = stats.totalBreaks >= 50
            ),
            Achievement(
                id = "7_streak",
                title = "7 Streak",
                description = "Reach a 7-day break streak",
                icon = "🔥",
                unlocked = stats.currentStreak >= 7 || stats.longestStreak >= 7
            ),
            Achievement(
                id = "30_streak",
                title = "30 Streak",
                description = "Reach a 30-day break streak",
                icon = "🏆",
                unlocked = stats.currentStreak >= 30 || stats.longestStreak >= 30
            ),
            Achievement(
                id = "20_stretches",
                title = "20 Neck Stretches",
                description = "Perform 20 neck stretches",
                icon = "🦒",
                unlocked = stats.neckStretchingCount >= 20
            ),
            Achievement(
                id = "vigilant_eye",
                title = "Vigilant Eye",
                description = "Detect 5 automatic look-aways",
                icon = "👁️",
                unlocked = stats.eyeTrackingLookAways >= 5
            )
        )
    }
}

object StatsManager {
    private const val PREFS_NAME = "look_away_stats_prefs"
    private const val KEY_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_TOTAL_BREAKS = "total_breaks"
    private const val KEY_DAILY_GOAL_MINUTES = "daily_goal_minutes"
    private const val KEY_TODAY_SCREEN_TIME = "today_screen_time"
    private const val KEY_NECK_STRETCHING = "neck_stretching_count"
    private const val KEY_EYE_TRACKING_ENABLED = "eye_tracking_enabled"
    private const val KEY_EYE_TRACKING_LOOK_AWAYS = "eye_tracking_look_aways"
    private const val KEY_LAST_BREAK_DATE = "last_break_date"

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getLast7DaysDates(): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<String>()
        repeat(7) {
            dates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dates
    }

    fun getWeekBreakCounts(context: Context, weekOffset: Int): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(weekOffset * 7))

        val dates = mutableListOf<String>()
        repeat(7) {
            dates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dates.reversed().map { dateStr ->
            prefs.getInt("break_count_$dateStr", 0)
        }
    }

    fun getAllTimeAverageBreaks(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalBreaks = prefs.getInt(KEY_TOTAL_BREAKS, 0)
        val allKeys = prefs.all.keys
        val breakDays = allKeys.filter { it.startsWith("break_count_") }.size
        if (breakDays == 0) return totalBreaks.toFloat()
        return totalBreaks.toFloat() / breakDays.toFloat()
    }

    fun getStats(context: Context): UserStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_BREAK_DATE, "") ?: ""
        val longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)
        val streak = prefs.getInt(KEY_STREAK, 0)

        val todayKey = "screen_time_$today"
        val autoScreenTime = if (UsageAccessHelper.hasUsageStatsPermission(context)) {
            ScreenTimeTracker.getTodayScreenTimeMinutes(context).toInt()
        } else {
            prefs.getInt(todayKey, prefs.getInt(KEY_TODAY_SCREEN_TIME, 0))
        }

        prefs.edit().putInt(todayKey, autoScreenTime).putInt(KEY_TODAY_SCREEN_TIME, autoScreenTime).apply()

        val last7Days = getLast7DaysDates()
        val weeklyScreenTime = last7Days.sumOf { dateStr ->
            if (dateStr == today && UsageAccessHelper.hasUsageStatsPermission(context)) {
                autoScreenTime
            } else {
                prefs.getInt("screen_time_$dateStr", 0)
            }
        }

        val weeklyBreakCounts = getWeekBreakCounts(context, 0)

        val neckStretchKey = "neck_stretching_$today"
        val neckStretching = prefs.getInt(neckStretchKey, prefs.getInt(KEY_NECK_STRETCHING, 0))
        val eyeTrackingEnabled = prefs.getBoolean(KEY_EYE_TRACKING_ENABLED, false)
        val eyeTrackingLookAways = prefs.getInt(KEY_EYE_TRACKING_LOOK_AWAYS, 0)

        return UserStats(
            currentStreak = streak,
            longestStreak = longestStreak,
            totalBreaks = prefs.getInt(KEY_TOTAL_BREAKS, 0),
            dailyGoalMinutes = prefs.getInt(KEY_DAILY_GOAL_MINUTES, 240),
            todayScreenTimeMinutes = autoScreenTime,
            weeklyScreenTimeMinutes = weeklyScreenTime,
            neckStretchingCount = neckStretching,
            eyeTrackingEnabled = eyeTrackingEnabled,
            eyeTrackingLookAways = eyeTrackingLookAways,
            weeklyBreakCounts = weeklyBreakCounts,
            lastBreakDate = lastDate,
        )
    }

    fun recordLookAwayBreak(context: Context): UserStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_BREAK_DATE, "") ?: ""

        val totalBreaks = prefs.getInt(KEY_TOTAL_BREAKS, 0) + 1
        var streak = prefs.getInt(KEY_STREAK, 0)

        if (lastDate != today) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            if (lastDate == yesterday || lastDate.isEmpty()) {
                streak += 1
            } else {
                streak = 1
            }
        } else {
            if (streak == 0) streak = 1
        }

        val longestStreak = maxOf(
            streak,
            prefs.getInt(KEY_LONGEST_STREAK, 0)
        )

        val todayBreakKey = "break_count_$today"
        val todayBreaks = prefs.getInt(todayBreakKey, 0) + 1

        prefs.edit()
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .putInt(KEY_TOTAL_BREAKS, totalBreaks)
            .putInt(todayBreakKey, todayBreaks)
            .putString(KEY_LAST_BREAK_DATE, today)
            .apply()

        return getStats(context)
    }

    fun updateDailyGoal(context: Context, goalMinutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DAILY_GOAL_MINUTES, maxOf(goalMinutes, 10)).apply()
    }

    fun addScreenTimeMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val todayKey = "screen_time_$today"
        val currentToday = prefs.getInt(todayKey, prefs.getInt(KEY_TODAY_SCREEN_TIME, 0))
        val totalToday = currentToday + minutes

        prefs.edit()
            .putInt(todayKey, totalToday)
            .putInt(KEY_TODAY_SCREEN_TIME, totalToday)
            .apply()
    }

    fun addNeckStretchCount(context: Context, delta: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val todayKey = "neck_stretching_$today"
        val current = prefs.getInt(todayKey, prefs.getInt(KEY_NECK_STRETCHING, 0))
        val total = maxOf(0, current + delta)

        prefs.edit()
            .putInt(todayKey, total)
            .putInt(KEY_NECK_STRETCHING, total)
            .apply()
    }

    fun updateEyeTrackingEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_EYE_TRACKING_ENABLED, enabled).apply()
    }

    fun incrementEyeTrackingLookAways(context: Context): UserStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_EYE_TRACKING_LOOK_AWAYS, 0)
        prefs.edit().putInt(KEY_EYE_TRACKING_LOOK_AWAYS, current + 1).apply()
        return getStats(context)
    }

    fun resetStats(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
