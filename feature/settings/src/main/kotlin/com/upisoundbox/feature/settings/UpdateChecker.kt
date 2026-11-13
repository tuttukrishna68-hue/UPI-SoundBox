package com.upisoundbox.feature.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object UpdateChecker {
    const val RELEASES_URL =
        "https://api.github.com/repos/tuttukrishna68-hue/UPI-SoundBox/releases/latest"
    const val UPDATE_MANIFEST_URL =
        "https://raw.githubusercontent.com/tuttukrishna68-hue/UPI-SoundBox/main/update.json"

    fun currentVersionName(context: Context): String {
        return try {
            packageInfo(context).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    suspend fun check(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val packageInfo = packageInfo(context)
            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val currentVersionName = packageInfo.versionName ?: "unknown"

            val latestRelease = fetchLatestRelease(currentVersionName)
            val remoteInfo = latestRelease ?: fetchManifest(currentVersionName)
            if (remoteInfo.versionCode > currentVersionCode) {
                UpdateCheckResult.Available(remoteInfo)
            } else {
                UpdateCheckResult.UpToDate(currentVersionName)
            }
        } catch (_: Exception) {
            UpdateCheckResult.Failed(
                "Couldn't check right now. Payment monitoring remains available offline."
            )
        }
    }

    private fun fetchLatestRelease(currentVersionName: String): UpdateInfo? {
        val json = fetchJson(RELEASES_URL, currentVersionName) ?: return null
        val tagName = json.optString("tag_name").trim()
        val versionName = tagName.removePrefix("v").ifBlank {
            json.optString("name").trim().removePrefix("v")
        }
        val assets = json.optJSONArray("assets") ?: return null
        var assetName = ""
        var downloadUrl = ""
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val candidateName = asset.optString("name").trim()
            val candidateUrl = asset.optString("browser_download_url").trim()
            if (candidateName.lowercase(Locale.US).endsWith(".apk") && candidateUrl.isNotBlank()) {
                assetName = candidateName
                downloadUrl = candidateUrl
                break
            }
        }
        val versionCode = Regex("-([0-9]+)\\.apk$", RegexOption.IGNORE_CASE)
            .find(assetName)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?: return null
        if (versionCode <= 0L || versionName.isBlank() || downloadUrl.isBlank()) return null

        return UpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            downloadUrl = downloadUrl,
            releaseNotes = json.optString("body").trim()
        )
    }

    private fun fetchManifest(currentVersionName: String): UpdateInfo {
        val json = fetchJson(UPDATE_MANIFEST_URL, currentVersionName)
            ?: throw IOException("GitHub update manifest unavailable")
        val remoteVersionCode = json.optLong("versionCode", -1L)
        val remoteVersionName = json.optString("versionName").trim()
        val downloadUrl = json.optString("downloadUrl").trim()
        val releaseNotes = json.optString("releaseNotes").trim()
        if (remoteVersionCode <= 0L || remoteVersionName.isBlank() || downloadUrl.isBlank()) {
            throw IOException("The update manifest is incomplete")
        }
        return UpdateInfo(
            versionCode = remoteVersionCode,
            versionName = remoteVersionName,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes
        )
    }

    private fun fetchJson(url: String, currentVersionName: String): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                useCaches = false
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "UPI-Soundbox/$currentVersionName")
            }
            when (connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                404 -> null
                else -> throw IOException("GitHub returned HTTP ${connection.responseCode}")
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun packageInfo(context: Context): PackageInfo {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}

