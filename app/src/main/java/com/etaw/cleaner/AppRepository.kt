package com.etaw.cleaner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepository {

    @Suppress("DEPRECATION")
    fun loadThirdPartyApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val infos = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            pm.getInstalledApplications(0)
        }
        val result = mutableListOf<AppInfo>()
        for (app in infos) {
            if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue
            try {
                val pkgInfo = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    pm.getPackageInfo(app.packageName, 0)
                }
                val label = app.loadLabel(pm).toString()
                val site = WebsiteTable.get(app.packageName)
                result.add(
                    AppInfo(
                        pkg = app.packageName,
                        name = label,
                        icon = app.loadIcon(pm),
                        installTime = pkgInfo.firstInstallTime,
                        website = site
                    )
                )
            } catch (e: Exception) {
                // package vanished mid-scan, skip
            }
        }
        result.sortBy { it.name.lowercase() }
        return result
    }

    suspend fun loadAppsAsync(context: Context): List<AppInfo> =
        withContext(Dispatchers.IO) { loadThirdPartyApps(context) }

    fun formatTime(millis: Long): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return fmt.format(java.util.Date(millis))
    }
}
