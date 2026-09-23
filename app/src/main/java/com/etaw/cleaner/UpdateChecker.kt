package com.etaw.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    // GitHub 仓库（Release 发布地址）
    const val REPO = "ETQWFD/ETAW-Cleaner"
    const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"

    data class UpdateInfo(
        val tag: String,
        val name: String,
        val body: String,
        val apkUrl: String?
    )

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "ETAW-Cleaner")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "")
                val name = json.optString("name", "")
                val desc = json.optString("body", "")
                var apk: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val url = a.optString("browser_download_url", "")
                        if (url.endsWith(".apk")) { apk = url; break }
                    }
                }
                UpdateInfo(tag, name, desc, apk)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 返回 true 表示有新版本 */
    fun hasNewer(currentVersionName: String, latestTag: String): Boolean {
        val a = normalize(currentVersionName)
        val b = normalize(latestTag)
        return compare(a, b) < 0
    }

    private fun normalize(v: String): List<Int> =
        v.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
