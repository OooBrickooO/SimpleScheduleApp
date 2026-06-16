package com.example.simpleschedule.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalLogger {
    private const val FILE_NAME = "debug_logs.txt"

    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logLine = "[$time] [$tag] $message\n"
            file.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun readLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.readText()
            } else {
                "暂无调试日志记录喵~"
            }
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }

    @Synchronized
    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
