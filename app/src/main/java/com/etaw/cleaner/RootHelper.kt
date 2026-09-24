package com.etaw.cleaner

/** root (su) 通道：无 Shizuku 时提供真实的系统级卸载与深度删除能力 */
object RootHelper {

    fun isAvailable(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.contains("uid=0")
    } catch (e: Exception) {
        false
    }

    fun exec(cmd: String): Pair<Boolean, String> = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        p.waitFor()
        (p.exitValue() == 0) to (out + err).trim()
    } catch (e: Exception) {
        false to (e.message ?: "error")
    }

    fun uninstall(pkg: String): Boolean =
        exec("pm uninstall --user 0 \"$pkg\"").first
}
