package com.example.lookaway.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DailyBreakCount(
    val dateString: String,
    val count: Int,
)

@Dao
interface BreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreak(breakEntity: BreakEntity)

    @Query("SELECT COUNT(*) FROM break_history WHERE dateString = :dateStr AND status = 'COMPLETED'")
    fun getTodayCompletedCountFlow(dateStr: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM break_history WHERE timestamp >= :sinceTimestamp AND status = 'COMPLETED'")
    fun getWeeklyCompletedCountFlow(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM break_history WHERE status = 'COMPLETED'")
    fun getTotalCompletedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM break_history WHERE status = 'PROMPTED'")
    fun getTotalPromptedCountFlow(): Flow<Int>

    @Query("SELECT dateString, COUNT(*) as count FROM break_history WHERE status = 'COMPLETED' AND timestamp >= :sinceTimestamp GROUP BY dateString ORDER BY dateString ASC")
    fun get7DayBreakCountsFlow(sinceTimestamp: Long): Flow<List<DailyBreakCount>>

    @Query("SELECT DISTINCT dateString FROM break_history WHERE status = 'COMPLETED' ORDER BY dateString DESC")
    fun getAllCompletedDatesFlow(): Flow<List<String>>

    @Query("SELECT * FROM break_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistoryFlow(): Flow<List<BreakEntity>>
}
