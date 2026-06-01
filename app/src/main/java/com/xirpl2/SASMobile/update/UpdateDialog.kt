package com.xirpl2.SASMobile.update

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
            setTextColor(Color.parseColor("#1B1B1F"))
        }

        val descText = TextView(context).apply {
            text = if (updateInfo.body.isNotBlank()) updateInfo.body else "Pembaruan tersedia untuk aplikasi SAS Mobile."
            textSize = 14f
            setTextColor(Color.parseColor("#49454F"))
            setPadding(0, (8 * context.resources.displayMetrics.density).toInt(), 0, 0)
        }

        layout.addView(versionText)
        layout.addView(descText)

        MaterialAlertDialogBuilder(context)
            .setTitle("Update Tersedia")
            .setView(layout)
            .setPositiveButton("Update") { dialog, _ ->
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
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val text = TextView(context).apply {
            text = "Mengunduh update..."
            textSize = 14f
            gravity = Gravity.CENTER
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
