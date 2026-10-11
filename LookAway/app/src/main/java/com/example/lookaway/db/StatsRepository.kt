package com.example.lookaway.db

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class DayBarData(
    val dayLabel: String,
    val dateString: String,
    val count: Int,
)

data class StatisticsUiState(
    val dailyBreaks: Int = 0,
    val weeklyBreaks: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val completionPercentage: Int = 100,
    val last7DaysData: List<DayBarData> = emptyList(),
)

class StatsRepository(private val db: AppDatabase) {

    companion object {
        fun getTodayString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        fun getLast7Days(): List<Pair<String, String>> {
            val list = mutableListOf<Pair<String, String>>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val calendar = Calendar.getInstance()

            for (i in 6 downTo 0) {
                val cal = calendar.clone() as Calendar
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(cal.time)
                val label = dayFormat.format(cal.time)
                list.add(Pair(dateStr, label))
            }
            return list
        }
    }

    suspend fun recordBreak(status: String) {
        withContext(Dispatchers.IO) {
            val entity = BreakEntity(
                timestamp = System.currentTimeMillis(),
                dateString = getTodayString(),
                status = status,
            )
            db.breakDao().insertBreak(entity)
        }
    }

    fun getStatisticsFlow(): Flow<StatisticsUiState> {
        val todayStr = getTodayString()

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val sevenDaysAgoTimestamp = calendar.timeInMillis

        return combine(
            db.breakDao().getTodayCompletedCountFlow(todayStr),
            db.breakDao().getWeeklyCompletedCountFlow(sevenDaysAgoTimestamp),
            db.breakDao().get7DayBreakCountsFlow(sevenDaysAgoTimestamp),
            db.breakDao().getAllCompletedDatesFlow(),
            db.breakDao().getTotalCompletedCountFlow(),
            db.breakDao().getTotalPromptedCountFlow(),
        ) { values ->

            val todayCompleted = values[0] as Int
            val weeklyCompleted = values[1] as Int
            val raw7Days = values[2] as List<DailyBreakCount>
            val allDates = values[3] as List<String>
            val totalCompleted = values[4] as Int
            val totalPrompted = values[5] as Int
            val last7Days = getLast7Days()
            val rawMap = raw7Days.associate { dailyBreak ->
                dailyBreak.dateString to dailyBreak.count
            }

            val barData = last7Days.map { (dateStr, label) ->
                DayBarData(
                    dayLabel = label,
                    dateString = dateStr,
                    count = rawMap[dateStr] ?: 0
                )
            }


            val (currentStreak, longestStreak) = calculateStreaks(allDates)

            val totalPrompts = maxOf(totalPrompted + totalCompleted, totalCompleted)
            val completionPercentage = if (totalPrompts > 0) {
                ((totalCompleted.toFloat() / totalPrompts.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                100
            }

            StatisticsUiState(
                dailyBreaks = todayCompleted,
                weeklyBreaks = weeklyCompleted,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                completionPercentage = completionPercentage,
                last7DaysData = barData,
            )
        }
    }

    private fun calculateStreaks(sortedDatesDesc: List<String>): Pair<Int, Int> {
        if (sortedDatesDesc.isEmpty()) return Pair(0, 0)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dates = sortedDatesDesc.mapNotNull {
            try {
                sdf.parse(it)
            } catch (_: Exception) {
                null
            }
        }.sortedDescending()

        if (dates.isEmpty()) return Pair(0, 0)

        val cal = Calendar.getInstance()
        val today = cal.time

        var currentStreak = 0
        var maxStreak = 0
        var tempStreak = 0

        val expectedCal = Calendar.getInstance()
        var streakActive = true

        for (i in dates.indices) {
            val date = dates[i]
            if (i == 0) {
                val diffDaysToday = ((today.time - date.time) / (1000 * 60 * 60 * 24)).toInt()
                if (diffDaysToday > 1) {
                    streakActive = false
                }
                tempStreak = 1
                expectedCal.time = date
            } else {
                expectedCal.add(Calendar.DAY_OF_YEAR, -1)
                val expectedStr = sdf.format(expectedCal.time)
                val actualStr = sdf.format(date)

                if (expectedStr == actualStr) {
                    tempStreak++
                } else {
                    if (streakActive && currentStreak == 0) {
                        currentStreak = tempStreak
                    }
                    maxStreak = maxOf(maxStreak, tempStreak)
                    tempStreak = 1
                    expectedCal.time = date
                }
            }
        }

        if (streakActive && currentStreak == 0) {
            currentStreak = tempStreak
        }
        maxStreak = maxOf(maxStreak, tempStreak)

        return Pair(currentStreak, maxStreak)
    }
}
