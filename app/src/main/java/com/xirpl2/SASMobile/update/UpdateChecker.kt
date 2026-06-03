package com.xirpl2.SASMobile.update

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val fileName: String,
    val body: String
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_OWNER = "xirpl2official-sudo"
    private const val GITHUB_REPO = "mobile_ta"

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager
                    .getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }

            val url = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "SASMobile")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode != 200) {
                Log.e(TAG, "GitHub API returned ${conn.responseCode}")
                conn.disconnect()
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val tagName = json.getString("tag_name") // e.g. "v1.0.4"
            val releaseBody = json.optString("body", "")

            // Parse version code from tag: v1.0.4 -> try to find APK asset with version code
            val assets = json.getJSONArray("assets")
            var downloadUrl = ""
            var fileName = ""

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    fileName = name
                    break
                }
            }

            if (downloadUrl.isEmpty()) {
                Log.w(TAG, "No APK asset found in latest release")
                return@withContext null
            }

            // Parse version code from tag name: "v1.0.4" -> extract version code
            // Convention: versionName in build.gradle matches tag, versionCode is integer
            // We compare by parsing the tag and comparing with current versionName
            val remoteVersionName = tagName.removePrefix("v")
            val currentVersionName = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName ?: "0"

            if (isNewerVersion(currentVersionName, remoteVersionName)) {
                return@withContext UpdateInfo(
                    versionName = remoteVersionName,
                    versionCode = 0, // not used for comparison
                    downloadUrl = downloadUrl,
                    fileName = fileName,
                    body = releaseBody
                )
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }

    private fun isNewerVersion(current: String, remote: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
