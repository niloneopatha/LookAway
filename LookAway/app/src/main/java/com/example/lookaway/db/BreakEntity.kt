package com.example.lookaway.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "break_history")
data class BreakEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val status: String, // "COMPLETED", "SNOOZED", "PROMPTED"
)
