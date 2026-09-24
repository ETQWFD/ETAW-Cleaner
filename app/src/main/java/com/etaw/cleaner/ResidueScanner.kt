package com.etaw.cleaner

import java.io.File

data class ResidueItem(
    val path: String,
    val sizeBytes: Long,
    val risky: Boolean
)

/**
 * 深度残留扫描器（白名单机制，保证不误删其他重要文件）：
 * 只扫描与卸载应用明确相关的目录 —— 系统数据目录、缓存、外置应用目录、下载目录中
 * 以包名命名的文件/文件夹，以及少数知名应用在 /sdcard 根目录留下的遗留目录（默认不勾选）。
 * 不触碰 /system、其他应用数据目录或任何无关文件。
 */
object ResidueScanner {

    /** 知名应用在 /sdcard 根目录留下的遗留目录（可能含聊天记录等个人重要数据，默认不勾选） */
    private val LEGACY = mapOf(
        "com.tencent.mm" to listOf("/sdcard/tencent/MicroMsg"),
        "com.tencent.mobileqq" to listOf("/sdcard/tencent"),
        "com.zhihu.android" to listOf("/sdcard/zhihu"),
        "com.netease.cloudmusic" to listOf("/sdcard/netease/cloudmusic"),
        "com.tencent.qqlive" to listOf("/sdcard/tencent/QQBrowser"),
        "tv.danmaku.bili" to listOf("/sdcard/Android/data/tv.danmaku.bili", "/sdcard/Download/bilibili")
    )

    fun legacyPaths(pkg: String): List<String> = LEGACY[pkg] ?: emptyList()

    fun formatSize(bytes: Long): String {
        if (bytes >= 1048576L) return "%.1f MB".format(bytes / 1048576.0)
        if (bytes >= 1024L) return "%.0f KB".format(bytes / 1024.0)
        return "$bytes B"
    }

    private fun sizeOfFile(f: File): Long {
        return if (f.isDirectory) {
            var total = 0L
            f.listFiles()?.forEach { total += sizeOfFile(it) }
            total
        } else {
            f.length()
        }
    }

    /** 无 Shizuku/root 时的外置存储扫描（新系统下部分目录不可读，将提示授权） */
    fun externalScan(pkg: String): List<ResidueItem> {
        val candidates = mutableListOf<String>()
        candidates += "/sdcard/Android/data/$pkg"
        candidates += "/sdcard/Android/obb/$pkg"
        candidates += "/sdcard/Android/media/$pkg"
        candidates += "/sdcard/$pkg"
        val downloadDir = File("/sdcard/Download")
        if (downloadDir.isDirectory) {
            downloadDir.listFiles()?.forEach { f ->
                if (f.name.startsWith(pkg)) candidates += f.absolutePath
            }
        }
        candidates += legacyPaths(pkg)
        val result = mutableListOf<ResidueItem>()
        for (p in candidates.distinct()) {
            val f = File(p)
            if (f.exists()) {
                result.add(ResidueItem(p, sizeOfFile(f), p in legacyPaths(pkg)))
            }
        }
        return result
    }

    /** 通过 Shizuku / root shell 深度扫描（含 /data 系统数据目录与缓存） */
    fun deepScan(pkg: String, exec: (String) -> Pair<Boolean, String>): List<ResidueItem> {
        val legacy = legacyPaths(pkg)
        val script = buildString {
            append("for p in")
            append(" /data/data/$pkg /data/user/0/$pkg /data/user_de/0/$pkg")
            append(" /data/app/${pkg}* /data/app/~~*/${pkg}* /data/app/~~*/~*/${pkg}*")
            append(" /data/media/0/Android/data/$pkg /data/media/0/Android/obb/$pkg /data/media/0/Android/media/$pkg")
            append(" /sdcard/Android/data/$pkg /sdcard/Android/obb/$pkg /sdcard/Android/media/$pkg")
            append(" /sdcard/$pkg /sdcard/Download/${pkg}*")
            legacy.forEach { append(" $it") }
            append("; do")
            append(" if [ -e \"${'$'}p\" ]; then sz=$(du -sk \"${'$'}p\" 2>/dev/null | awk '{print ${'$'}1}'); echo \"${'$'}p ${'$'}{sz:-0}\"; fi; done")
        }
        val (ok, out) = exec(script)
        if (!ok) return emptyList()
        val result = mutableListOf<ResidueItem>()
        for (line in out.lines()) {
            if (line.isBlank()) continue
            val idx = line.lastIndexOf(' ')
            if (idx <= 0) continue
            val path = line.substring(0, idx)
            val kb = line.substring(idx + 1).toLongOrNull() ?: 0L
            result.add(ResidueItem(path, kb * 1024L, path in legacy))
        }
        return result.distinctBy { it.path }
    }

    /** 深度删除选中的残留，返回实际删除成功数量 */
    fun deleteViaShell(paths: List<String>, exec: (String) -> Pair<Boolean, String>): Int {
        if (paths.isEmpty()) return 0
        val quoted = paths.joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
        exec("rm -rf $quoted")
        // 复核哪些仍然存在
        val remain = paths.count { p -> File(p).exists() }
        return paths.size - remain
    }
}
