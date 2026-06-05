package com.xirpl2.SASMobile.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xirpl2.SASMobile.NotificationCenterActivity
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.network.RetrofitClient
import java.util.concurrent.TimeUnit

object NotificationHelper {

    const val CHANNEL_ID = "sas_notifications"
    private const val CHANNEL_NAME = "Notifikasi SAS"
    private const val CHANNEL_DESC = "Notifikasi dari sistem absensi sholat"
    private const val TAG = "NotificationHelper"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun schedulePollWorker(context: Context) {
        val request = PeriodicWorkRequestBuilder<NotificationPollWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NotificationPollWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun showNotification(context: Context, id: Int, title: String, message: String, index: Int = 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted, skipping notification")
                return
            }
        }

        val intent = Intent(context, NotificationCenterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        try {
            val uniqueId = id * 1000 + index
            NotificationManagerCompat.from(context).notify(uniqueId, notification)
            Log.d(TAG, "Shown system notification uniqueId=$uniqueId, apiId=$id, title=$title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show system notification: ${e.message}")
        }
    }

    /**
     * Immediate poll for notifications. Safe to call from onResume.
     * Rate-limited: won't run more than once per minute.
     */
    suspend fun pollAndShowNotifications(context: Context) {
        val userData = SecurePreferences.getUserData(context)
        val token = userData.getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        val prefs = context.getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
        val lastPoll = prefs.getLong("last_immediate_poll", 0)
        val now = System.currentTimeMillis()
        if (now - lastPoll < 60_000) {
            Log.d(TAG, "Immediate poll skipped (rate limited)")
            return
        }
        prefs.edit().putLong("last_immediate_poll", now).apply()

        try {
            val response = RetrofitClient.apiService.getNotifications("Bearer $token", page = 1, limit = 20)
            if (!response.isSuccessful) {
                Log.w(TAG, "API returned ${response.code()}")
                return
            }

            val notifications = response.body()?.data ?: emptyList()
            val lastSeenId = prefs.getInt("last_seen_notif_id", 0)

            // Show ALL unread notifications as system/native notifications
            val newNotifs = notifications
                .filter {
                    !it.isRead &&
                    it.id > lastSeenId
                }
                .sortedByDescending { it.id }
                .take(10) // Limit to 10 notifications at once to avoid spam

            for ((index, notif) in newNotifs.withIndex()) {
                showNotification(context, notif.id, notif.title, notif.message, index)
            }

            // Always update last_seen to max API ID to prevent re-showing
            val apiMaxId = notifications.maxOfOrNull { it.id } ?: 0
            if (apiMaxId > lastSeenId) {
                prefs.edit().putInt("last_seen_notif_id", apiMaxId).apply()
            }

            val unreadCount = notifications.count { !it.isRead }
            prefs.edit().putInt("notification_count", unreadCount).apply()
            NotificationCounterManager.setCounter(unreadCount)

            Log.d(TAG, "Immediate poll: ${notifications.size} total, ${newNotifs.size} new shown")
        } catch (e: Exception) {
            Log.w(TAG, "Immediate poll failed: ${e.message}")
        }
    }
}
