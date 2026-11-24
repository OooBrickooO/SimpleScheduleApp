package com.example.simpleschedule.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.AppDao
import com.example.simpleschedule.domain.ScheduleResolver
import com.example.simpleschedule.utils.LocalLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderEngine {
    private const val REQUEST_CODE_REMIND = 1001
    private const val REQUEST_CODE_CANCEL = 1002
    private const val REQUEST_CODE_WIDGET = 1003
    private const val REQUEST_CODE_CLASS_START = 1004

    fun cancelAllReminderAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val showPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMIND,
            Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_SHOW_REMINDER" },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        showPending?.let { alarmManager.cancel(it); it.cancel() }

        val startPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CLASS_START,
            Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_CLASS_START" },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        startPending?.let { alarmManager.cancel(it); it.cancel() }

        val cancelPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CANCEL,
            Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_DISMISS_REMINDER" },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        cancelPending?.let { alarmManager.cancel(it); it.cancel() }

        LocalLogger.log(context, "ReminderEngine", "已取消所有已注册的上课提醒闹钟")
    }

    suspend fun calculateAndScheduleNext(context: Context, appDao: AppDao) {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[SettingsKeys.REMINDER_ENABLED] ?: false
        if (!enabled) {
            cancelAllReminderAlarms(context)
            return
        }

        val advanceMins = prefs[SettingsKeys.REMINDER_ADVANCE_MINS] ?: 20
        val isVoiceOn = prefs[SettingsKeys.REMINDER_VOICE_ENABLED] ?: true
        val isNotifyOn = prefs[SettingsKeys.REMINDER_NOTIFY_ENABLED] ?: true

        val savedId = prefs[SettingsKeys.CURRENT_SCHEDULE_ID] ?: return
        val groups = appDao.getAllScheduleGroups().firstOrNull() ?: return
        val currentSchedule = groups.find { it.id == savedId } ?: return

        val rawCourses = appDao.getCoursesBySchedule(currentSchedule.id).firstOrNull() ?: return
        val overrides = appDao.getAllOverrides().firstOrNull() ?: emptyList()
        val overrideMap = overrides.associateBy { it.courseId }
        val timeNodes = appDao.getTimeNodes(currentSchedule.timetableId)
        val timeNodeMap = timeNodes.associateBy { it.nodeIndex }
        val makeUpRules = appDao.getMakeUpRulesBySchedule(currentSchedule.id).firstOrNull() ?: emptyList()

        val nextEvent = ScheduleResolver.resolveNextUpcomingCourse(
            nowMillis = System.currentTimeMillis(),
            advanceMins = advanceMins,
            startDateStr = currentSchedule.startDate,
            rawCourses = rawCourses,
            overrideMap = overrideMap,
            timeNodeMap = timeNodeMap,
            makeUpRules = makeUpRules,
            lookaheadDays = 14
        ) ?: return

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            LocalLogger.log(
                context,
                "ReminderEngine",
                "成功计算下一节课：${nextEvent.course.name}, 触发=${sdf.format(Date(nextEvent.triggerMillis))}, 上课=${sdf.format(Date(nextEvent.classStartMillis))}, 下课=${sdf.format(Date(nextEvent.classEndMillis))}"
            )
        } catch (_: Exception) {
        }

        scheduleAlarmByParams(
            context = context,
            courseName = nextEvent.course.name,
            location = nextEvent.course.location,
            timeStr = nextEvent.timeRange,
            triggerMillis = nextEvent.triggerMillis,
            classStartMillis = nextEvent.classStartMillis,
            classEndMillis = nextEvent.classEndMillis,
            showNotify = isNotifyOn,
            playVoice = isVoiceOn,
            colorTheme = nextEvent.course.colorTheme
        )
    }

    @SuppressLint("MissingPermission")
    fun scheduleAlarmByParams(
        context: Context,
        courseName: String,
        location: String,
        timeStr: String,
        triggerMillis: Long,
        classStartMillis: Long,
        classEndMillis: Long,
        showNotify: Boolean,
        playVoice: Boolean,
        colorTheme: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val showIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = "ACTION_SHOW_REMINDER"
            putExtra("COURSE_NAME", courseName)
            putExtra("LOCATION", location)
            putExtra("TIME_STR", timeStr)
            putExtra("CLASS_START_MILLIS", classStartMillis)
            putExtra("CLASS_END_MILLIS", classEndMillis)
            putExtra("SHOW_NOTIFY", showNotify)
            putExtra("PLAY_VOICE", playVoice)
            putExtra("COLOR_THEME", colorTheme)
        }
        val showPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMIND,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = "ACTION_CLASS_START"
            putExtra("COURSE_NAME", courseName)
            putExtra("LOCATION", location)
            putExtra("TIME_STR", timeStr)
            putExtra("CLASS_START_MILLIS", classStartMillis)
            putExtra("CLASS_END_MILLIS", classEndMillis)
            putExtra("COLOR_THEME", colorTheme)
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CLASS_START,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = "ACTION_DISMISS_REMINDER"
        }
        val cancelPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CANCEL,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                LocalLogger.log(context, "ReminderEngine", "注册精确闹钟失败：缺少系统精确闹钟权限")
                return
            }

            val currentTime = System.currentTimeMillis()
            if (triggerMillis > currentTime) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPending)
                alarmManager.setAlarmClock(alarmClockInfo, showPending)
            }
            if (classStartMillis > currentTime) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, classStartMillis, startPending)
            }
            if (classEndMillis > currentTime) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, classEndMillis, cancelPending)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    suspend fun scheduleNextWidgetUpdate(context: Context, appDao: AppDao) {
        val prefs = context.dataStore.data.first()
        val savedId = prefs[SettingsKeys.CURRENT_SCHEDULE_ID] ?: return
        val groups = appDao.getAllScheduleGroups().firstOrNull() ?: return
        val currentSchedule = groups.find { it.id == savedId } ?: return

        val rawCourses = appDao.getCoursesBySchedule(currentSchedule.id).firstOrNull() ?: emptyList()
        val overrides = appDao.getAllOverrides().firstOrNull() ?: emptyList()
        val overrideMap = overrides.associateBy { it.courseId }
        val timeNodes = appDao.getTimeNodes(currentSchedule.timetableId)
        val timeNodeMap = timeNodes.associateBy { it.nodeIndex }
        val makeUpRules = appDao.getMakeUpRulesBySchedule(currentSchedule.id).firstOrNull() ?: emptyList()

        val nextUpdateMillis = ScheduleResolver.resolveNextWidgetUpdateMillis(
            nowMillis = System.currentTimeMillis(),
            startDateStr = currentSchedule.startDate,
            rawCourses = rawCourses,
            overrideMap = overrideMap,
            timeNodeMap = timeNodeMap,
            makeUpRules = makeUpRules
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_UPDATE_WIDGET" }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WIDGET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextUpdateMillis, pendingIntent)
        } catch (_: Exception) {
        }
    }
}
