package com.xirpl2.SASMobile

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SASMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
