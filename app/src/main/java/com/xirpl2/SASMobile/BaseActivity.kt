package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.xirpl2.SASMobile.utils.UniversalSafeNavigator

/**
 * BaseActivity provides universal lifecycle management, transition state tracking,
 * and memory optimizations for all activities in the application.
 */
abstract class BaseActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "BaseActivity"
    }

    protected var isTransitioning = false
    private var isForegrounded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Global configuration for all activities can be added here
        Log.d(TAG, "onCreate: ${this::class.java.simpleName}")
    }

    override fun onResume() {
        super.onResume()
        isTransitioning = false
        isForegrounded = true
        SASMobileApp.setIsForegrounded(true)
        Log.d(TAG, "onResume: ${this::class.java.simpleName}")
    }

    override fun onPause() {
        isForegrounded = false
        SASMobileApp.setIsForegrounded(false)
        performPostPauseOptimizations()
        super.onPause()
        Log.d(TAG, "onPause: ${this::class.java.simpleName}")
    }

    /**
     * Safely navigates to a target activity using UniversalSafeNavigator.
     * Prevents multiple concurrent transitions.
     */
    protected fun safeNavigateTo(
        targetClass: Class<*>,
        finishCurrent: Boolean = false,
        intentModifier: ((Intent) -> Unit)? = null
    ) {
        if (isTransitioning) return
        isTransitioning = true
        
        UniversalSafeNavigator.navigateTo(this, targetClass, finishCurrent, intentModifier)
    }

    /**
     * Strategic memory optimization triggered after the activity is no longer 
     * in the immediate foreground.
     */
    private fun performPostPauseOptimizations() {
        // Clear activity-specific volatile caches if needed
        // Request a lightweight GC only if the app is moving to background
        if (!isForegrounded) {
            System.gc()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ${this::class.java.simpleName}")
        super.onDestroy()
    }
}
