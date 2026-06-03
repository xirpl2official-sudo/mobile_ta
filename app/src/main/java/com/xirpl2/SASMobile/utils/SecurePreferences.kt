package com.xirpl2.SASMobile.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Provides encrypted SharedPreferences for sensitive data (auth tokens, user session).
 * Migrates existing plaintext SharedPreferences to encrypted storage on first access.
 */
object SecurePreferences {

    private const val ENC_PREFIX = "enc_"

    fun getUserSession(context: Context): SharedPreferences {
        return getEncrypted(context, "user_session")
    }

    fun getUserData(context: Context): SharedPreferences {
        return getEncrypted(context, "UserData")
    }

    /** Temporary storage for password-reset flow data (NIS, email, OTP). */
    fun getPasswordResetData(context: Context): SharedPreferences {
        return getEncrypted(context, "password_reset")
    }

    /** Brute force protection state (failed attempts, cooldown timestamp). */
    fun getBruteForceData(context: Context): SharedPreferences {
        return getEncrypted(context, "login_brute_force")
    }

    fun clearPasswordResetData(context: Context) {
        getPasswordResetData(context).edit().clear().commit()
    }

    private fun getEncrypted(context: Context, plainFileName: String): SharedPreferences {
        val encFileName = ENC_PREFIX + plainFileName

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            encFileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // One-time migration from plaintext SharedPreferences
        if (encryptedPrefs.all.isEmpty()) {
            val plainPrefs = context.getSharedPreferences(plainFileName, Context.MODE_PRIVATE)
            if (plainPrefs.all.isNotEmpty()) {
                val editor = encryptedPrefs.edit()
                for ((key, value) in plainPrefs.all) {
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                    }
                }
                val migrated = editor.commit()
                // Only clear plaintext after successful migration
                if (migrated) {
                    plainPrefs.edit().clear().commit()
                }
            }
        }

        return encryptedPrefs
    }
}
