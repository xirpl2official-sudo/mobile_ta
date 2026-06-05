package com.xirpl2.SASMobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xirpl2.SASMobile.utils.AnrDetector
import com.xirpl2.SASMobile.utils.NotificationHelper
import com.xirpl2.SASMobile.utils.UniversalSafeNavigator
import com.xirpl2.SASMobile.update.UpdateChecker
import com.xirpl2.SASMobile.update.UpdateDialog
import com.xirpl2.SASMobile.update.UpdateDownloader
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BaseActivity provides universal lifecycle management, transition state tracking,
 * ANR protection, and memory optimizations for all activities.
 */
abstract class BaseActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "BaseActivity"
        private var updateCheckedThisSession = false
        private var notificationsCheckedThisSession = false
        private var notifiedRequestedThisSession = false
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
        requestNotificationPermission()
        checkForUpdate()
        checkNotificationsImmediate()
    }

    private fun checkNotificationsImmediate() {
        if (notificationsCheckedThisSession) return
        lifecycleScope.launch {
            try {
                val userData = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@BaseActivity)
                val token = userData.getString("auth_token", "") ?: ""
                if (token.isEmpty()) return@launch
                if (notificationsCheckedThisSession) return@launch
                notificationsCheckedThisSession = true
                NotificationHelper.pollAndShowNotifications(this@BaseActivity)
            } catch (e: Exception) {
                Log.w(TAG, "Immediate notification check failed: ${e.message}")
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (notifiedRequestedThisSession) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) return

        notifiedRequestedThisSession = true
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
    }

    private fun checkForUpdate() {
        if (updateCheckedThisSession) return
        updateCheckedThisSession = true
        lifecycleScope.launch {
            try {
                val updateInfo = UpdateChecker.checkForUpdate(this@BaseActivity)
                if (updateInfo != null && !isFinishing && !isDestroyed) {
                    UpdateDialog.show(this@BaseActivity, updateInfo) {
                        val downloadingDialog = UpdateDialog.showDownloading(this@BaseActivity)
                        UpdateDownloader.downloadAndInstall(
                            this@BaseActivity, updateInfo,
                            onComplete = { downloadingDialog.dismiss() }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed: ${e.message}")
            }
        }
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

    /**
     * Prevents rapid double-clicks by tracking last click time per view hash.
     * Use on submit/save/delete buttons. Minimum interval: 600ms.
     */
    private val clickTimestamps = ConcurrentHashMap<Int, Long>()

    protected fun View.setSafeOnClickListener(minInterval: Long = 600L, action: () -> Unit) {
        setOnClickListener {
            val now = System.currentTimeMillis()
            val lastClick = clickTimestamps[this.hashCode()] ?: 0L
            if (now - lastClick < minInterval) return@setOnClickListener
            clickTimestamps[this.hashCode()] = now
            action()
        }
    }

    override fun onDestroy() {
        transitionHandler.removeCallbacksAndMessages(null)
        clickTimestamps.clear()
        Log.d(TAG, "onDestroy: ${this::class.java.simpleName}")
        super.onDestroy()
    }
}
