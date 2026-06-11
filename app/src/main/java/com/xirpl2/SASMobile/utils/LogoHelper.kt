package com.xirpl2.SASMobile.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.File

object LogoHelper {

    private const val LOGO_FILE = "custom_logo.png"

    fun loadLogo(ivLogo: ImageView) {
        val file = File(ivLogo.context.filesDir, LOGO_FILE)
        if (file.exists()) {
            ivLogo.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        }
    }

    fun saveLogo(context: Context, uri: Uri): Boolean {
        val file = File(context.filesDir, LOGO_FILE)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun resetLogo(context: Context) {
        val file = File(context.filesDir, LOGO_FILE)
        if (file.exists()) file.delete()
    }

    fun hasCustomLogo(context: Context): Boolean {
        return File(context.filesDir, LOGO_FILE).exists()
    }
}
