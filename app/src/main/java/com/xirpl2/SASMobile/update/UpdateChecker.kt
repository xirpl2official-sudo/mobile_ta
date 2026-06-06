package com.xirpl2.SASMobile.update

import android.content.Context
import android.util.Log
import com.xirpl2.SASMobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val currentVersionCode = BuildConfig.VERSION_CODE
            val currentVersionName = BuildConfig.VERSION_NAME

            val url = URL("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "SASMobile")
            if (BuildConfig.GITHUB_TOKEN.isNotEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
            }
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
            val tagName = json.getString("tag_name")
            val releaseBody = json.optString("body", "")

            // Parse remote versionCode from release body convention: "versionCode: N"
            var remoteVersionCode = 0
            val vcMatch = Regex("""versionCode[:\s]+(\d+)""", RegexOption.IGNORE_CASE)
                .find(releaseBody)
            if (vcMatch != null) {
                remoteVersionCode = vcMatch.groupValues[1].toIntOrNull() ?: 0
            }

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

            val remoteVersionName = tagName.removePrefix("v").trim()

            val isNewer = when {
                remoteVersionCode > 0 && currentVersionCode > 0 ->
                    remoteVersionCode > currentVersionCode
                else -> isNewerVersion(currentVersionName, remoteVersionName)
            }

            if (isNewer) {
                return@withContext UpdateInfo(
                    versionName = remoteVersionName,
                    versionCode = remoteVersionCode,
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
        val cleanCurrent = current.trim()
        val cleanRemote = remote.trim()
        val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
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
