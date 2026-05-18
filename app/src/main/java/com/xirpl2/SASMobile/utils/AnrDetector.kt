package com.xirpl2.SASMobile.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.xirpl2.SASMobile.MasukActivity

/**
 * AnrDetector provides mechanisms to record UI interactions and detect potential ANR states,
 * triggering graceful recoveries when the system becomes unresponsive.
 */
object AnrDetector {
    private const val TAG = "AnrDetector"
    private const val ANR_THRESHOLD = 5000L // 5 seconds threshold for unresponsiveness
    
    @Volatile
    private var lastUiInteraction = System.currentTimeMillis()

    /**
     * Records a successful UI interaction (touch, key press).
     */
    fun recordUiInteraction() {
        lastUiInteraction = System.currentTimeMillis()
    }

    /**
     * Checks if the app has been unresponsive for longer than the threshold.
     */
    fun isPotentialAnr(): Boolean {
        val idleTime = System.currentTimeMillis() - lastUiInteraction
        return idleTime > ANR_THRESHOLD
    }

    /**
     * Handles potential ANR by attempting a graceful restart of the application.
     */
    fun handlePotentialAnr(context: Context) {
        if (isPotentialAnr()) {
            Log.w(TAG, "Potential ANR detected (idle for > ${ANR_THRESHOLD}ms). Initiating graceful recovery.")
            
            try {
                // Prepare safe restart intent
                val intent = Intent(context, MasukActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                
                // Kill process after a short delay to allow intent delivery
                Thread {
                    try {
                        Thread.sleep(1000)
                        android.os.Process.killProcess(android.os.Process.myPid())
                    } catch (e: Exception) {
                        // Ignore sleep interruptions
                    }
                }.start()
            } catch (e: Exception) {
                Log.e(TAG, "ANR recovery failed: ${e.message}")
            }
        }
    }
}
