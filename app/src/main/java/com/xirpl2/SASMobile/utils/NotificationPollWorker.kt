package com.xirpl2.SASMobile.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xirpl2.SASMobile.network.RetrofitClient
import java.util.concurrent.TimeUnit

class NotificationPollWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "notification_poll_worker"
        private const val TAG = "NotifPollWorker"

        /**
         * Schedules a periodic worker that polls for new notifications every 15 minutes.
         * Safe to call multiple times — uses KEEP policy so only one instance runs.
         * Should be called from beranda activities on startup.
         */
        fun schedulePeriodicPoll(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationPollWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Periodic notification poll scheduled (15 min interval)")
        }
    }

    override suspend fun doWork(): Result {
        val userData = SecurePreferences.getUserData(applicationContext)
        val token = userData.getString("auth_token", "") ?: ""
        if (token.isEmpty()) return Result.success()

        return try {
            val response = RetrofitClient.apiService.getNotifications("Bearer $token", page = 1, limit = 20)
            if (response.isSuccessful) {
                val notifications = response.body()?.data ?: emptyList()
                val prefs = applicationContext.getSharedPreferences("NotificationData", Context.MODE_PRIVATE)

                // Cross-session dedup: only show notifications with id > last seen
                val lastSeenId = prefs.getInt("last_seen_notif_id", 0)
                val newNotifs = notifications
                    .filter {
                        !it.isRead &&
                        it.id > lastSeenId &&
                        it.id !in NotificationHelper.shownNotificationIds
                    }
                    .sortedByDescending { it.id }
                    .take(10)

                for ((index, notif) in newNotifs.withIndex()) {
                    NotificationHelper.showNotification(
                        applicationContext,
                        notif.id,
                        notif.title,
                        notif.message,
                        index
                    )
                    NotificationHelper.shownNotificationIds.add(notif.id)
                }

                val apiMaxId = notifications.maxOfOrNull { it.id } ?: 0
                if (apiMaxId > lastSeenId) {
                    prefs.edit().putInt("last_seen_notif_id", apiMaxId).commit()
                }

                val unreadCount = notifications.count { !it.isRead }
                prefs.edit().putInt("notification_count", unreadCount).apply()
                NotificationCounterManager.setCounter(unreadCount)

                Log.d(TAG, "Polled ${notifications.size} notifications, ${newNotifs.size} new shown")
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Poll failed: ${e.message}")
            Result.retry()
        }
    }
}
