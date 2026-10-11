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
    val neckStretchingMinutes: Int = 0,
    val lastBreakDate: String = "",
)

object StatsManager {
    private const val PREFS_NAME = "look_away_stats_prefs"
    private const val KEY_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_TOTAL_BREAKS = "total_breaks"
    private const val KEY_DAILY_GOAL_MINUTES = "daily_goal_minutes"
    private const val KEY_TODAY_SCREEN_TIME = "today_screen_time"
    private const val KEY_NECK_STRETCHING = "neck_stretching_minutes"
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

        val neckStretchKey = "neck_stretching_$today"
        val neckStretching = prefs.getInt(neckStretchKey, prefs.getInt(KEY_NECK_STRETCHING, 0))

        return UserStats(
            currentStreak = streak,
            longestStreak = longestStreak,
            totalBreaks = prefs.getInt(KEY_TOTAL_BREAKS, 0),
            dailyGoalMinutes = prefs.getInt(KEY_DAILY_GOAL_MINUTES, 240),
            todayScreenTimeMinutes = autoScreenTime,
            weeklyScreenTimeMinutes = weeklyScreenTime,
            neckStretchingMinutes = neckStretching,
            lastBreakDate = lastDate,
        )
    }

    fun recordLookAwayBreak(context: Context): UserStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val streak = prefs.getInt(KEY_STREAK, 0) + 1
        val longestStreak = maxOf(
            streak,
            prefs.getInt(KEY_LONGEST_STREAK, 0)
        )
        val totalBreaks = prefs.getInt(KEY_TOTAL_BREAKS, 0) + 1

        prefs.edit()
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .putInt(KEY_TOTAL_BREAKS, totalBreaks)
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

    fun addNeckStretchingMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val todayKey = "neck_stretching_$today"
        val current = prefs.getInt(todayKey, prefs.getInt(KEY_NECK_STRETCHING, 0))
        val total = current + minutes

        prefs.edit()
            .putInt(todayKey, total)
            .putInt(KEY_NECK_STRETCHING, total)
            .apply()
    }

    fun resetStats(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
