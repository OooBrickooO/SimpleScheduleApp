package com.example.simpleschedule.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.simpleschedule.R
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.AppDatabase
import com.example.simpleschedule.utils.LocalLogger
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class CourseAlarmService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val TTS_CHANNEL_ID = "CourseTtsChannel_v1"

        @Volatile
        private var activeInstance: CourseAlarmService? = null
        private val mainHandler = Handler(Looper.getMainLooper())
        private val handoffLock = Any()
        private var handoffWakeLock: PowerManager.WakeLock? = null

        @SuppressLint("WakelockTimeout")
        fun acquireHandoffWakeLock(context: Context, timeoutMs: Long = 10_000L) {
            runCatching {
                val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                synchronized(handoffLock) {
                    if (handoffWakeLock?.isHeld == true) {
                        runCatching { handoffWakeLock?.release() }
                    }
                    handoffWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleSchedule:AlarmHandoff").apply {
                        setReferenceCounted(false)
                        acquire(timeoutMs)
                    }
                }
            }
        }

        fun releaseHandoffWakeLock() {
            synchronized(handoffLock) {
                runCatching {
                    if (handoffWakeLock?.isHeld == true) {
                        handoffWakeLock?.release()
                    }
                }
                handoffWakeLock = null
            }
        }

        fun dismissActiveNotificationAndStop(context: Context) {
            val appContext = context.applicationContext
            val execute = {
                activeInstance?.stopForegroundAndCleanUp()
                runCatching {
                    appContext.stopService(Intent(appContext, CourseAlarmService::class.java))
                }
                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIFICATION_ID)
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                execute()
            } else {
                if (activeInstance == null) {
                    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(NOTIFICATION_ID)
                }
                mainHandler.post(execute)
            }
        }
    }

    private var activeSessionId = 0
    private var tts: TextToSpeech? = null
    private var ttsWakeLock: PowerManager.WakeLock? = null
    private var ttsTimeoutRunnable: Runnable? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: Any? = null
    @Suppress("DEPRECATION")
    private val legacyAudioFocusListener = AudioManager.OnAudioFocusChangeListener { }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent == null) {
                stopForegroundAndCleanUp()
                return START_NOT_STICKY
            }

            val courseName = intent.getStringExtra(ReminderEngine.EXTRA_COURSE_NAME) ?: "未知课程"
            val location = intent.getStringExtra(ReminderEngine.EXTRA_LOCATION) ?: "未排地点"
            val timeStr = intent.getStringExtra(ReminderEngine.EXTRA_TIME_STR) ?: "00:00"
            val classStartMillis = intent.getLongExtra(ReminderEngine.EXTRA_CLASS_START_MILLIS, 0L)
            val colorTheme = intent.getStringExtra(ReminderEngine.EXTRA_COLOR_THEME) ?: "slate"
            val showNotify = intent.getBooleanExtra(ReminderEngine.EXTRA_SHOW_NOTIFY, true)
            val islandEnabled = intent.getBooleanExtra(SettingsKeys.DYNAMIC_ISLAND_ENABLED.name, false)
            val playVoice = intent.getBooleanExtra(ReminderEngine.EXTRA_PLAY_VOICE, false)
            val isTest = intent.getBooleanExtra(ReminderEngine.EXTRA_IS_TEST, false)

            activeSessionId += 1
            val sessionId = activeSessionId
            val isVoiceOnlyMode = !showNotify && playVoice

            LocalLogger.log(
                this,
                "Service",
                "onStartCommand: 课程=$courseName, 地点=$location, showNotify=$showNotify, island=$islandEnabled, playVoice=$playVoice"
            )

            val notification = if (showNotify) {
                buildCourseNotification(
                    context = this,
                    courseName = courseName,
                    location = location,
                    timeStr = timeStr,
                    classStartMillis = classStartMillis,
                    colorTheme = colorTheme,
                    islandEnabled = islandEnabled,
                    isTest = isTest
                )
            } else {
                buildVoiceOnlyNotification(courseName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            val shortCapsule = CourseLocationFormatter.formatShortCapsuleLocation(location)
            ReminderDiagnosticStore.record(
                context = this,
                eventType = "FGS_NOTIFY",
                courseName = courseName,
                actualMillis = System.currentTimeMillis(),
                detail = if (isVoiceOnlyMode) {
                    "静音播报前台保活(无课程通知)"
                } else if (islandEnabled) {
                    "前台通知已显示, 流体云=开, 胶囊=$shortCapsule"
                } else {
                    "前台通知已显示, 流体云=关"
                }
            )

            if (showNotify) {
                scheduleSafetyDismissAlarm(courseName, classStartMillis, isTest)
            }

            if (playVoice) {
                startTtsPlayback(
                    sessionId = sessionId,
                    courseName = courseName,
                    location = location,
                    classStartMillis = classStartMillis,
                    isTest = isTest,
                    voiceOnlyMode = isVoiceOnlyMode
                )
            } else if (!showNotify) {
                stopForegroundAndCleanUp()
            }

            return START_NOT_STICKY
        } finally {
            releaseHandoffWakeLock()
        }
    }

    private fun buildVoiceOnlyNotification(courseName: String): android.app.Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(TTS_CHANNEL_ID, "课前语音播报", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, TTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("课前语音播报")
            .setContentText("正在播报 $courseName")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun scheduleSafetyDismissAlarm(courseName: String, classStartMillis: Long, isTest: Boolean) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val delayMs = if (classStartMillis > 0L) {
            maxOf(15_000L, (classStartMillis - System.currentTimeMillis()) + 30_000L)
        } else {
            30 * 60 * 1000L
        }
        val intent = Intent(this, CourseAlarmReceiver::class.java).apply {
            action = ReminderEngine.ACTION_DISMISS_REMINDER
            putExtra(ReminderEngine.EXTRA_COURSE_NAME, courseName)
            putExtra(ReminderEngine.EXTRA_CLASS_START_MILLIS, classStartMillis)
            putExtra(ReminderEngine.EXTRA_IS_TEST, isTest)
            putExtra(ReminderEngine.EXTRA_DISMISS_REASON, "safety_timeout")
        }
        val pending = PendingIntent.getBroadcast(
            this,
            ReminderEngine.REQUEST_CODE_SAFETY_DISMISS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMs,
                pending
            )
        } catch (_: SecurityException) {
        }
    }

    private fun cancelSafetyDismissAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            this,
            ReminderEngine.REQUEST_CODE_SAFETY_DISMISS,
            Intent(this, CourseAlarmReceiver::class.java).apply {
                action = ReminderEngine.ACTION_DISMISS_REMINDER
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun startTtsPlayback(
        sessionId: Int,
        courseName: String,
        location: String,
        classStartMillis: Long,
        isTest: Boolean,
        voiceOnlyMode: Boolean
    ) {
        cleanupTtsResources()

        val ttsInitStartMillis = System.currentTimeMillis()
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            ttsWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleSchedule:TtsWakeLock").apply {
                setReferenceCounted(false)
                acquire(18_000L)
            }
        }
        requestSpeechAudioFocus()

        val timeout = Runnable {
            ReminderDiagnosticStore.record(
                context = this,
                eventType = "TTS",
                courseName = courseName,
                actualMillis = System.currentTimeMillis(),
                detail = "播报超时结束(15s)"
            )
            finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
        }
        ttsTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, 15_000L)

        val speakLocation = location.ifBlank { "未知地点" }.replace("楼", "")
        val textToSpeak = "您接下来在 $speakLocation 有一节 $courseName 课。请准备。"

        tts = TextToSpeech(applicationContext) { status ->
            if (sessionId != activeSessionId) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                if (engine == null) {
                    finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
                    return@TextToSpeech
                }
                val langResult = engine.setLanguage(Locale.CHINESE)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ReminderDiagnosticStore.record(
                        context = this,
                        eventType = "TTS",
                        courseName = courseName,
                        actualMillis = System.currentTimeMillis(),
                        detail = "失败：系统缺少中文TTS引擎"
                    )
                    finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
                    return@TextToSpeech
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    engine.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val initElapsed = System.currentTimeMillis() - ttsInitStartMillis
                        ReminderDiagnosticStore.record(
                            context = this@CourseAlarmService,
                            eventType = "TTS",
                            courseName = courseName,
                            actualMillis = System.currentTimeMillis(),
                            detail = "开始发声(引擎初始化耗时 ${initElapsed}ms)"
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        ReminderDiagnosticStore.record(
                            context = this@CourseAlarmService,
                            eventType = "TTS",
                            courseName = courseName,
                            actualMillis = System.currentTimeMillis(),
                            detail = "播报完成"
                        )
                        mainHandler.post {
                            finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        ReminderDiagnosticStore.record(
                            context = this@CourseAlarmService,
                            eventType = "TTS",
                            courseName = courseName,
                            actualMillis = System.currentTimeMillis(),
                            detail = "TTS 播报错误"
                        )
                        mainHandler.post {
                            finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        ReminderDiagnosticStore.record(
                            context = this@CourseAlarmService,
                            eventType = "TTS",
                            courseName = courseName,
                            actualMillis = System.currentTimeMillis(),
                            detail = "TTS 播报错误(code=$errorCode)"
                        )
                        mainHandler.post {
                            finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
                        }
                    }
                })

                val ttsParams = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "CourseTTS_$sessionId")
                }
                val speakResult = engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, ttsParams, "CourseTTS_$sessionId")
                if (speakResult != TextToSpeech.SUCCESS) {
                    ReminderDiagnosticStore.record(
                        context = this,
                        eventType = "TTS",
                        courseName = courseName,
                        actualMillis = System.currentTimeMillis(),
                        detail = "TextToSpeech.speak 返回失败($speakResult)"
                    )
                    finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
                }
            } else {
                ReminderDiagnosticStore.record(
                    context = this,
                    eventType = "TTS",
                    courseName = courseName,
                    actualMillis = System.currentTimeMillis(),
                    detail = "TTS 初始化失败(status=$status)"
                )
                finishTtsSession(sessionId, classStartMillis, isTest, voiceOnlyMode)
            }
        }
    }

    private fun finishTtsSession(
        sessionId: Int,
        classStartMillis: Long,
        isTest: Boolean,
        voiceOnlyMode: Boolean
    ) {
        if (sessionId != activeSessionId) return
        cleanupTtsResources()
        if (voiceOnlyMode) {
            stopForegroundAndCleanUp()
            val appContext = applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                if (!isTest && classStartMillis > 0L) {
                    appContext.dataStore.edit { prefs ->
                        prefs[SettingsKeys.LAST_DISMISSED_CLASS_START_MILLIS] = classStartMillis
                    }
                }
                val dao = AppDatabase.getDatabase(appContext).appDao()
                ReminderEngine.calculateAndScheduleNext(appContext, dao)
            }
        }
    }

    private fun requestSpeechAudioFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = am
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(legacyAudioFocusListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                legacyAudioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonSpeechAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (audioFocusRequest as? AudioFocusRequest)?.let { req ->
                runCatching { am.abandonAudioFocusRequest(req) }
            }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(legacyAudioFocusListener)
        }
        audioFocusRequest = null
    }

    private fun cleanupTtsResources() {
        ttsTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        ttsTimeoutRunnable = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        abandonSpeechAudioFocus()
        runCatching {
            if (ttsWakeLock?.isHeld == true) {
                ttsWakeLock?.release()
            }
        }
        ttsWakeLock = null
    }

    fun stopForegroundAndCleanUp() {
        activeSessionId += 1
        cleanupTtsResources()
        cancelSafetyDismissAlarm()
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        LocalLogger.log(this, "Service", "onDestroy: 服务销毁并释放通知/TTS资源")
        if (activeInstance === this) {
            activeInstance = null
        }
        activeSessionId += 1
        cleanupTtsResources()
        cancelSafetyDismissAlarm()
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }
}
