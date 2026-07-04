package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FetchResult
import com.example.data.LogEntry
import com.example.data.LogRepository
import com.example.service.LogPollingService
import com.example.service.MyFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LogRepository(application)
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences(
        LogPollingService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isServiceActive = MutableStateFlow(LogPollingService.isServiceRunning)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _pollingInterval = MutableStateFlow(
        sharedPreferences.getLong(LogPollingService.KEY_INTERVAL, LogPollingService.DEFAULT_INTERVAL)
    )
    val pollingInterval: StateFlow<Long> = _pollingInterval.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(LogPollingService.KEY_NOTIFY_ENABLED, true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _autoScrollEnabled = MutableStateFlow(true)
    val autoScrollEnabled: StateFlow<Boolean> = _autoScrollEnabled.asStateFlow()

    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _logUrl = MutableStateFlow(
        sharedPreferences.getString(LogPollingService.KEY_LOG_URL, LogPollingService.DEFAULT_LOG_URL) ?: LogPollingService.DEFAULT_LOG_URL
    )
    val logUrl: StateFlow<String> = _logUrl.asStateFlow()

    private val _fcmToken = MutableStateFlow(
        sharedPreferences.getString(MyFirebaseMessagingService.KEY_FCM_TOKEN, "") ?: ""
    )
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    // Filtered logs stream
    val filteredLogs: StateFlow<List<LogEntry>> = repository.allLogs
        .combine(_searchQuery) { logs, query ->
            if (query.isBlank()) {
                logs
            } else {
                logs.filter { it.content.contains(query, ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Fetch current FCM token
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("LogViewModel", "Fetched FCM token: $token")
                    sharedPreferences.edit().putString(MyFirebaseMessagingService.KEY_FCM_TOKEN, token).apply()
                    _fcmToken.value = token
                } else {
                    Log.w("LogViewModel", "Fetching FCM token failed", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e("LogViewModel", "Error initializing FCM Token", e)
        }

        // Periodically refresh the running state of the service to keep UI in sync
        viewModelScope.launch {
            while (true) {
                _isServiceActive.value = LogPollingService.isServiceRunning
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAutoScroll() {
        _autoScrollEnabled.value = !_autoScrollEnabled.value
    }

    fun setPollingInterval(ms: Long) {
        sharedPreferences.edit().putLong(LogPollingService.KEY_INTERVAL, ms).apply()
        _pollingInterval.value = ms
        // If service is running, restart it to apply the new interval
        if (_isServiceActive.value) {
            restartService()
        }
    }

    fun setLogUrl(url: String) {
        val trimmed = url.trim()
        sharedPreferences.edit().putString(LogPollingService.KEY_LOG_URL, trimmed).apply()
        _logUrl.value = trimmed
        // Reset/clear logs when switching endpoints to prevent index mismatch of completely different log streams
        clearLogs()
        // If service is running, restart it to apply the new endpoint
        if (_isServiceActive.value) {
            restartService()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(LogPollingService.KEY_NOTIFY_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun toggleService() {
        val context = getApplication<Application>().applicationContext
        if (_isServiceActive.value) {
            val intent = Intent(context, LogPollingService::class.java)
            context.stopService(intent)
            _isServiceActive.value = false
            sharedPreferences.edit().putBoolean(LogPollingService.KEY_SERVICE_STATE, false).apply()
        } else {
            val intent = Intent(context, LogPollingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isServiceActive.value = true
            sharedPreferences.edit().putBoolean(LogPollingService.KEY_SERVICE_STATE, true).apply()
        }
    }

    private fun restartService() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, LogPollingService::class.java)
        context.stopService(intent)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun refreshLogsManually() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = repository.fetchAndSyncLogs()
            _isRefreshing.value = false
            when (result) {
                is FetchResult.Success -> {
                    _lastUpdated.value = System.currentTimeMillis()
                }
                is FetchResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

}
