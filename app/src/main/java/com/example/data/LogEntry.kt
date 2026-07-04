package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lineIndex: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
