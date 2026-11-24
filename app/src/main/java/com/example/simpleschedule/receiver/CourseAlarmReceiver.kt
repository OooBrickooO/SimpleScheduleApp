package com.example.simpleschedule.receiver

import android.annotation.SuppressLint
import android.app.Notification
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
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.simpleschedule.MainActivity
import com.example.simpleschedule.R
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.AppDatabase
import com.example.simpleschedule.ui.theme.getPalette
import com.example.simpleschedule.utils.LocalLogger
import com.example.simpleschedule.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

fun buildCourseNotification(
    context: Context,
    courseName: String,
    location: String,
    timeStr: String,
    classStartMillis: Long,
    colorTheme: String,
    islandEnabled: Boolean,
    isTest: Boolean = false
): Notification {
    val channelId = "CourseAlarmChannel_v2"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "上课提醒", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    val cancelIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
        action = ReminderEngine.ACTION_DISMISS_REMINDER
        putExtra(ReminderEngine.EXTRA_COURSE_NAME, courseName)
        putExtra(ReminderEngine.EXTRA_CLASS_START_MILLIS, classStartMillis)
        putExtra(ReminderEngine.EXTRA_IS_TEST, isTest)
        putExtra(ReminderEngine.EXTRA_DISMISS_REASON, "manual_click")
    }
    val cancelPending = PendingIntent.getBroadcast(
        context,
        ReminderEngine.REQUEST_CODE_MANUAL_DISMISS,
        cancelIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val swipeDeleteIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
        action = ReminderEngine.ACTION_DISMISS_REMINDER
        putExtra(ReminderEngine.EXTRA_COURSE_NAME, courseName)
        putExtra(ReminderEngine.EXTRA_CLASS_START_MILLIS, classStartMillis)
        putExtra(ReminderEngine.EXTRA_IS_TEST, isTest)
        putExtra(ReminderEngine.EXTRA_DISMISS_REASON, "swipe_delete")
    }
    val swipeDeletePending = PendingIntent.getBroadcast(
        context,
        ReminderEngine.REQUEST_CODE_SWIPE_DISMISS,
        swipeDeleteIntent,
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
    val displayLocation = CourseLocationFormatter.formatDisplayLocation(location)
    val contentText = displayLocation.replace("楼", "").ifBlank { displayLocation }

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
        .setDeleteIntent(swipeDeletePending)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "一键清除",
            cancelPending
        )

    // Auto-expire when class actually starts
    if (classStartMillis > 0) {
        val duration = classStartMillis - System.currentTimeMillis()
        if (duration > 0) {
            builder.setTimeoutAfter(duration)
        }
    }

    if (classStartMillis > 0 && !islandEnabled) {
        builder.setWhen(classStartMillis)
        builder.setUsesChronometer(true)
        builder.setChronometerCountDown(true)
    }

    if (islandEnabled) {
        builder.setColorized(false) // 绝对不能设为 true，否则 Android 16 拒绝提升为胶囊通知
        val shortLocation = CourseLocationFormatter.formatShortCapsuleLocation(location)
        builder.setRequestPromotedOngoing(true)
        builder.setShortCriticalText(shortLocation)
    } else {
        builder.setColorized(true)
    }

    return builder.build()
}

class CourseAlarmReceiver : BroadcastReceiver() {
    private var fallbackTts: TextToSpeech? = null

    @SuppressLint("MissingPermission", "WakelockTimeout")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        LocalLogger.log(context, "Receiver", "onReceive 触发: action=$action")
        val appContext = context.applicationContext

