package com.xirpl2.SASMobile.update

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.R

object UpdateDialog {

    fun show(context: Context, updateInfo: UpdateInfo, onDownload: () -> Unit) {
        val padding = (16 * context.resources.displayMetrics.density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        val versionText = TextView(context).apply {
            text = "Versi ${updateInfo.versionName} tersedia"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        }

        val descText = TextView(context).apply {
            text = if (updateInfo.body.isNotBlank()) updateInfo.body else "Pembaruan tersedia untuk aplikasi SAS Mobile."
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, (8 * context.resources.displayMetrics.density).toInt(), 0, 0)
        }

        layout.addView(versionText)
        layout.addView(descText)

        MaterialAlertDialogBuilder(context)
            .setTitle("Update Tersedia")
            .setView(layout)
            .setPositiveButton("Perbarui") { dialog, _ ->
                dialog.dismiss()
                onDownload()
            }
            .setNegativeButton("Nanti") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    fun showDownloading(context: Context): androidx.appcompat.app.AlertDialog {
        val padding = (24 * context.resources.displayMetrics.density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = ContextCompat.getColorStateList(context, R.color.primary)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val text = TextView(context).apply {
            text = "Mengunduh update..."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, (12 * context.resources.displayMetrics.density).toInt(), 0, 0)
        }

        layout.addView(progressBar)
        layout.addView(text)

        return MaterialAlertDialogBuilder(context)
            .setTitle("Mengunduh")
            .setView(layout)
            .setCancelable(false)
            .show()
    }
}
