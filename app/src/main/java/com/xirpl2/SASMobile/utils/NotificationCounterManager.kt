package com.xirpl2.SASMobile.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Singleton manager for notification counter state.
 * LiveData survives configuration changes and respects lifecycle automatically.
 */
object NotificationCounterManager {

    private val _counter = MutableLiveData(0)
    val counter: LiveData<Int> = _counter

    /**
     * Set the counter value.
     */
    fun setCounter(value: Int) {
        _counter.postValue(value)
    }

    /**
     * Read current count from SharedPreferences and update LiveData.
     * Called from BerandaActivity.onResume to sync on return.
     */
    fun syncFromPreferences(context: Context) {
        val sharedPref = context.getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
        val count = sharedPref.getInt("notification_count", 0)
        _counter.postValue(count)
    }

    /**
     * Clear counter in SharedPreferences and update LiveData.
     */
    fun clearCounter(context: Context) {
        val sharedPref = context.getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
        sharedPref.edit().putInt("notification_count", 0).apply()
        _counter.postValue(0)
    }
}
