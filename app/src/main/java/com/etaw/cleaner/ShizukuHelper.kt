package com.etaw.cleaner

import android.content.pm.PackageManager
import java.io.File

object ShizukuHelper {

    const val REQUEST_CODE = 1001

    fun isAvailable(): Boolean = try {
        rikka.shizuku.Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    fun isGranted(): Boolean = try {
        rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    fun isReady(): Boolean = isAvailable() && isGranted()

    fun requestPermission() {
        try {
            rikka.shizuku.Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: Exception) {
            // ignored
        }
    }

    fun exec(cmd: String): Pair<Boolean, String> {
        if (!isReady()) return false to ""
        return try {
            val p = rikka.shizuku.Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            p.outputStream.close()
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            (code == 0) to (out + err).trim()
        } catch (e: Exception) {
            false to (e.message ?: "error")
        }
    }

    fun uninstall(pkg: String): Boolean =
        exec("pm uninstall --user 0 \"$pkg\"").first

    fun findResidues(pkg: String): List<String> {
        if (!isReady()) return emptyList()
        val script = """
            for p in /data/data/$pkg /data/user/0/$pkg /data/user_de/0/$pkg \
                /data/app/${pkg}* /data/app/~~*/${pkg}* /data/app/~~*/~*/${pkg}* \
                /data/media/0/Android/data/$pkg /data/media/0/Android/obb/$pkg /data/media/0/Android/media/$pkg \
                /sdcard/Android/data/$pkg /sdcard/Android/obb/$pkg /sdcard/Android/media/$pkg; do
                if [ -e "${'$'}p" ]; then echo "${'$'}p"; fi
            done
        """.trimIndent()
        val (_, out) = exec(script)
        return out.lines().filter { it.isNotBlank() }
    }

    fun deleteResidues(paths: List<String>): String {
        if (paths.isEmpty()) return ""
        val quoted = paths.joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
        val (_, out) = exec("rm -rf $quoted")
        return out
    }

    /** 无 Shizuku 时尝试清理外置存储残留（旧版本系统可成功） */
    fun tryExternalClean(pkg: String): Int {
        var count = 0
        val candidates = listOf(
            "/sdcard/Android/data/$pkg",
            "/sdcard/Android/obb/$pkg",
            "/sdcard/Android/media/$pkg"
        )
        for (path in candidates) {
            try {
                val f = File(path)
                if (f.exists() && f.deleteRecursively()) count++
            } catch (e: Exception) {
                // ignore
            }
        }
        return count
    }
}
