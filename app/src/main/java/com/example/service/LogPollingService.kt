package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FetchResult
import com.example.data.LogEntry
import com.example.data.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LogPollingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: LogRepository
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "LogPollingService"
        const val CHANNEL_SERVICE_ID = "log_monitor_service_channel"
        const val CHANNEL_ALERT_ID = "log_monitor_alert_channel"
        
        const val PREFS_NAME = "LogMonitorPrefs"
        const val KEY_INTERVAL = "polling_interval_ms"
        const val KEY_NOTIFY_ENABLED = "notifications_enabled"
        const val KEY_SERVICE_STATE = "service_state_enabled"
        const val KEY_LOG_URL = "log_url"
        
        const val DEFAULT_INTERVAL = 10000L // 10 seconds
        const val DEFAULT_LOG_URL = "https://xbwf.top/hook/webhook.log"

        @Volatile
        var isServiceRunning = false
            private set

        // Shared Flow to notify active UI immediately if running
        private val _newLogsFlow = MutableSharedFlow<List<LogEntry>>(extraBufferCapacity = 10)
        val newLogsFlow = _newLogsFlow.asSharedFlow()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        repository = LogRepository(applicationContext)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isServiceRunning = true
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        
        // Start Foreground with persistent notification
        val notification = createServiceNotification("正在后台监控日志...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1001, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            // Fallback to non-foreground start (could be killed, but won't crash instantly)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // On Android 12+, starting foreground can fail if background constraints are violated.
                // We proceed since we declared the right permissions.
            }
        }

        // Start polling loop
        startPollingLoop()

        return START_STICKY
    }

    private fun startPollingLoop() {
        serviceScope.launch {
            while (isServiceRunning) {
                val interval = sharedPreferences.getLong(KEY_INTERVAL, DEFAULT_INTERVAL)
                val notifyEnabled = sharedPreferences.getBoolean(KEY_NOTIFY_ENABLED, true)

                Log.d(TAG, "Polling webhook.log... Next poll in $interval ms")
                val result = repository.fetchAndSyncLogs()

                if (result is FetchResult.Success && result.newLines.isNotEmpty()) {
                    Log.d(TAG, "Discovered ${result.newLines.size} new log lines!")
                    
                    // Emit to flow for active UI
                    _newLogsFlow.tryEmit(result.newLines)

                    // Post system notification if enabled
                    if (notifyEnabled) {
                        triggerSystemNotification(result.newLines)
                    }

                    // Update service notification showing the latest update count
                    updateServiceNotification("最新收到 ${result.newLines.size} 行新日志")
                } else if (result is FetchResult.Error) {
                    Log.e(TAG, "Polling failed: ${result.message}")
                    updateServiceNotification("监控中 (获取失败: ${result.message})")
                } else {
                    updateServiceNotification("监控中 (一切正常，无新日志)")
                }

                delay(interval)
            }
        }
    }

    private fun triggerSystemNotification(newLines: List<LogEntry>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var isKuma = false
        var kumaTitle = ""
        var kumaMsg = ""
        
        val lastLine = newLines.lastOrNull()?.content ?: ""
        if (lastLine.isNotEmpty()) {
            try {
                val json = org.json.JSONObject(lastLine)
                if (json.has("heartbeat") || json.has("monitor") || json.has("msg")) {
                    isKuma = true
                    val monitorObj = if (json.has("monitor")) json.getJSONObject("monitor") else null
                    val heartbeatObj = if (json.has("heartbeat")) json.getJSONObject("heartbeat") else null
                    
                    val monitorName = monitorObj?.optString("name") 
                        ?: json.optString("msg")?.substringBefore("]")?.trim('[', ' ') 
                        ?: "Uptime Kuma Monitor"
                        
                    val status = heartbeatObj?.optInt("status", -1) ?: (
                        if (json.optString("msg").contains("Up", ignoreCase = true) || json.optString("msg").contains("✅")) 1 
                        else if (json.optString("msg").contains("Down", ignoreCase = true) || json.optString("msg").contains("🔴") || json.optString("msg").contains("❌")) 0 
                        else -1
                    )
                    
                    val msg = heartbeatObj?.optString("msg") ?: json.optString("msg") ?: "Status changed"
                    val ping = heartbeatObj?.optInt("ping", -1) ?: -1
                    
                    val statusPrefix = when (status) {
                        1 -> "🟢 【UP】"
                        0 -> "🔴 【DOWN】"
                        else -> "🟡 【PENDING】"
                    }
                    
                    kumaTitle = "$statusPrefix $monitorName"
                    kumaMsg = if (ping != -1 && ping > 0) "$msg (Latency: ${ping}ms)" else msg
                }
            } catch (e: Exception) {
                // Ignore, will use fallback below
            }
        }

        val title = if (isKuma) kumaTitle else "新日志产生 (${newLines.size} 条)"
        val text = if (isKuma) kumaMsg else {
            if (newLines.size == 1) {
                newLines.first().content
            } else {
                "最新日志: ${newLines.last().content}"
            }
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
        
        // Add up to 5 lines of context in expanded notification
        val displayLines = newLines.takeLast(5)
        displayLines.forEach { entry ->
            // Try to make lines nicer if they are Kuma JSON payloads
            val displayContent = try {
                val j = org.json.JSONObject(entry.content)
                val mName = j.optJSONObject("monitor")?.optString("name") ?: j.optString("msg")?.substringBefore("]")?.trim('[', ' ')
                val hb = j.optJSONObject("heartbeat")
                val statVal = hb?.optInt("status", -1) ?: (
                    if (j.optString("msg").contains("Up", ignoreCase = true) || j.optString("msg").contains("✅")) 1 
                    else if (j.optString("msg").contains("Down", ignoreCase = true) || j.optString("msg").contains("🔴") || j.optString("msg").contains("❌")) 0 
                    else -1
                )
                val mVal = hb?.optString("msg") ?: j.optString("msg") ?: "Change"
                val prefix = if (statVal == 1) "🟢" else if (statVal == 0) "🔴" else "🟡"
                if (mName != null) "$prefix $mName: $mVal" else entry.content
            } catch (e: Exception) {
                entry.content
            }
            inboxStyle.addLine(displayContent)
        }
        
        if (newLines.size > 5) {
            inboxStyle.setSummaryText("+ 还有 ${newLines.size - 5} 条日志")
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_icon_fg))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use rolling ID for distinct notifications, or single ID to stack
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun createServiceNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_icon_fg))
            .setContentTitle("日志实时监控服务")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateServiceNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = createServiceNotification(contentText)
        notificationManager.notify(1001, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Background Service channel (Low importance)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "日志监控后台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示日志监控服务的后台运行状态"
            }

            // Delete old channel to ensure the new default settings take effect
            notificationManager.deleteNotificationChannel(CHANNEL_ALERT_ID)

            // Real-time notification channel (High importance)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "日志更新提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "当 webhook.log 产生新日志时发送系统通知"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        isServiceRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
