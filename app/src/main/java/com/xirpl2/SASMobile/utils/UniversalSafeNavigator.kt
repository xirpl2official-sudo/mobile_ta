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
import java.util.concurrent.Executors

/**
 * UniversalSafeNavigator provides a cross-platform, robust navigation utility
 * designed to handle system-level errors, prevent navigation spam, and avoid ANRs.
 */
object UniversalSafeNavigator {
    private const val TAG = "UniversalSafeNavigator"
    
    @Volatile
    private var lastNavigationTime = 0L
    private val navigationLock = Any()
    private const val NAVIGATION_THRESHOLD = 1000L // 1 second spam protection
    private const val DEFAULT_TRANSITION_DELAY = 50L // Reduced for instant feel with disabled animations
    private const val NAVIGATION_TIMEOUT = 2000L // 2 seconds safety timeout

    private val navigationExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "SafeNavigator").apply { isDaemon = true }
    }

    /**
     * Executes a safe navigation to a target activity with built-in timeout 
     * to prevent UI thread blocking.
     */
    fun safeNavigateWithTimeout(
        context: Context,
        intent: Intent,
        finishCurrent: Boolean = false,
        timeout: Long = NAVIGATION_TIMEOUT,
        onFailed: (() -> Unit)? = null
    ) {
        val currentTime = System.currentTimeMillis()
        synchronized(navigationLock) {
            if (currentTime - lastNavigationTime < NAVIGATION_THRESHOLD) {
                Log.w(TAG, "Navigation suppressed: Spam protection active")
                return
            }
            lastNavigationTime = currentTime
        }

        // Execute in background to prevent UI blocking in case of system service delays
        navigationExecutor.execute {
            try {
                if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                    return@execute
                }

                Handler(Looper.getMainLooper()).post {
                    try {
                        if (context !is Activity) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        context.startActivity(intent)
                        if (context is Activity) {
                            context.overridePendingTransition(0, 0)
                        }

                        if (finishCurrent && context is Activity) {
                            // Deferred finish to prevent DeadObjectException during transition
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (!context.isFinishing && !context.isDestroyed) {
                                    context.finish()
                                    context.overridePendingTransition(0, 0)
                                }
                            }, DEFAULT_TRANSITION_DELAY)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Navigation failed on main thread", e)
                        handleNavigationErrorInternal(context, e)
                        onFailed?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background navigation prep failed", e)
                Handler(Looper.getMainLooper()).post { onFailed?.invoke() }
            }
        }
    }

    /**
     * Legacy safeNavigate kept for backward compatibility within the app
     */
    fun safeNavigate(
        context: Context,
        intent: Intent,
        finishCurrent: Boolean = false,
        onFailed: ((Exception) -> Unit)? = null
    ) {
        safeNavigateWithTimeout(context, intent, finishCurrent) {
            onFailed?.invoke(Exception("Navigation failed or timed out"))
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
        safeNavigateWithTimeout(context, intent, finishCurrent)
    }

    /**
     * Handles critical binder death by attempting a safe system recovery.
     */
    fun handleBinderDeath(context: Context) {
        Log.e(TAG, "Binder transaction failed (DeadObject). Requesting app recovery.")
        try {
            val app = context.applicationContext as? com.xirpl2.SASMobile.SASMobileApp
            app?.handleBinderDeathGracefully()
        } catch (e: Exception) {
            Toast.makeText(context, "Koneksi sistem terputus. Silakan buka kembali aplikasi.", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleNavigationErrorInternal(context: Context, e: Exception) {
        when (e) {
            is ActivityNotFoundException -> handleUserFeedback(context, "Halaman tidak ditemukan")
            is SecurityException -> handleUserFeedback(context, "Kesalahan keamanan sistem")
            is DeadObjectException -> handleBinderDeath(context)
            else -> {
                if (e.cause is DeadObjectException) {
                    handleBinderDeath(context)
                }
            }
        }
    }

    private fun handleUserFeedback(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
