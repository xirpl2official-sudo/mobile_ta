package com.xirpl2.SASMobile

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.DeadObjectException
import android.os.TransactionTooLargeException
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * SASMobileApp is the central application class that manages global error handling,
 * app state tracking, and cross-platform optimizations.
 */
class SASMobileApp : Application() {
    
    companion object {
        private const val TAG = "SASMobileApp"
        private val isForegrounded = AtomicBoolean(false)

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
        
        // Universal UI consistency: Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setupGlobalExceptionHandler()
        setupUniversalOptimizations()
    }

    /**
     * Catches and handles system-level binder deaths and transaction errors.
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

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
                    // Standard error handling for other exceptions
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        }
    }

    /**
     * Initializes optimizations that work universally across all Android platforms.
     */
    private fun setupUniversalOptimizations() {
        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                Log.d(TAG, "onTrimMemory level: $level")
                if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                    // Aggressive cleanup for low memory or backgrounded app
                    clearPendingOperations()
                    System.gc()
                }
            }

            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
            override fun onLowMemory() {
                Log.e(TAG, "System is critically low on memory!")
                clearPendingOperations()
                System.gc()
            }
        })
    }

    /**
     * Clears internal caches and pending work to release resources.
     */
    private fun clearPendingOperations() {
        // Clear global volatile caches here
        // e.g., clear memory image caches, pending network requests, etc.
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
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            
            // Wait slightly for launch to register before exiting process
            Thread.sleep(200)
            exitProcess(0)
        } catch (e: Exception) {
            Log.e(TAG, "App restart failed: ${e.message}")
            exitProcess(1)
        }
    }
}
