package com.xirpl2.SASMobile.network

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.xirpl2.SASMobile.MasukActivity
import com.xirpl2.SASMobile.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp Authenticator that intercepts 401 responses, attempts to refresh
 * the access token using the stored refresh token, and retries the request.
 * If refresh fails, clears the session and redirects to the login screen.
 */
class TokenAuthenticator(private val context: Context) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
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
                val refreshResponse = runBlocking {
                    RetrofitClient.apiService.refreshToken(RefreshRequest(refreshToken))
                }

                if (refreshResponse.isSuccessful && refreshResponse.body()?.data != null) {
                    val userData = refreshResponse.body()!!.data!!
                    val newToken = userData.token
                    val newRefreshToken = userData.refresh_token

                    if (newToken.isNullOrEmpty()) {
                        Log.e(TAG, "Refresh succeeded but no new token returned")
                        forceLogout()
                        return null
                    }

                    // Save new tokens
                    with(prefs.edit()) {
                        putString("auth_token", newToken)
                        if (!newRefreshToken.isNullOrEmpty()) {
                            putString("refresh_token", newRefreshToken)
                        }
                        apply()
                    }

                    // Also update UserData prefs
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

        // Redirect to login screen on the main thread
        val intent = Intent(context, MasukActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        Handler(Looper.getMainLooper()).post {
            context.startActivity(intent)
        }
    }
}
