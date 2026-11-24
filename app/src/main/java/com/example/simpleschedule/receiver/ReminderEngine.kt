package com.example.simpleschedule.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
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
    const val ACTION_SHOW_REMINDER = "ACTION_SHOW_REMINDER"
    const val ACTION_CLASS_START = "ACTION_CLASS_START"
    const val ACTION_DISMISS_REMINDER = "ACTION_DISMISS_REMINDER"
    const val ACTION_UPDATE_WIDGET = "ACTION_UPDATE_WIDGET"

    const val EXTRA_COURSE_NAME = "COURSE_NAME"
    const val EXTRA_LOCATION = "LOCATION"
    const val EXTRA_TIME_STR = "TIME_STR"
    const val EXTRA_CLASS_START_MILLIS = "CLASS_START_MILLIS"
    const val EXTRA_CLASS_END_MILLIS = "CLASS_END_MILLIS"
    const val EXTRA_SHOW_NOTIFY = "SHOW_NOTIFY"
    const val EXTRA_PLAY_VOICE = "PLAY_VOICE"
    const val EXTRA_COLOR_THEME = "COLOR_THEME"
    const val EXTRA_SCHEDULED_TRIGGER_MILLIS = "SCHEDULED_TRIGGER_MILLIS"
    const val EXTRA_IS_TEST = "IS_TEST"
    const val EXTRA_DISMISS_REASON = "DISMISS_REASON"

    const val REQUEST_CODE_REMIND = 1001
    const val REQUEST_CODE_CANCEL = 1002
    const val REQUEST_CODE_WIDGET = 1003
    const val REQUEST_CODE_CLASS_START = 1004
    const val REQUEST_CODE_MANUAL_DISMISS = 1005
    const val REQUEST_CODE_SWIPE_DISMISS = 1006
    const val REQUEST_CODE_SAFETY_DISMISS = 1007

    fun cancelDismissAndFollowUpAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        cancelBroadcastAlarm(alarmManager, context, REQUEST_CODE_CANCEL, ACTION_DISMISS_REMINDER)
        cancelBroadcastAlarm(alarmManager, context, REQUEST_CODE_CLASS_START, ACTION_CLASS_START)
        cancelBroadcastAlarm(alarmManager, context, REQUEST_CODE_SAFETY_DISMISS, ACTION_DISMISS_REMINDER)
    }

    fun cancelAllReminderAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelBroadcastAlarm(alarmManager, context, REQUEST_CODE_REMIND, ACTION_SHOW_REMINDER)
        cancelDismissAndFollowUpAlarms(context)
        LocalLogger.log(context, "ReminderEngine", "已取消所有已注册的上课提醒闹钟")
    }

    private fun cancelBroadcastAlarm(
        alarmManager: AlarmManager,
        context: Context,
        requestCode: Int,
        action: String
    ) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, CourseAlarmReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    suspend fun calculateAndScheduleNext(
        context: Context,
        appDao: AppDao,
        resetHandledState: Boolean = false
    ) {
        val appContext = context.applicationContext
        if (resetHandledState) {
            appContext.dataStore.edit { prefs ->
                prefs[SettingsKeys.LAST_TRIGGERED_CLASS_START_MILLIS] = 0L
                prefs[SettingsKeys.LAST_DISMISSED_CLASS_START_MILLIS] = 0L
            }
        }

        val prefs = appContext.dataStore.data.first()
        val enabled = prefs[SettingsKeys.REMINDER_ENABLED] ?: false
        if (!enabled) {
            cancelAllReminderAlarms(appContext)
            CourseAlarmService.dismissActiveNotificationAndStop(appContext)
            return
        }

        val nowMillis = System.currentTimeMillis()
        val lastTriggered = prefs[SettingsKeys.LAST_TRIGGERED_CLASS_START_MILLIS] ?: 0L
        val lastDismissed = prefs[SettingsKeys.LAST_DISMISSED_CLASS_START_MILLIS] ?: 0L

        // Active-reminder guard: keep the currently displayed notification's dismiss alarm intact
        if (!resetHandledState && lastTriggered > nowMillis && lastDismissed < lastTriggered) {
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
            nowMillis = nowMillis,
            advanceMins = advanceMins,
            startDateStr = currentSchedule.startDate,
            rawCourses = rawCourses,
            overrideMap = overrideMap,
            timeNodeMap = timeNodeMap,
            makeUpRules = makeUpRules,
            lookaheadDays = 14,
            minClassStartExclusiveMillis = maxOf(nowMillis, lastDismissed)
        )
        if (nextEvent == null) {
            cancelAllReminderAlarms(appContext)
            return
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            LocalLogger.log(
                appContext,
                "ReminderEngine",
                "成功计算下一节课：${nextEvent.course.name}, 触发=${sdf.format(Date(nextEvent.triggerMillis))}, 上课=${sdf.format(Date(nextEvent.classStartMillis))}, 下课=${sdf.format(Date(nextEvent.classEndMillis))}"
            )
        } catch (_: Exception) {
        }

        scheduleAlarmByParams(
            context = appContext,
            courseName = nextEvent.course.name,
            location = nextEvent.course.location,
            timeStr = nextEvent.timeRange,
            triggerMillis = nextEvent.triggerMillis,
            classStartMillis = nextEvent.classStartMillis,
            classEndMillis = nextEvent.classEndMillis,
            showNotify = isNotifyOn,
            playVoice = isVoiceOn,
            colorTheme = nextEvent.course.colorTheme,
            isTest = false
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
        colorTheme: String,
        isTest: Boolean = false
    ) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel legacy ACTION_CLASS_START (1004) if any older version registered it
        cancelBroadcastAlarm(alarmManager, appContext, REQUEST_CODE_CLASS_START, ACTION_CLASS_START)

        val showIntent = Intent(appContext, CourseAlarmReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_COURSE_NAME, courseName)
            putExtra(EXTRA_LOCATION, location)
            putExtra(EXTRA_TIME_STR, timeStr)
            putExtra(EXTRA_CLASS_START_MILLIS, classStartMillis)
            putExtra(EXTRA_CLASS_END_MILLIS, classEndMillis)
            putExtra(EXTRA_SHOW_NOTIFY, showNotify)
            putExtra(EXTRA_PLAY_VOICE, playVoice)
            putExtra(EXTRA_COLOR_THEME, colorTheme)
            putExtra(EXTRA_SCHEDULED_TRIGGER_MILLIS, triggerMillis)
            putExtra(EXTRA_IS_TEST, isTest)
        }
        val showPending = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_REMIND,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(appContext, CourseAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_REMINDER
            putExtra(EXTRA_COURSE_NAME, courseName)
            putExtra(EXTRA_CLASS_START_MILLIS, classStartMillis)
            putExtra(EXTRA_SCHEDULED_TRIGGER_MILLIS, classStartMillis)
            putExtra(EXTRA_IS_TEST, isTest)
            putExtra(EXTRA_DISMISS_REASON, "class_start")
        }
        val cancelPending = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_CANCEL,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                LocalLogger.log(appContext, "ReminderEngine", "注册精确闹钟失败：缺少系统精确闹钟权限")
                ReminderDiagnosticStore.record(
                    context = appContext,
                    eventType = "ERROR",
                    courseName = courseName,
                    scheduledMillis = triggerMillis,
                    detail = "缺少精确闹钟权限，未注册提醒闹钟"
                )
                return
            }

            val currentTime = System.currentTimeMillis()
            if (triggerMillis > currentTime) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPending)
                alarmManager.setAlarmClock(alarmClockInfo, showPending)
            }
            if (classStartMillis > currentTime) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, classStartMillis, cancelPending)
            }

            val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
            val triggerLabel = timeFmt.format(Date(triggerMillis))
            val dismissLabel = if (classStartMillis > 0L) timeFmt.format(Date(classStartMillis)) else "无"
            ReminderDiagnosticStore.record(
                context = appContext,
                eventType = "SCHEDULED",
                courseName = courseName,
                scheduledMillis = triggerMillis,
                detail = "触发=$triggerLabel, 上课消通知=$dismissLabel, 通知=$showNotify, 播报=$playVoice, 测试=$isTest"
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            ReminderDiagnosticStore.record(
                context = appContext,
                eventType = "ERROR",
                courseName = courseName,
                scheduledMillis = triggerMillis,
                detail = "注册闹钟 SecurityException: ${e.message}"
            )
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
        val intent = Intent(context, CourseAlarmReceiver::class.java).apply { action = ACTION_UPDATE_WIDGET }
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
