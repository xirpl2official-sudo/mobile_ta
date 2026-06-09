package com.xirpl2.SASMobile.helper

import androidx.test.platform.app.InstrumentationRegistry

object AuthHelper {

    fun loginAsSiswiP() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = ctx.getSharedPreferences("UserDataPref", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("nama_siswa", "Siti Aminah")
            .putString("nis", "20228PL001")
            .putString("jenis_kelamin", "P")
            .putString("user_role", "siswa")
            .apply()
        val session = ctx.getSharedPreferences("UserSessionPref", android.content.Context.MODE_PRIVATE)
        session.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("user_role", "siswa")
            .putString("user_jk", "P")
            .apply()
    }

    fun loginAsSiswaL() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = ctx.getSharedPreferences("UserDataPref", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("nama_siswa", "Ahmad Fauzi")
            .putString("nis", "20228PL002")
            .putString("jenis_kelamin", "L")
            .putString("user_role", "siswa")
            .apply()
        val session = ctx.getSharedPreferences("UserSessionPref", android.content.Context.MODE_PRIVATE)
        session.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("user_role", "siswa")
            .putString("user_jk", "L")
            .apply()
    }

    fun loginAsAdmin() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = ctx.getSharedPreferences("UserDataPref", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("nama_siswa", "Administrator")
            .putString("nis", "198501012020011001")
            .putString("jenis_kelamin", "L")
            .putString("user_role", "admin")
            .apply()
        val session = ctx.getSharedPreferences("UserSessionPref", android.content.Context.MODE_PRIVATE)
        session.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("user_role", "admin")
            .apply()
    }

    fun loginAsGuruP() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = ctx.getSharedPreferences("UserDataPref", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("nama_siswa", "Ustadzah Aisyah")
            .putString("nis", "198601012020012002")
            .putString("jenis_kelamin", "P")
            .putString("user_role", "guru")
            .apply()
        val session = ctx.getSharedPreferences("UserSessionPref", android.content.Context.MODE_PRIVATE)
        session.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("user_role", "guru")
            .putString("user_jk", "P")
            .apply()
    }

    fun loginAsGuruL() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = ctx.getSharedPreferences("UserDataPref", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("nama_siswa", "Ustadz Ahmad")
            .putString("nis", "198701012020012003")
            .putString("jenis_kelamin", "L")
            .putString("user_role", "guru")
            .apply()
        val session = ctx.getSharedPreferences("UserSessionPref", android.content.Context.MODE_PRIVATE)
        session.edit()
            .putString("auth_token", TestData.VALID_TOKEN)
            .putString("user_role", "guru")
            .putString("user_jk", "L")
            .apply()
    }

    fun clearSession() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.getSharedPreferences("UserDataPref", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        ctx.getSharedPreferences("UserSessionPref", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