        when (action) {
            Intent.ACTION_BOOT_COMPLETED, ReminderEngine.ACTION_UPDATE_WIDGET -> {
                val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    "WidgetRefreshWork",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }

            ReminderEngine.ACTION_SHOW_REMINDER -> {
                val actualMillis = System.currentTimeMillis()
                val courseName = intent.getStringExtra(ReminderEngine.EXTRA_COURSE_NAME) ?: "未知课程"
                val location = intent.getStringExtra(ReminderEngine.EXTRA_LOCATION) ?: "未知地点"
                val timeStr = intent.getStringExtra(ReminderEngine.EXTRA_TIME_STR) ?: "00:00"
                val classStartMillis = intent.getLongExtra(ReminderEngine.EXTRA_CLASS_START_MILLIS, 0L)
                val showNotify = intent.getBooleanExtra(ReminderEngine.EXTRA_SHOW_NOTIFY, true)
                val playVoice = intent.getBooleanExtra(ReminderEngine.EXTRA_PLAY_VOICE, true)
                val colorTheme = intent.getStringExtra(ReminderEngine.EXTRA_COLOR_THEME) ?: "slate"
                val scheduledMillis = intent.getLongExtra(ReminderEngine.EXTRA_SCHEDULED_TRIGGER_MILLIS, 0L)
                val isTest = intent.getBooleanExtra(ReminderEngine.EXTRA_IS_TEST, false)

                ReminderDiagnosticStore.record(
                    context = appContext,
                    eventType = "WAKEUP",
                    courseName = courseName,
                    scheduledMillis = scheduledMillis,
                    actualMillis = actualMillis,
                    detail = "通知=$showNotify, 播报=$playVoice, 测试=$isTest"
                )

                CourseAlarmService.acquireHandoffWakeLock(appContext, 12_000L)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    var handedOffToService = false
                    try {
                        val prefs = appContext.dataStore.data.first()
                        val islandEnabled = prefs[SettingsKeys.DYNAMIC_ISLAND_ENABLED] ?: false

                        if (!isTest && classStartMillis > 0L) {
                            appContext.dataStore.edit { editPrefs ->
                                editPrefs[SettingsKeys.LAST_TRIGGERED_CLASS_START_MILLIS] = classStartMillis
                            }
                        }

                        if (showNotify || playVoice) {
                            val serviceIntent = Intent(appContext, CourseAlarmService::class.java).apply {
                                putExtra(ReminderEngine.EXTRA_COURSE_NAME, courseName)
                                putExtra(ReminderEngine.EXTRA_LOCATION, location)
                                putExtra(ReminderEngine.EXTRA_TIME_STR, timeStr)
                                putExtra(ReminderEngine.EXTRA_CLASS_START_MILLIS, classStartMillis)
                                putExtra(ReminderEngine.EXTRA_COLOR_THEME, colorTheme)
                                putExtra(ReminderEngine.EXTRA_SHOW_NOTIFY, showNotify)
                                putExtra(SettingsKeys.DYNAMIC_ISLAND_ENABLED.name, islandEnabled)
                                putExtra(ReminderEngine.EXTRA_PLAY_VOICE, playVoice)
                                putExtra(ReminderEngine.EXTRA_IS_TEST, isTest)
                            }
                            try {
                                ContextCompat.startForegroundService(appContext, serviceIntent)
                                handedOffToService = true
                                pendingResult.finish()
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                ReminderDiagnosticStore.record(
                                    context = appContext,
                                    eventType = "ERROR",
                                    courseName = courseName,
                                    actualMillis = System.currentTimeMillis(),
                                    detail = "启动前台服务失败，启用兜底通知/播报: ${t.message}"
                                )
                                if (showNotify) {
                                    val notification = buildCourseNotification(
                                        context = appContext,
                                        courseName = courseName,
                                        location = location,
                                        timeStr = timeStr,
                                        classStartMillis = classStartMillis,
                                        colorTheme = colorTheme,
                                        islandEnabled = islandEnabled,
                                        isTest = isTest
                                    )
                                    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    nm.notify(CourseAlarmService.NOTIFICATION_ID, notification)
                                }
                                if (playVoice) {
                                    speakFallbackInReceiver(appContext, courseName, location, pendingResult)
                                } else {
                                    pendingResult.finish()
                                }
                            }
                        } else {
                            if (!isTest && classStartMillis > 0L) {
                                appContext.dataStore.edit { editPrefs ->
                                    editPrefs[SettingsKeys.LAST_DISMISSED_CLASS_START_MILLIS] = classStartMillis
                                }
                            }
                            val dao = AppDatabase.getDatabase(appContext).appDao()
                            ReminderEngine.calculateAndScheduleNext(appContext, dao)
                            pendingResult.finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ReminderDiagnosticStore.record(
                            context = appContext,
                            eventType = "ERROR",
                            courseName = courseName,
                            actualMillis = System.currentTimeMillis(),
                            detail = "Receiver 触发异常: ${e.message}"
                        )
                        pendingResult.finish()
                    } finally {
                        if (!handedOffToService) {
                            CourseAlarmService.releaseHandoffWakeLock()
                        }
                    }
                }
            }

            ReminderEngine.ACTION_DISMISS_REMINDER, ReminderEngine.ACTION_CLASS_START -> {
                val actualMillis = System.currentTimeMillis()
                val courseName = intent.getStringExtra(ReminderEngine.EXTRA_COURSE_NAME) ?: "当前课程"
                val classStartMillis = intent.getLongExtra(ReminderEngine.EXTRA_CLASS_START_MILLIS, 0L)
                val scheduledMillis = intent.getLongExtra(ReminderEngine.EXTRA_SCHEDULED_TRIGGER_MILLIS, 0L)
                val isTest = intent.getBooleanExtra(ReminderEngine.EXTRA_IS_TEST, false)
                val reason = intent.getStringExtra(ReminderEngine.EXTRA_DISMISS_REASON)
                    ?: if (action == ReminderEngine.ACTION_CLASS_START) "legacy_class_start" else "dismiss"

                LocalLogger.log(appContext, "Receiver", "执行清理 ($reason): 取消闹钟并同步停止前台服务")

                ReminderEngine.cancelDismissAndFollowUpAlarms(appContext)
                CourseAlarmService.dismissActiveNotificationAndStop(appContext)

                val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = runCatching {
                    pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleSchedule:DismissWakeLock").apply {
                        setReferenceCounted(false)
                        acquire(10_000L)
                    }
                }.getOrNull()

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (!isTest && classStartMillis > 0L) {
                            appContext.dataStore.edit { editPrefs ->
                                editPrefs[SettingsKeys.LAST_DISMISSED_CLASS_START_MILLIS] = classStartMillis
                            }
                        }
                        val dao = AppDatabase.getDatabase(appContext).appDao()
                        ReminderEngine.calculateAndScheduleNext(appContext, dao)

                        val reasonLabel = when (reason) {
                            "manual_click" -> "手动点击一键清除"
                            "swipe_delete" -> "通知栏划掉"
                            "class_start" -> "上课时间到达自动消除"
                            "safety_timeout" -> "安全兜底超时清理"
                            else -> reason
                        }
                        ReminderDiagnosticStore.record(
                            context = appContext,
                            eventType = "DISMISS",
                            courseName = courseName,
                            scheduledMillis = if (reason == "class_start") scheduledMillis else 0L,
                            actualMillis = actualMillis,
                            detail = "$reasonLabel -> 已停服移除通知并调度下节"
                        )

                        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                        WorkManager.getInstance(appContext).enqueueUniqueWork(
                            "WidgetRefreshWork",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        runCatching {
                            if (wakeLock?.isHeld == true) wakeLock.release()
                        }
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun speakFallbackInReceiver(
        appContext: Context,
        courseName: String,
        location: String,
        pendingResult: PendingResult
    ) {
        Handler(Looper.getMainLooper()).post {
            val handler = Handler(Looper.getMainLooper())
            val speakLocation = location.ifBlank { "未知地点" }.replace("楼", "")
            val textToSpeak = "您接下来在 $speakLocation 有一节 $courseName 课。请准备。"

            var isFinished = false
            val timeoutRunnable = Runnable {
                if (!isFinished) {
                    isFinished = true
                    runCatching {
                        fallbackTts?.stop()
                        fallbackTts?.shutdown()
                    }
                    fallbackTts = null
                    pendingResult.finish()
                }
            }
            handler.postDelayed(timeoutRunnable, 8000L)

            fallbackTts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = fallbackTts?.setLanguage(Locale.CHINESE)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        fallbackTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            private fun finishTts() {
                                handler.post {
                                    if (!isFinished) {
                                        isFinished = true
                                        handler.removeCallbacks(timeoutRunnable)
                                        runCatching {
                                            fallbackTts?.stop()
                                            fallbackTts?.shutdown()
                                        }
                                        fallbackTts = null
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
                        val params = Bundle().apply {
                            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CourseFallbackTTS")
                        }
                        fallbackTts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "CourseFallbackTTS")
                    } else {
                        handler.removeCallbacks(timeoutRunnable)
                        if (!isFinished) {
                            isFinished = true
                            pendingResult.finish()
                        }
                    }
                } else {
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isFinished) {
                        isFinished = true
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
