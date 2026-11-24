package com.example.simpleschedule.receiver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.simpleschedule.MainActivity
import com.example.simpleschedule.R
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.ui.theme.getPalette
import com.example.simpleschedule.utils.LocalLogger
import com.example.simpleschedule.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

enum class NotificationState {
    UPCOMING,
    IN_CLASS
}

fun buildCourseNotification(
    context: Context,
    state: NotificationState,
    courseName: String,
    location: String,
    timeStr: String,
    classStartMillis: Long,
    classEndMillis: Long,
    colorTheme: String,
    islandEnabled: Boolean
): android.app.Notification {
    val channelId = "CourseAlarmChannel_v2"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "上课提醒", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    val cancelIntent = Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_DISMISS_REMINDER" }
    val cancelPending = PendingIntent.getBroadcast(
        context,
        SettingsKeys.REMINDER_ADVANCE_MINS.hashCode(),
        cancelIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val openIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory("android.intent.category.NAVIGATION") // 兼容 ColorOS 16 要求的标准场景类别
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val openPending = PendingIntent.getActivity(
        context,
        0,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    val palette = try {
        getPalette(colorTheme, true)
    } catch (_: Exception) {
        null
    }
    val accentColor = palette?.accent?.toArgb() ?: 0xFF6B7280.toInt()

    val titleText = courseName
    val contentText = location.replace("楼", "")

    val style = NotificationCompat.BigTextStyle()
        .bigText("$courseName\n$contentText")

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(titleText)
        .setContentText(contentText)
        .setStyle(style)
        .setColor(accentColor)
        .setCategory(NotificationCompat.CATEGORY_NAVIGATION) // 兼容 ColorOS 16 要求的标准场景分类
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(true)
        .setContentIntent(openPending)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "一键清除",
            cancelPending
        )

    if (classEndMillis > 0) {
        val duration = classEndMillis - System.currentTimeMillis()
        if (duration > 0) {
            builder.setTimeoutAfter(duration)
        }
    }

    if (state == NotificationState.UPCOMING) {
        if (classStartMillis > 0 && !islandEnabled) {
            builder.setWhen(classStartMillis)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
        }
    } else {
        if (classEndMillis > 0 && !islandEnabled) {
            builder.setWhen(classEndMillis)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
        }
    }

    if (islandEnabled) {
        builder.setColorized(false) // 绝对不能设为 true，否则 Android 16 拒绝提升为胶囊通知
        val cleanLocation = location
            .replace("上课地点：", "")
            .replace("上课地点:", "")
            .replace("地点：", "")
            .replace("地点:", "")
            .replace("教室：", "")
            .replace("教室:", "")
            .replace("楼", "")
            .trim()
        val shortLocation = cleanLocation.take(7)
        builder.extras.putBoolean("android.requestPromotedOngoing", true)
        builder.extras.putCharSequence("android.shortCriticalText", shortLocation)
    } else {
        builder.setColorized(true)
    }

    return builder.build()
}

class CourseAlarmReceiver : BroadcastReceiver() {
    private var tts: TextToSpeech? = null

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        LocalLogger.log(context, "Receiver", "onReceive 触发: action=${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, "ACTION_UPDATE_WIDGET" -> {
                val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
            "ACTION_SHOW_REMINDER" -> {
                val courseName = intent.getStringExtra("COURSE_NAME") ?: "未知课程"
                val location = intent.getStringExtra("LOCATION") ?: "未知地点"
                val timeStr = intent.getStringExtra("TIME_STR") ?: "00:00"
                val classStartMillis = intent.getLongExtra("CLASS_START_MILLIS", 0L)
                val classEndMillis = intent.getLongExtra("CLASS_END_MILLIS", 0L)
                val showNotify = intent.getBooleanExtra("SHOW_NOTIFY", true)
                val playVoice = intent.getBooleanExtra("PLAY_VOICE", true)
                val colorTheme = intent.getStringExtra("COLOR_THEME") ?: "slate"

                val appContext = context.applicationContext
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefs = appContext.dataStore.data.first()
                        val islandEnabled = prefs[SettingsKeys.DYNAMIC_ISLAND_ENABLED] ?: false

                        if (showNotify) {
                            Handler(Looper.getMainLooper()).post {
                                showNotification(
                                    appContext,
                                    courseName,
                                    location,
                                    timeStr,
                                    classStartMillis,
                                    classEndMillis,
                                    colorTheme,
                                    islandEnabled
                                )
                            }
                        }

                        if (playVoice) {
                            Handler(Looper.getMainLooper()).post {
                                val handler = Handler(Looper.getMainLooper())
                                val textToSpeak = "您接下来在 ${location.replace("楼", "")} 有一节 $courseName 课。请准备。"

                                var isFinished = false
                                val timeoutRunnable = Runnable {
                                    if (!isFinished) {
                                        isFinished = true
                                        try {
                                            tts?.stop()
                                            tts?.shutdown()
                                            tts = null
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        pendingResult.finish()
                                    }
                                }

                                handler.postDelayed(timeoutRunnable, 8000)

                                tts = TextToSpeech(appContext) { status ->
                                    if (status == TextToSpeech.SUCCESS) {
                                        val result = tts?.setLanguage(Locale.CHINESE)
                                        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                                            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                                private fun finishTts() {
                                                    handler.post {
                                                        if (!isFinished) {
                                                            isFinished = true
                                                            handler.removeCallbacks(timeoutRunnable)
                                                            try {
                                                                tts?.stop()
                                                                tts?.shutdown()
                                                                tts = null
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                            pendingResult.finish()
                                                        }
                                                    }
                                                }

                                                override fun onStart(utteranceId: String?) {}
                                                override fun onDone(utteranceId: String?) { finishTts() }
                                                override fun onError(utteranceId: String?) { finishTts() }
                                                @Deprecated("Deprecated in Java")
                                                override fun onError(utteranceId: String?, errorCode: Int) { finishTts() }
                                            })

                                            val ttsParams = Bundle().apply {
                                                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CourseTTS")
                                            }
                                            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams, "CourseTTS")
                                        } else {
                                            Toast.makeText(appContext, "语音播报失败：系统缺少中文TTS引擎", Toast.LENGTH_LONG).show()
                                            handler.removeCallbacks(timeoutRunnable)
                                            pendingResult.finish()
                                        }
                                    } else {
                                        handler.removeCallbacks(timeoutRunnable)
                                        pendingResult.finish()
                                    }
                                }
                            }
                        } else {
                            pendingResult.finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingResult.finish()
                    }
                }
            }
            "ACTION_CLASS_START" -> {
                val courseName = intent.getStringExtra("COURSE_NAME") ?: "未知课程"
                val location = intent.getStringExtra("LOCATION") ?: "未知地点"
                val timeStr = intent.getStringExtra("TIME_STR") ?: "00:00"
                val classStartMillis = intent.getLongExtra("CLASS_START_MILLIS", 0L)
                val classEndMillis = intent.getLongExtra("CLASS_END_MILLIS", 0L)
                val colorTheme = intent.getStringExtra("COLOR_THEME") ?: "slate"

                val appContext = context.applicationContext
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefs = appContext.dataStore.data.first()
                        val islandEnabled = prefs[SettingsKeys.DYNAMIC_ISLAND_ENABLED] ?: false

                        Handler(Looper.getMainLooper()).post {
                            try {
                                updateNotificationState(
                                    appContext,
                                    NotificationState.IN_CLASS,
                                    courseName,
                                    location,
                                    timeStr,
                                    classStartMillis,
                                    classEndMillis,
                                    colorTheme,
                                    islandEnabled
                                )
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingResult.finish()
                    }
                }
            }
            "ACTION_DISMISS_REMINDER" -> {
                LocalLogger.log(context, "Receiver", "下课清理 ACTION_DISMISS_REMINDER: 取消通知并停止前台服务")
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(1001)
                try {
                    context.stopService(Intent(context, CourseAlarmService::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        context: Context,
        courseName: String,
        location: String,
        timeStr: String,
        classStartMillis: Long,
        classEndMillis: Long,
        colorTheme: String,
        islandEnabled: Boolean
    ) {
        if (islandEnabled) {
            val serviceIntent = Intent(context, CourseAlarmService::class.java).apply {
                putExtra("STATE", "UPCOMING")
                putExtra("COURSE_NAME", courseName)
                putExtra("LOCATION", location)
                putExtra("TIME_STR", timeStr)
                putExtra("CLASS_START_MILLIS", classStartMillis)
                putExtra("CLASS_END_MILLIS", classEndMillis)
                putExtra("COLOR_THEME", colorTheme)
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                val notification = buildCourseNotification(context, NotificationState.UPCOMING, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, true)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1001, notification)
            }
        } else {
            val notification = buildCourseNotification(context, NotificationState.UPCOMING, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, false)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1001, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateNotificationState(
        context: Context,
        state: NotificationState,
        courseName: String,
        location: String,
        timeStr: String,
        classStartMillis: Long,
        classEndMillis: Long,
        colorTheme: String,
        islandEnabled: Boolean
    ) {
        if (islandEnabled) {
            val serviceIntent = Intent(context, CourseAlarmService::class.java).apply {
                putExtra("STATE", "IN_CLASS")
                putExtra("COURSE_NAME", courseName)
                putExtra("LOCATION", location)
                putExtra("TIME_STR", timeStr)
                putExtra("CLASS_START_MILLIS", classStartMillis)
                putExtra("CLASS_END_MILLIS", classEndMillis)
                putExtra("COLOR_THEME", colorTheme)
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                val notification = buildCourseNotification(context, state, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, true)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1001, notification)
            }
        } else {
            val notification = buildCourseNotification(context, state, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, false)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1001, notification)
        }
    }
}
