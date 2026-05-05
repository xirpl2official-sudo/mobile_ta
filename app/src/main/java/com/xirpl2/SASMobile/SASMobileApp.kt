package com.xirpl2.SASMobile

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SASMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
