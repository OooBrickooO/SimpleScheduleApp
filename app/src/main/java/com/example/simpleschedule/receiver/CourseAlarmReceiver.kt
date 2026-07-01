package com.example.simpleschedule.receiver

import com.example.simpleschedule.R
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.*
import com.example.simpleschedule.widget.WidgetUpdateWorker
import com.example.simpleschedule.MainActivity
import com.example.simpleschedule.utils.LocalLogger

import android.annotation.SuppressLint
import java.io.InputStream
import jxl.Workbook
import android.app.Application
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable as glanceClickable
import androidx.glance.background as glanceBackground
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize as glanceFillMaxSize
import androidx.glance.layout.fillMaxWidth as glanceFillMaxWidth
import androidx.glance.layout.height as glanceHeight
import androidx.glance.layout.padding as glancePadding
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.TextAlign as GlanceTextAlign
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

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
    val CHANNEL_ID = "CourseAlarmChannel_v2"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "上课提醒", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    val cancelIntent = Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_DISMISS_REMINDER" }
    val cancelPending = PendingIntent.getBroadcast(context, SettingsKeys.REMINDER_ADVANCE_MINS.hashCode(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val openIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory("android.intent.category.NAVIGATION") // 兼容 ColorOS 16 要求的标准场景类别
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val openPending = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

    val palette = try {
        com.example.simpleschedule.ui.theme.getPalette(colorTheme, true)
    } catch (e: Exception) {
        null
    }
    val accentColor = palette?.accent?.toArgb() ?: 0xFF6B7280.toInt()

    val titleText = courseName
    val contentText = location.replace("楼", "")

    val style = NotificationCompat.BigTextStyle()
        .bigText("$courseName\n$contentText")

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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
            builder.getExtras().putBoolean("android.requestPromotedOngoing", true)
            builder.getExtras().putCharSequence("android.shortCriticalText", shortLocation)
        } else {
            builder.setColorized(true)
        }
    } else {
        if (classEndMillis > 0 && !islandEnabled) {
            builder.setWhen(classEndMillis)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
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
            builder.getExtras().putBoolean("android.requestPromotedOngoing", true)
            builder.getExtras().putCharSequence("android.shortCriticalText", shortLocation)
        } else {
            builder.setColorized(true)
        }
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

                LocalLogger.log(context, "Receiver", "课前提醒 ACTION_SHOW_REMINDER: 课程=$courseName, 通知=$showNotify, 语音=$playVoice")
                val pendingResult = goAsync()
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val prefs = context.dataStore.data.first()
                        val islandEnabled = prefs[SettingsKeys.DYNAMIC_ISLAND_ENABLED] ?: false

                        if (showNotify) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                showNotification(context, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, islandEnabled)
                            }
                        }

                        if (playVoice) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                val handler = android.os.Handler(android.os.Looper.getMainLooper())
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

                                tts = TextToSpeech(context) { status ->
                                    if (status == TextToSpeech.SUCCESS) {
                                        val result = tts?.setLanguage(Locale.CHINESE)
                                        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                                            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
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

                                                override fun onDone(utteranceId: String?) {
                                                    finishTts()
                                                }

                                                override fun onError(utteranceId: String?) {
                                                    finishTts()
                                                }

                                                @Deprecated("Deprecated in Java")
                                                override fun onError(utteranceId: String?, errorCode: Int) {
                                                    finishTts()
                                                }
                                            })

                                            val ttsParams = Bundle().apply {
                                                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CourseTTS")
                                            }
                                            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams, "CourseTTS")
                                        } else {
                                            Toast.makeText(context, "语音播报失败：系统缺少中文TTS引擎", Toast.LENGTH_LONG).show()
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

                LocalLogger.log(context, "Receiver", "上课开始 ACTION_CLASS_START: 课程=$courseName")
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val prefs = context.dataStore.data.first()
                        val islandEnabled = prefs[SettingsKeys.DYNAMIC_ISLAND_ENABLED] ?: false

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            updateNotificationState(context, NotificationState.IN_CLASS, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, islandEnabled)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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
        LocalLogger.log(context, "Receiver", "showNotification: 开启胶囊=$islandEnabled, 课程=$courseName")
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
                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                LocalLogger.log(context, "Receiver", "成功调用 startForegroundService 启动 CourseAlarmService")
            } catch (e: Exception) {
                LocalLogger.log(context, "Receiver", "前台服务启动崩溃拦截 (FGS限制)：${e.message}")
                e.printStackTrace()
                // 后台启动前台服务受限时（如 Android 12+ FGS 限制），直接在广播接收器内弹出标准通知兜底
                val notification = buildCourseNotification(context, NotificationState.UPCOMING, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, true)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1001, notification)
                LocalLogger.log(context, "Receiver", "已降级直接通过接收器展示标准通知")
            }
        } else {
            val notification = buildCourseNotification(context, NotificationState.UPCOMING, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, false)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1001, notification)
            LocalLogger.log(context, "Receiver", "直接通过接收器展示普通标准通知")
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
        LocalLogger.log(context, "Receiver", "updateNotificationState: 状态=$state, 开启胶囊=$islandEnabled")
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
                androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                LocalLogger.log(context, "Receiver", "成功调用 startForegroundService 更新服务状态 (IN_CLASS)")
            } catch (e: Exception) {
                LocalLogger.log(context, "Receiver", "更新前台服务崩溃拦截 (FGS限制)：${e.message}")
                e.printStackTrace()
                // 后台启动前台服务受限时，直接在广播接收器内更新通知兜底
                val notification = buildCourseNotification(context, state, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, true)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1001, notification)
                LocalLogger.log(context, "Receiver", "已降级直接通过接收器更新标准通知")
            }
        } else {
            val notification = buildCourseNotification(context, state, courseName, location, timeStr, classStartMillis, classEndMillis, colorTheme, false)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1001, notification)
            LocalLogger.log(context, "Receiver", "直接通过接收器更新普通标准通知")
        }
    }
}

// --- 8. 桌面小组件 (Jetpack Glance) ---

