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
 */
object JunkScanner {

    private const val MIN_SIZE = 100L * 1024L // 小于 100KB 不列入

    fun formatSize(bytes: Long): String = ResidueScanner.formatSize(bytes)

    private fun dirSize(f: File): Long {
        if (f.isFile) return f.length()
        var total = 0L
        f.listFiles()?.forEach { total += dirSize(it) }
        return total
    }

    fun scan(): List<JunkItem> {
        val result = mutableListOf<JunkItem>()
        val seen = HashSet<String>()

        // 1) Download 目录：旧安装包 / 临时日志文件
        val dl = File("/sdcard/Download")
        if (dl.isDirectory) {
            dl.listFiles()?.forEach { f ->
                if (!f.exists()) return@forEach
                val sz = if (f.isDirectory) dirSize(f) else f.length()
                if (sz < MIN_SIZE) return@forEach
                val name = f.name.lowercase()
                when {
                    name.endsWith(".apk") -> {
                        val label = f.name.removeSuffix(".apk")
                        result.add(JunkItem(f.absolutePath, sz, "旧安装包", label.take(40)))
                        seen += f.absolutePath
                    }
                    name.endsWith(".log") || name.endsWith(".tmp") ||
                    name.endsWith(".bak") || name.endsWith(".crdownload") ||
                    name.endsWith(".old") || name.endsWith(".temp") -> {
                        result.add(JunkItem(f.absolutePath, sz, "临时/日志文件", f.name.take(40)))
                        seen += f.absolutePath
                    }
                }
            }
        }

        // 2) 缩略图缓存
        listOf("/sdcard/DCIM/.thumbnails", "/sdcard/.thumbnails", "/sdcard/Pictures/.thumbnails")
            .forEach { p ->
                val f = File(p)
                if (f.exists() && dirSize(f) >= MIN_SIZE) {
                    result.add(JunkItem(p, dirSize(f), "缩略图缓存", "系统相册"))
                }
            }

        // 3) 常见临时目录
        listOf("/sdcard/cache", "/sdcard/temp", "/sdcard/tmp", "/sdcard/.cache")
            .forEach { p ->
                val f = File(p)
                if (f.isDirectory && dirSize(f) >= MIN_SIZE) {
                    result.add(JunkItem(p, dirSize(f), "临时目录", "系统/应用"))
                }
            }

        // 4) Android/media 下各应用缓存（可读时）
        val media = File("/sdcard/Android/media")
        if (media.isDirectory) {
            media.listFiles()?.forEach { appDir ->
                val cache = File(appDir, "cache")
                if (cache.isDirectory) {
                    val sz = dirSize(cache)
                    if (sz >= MIN_SIZE) {
                        result.add(JunkItem(cache.absolutePath, sz, "应用缓存", appDir.name))
                    }
                }
            }
        }

        // 5) Android/data 下各应用缓存（无权限时跳过）
        val dataDir = File("/sdcard/Android/data")
        if (dataDir.isDirectory) {
            dataDir.listFiles()?.forEach { appDir ->
                val cache = File(appDir, "cache")
                if (cache.isDirectory) {
                    val sz = dirSize(cache)
                    if (sz >= MIN_SIZE) {
                        result.add(JunkItem(cache.absolutePath, sz, "应用缓存", appDir.name))
                    }
                }
            }
        }

        return result.sortedByDescending { it.sizeBytes }
    }

    /** 删除选中项，返回真实删除数量 */
    fun delete(paths: List<String>): Int {
        var ok = 0
        paths.forEach { p ->
            try {
                val f = File(p)
                if (f.exists() && f.deleteRecursively()) ok++
            } catch (e: Exception) { }
        }
        return ok
    }
}
