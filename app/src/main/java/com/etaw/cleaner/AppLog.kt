package com.etaw.cleaner

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 详细运行日志：内存环形缓冲 + 持久化到 filesDir/app_log.txt（只追加，不提供删除入口）。
 * 用户可在日志页查看并复制，但不能清空。
 */
object AppLog {

    private const val MAX_MEM = 400
    private val buf = ArrayDeque<String>(MAX_MEM)
    private val fileHolder = ThreadLocal<File?>()
    // SimpleDateFormat 非线程安全（崩溃处理器可能在任何线程触发），用 ThreadLocal 隔离
    private val fmt = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }
    private fun now(): String = fmt.get().format(Date())

    fun init(ctx: Context) {
        if (fileHolder.get() == null) {
            fileHolder.set(File(ctx.applicationContext.filesDir, "app_log.txt"))
            // 崩溃前先把堆栈写进日志，便于排查闪退
            val prev = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                e("CRASH", "${t.name}: ${e}")
                e("CRASH", e.stackTrace.joinToString("\n") { "    at $it" })
                prev?.uncaughtException(t, e)
            }
            w("AppLog", "ETAW Cleaner started")
        }
    }

    private fun write(line: String) {
        synchronized(this) {
            buf.addLast(line)
            while (buf.size > MAX_MEM) buf.removeFirst()
            try {
                fileHolder.get()?.let { f ->
                    FileWriter(f, true).use { it.append(line).append('\n') }
                }
            } catch (e: Exception) { /* 忽略持久化失败 */ }
        }
    }

    fun d(tag: String, msg: String) = write("[D ${now()}] $tag: $msg")
    fun i(tag: String, msg: String) = write("[I ${now()}] $tag: $msg")
    fun w(tag: String, msg: String) = write("[W ${now()}] $tag: $msg")
    fun e(tag: String, msg: String) = write("[E ${now()}] $tag: $msg")

    /** 最新在前 */
    fun all(): List<String> = synchronized(this) { buf.reversed() }

    fun fullText(): String = synchronized(this) { buf.joinToString("\n") }
}
