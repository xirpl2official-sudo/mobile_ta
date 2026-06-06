package com.xirpl2.SASMobile.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * SafeNavigator provides a robust way to handle Activity transitions,
 * specifically targeting Binder transaction failures (DeadObjectException)
 * and Samsung One UI transition race conditions.
 */
object SafeNavigator {
    private const val TAG = "SafeNavigator"
    private const val DEFAULT_TRANSITION_DELAY = 250L // Optimal for Samsung HoneySpace

    /**
     * Navigates to a new activity and finishes the current one safely.
     * Includes a delay to ensure the system process (SurfaceFlinger/WindowManager) 
     * is ready for the new buffer stream.
     */
    fun navigateAndFinish(
        currentActivity: Activity,
        targetClass: Class<*>,
        intentModifier: ((Intent) -> Unit)? = null,
        delayMillis: Long = DEFAULT_TRANSITION_DELAY
    ) {
        if (currentActivity.isFinishing || currentActivity.isDestroyed) {
            Log.w(TAG, "Navigation aborted: Current activity is already finishing/destroyed")
            return
        }

        val intent = Intent(currentActivity, targetClass)
        try {
            intentModifier?.invoke(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Intent modifier failed: ${e.message}")
            return
        }
        
        // Ensure atomic transition by using specific flags
        if (intent.flags == 0) {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            currentActivity.startActivity(intent)
            
            // Critical Fix: Deferred finish to prevent DeadObjectException
            Handler(Looper.getMainLooper()).postDelayed({
                if (!currentActivity.isFinishing && !currentActivity.isDestroyed) {
                    try {
                        currentActivity.finish()
                        Log.d(TAG, "Activity ${currentActivity.localClassName} finished safely after delay")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to finish activity: ${e.message}")
                    }
                }
            }, delayMillis)
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical navigation failure: ${e.message}")
            // Emergency fallback: If startActivity fails, don't finish current activity
        }
    }

    /**
     * Safely starts an activity without finishing the current one.
     */
    fun navigate(
        context: Context,
        targetClass: Class<*>,
        intentModifier: ((Intent) -> Unit)? = null
    ) {
        val intent = Intent(context, targetClass)
        intentModifier?.invoke(intent)
        
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Safe navigation failed: ${e.message}")
        }
    }
}
