package com.example.simpleschedule.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalLogger {
    fun log(context: Context, tag: String, message: String) {
        // No-op in production release
    }

    fun readLogs(context: Context): String {
        return "暂无调试日志记录喵~"
    }

    fun clearLogs(context: Context) {
        // No-op in production release
    }
}
