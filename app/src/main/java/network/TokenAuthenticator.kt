package com.xirpl2.SASMobile.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xirpl2.SASMobile.MasukActivity
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.SASMobileApp
import com.xirpl2.SASMobile.model.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator that intercepts 401 responses, attempts to refresh
 * the access token using the stored refresh token, and retries the request.
 * If refresh fails, clears the session and redirects to the login screen.
 *
 * DUAL-STORE CONSISTENCY: auth_token is kept in both "user_session" and
 * "UserData" SharedPreferences. This class reads from "user_session" and
 * writes the refreshed token to BOTH stores. See also: MasukActivity.saveUserSession()
 * and MasukActivity.clearUserSession().
 */
class TokenAuthenticator(private val context: Context) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val SESSION_EXPIRED_NOTIFICATION_ID = 9001
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry the refresh endpoint itself to avoid infinite loops
        if (response.request.url.encodedPath.contains("tokens/refresh")) {
            return null
        }

        // Don't retry more than once
        val priorAttempts = generateSequence(response.priorResponse) { it.priorResponse }.count()
        if (priorAttempts >= 1) {
            return null
        }

        synchronized(this) {
            val prefs = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(context)
            val currentToken = prefs.getString("auth_token", null)

            // If another thread already refreshed the token (it changed since this request was made),
            // just retry with the new token
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()
            if (currentToken != null && requestToken != null && currentToken != requestToken) {
                Log.d(TAG, "Token already refreshed by another thread, retrying with new token")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Attempt refresh
            val refreshToken = prefs.getString("refresh_token", null)
            if (refreshToken.isNullOrEmpty()) {
                Log.w(TAG, "No refresh token available, redirecting to login")
                forceLogout()
                return null
            }

            return try {
                val refreshResponse = RetrofitClient.apiService
                    .refreshTokenSync(RefreshRequest(refreshToken))
                    .execute()

                if (refreshResponse.isSuccessful && refreshResponse.body()?.data != null) {
                    val userData = refreshResponse.body()!!.data!!
                    val newToken = userData.token
                    val newRefreshToken = userData.refresh_token

                    if (newToken.isNullOrEmpty()) {
                        Log.e(TAG, "Refresh succeeded but no new token returned")
                        forceLogout()
                        return null
                    }

                    // Store 1: update "user_session" with new tokens
                    with(prefs.edit()) {
                        putString("auth_token", newToken)
                        if (!newRefreshToken.isNullOrEmpty()) {
                            putString("refresh_token", newRefreshToken)
                        }
                        apply()
                    }

                    // Store 2: keep "UserData" in sync (some activities read auth_token from here)
                    val userDataPrefs = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(context)
                    userDataPrefs.edit().putString("auth_token", newToken).apply()

                    Log.d(TAG, "Token refreshed successfully, retrying request")
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                } else {
                    Log.w(TAG, "Token refresh failed (code=${refreshResponse.code()}), redirecting to login")
                    forceLogout()
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh error: ${e.message}")
                forceLogout()
                null
            }
        }
    }

    private fun forceLogout() {
        // Clear all session data
        com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(context)
            .edit().clear().apply()
        com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(context)
            .edit().clear().apply()

        val loginIntent = Intent(context, MasukActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        if (SASMobileApp.isAppInForeground()) {
            // App is in foreground: safe to start activity directly (Android 10+ allows this)
            Handler(Looper.getMainLooper()).post {
                context.startActivity(loginIntent)
            }
        } else {
            // App is in background: Android 10+ blocks startActivity from background.
            // Show a notification so the user can tap to return to the login screen.
            Log.w(TAG, "App in background, showing session-expired notification instead of starting activity")
            showSessionExpiredNotification(loginIntent)
        }
    }

    /**
     * Displays a high-priority notification that takes the user to the login screen.
     * Called when token refresh fails while the app is in the background, since
     * Android 10+ (API 29) restricts starting activities from background contexts.
     */
    private fun showSessionExpiredNotification(loginIntent: Intent) {
        val channelId = "session_expired"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sesi Berakhir",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat sesi login telah berakhir"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            loginIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sesi Berakhir")
            .setContentText("Sesi login Anda telah berakhir. Ketuk untuk masuk kembali.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(SESSION_EXPIRED_NOTIFICATION_ID, notification)
    }
}
