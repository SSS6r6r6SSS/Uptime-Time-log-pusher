package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM log_entries ORDER BY lineIndex ASC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Query("SELECT MAX(lineIndex) FROM log_entries")
    suspend fun getLatestLogIndex(): Int?

    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun getLogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntry>)

    @Query("DELETE FROM log_entries")
    suspend fun clearAllLogs()
}
