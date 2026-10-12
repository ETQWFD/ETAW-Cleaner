package com.etaw.cleaner

import java.io.File

data class JunkItem(
    val path: String,
    val sizeBytes: Long,
    val category: String,
    val ownerApp: String
)

/**
 * 全局垃圾深度扫描：只识别明确的垃圾类型（旧安装包、临时/日志文件、缩略图缓存、
 * 应用外置缓存），白名单式扫描，绝不触碰用户文档、下载目录中的照片/资料等。
 * 每个目录都做异常保护，无权限/不可读时直接跳过，绝不崩溃。
 */
object JunkScanner {

    private const val MIN_SIZE = 100L * 1024L // 小于 100KB 不列入

    fun formatSize(bytes: Long): String = ResidueScanner.formatSize(bytes)

    private fun safeSize(f: File): Long = try {
        if (f.isFile) f.length() else {
            var total = 0L
            f.listFiles()?.forEach { total += safeSize(it) }
            total
        }
    } catch (e: Exception) { 0L }

    private fun safeList(f: File): List<File> = try {
        f.listFiles()?.toList() ?: emptyList()
    } catch (e: Exception) { emptyList() }

    fun scan(): List<JunkItem> {
        val result = mutableListOf<JunkItem>()

        // 1) Download 目录：旧安装包 / 临时日志文件
        try {
            val dl = File("/sdcard/Download")
            if (dl.isDirectory) {
                safeList(dl).forEach { f ->
                    val sz = safeSize(f)
                    if (sz < MIN_SIZE) return@forEach
                    val name = f.name.lowercase()
                    when {
                        name.endsWith(".apk") ->
                            result.add(JunkItem(f.absolutePath, sz, "旧安装包", f.name.removeSuffix(".apk").take(40)))
                        name.endsWith(".log") || name.endsWith(".tmp") || name.endsWith(".bak") ||
                        name.endsWith(".crdownload") || name.endsWith(".old") || name.endsWith(".temp") ->
                            result.add(JunkItem(f.absolutePath, sz, "临时/日志文件", f.name.take(40)))
                    }
                }
            }
        } catch (e: Exception) { AppLog.e("JunkScan", "Download scan failed: ${e.message}") }

        // 2) 缩略图缓存
        try {
            listOf("/sdcard/DCIM/.thumbnails", "/sdcard/.thumbnails", "/sdcard/Pictures/.thumbnails").forEach { p ->
                val f = File(p)
                val sz = safeSize(f)
                if (f.exists() && sz >= MIN_SIZE)
                    result.add(JunkItem(p, sz, "缩略图缓存", "系统相册"))
            }
        } catch (e: Exception) { AppLog.e("JunkScan", "thumb scan failed: ${e.message}") }

        // 3) 常见临时目录
        try {
            listOf("/sdcard/cache", "/sdcard/temp", "/sdcard/tmp", "/sdcard/.cache").forEach { p ->
                val f = File(p)
                val sz = safeSize(f)
                if (f.isDirectory && sz >= MIN_SIZE)
                    result.add(JunkItem(p, sz, "临时目录", "系统/应用"))
            }
        } catch (e: Exception) { AppLog.e("JunkScan", "tmp dir scan failed: ${e.message}") }

        // 4) Android/media、Android/data 下各应用缓存（无权限自动跳过）
        try {
            listOf("/sdcard/Android/media", "/sdcard/Android/data").forEach { root ->
                val d = File(root)
                if (!d.isDirectory) return@forEach
                safeList(d).forEach { appDir ->
                    val cache = File(appDir, "cache")
                    val sz = safeSize(cache)
                    if (cache.isDirectory && sz >= MIN_SIZE)
                        result.add(JunkItem(cache.absolutePath, sz, "应用缓存", appDir.name))
                }
            }
        } catch (e: Exception) { AppLog.e("JunkScan", "app cache scan failed: ${e.message}") }

        return result.distinctBy { it.path }.sortedByDescending { it.sizeBytes }
    }

    /** 删除选中项，返回真实删除数量 */
    fun delete(paths: List<String>): Int {
        var ok = 0
        paths.forEach { p ->
            try {
                val f = File(p)
                if (f.exists() && f.deleteRecursively()) ok++
            } catch (e: Exception) { AppLog.e("JunkScan", "delete $p failed: ${e.message}") }
        }
        return ok
    }
}
