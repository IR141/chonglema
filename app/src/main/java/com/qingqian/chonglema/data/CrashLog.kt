package com.qingqian.chonglema.data

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 崩溃日志：把未捕获异常写到 filesDir/crash/last_crash.txt，设置页可导出。
 * 只保留最后一次崩溃，避免无限增长。
 */
object CrashLog {

    private const val DIR = "crash"
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(app, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun file(context: Context): File =
        File(File(context.filesDir, DIR), FILE)

    fun read(context: Context): String? =
        file(context).takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val text = buildString {
            append("时间：").append(stamp).append('\n')
            append("线程：").append(thread.name).append('\n')
            append("机型：").append(android.os.Build.MANUFACTURER).append(' ')
            append(android.os.Build.MODEL).append('\n')
            append("系统：Android ").append(android.os.Build.VERSION.RELEASE)
            append("（API ").append(android.os.Build.VERSION.SDK_INT).append("）\n\n")
            append(throwable.stackTraceToString())
        }
        val target = file(context)
        target.parentFile?.mkdirs()
        target.writeText(text)
    }
}
