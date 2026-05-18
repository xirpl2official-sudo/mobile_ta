package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.xirpl2.SASMobile.utils.AnrDetector
import com.xirpl2.SASMobile.utils.UniversalSafeNavigator
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BaseActivity provides universal lifecycle management, transition state tracking,
 * ANR protection, and memory optimizations for all activities.
 */
abstract class BaseActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "BaseActivity"
    }

    protected val isTransitioning = AtomicBoolean(false)
    private var isForegrounded = false
    private val transitionHandler = Handler(Looper.getMainLooper())
    protected val anrWatchdogActive = AtomicBoolean(false)

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isTransitioning.get()) {
                Log.w(TAG, "Back press ignored during active transition")
                return
            }
            // Disable this callback and call onBackPressed to trigger default behavior
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        Log.d(TAG, "onCreate: ${this::class.java.simpleName}")
    }

    override fun onResume() {
        super.onResume()
        isTransitioning.set(false)
        isForegrounded = true
        anrWatchdogActive.set(false)
        SASMobileApp.setIsForegrounded(true)
        AnrDetector.recordUiInteraction()
        Log.d(TAG, "onResume: ${this::class.java.simpleName}")
    }

    override fun onPause() {
        isForegrounded = false
        SASMobileApp.setIsForegrounded(false)
        
        // Prevent pending transitions from blocking UI during app pause
        transitionHandler.removeCallbacksAndMessages(null)
        anrWatchdogActive.set(true)
        
        super.onPause()
        Log.d(TAG, "onPause: ${this::class.java.simpleName}")
    }

    /**
     * Safely navigates to a target activity using UniversalSafeNavigator with timeout protection.
     */
    protected fun safeNavigateTo(
        targetClass: Class<*>,
        finishCurrent: Boolean = false,
        timeout: Long = 1500L,
        force: Boolean = false   // ← TAMBAHKAN parameter ini
    ) {
        if (!force && (isTransitioning.get() || anrWatchdogActive.get())) {
            Log.w(TAG, "Navigation blocked: Transition in progress or activity paused")
            return
        }
        
        isTransitioning.set(true)
        anrWatchdogActive.set(false)  // ← TAMBAHKAN: clear watchdog saat force navigate
        
        val intent = Intent(this, targetClass)
        UniversalSafeNavigator.safeNavigateWithTimeout(
            this,
            intent,
            finishCurrent,
            timeout
        ) {
            transitionHandler.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    isTransitioning.set(false)
                }
            }, 500)
        }
    }
    // --- ANR Protection: Record user interactions ---

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        AnrDetector.recordUiInteraction()
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        AnrDetector.recordUiInteraction()
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        transitionHandler.removeCallbacksAndMessages(null)
        Log.d(TAG, "onDestroy: ${this::class.java.simpleName}")
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
