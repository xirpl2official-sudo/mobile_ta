package com.xirpl2.SASMobile.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

object UpdateDownloader {

    private var downloadId: Long = -1
    private var onCompleteReceiver: BroadcastReceiver? = null

    fun downloadAndInstall(context: Context, updateInfo: UpdateInfo, onProgress: ((Int) -> Unit)? = null) {
        val fileName = "SASMobile-v${updateInfo.versionName}.apk"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        // Delete old file if exists
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("SAS Mobile Update")
            .setDescription("Mengunduh versi ${updateInfo.versionName}...")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        // Monitor download progress
        val progressThread = Thread {
            var downloading = true
            while (downloading) {
                try {
                    Thread.sleep(500)
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                                val total = cursor.getLong(totalIdx)
                                val downloaded = cursor.getLong(downloadedIdx)
                                val status = cursor.getInt(statusIdx)

                                if (total > 0) {
                                    val progress = ((downloaded * 100) / total).toInt()
                                    onProgress?.invoke(progress)
                                }

                                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                                    downloading = false
                                }
                            }
                        } finally {
                            cursor.close()
                        }
                    }
                } catch (_: Exception) {
                    downloading = false
                }
            }
        }
        progressThread.isDaemon = true
        progressThread.start()

        // Register receiver for download complete
        unregisterReceiver(context)
        onCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    unregisterReceiver(ctx)
                    installApk(ctx, file)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(onCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "File APK tidak ditemukan. Silakan coba lagi.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Gagal membuka installer", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun unregisterReceiver(context: Context) {
        onCompleteReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
            onCompleteReceiver = null
        }
    }
}
