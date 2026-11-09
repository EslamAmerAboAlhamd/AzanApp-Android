package com.eslam.azanapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class Available(val version: String, val url: String) : UpdateResult()
    object Failed : UpdateResult()
}

object UpdateChecker {
    // غيّره لريبو تطبيق الأندرويد عندك (لازم يكون Public وفيه Releases)
    const val REPO = "EslamAmerAboAlhamd/AzanApp-Android"

    fun currentVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }

    private fun parse(v: String): List<Int> =
        v.trim().trimStart('v', 'V').split(".").map { p ->
            p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }

    private fun isNewer(a: String, b: String): Boolean {
        val x = parse(a)
        val y = parse(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val p = x.getOrElse(i) { 0 }
            val q = y.getOrElse(i) { 0 }
            if (p != q) return p > q
        }
        return false
    }

    suspend fun check(ctx: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "AzanApp")

            when (conn.responseCode) {
                404 -> return@withContext UpdateResult.UpToDate   // لسه مفيش Release
                200 -> { }
                else -> return@withContext UpdateResult.Failed
            }

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val url = json.getString("html_url")

            if (isNewer(tag, currentVersion(ctx))) UpdateResult.Available(tag.trimStart('v', 'V'), url)
            else UpdateResult.UpToDate
        } catch (e: Exception) {
            UpdateResult.Failed
        }
    }
}