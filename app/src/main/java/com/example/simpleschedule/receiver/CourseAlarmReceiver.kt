package com.example.simpleschedule.receiver

import com.seekai.simpleschedule.R
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.*
import com.example.simpleschedule.widget.WidgetUpdateWorker
import com.example.simpleschedule.MainActivity
import com.example.simpleschedule.service.DynamicIslandService

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

class CourseAlarmReceiver : BroadcastReceiver() {
    private var tts: TextToSpeech? = null

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
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
                val showNotify = intent.getBooleanExtra("SHOW_NOTIFY", true)
                val playVoice = intent.getBooleanExtra("PLAY_VOICE", true)
                val colorTheme = intent.getStringExtra("COLOR_THEME") ?: "slate"

                val pendingResult = goAsync()
                kotlinx.coroutines.GlobalScope.launch {
                    val islandEnabled = try {
                        val prefs = context.dataStore.data.firstOrNull()
                        prefs?.get(SettingsKeys.DYNAMIC_ISLAND_ENABLED) ?: false
                    } catch (e: Exception) { false }

                    if (islandEnabled && Build.VERSION.SDK_INT < 36 && classStartMillis > System.currentTimeMillis() && Settings.canDrawOverlays(context)) {
                        try {
                            val serviceIntent = Intent(context, DynamicIslandService::class.java).apply {
                                putExtra("COURSE_NAME", courseName)
                                putExtra("LOCATION", location)
                                putExtra("CLASS_START_MILLIS", classStartMillis)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (showNotify || islandEnabled) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            showNotification(context, courseName, location, timeStr, classStartMillis, colorTheme, islandEnabled)
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
                            
                            // 8秒安全超时限制，确保广播接收器不挂起并防止ANR
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
                }
            }
            "ACTION_DISMISS_REMINDER" -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(1001)

                if (Build.VERSION.SDK_INT < 36) {
                    try {
                        val serviceIntent = Intent(context, DynamicIslandService::class.java)
                        context.stopService(serviceIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, courseName: String, location: String, timeStr: String, classStartMillis: Long, colorTheme: String, islandEnabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val CHANNEL_ID = "CourseAlarmChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "上课提醒", NotificationManager.IMPORTANCE_HIGH))
        }

        val cancelIntent = Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_DISMISS_REMINDER" }
        val cancelPending = PendingIntent.getBroadcast(context, SettingsKeys.REMINDER_ADVANCE_MINS.hashCode(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPending = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = if (islandEnabled && Build.VERSION.SDK_INT >= 36) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("即将上课: $courseName")
                .setContentText("地点: ${location.replace("楼", "")} ($timeStr)")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "一键清除", cancelPending)

            if (classStartMillis > 0) {
                builder.setUsesChronometer(true)
                builder.setChronometerCountDown(true)
                builder.setWhen(classStartMillis)
            }

            builder.getExtras().putBoolean("android.requestPromotedOngoing", true)

            builder.build()
        } else {
            val remoteViews = RemoteViews(context.packageName, R.layout.notification_course)
            remoteViews.setTextViewText(R.id.tv_time, timeStr)
            remoteViews.setTextViewText(R.id.tv_location, location.replace("楼", ""))
            remoteViews.setTextViewText(R.id.tv_course_name, courseName)

            if (classStartMillis > 0) {
                remoteViews.setChronometer(R.id.chronometer, classStartMillis, null, true)
                remoteViews.setChronometerCountDown(R.id.chronometer, true)
            }

            try {
                val palette = com.example.simpleschedule.ui.theme.getPalette(colorTheme, true)
                val colorInt = palette.accent.toArgb()
                remoteViews.setInt(R.id.view_stripe, "setBackgroundColor", colorInt)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            remoteViews.setOnClickPendingIntent(R.id.btn_mute, cancelPending)

            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(openPending)
                .build()
        }

        notificationManager.notify(1001, notification)
    }
}

// --- 8. 桌面小组件 (Jetpack Glance) ---

