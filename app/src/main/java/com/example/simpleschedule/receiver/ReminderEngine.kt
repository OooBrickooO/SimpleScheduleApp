package com.example.simpleschedule // 请修改为你真实的包名

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

object ReminderEngine {
    private const val REQUEST_CODE_REMIND = 1001
    private const val REQUEST_CODE_CANCEL = 1002

    suspend fun calculateAndScheduleNext(context: Context, appDao: AppDao) {
        val prefs = context.dataStore.data.firstOrNull() ?: return
        val enabled = prefs[SettingsKeys.REMINDER_ENABLED] ?: false
        if (!enabled) return

        val advanceMins = prefs[SettingsKeys.REMINDER_ADVANCE_MINS] ?: 20
        val isVoiceOn = prefs[SettingsKeys.REMINDER_VOICE_ENABLED] ?: true
        val isNotifyOn = prefs[SettingsKeys.REMINDER_NOTIFY_ENABLED] ?: true

        val savedId = prefs[SettingsKeys.CURRENT_SCHEDULE_ID] ?: return
        val groups = appDao.getAllScheduleGroups().firstOrNull() ?: return
        val currentSchedule = groups.find { it.id == savedId } ?: return

        val cal = Calendar.getInstance()
        var currentWeek = 1
        if (currentSchedule.startDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startCal = Calendar.getInstance().apply { time = sdf.parse(currentSchedule.startDate) ?: Date() }
                while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) startCal.add(Calendar.DAY_OF_YEAR, -1)
                val currentCalForWeek = Calendar.getInstance()
                while (currentCalForWeek.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) currentCalForWeek.add(Calendar.DAY_OF_YEAR, -1)
                val diffMillis = currentCalForWeek.timeInMillis - startCal.timeInMillis
                currentWeek = ((diffMillis / (1000 * 60 * 60 * 24)) / 7).toInt() + 1
                if (currentWeek < 1) currentWeek = 1
            } catch (e: Exception) {}
        }

        val rawCourses = appDao.getCoursesBySchedule(currentSchedule.id).firstOrNull() ?: return
        val overrides = appDao.getAllOverrides().firstOrNull() ?: emptyList()
        val overrideMap = overrides.associateBy { it.courseId }
        val timeNodes = appDao.getTimeNodes(currentSchedule.timetableId)
        val timeNodeMap = timeNodes.associateBy { it.nodeIndex }

        val currentTimeMillis = System.currentTimeMillis()
        var nextTriggerTimeMillis = Long.MAX_VALUE
        var nextCourseToRemind: Course? = null
        var nextCourseStartStr = ""
        var nextCourseEndStr = ""

        for (dayOffset in 0..3) {
            val evalCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            val evalDayOfWeek = if (evalCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else evalCal.get(Calendar.DAY_OF_WEEK) - 1

            val validCourses = rawCourses.mapNotNull { course ->
                val over = overrideMap[course.id]
                val effectiveDay = over?.newDayOfWeek ?: course.dayOfWeek
                val effectiveStart = over?.newStartNode ?: course.startNode
                val effectiveEnd = over?.newEndNode ?: course.endNode
                if (effectiveDay != evalDayOfWeek) return@mapNotNull null

                val weeksList = course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }
                if (!weeksList.contains(currentWeek)) return@mapNotNull null

                val startNodeInfo = timeNodeMap[effectiveStart] ?: return@mapNotNull null
                val endNodeInfo = timeNodeMap[effectiveEnd] ?: return@mapNotNull null

                val courseTimeCal = evalCal.clone() as Calendar
                val timeParts = startNodeInfo.startTime.split(":")
                courseTimeCal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                courseTimeCal.set(Calendar.MINUTE, timeParts[1].toInt())
                courseTimeCal.set(Calendar.SECOND, 0)

                val triggerTime = courseTimeCal.timeInMillis - advanceMins * 60000L

                if (triggerTime > currentTimeMillis) {
                    Triple(course, triggerTime, "${startNodeInfo.startTime}-${endNodeInfo.endTime}")
                } else null
            }

            if (validCourses.isNotEmpty()) {
                val closest = validCourses.minByOrNull { it.second }!!
                nextTriggerTimeMillis = closest.second
                nextCourseToRemind = closest.first
                nextCourseStartStr = closest.third.split("-")[0]
                nextCourseEndStr = closest.third.split("-")[1]
                break
            }
        }

        if (nextCourseToRemind != null) {
            val classStartCal = Calendar.getInstance().apply { timeInMillis = nextTriggerTimeMillis + advanceMins * 60000L }
            scheduleAlarmByParams(context, nextCourseToRemind.name, nextCourseToRemind.location, "$nextCourseStartStr-$nextCourseEndStr", nextTriggerTimeMillis, classStartCal.timeInMillis, isNotifyOn, isVoiceOn)
        }
    }

    @SuppressLint("MissingPermission")
    fun scheduleAlarmByParams(context: Context, courseName: String, location: String, timeStr: String, triggerMillis: Long, classStartMillis: Long, showNotify: Boolean, playVoice: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

        val showIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = "ACTION_SHOW_REMINDER"
            putExtra("COURSE_NAME", courseName)
            putExtra("LOCATION", location)
            putExtra("TIME_STR", timeStr)
            putExtra("CLASS_START_MILLIS", classStartMillis)
            putExtra("SHOW_NOTIFY", showNotify)
            putExtra("PLAY_VOICE", playVoice)
        }
        val showPending = PendingIntent.getBroadcast(context, REQUEST_CODE_REMIND, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val cancelIntent = Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_DISMISS_REMINDER" }
        val cancelPending = PendingIntent.getBroadcast(context, REQUEST_CODE_CANCEL, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return

            val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(triggerMillis, showPending)
            alarmManager.setAlarmClock(alarmClockInfo, showPending)
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, classStartMillis, cancelPending)
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    suspend fun scheduleNextWidgetUpdate(context: Context, appDao: AppDao) {
        val prefs = context.dataStore.data.firstOrNull() ?: return
        val savedId = prefs[SettingsKeys.CURRENT_SCHEDULE_ID] ?: return
        val groups = appDao.getAllScheduleGroups().firstOrNull() ?: return
        val currentSchedule = groups.find { it.id == savedId } ?: return

        val cal = Calendar.getInstance()
        val todayDayOfWeek = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1

        var currentWeek = 1
        if (currentSchedule.startDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startCal = Calendar.getInstance().apply { time = sdf.parse(currentSchedule.startDate) ?: Date() }
                while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) startCal.add(Calendar.DAY_OF_YEAR, -1)
                val currentCalForWeek = Calendar.getInstance()
                while (currentCalForWeek.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) currentCalForWeek.add(Calendar.DAY_OF_YEAR, -1)
                val diffMillis = currentCalForWeek.timeInMillis - startCal.timeInMillis
                currentWeek = ((diffMillis / (1000 * 60 * 60 * 24)) / 7).toInt() + 1
                if (currentWeek < 1) currentWeek = 1
            } catch (e: Exception) {}
        }

        val rawCourses = appDao.getCoursesBySchedule(currentSchedule.id).firstOrNull() ?: emptyList()
        val overrides = appDao.getAllOverrides().firstOrNull() ?: emptyList()
        val overrideMap = overrides.associateBy { it.courseId }
        val timeNodes = appDao.getTimeNodes(currentSchedule.timetableId)
        val timeNodeMap = timeNodes.associateBy { it.nodeIndex }

        val todayCoursesEndMillis = rawCourses.mapNotNull { course ->
            val over = overrideMap[course.id]
            val effectiveDay = over?.newDayOfWeek ?: course.dayOfWeek
            if (effectiveDay != todayDayOfWeek) return@mapNotNull null

            val weeksList = course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }
            if (!weeksList.contains(currentWeek)) return@mapNotNull null

            val effectiveEnd = over?.newEndNode ?: course.endNode
            val endNodeInfo = timeNodeMap[effectiveEnd] ?: return@mapNotNull null

            val timeParts = endNodeInfo.endTime.split(":")
            if (timeParts.size != 2) return@mapNotNull null

            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }
            val millis = endCal.timeInMillis
            if (millis > System.currentTimeMillis()) millis else null
        }

        var nextUpdateMillis = todayCoursesEndMillis.minOrNull()

        if (nextUpdateMillis == null) {
            val midnightCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 1)
            }
            nextUpdateMillis = midnightCal.timeInMillis
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_UPDATE_WIDGET" }
        val pendingIntent = PendingIntent.getBroadcast(context, 1003, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextUpdateMillis!!, pendingIntent)
        } catch (e: Exception) {}
    }
}

