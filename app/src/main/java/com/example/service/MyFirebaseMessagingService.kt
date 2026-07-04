package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.LogEntry
import com.example.data.LogRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val TAG = "MyFirebaseMessaging"
        const val PREFS_NAME = "LogMonitorPrefs"
        const val KEY_FCM_TOKEN = "fcm_token"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        // Cache FCM token in SharedPreferences for UI display
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 1. Process data payload or notification body
        val data = remoteMessage.data
        var content = ""

        if (data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: $data")
            content = if (data.containsKey("payload")) {
                data["payload"] ?: ""
            } else if (data.containsKey("content")) {
                data["content"] ?: ""
            } else if (data.containsKey("monitor") || data.containsKey("heartbeat") || data.containsKey("msg")) {
                // Construct standard Kuma JSON payload from flat key-value pairs if sent flat
                try {
                    val json = JSONObject()
                    data.forEach { (key, value) ->
                        try {
                            if (key == "monitor" || key == "heartbeat") {
                                json.put(key, JSONObject(value))
                            } else {
                                json.put(key, value)
                            }
                        } catch (e: Exception) {
                            json.put(key, value)
                        }
                    }
                    json.toString()
                } catch (e: Exception) {
                    JSONObject(data as Map<*, *>).toString()
                }
            } else {
                // Default to serialize whole map to JSON
                try {
                    JSONObject(data as Map<*, *>).toString()
                } catch (e: Exception) {
                    data.toString()
                }
            }
        }

        // If content is still empty, look at notification body
        val notification = remoteMessage.notification
        if (content.isEmpty() && notification != null) {
            val title = notification.title ?: "FCM Notification"
            val body = notification.body ?: ""
            content = "{\"msg\":\"[$title] $body\"}"
        }

        if (content.isEmpty()) {
            content = "{\"msg\":\"收到空推送消息\"}"
        }

        // 2. Insert into local Room database asynchronously and post system notification
        serviceScope.launch {
            try {
                val repository = LogRepository(applicationContext)
                val insertedLog = repository.insertCustomLog(content)
                
                // Show standard notification alert
                triggerSystemNotification(insertedLog)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist and notify FCM message", e)
            }
        }
    }

    private fun triggerSystemNotification(entry: LogEntry) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

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

        try {
            val json = JSONObject(entry.content)
            if (json.has("heartbeat") || json.has("monitor") || json.has("msg")) {
                isKuma = true
                val monitorObj = if (json.has("monitor")) json.getJSONObject("monitor") else null
                val heartbeatObj = if (json.has("heartbeat")) json.getJSONObject("heartbeat") else null

                val monitorName = monitorObj?.optString("name")
                    ?: json.optString("msg")?.substringBefore("]")?.trim('[', ' ')
                    ?: "FCM Monitor Event"

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
            // Ignore, will use standard text notification
        }

        val title = if (isKuma) kumaTitle else "FCM 实时推送日志"
        val text = if (isKuma) kumaMsg else entry.content

        val notification = NotificationCompat.Builder(this, LogPollingService.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_icon_fg))
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use rolling notification ID
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channel to ensure the new default settings take effect
            notificationManager.deleteNotificationChannel(LogPollingService.CHANNEL_ALERT_ID)
            
            val alertChannel = NotificationChannel(
                LogPollingService.CHANNEL_ALERT_ID,
                "日志更新提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "当产生新日志或收到推送时发送系统通知"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
