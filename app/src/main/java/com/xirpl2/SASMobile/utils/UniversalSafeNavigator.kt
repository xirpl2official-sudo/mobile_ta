package com.xirpl2.SASMobile.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * UniversalSafeNavigator provides a cross-platform, robust navigation utility
 * designed to handle system-level errors and prevent navigation spam.
 */
object UniversalSafeNavigator {
    private const val TAG = "UniversalSafeNavigator"
    
    @Volatile
    private var lastNavigationTime = 0L
    private const val NAVIGATION_THRESHOLD = 1000L // 1 second spam protection
    private const val DEFAULT_TRANSITION_DELAY = 300L // Stable buffer for all manufacturers

    /**
     * Executes a safe navigation to a target activity.
     * Handles Binder death, security exceptions, and missing activities.
     */
    fun safeNavigate(
        context: Context,
        intent: Intent,
        finishCurrent: Boolean = false,
        onFailed: ((Exception) -> Unit)? = null
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationTime < NAVIGATION_THRESHOLD) {
            Log.w(TAG, "Navigation suppressed: Spam protection active")
            return
        }
        lastNavigationTime = currentTime

        try {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            if (finishCurrent && context is Activity) {
                // Deferred finish to prevent DeadObjectException during transition
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!context.isFinishing && !context.isDestroyed) {
                        context.finish()
                    }
                }, DEFAULT_TRANSITION_DELAY)
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Target activity not found: ${e.message}")
            handleNavigationError(context, "Halaman tidak ditemukan")
            onFailed?.invoke(e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during navigation: ${e.message}")
            handleNavigationError(context, "Kesalahan keamanan sistem")
            onFailed?.invoke(e)
        } catch (e: Exception) {
            if (e is DeadObjectException || e.cause is DeadObjectException) {
                handleBinderDeath(context)
            } else {
                Log.e(TAG, "Unexpected navigation error: ${e.message}")
                onFailed?.invoke(e)
            }
        }
    }

    /**
     * Convenience method to navigate by class type.
     */
    fun navigateTo(
        context: Context,
        targetClass: Class<*>,
        finishCurrent: Boolean = false,
        intentModifier: ((Intent) -> Unit)? = null
    ) {
        val intent = Intent(context, targetClass)
        intentModifier?.invoke(intent)
        safeNavigate(context, intent, finishCurrent)
    }

    /**
     * Handles critical binder death by attempting a safe system recovery.
     */
    fun handleBinderDeath(context: Context) {
        Log.e(TAG, "Binder transaction failed (DeadObject). Requesting app recovery.")
        // In case of binder death, the app process is likely unstable.
        // We notify the application class to handle the restart logic.
        try {
            val app = context.applicationContext as? com.xirpl2.SASMobile.SASMobileApp
            app?.handleBinderDeathGracefully()
        } catch (e: Exception) {
            // Hard fallback
            Toast.makeText(context, "Koneksi sistem terputus. Silakan buka kembali aplikasi.", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleNavigationError(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
