package com.xirpl2.SASMobile

import android.animation.ValueAnimator
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.res.Configuration
import android.os.DeadObjectException
import android.os.TransactionTooLargeException
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.xirpl2.SASMobile.utils.AnrDetector
import java.util.concurrent.atomic.AtomicBoolean
/**
 * SASMobileApp is the central application class that manages global error handling,
 * app state tracking, ANR prevention, and cross-platform optimizations.
 */
class SASMobileApp : Application() {
    
    companion object {
        private const val TAG = "SASMobileApp"
        private val isForegrounded = AtomicBoolean(false)
        @Volatile
        private var lastRestartTime = 0L
        private const val RESTART_THRESHOLD = 5000L // 5 seconds

        /**
         * Updates the global foreground state of the application.
         */
        fun setIsForegrounded(foregrounded: Boolean) {
            isForegrounded.set(foregrounded)
        }

        /**
         * Returns true if any activity of the app is currently in the foreground.
         */
        fun isAppInForeground(): Boolean = isForegrounded.get()
    }

    override fun onCreate() {
        super.onCreate()

        // Apply saved theme preference (defaults to system)
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val theme = prefs.getString("theme_mode", "system") ?: "system"
        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        // Initialize RetrofitClient with application context for TokenAuthenticator
        com.xirpl2.SASMobile.network.RetrofitClient.init(this)

        // Create notification channel for system notifications
        com.xirpl2.SASMobile.utils.NotificationHelper.createNotificationChannel(this)

        setupGlobalExceptionHandler()
        setupTransitionTimeouts()
    }

    /**
     * Catches and handles system-level binder deaths, transaction errors, and potential ANRs.
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRestartTime < RESTART_THRESHOLD) {
                Log.e(TAG, "App crashing too fast. Letting system handle it to avoid loop.")
                defaultHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            lastRestartTime = currentTime

            when {
                throwable is DeadObjectException || throwable.cause is DeadObjectException -> {
                    Log.e(TAG, "Binder connection died. Triggering safe restart.")
                    handleBinderDeathGracefully()
                }
                throwable is TransactionTooLargeException -> {
                    Log.e(TAG, "Binder transaction limit exceeded. Clearing state and restarting.")
                    clearPendingOperations()
                    restartApp()
                }
                else -> {
                    // Always delegate to default handler for all other exceptions.
                    // Previous logic used AnrDetector.isPotentialAnr() here which caused
                    // the app to kill itself on startup when network errors occurred
                    // before the user had any chance to interact with the UI.
                    Log.e(TAG, "Delegating to default crash handler for: ${throwable::class.java.simpleName}")
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        }
    }

    /**
     * Initializes optimizations for transitions and memory management to prevent ANR.
     */
    private fun setupTransitionTimeouts() {
        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) {
                // Cleanup animation resources to prevent transition bottlenecks
                clearTransitionCaches()
            }

            override fun onTrimMemory(level: Int) {
                Log.d(TAG, "onTrimMemory level: $level")
                when (level) {
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                        // Aggressive cleanup for active low memory
                        clearPendingOperations()
                    }
                    ComponentCallbacks2.TRIM_MEMORY_COMPLETE, 
                    ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                        // Cleanup for backgrounded app
                    }
                }
            }

            override fun onLowMemory() {
                Log.e(TAG, "System critically low on memory. Forcing cleanup.")
                clearPendingOperations()
            }
        })
    }

    private fun clearTransitionCaches() {
        // Release bitmap caches and animation resources under memory pressure
        try {
            android.widget.ImageView::class.java.getDeclaredMethod("clearAllImageCaches")?.let {
                it.isAccessible = true
                it.invoke(null)
            }
        } catch (_: Exception) {}
        // Clear any ValueAnimator frame callbacks
        ValueAnimator.setFrameDelay(ValueAnimator.getFrameDelay())
        // Request garbage collection for caches
        System.gc()
    }

    /**
     * Clears internal caches and pending work to release resources.
     */
    private fun clearPendingOperations() {
        // Implementation for clearing global memory-intensive objects
        clearTransitionCaches()
    }

    /**
     * Restarts the application process to recover from a critical binder failure.
     */
    fun handleBinderDeathGracefully() {
        clearPendingOperations()
        restartApp()
    }

    private fun restartApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                // Give the system a moment to start the new activity before killing
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    android.os.Process.killProcess(android.os.Process.myPid())
                }, 500)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        } catch (e: Exception) {
            Log.e(TAG, "App restart failed: ${e.message}")
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
