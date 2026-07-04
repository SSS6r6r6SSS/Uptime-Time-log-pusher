package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class FetchResult {
    data class Success(val newLines: List<LogEntry>) : FetchResult()
    data class Error(val message: String) : FetchResult()
}

class LogRepository(private val context: Context) {
    private val logDao = AppDatabase.getDatabase(context).logDao()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val allLogs: Flow<List<LogEntry>> = logDao.getAllLogs()

    suspend fun getLogCount(): Int = withContext(Dispatchers.IO) {
        logDao.getLogCount()
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearAllLogs()
    }

    suspend fun fetchAndSyncLogs(): FetchResult = withContext(Dispatchers.IO) {
        val sharedPreferences = context.getSharedPreferences("LogMonitorPrefs", Context.MODE_PRIVATE)
        val url = sharedPreferences.getString("log_url", "https://xbwf.top/hook/webhook.log")?.trim() ?: "https://xbwf.top/hook/webhook.log"
        
        if (url.isEmpty()) {
            return@withContext FetchResult.Error("日志 URL 未配置，请在系统设置中设置")
        }

        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        val request = try {
            Request.Builder()
                .url(finalUrl)
                .build()
        } catch (e: Exception) {
            return@withContext FetchResult.Error("无效的 URL 格式: ${e.localizedMessage}")
        }

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult.Error("HTTP Error: ${response.code}")
                }

                val bodyString = response.body?.string() ?: ""
                // Parse lines and keep indices
                val allLines = bodyString.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()

                val latestIndex = logDao.getLatestLogIndex() ?: -1
                Log.d("LogRepository", "Fetched ${allLines.size} lines. Latest local index was $latestIndex")

                val newEntries = mutableListOf<LogEntry>()

                if (allLines.size < latestIndex + 1) {
                    // Log was cleared or rotated on server, clear and reload all
                    logDao.clearAllLogs()
                    allLines.forEachIndexed { idx, line ->
                        newEntries.add(LogEntry(lineIndex = idx, content = line))
                    }
                    if (newEntries.isNotEmpty()) {
                        logDao.insertLogs(newEntries)
                    }
                    // Since it's a full reload/rotation, return as success but perhaps limit notification overload
                    return@withContext FetchResult.Success(newEntries)
                } else if (allLines.size > latestIndex + 1) {
                    // New lines have been added
                    for (i in (latestIndex + 1) until allLines.size) {
                        newEntries.add(LogEntry(lineIndex = i, content = allLines[i]))
                    }
                    if (newEntries.isNotEmpty()) {
                        logDao.insertLogs(newEntries)
                    }
                    return@withContext FetchResult.Success(newEntries)
                }

                return@withContext FetchResult.Success(emptyList())
            }
        } catch (e: IOException) {
            Log.e("LogRepository", "Network fetch failed", e)
            return@withContext FetchResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        } catch (e: Exception) {
            Log.e("LogRepository", "Error syncing logs", e)
            return@withContext FetchResult.Error("Error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun insertCustomLog(content: String): LogEntry = withContext(Dispatchers.IO) {
        val latestIndex = logDao.getLatestLogIndex() ?: -1
        val nextIndex = latestIndex + 1
        val entry = LogEntry(lineIndex = nextIndex, content = content)
        logDao.insertLogs(listOf(entry))
        Log.d("LogRepository", "Inserted custom log at index $nextIndex: $content")
        entry
    }
}
