package com.example.simpleschedule.viewmodel

import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.*
import com.example.simpleschedule.receiver.ReminderEngine
import com.example.simpleschedule.widget.CourseWidget

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

// --- 2. 状态管理 (ViewModel) ---

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val appDao = AppDatabase.getDatabase(application).appDao()

    val scheduleGroups = appDao.getAllScheduleGroups().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val timetableGroups = appDao.getAllTimetableGroups().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentScheduleId = MutableStateFlow("default_id")
    val currentScheduleId = _currentScheduleId.asStateFlow()

    private val _currentWeek = MutableStateFlow(9)
    val currentWeek = _currentWeek.asStateFlow()

    val isDarkTheme = application.dataStore.data.map { it[SettingsKeys.IS_DARK_THEME] }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val showSat = application.dataStore.data.map { it[SettingsKeys.SHOW_SAT] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val showSun = application.dataStore.data.map { it[SettingsKeys.SHOW_SUN] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val showNotThisWeek = application.dataStore.data.map { it[SettingsKeys.SHOW_NOT_THIS_WEEK] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val cellHeight = application.dataStore.data.map { it[SettingsKeys.CELL_HEIGHT] ?: 64f }.stateIn(viewModelScope, SharingStarted.Lazily, 64f)
    val cornerRadius = application.dataStore.data.map { it[SettingsKeys.CORNER_RADIUS] ?: 4f }.stateIn(viewModelScope, SharingStarted.Lazily, 4f)
    val hideTime = application.dataStore.data.map { it[SettingsKeys.HIDE_TIME] ?: false }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val bottomBlank = application.dataStore.data.map { it[SettingsKeys.BOTTOM_BLANK] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val vibration = application.dataStore.data.map { it[SettingsKeys.VIBRATION] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    val totalWeeks = application.dataStore.data.map { it[SettingsKeys.TOTAL_WEEKS] ?: 20 }.stateIn(viewModelScope, SharingStarted.Lazily, 20)
    val materialYou = application.dataStore.data.map { it[SettingsKeys.MATERIAL_YOU] ?: false }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val warnTimetableError = application.dataStore.data.map { it[SettingsKeys.WARN_TIMETABLE_ERROR] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val autoUpdate = application.dataStore.data.map { it[SettingsKeys.AUTO_UPDATE] ?: false }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val reminderEnabled = application.dataStore.data.map { it[SettingsKeys.REMINDER_ENABLED] ?: false }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val reminderVoiceEnabled = application.dataStore.data.map { it[SettingsKeys.REMINDER_VOICE_ENABLED] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val reminderNotifyEnabled = application.dataStore.data.map { it[SettingsKeys.REMINDER_NOTIFY_ENABLED] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val reminderAdvanceMins = application.dataStore.data.map { it[SettingsKeys.REMINDER_ADVANCE_MINS] ?: 20 }.stateIn(viewModelScope, SharingStarted.Lazily, 20)

    val widgetTranslucent = application.dataStore.data.map { it[SettingsKeys.WIDGET_TRANSLUCENT] ?: false }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun updateSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[key] = value }
            scheduleNextAlarm()
        }
    }
    fun updateSetting(key: Preferences.Key<Float>, value: Float) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[key] = value }
    }
    fun updateSetting(key: Preferences.Key<Int>, value: Int) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[key] = value }
            scheduleNextAlarm()
        }
    }
    fun updateSetting(key: Preferences.Key<String>, value: String) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[key] = value }
    }

    val activeTimeNodes = _currentScheduleId.flatMapLatest { sId ->
        appDao.getScheduleGroupFlow(sId).flatMapLatest { sg ->
            appDao.getTimeNodesFlow(sg?.timetableId ?: "tt_cjlu")
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val displayCourses = _currentScheduleId.flatMapLatest { scheduleId ->
        combine(appDao.getCoursesBySchedule(scheduleId), appDao.getAllOverrides()) { courses, overrides ->
            val overrideMap = overrides.associateBy { it.courseId }
            courses.map { course ->
                val override = overrideMap[course.id]
                DisplayCourse(course, override?.newDayOfWeek ?: course.dayOfWeek, override?.newStartNode ?: course.startNode, override?.newEndNode ?: course.endNode)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            ensureBuiltInTimetablesExist()
            val savedId = getApplication<Application>().dataStore.data.map { it[SettingsKeys.CURRENT_SCHEDULE_ID] }.firstOrNull()
            val groups = appDao.getAllScheduleGroups().firstOrNull()
            if (groups.isNullOrEmpty()) {
                initDefaultData()
            } else {
                val targetId = if (savedId != null && groups.any { it.id == savedId }) savedId else groups.first().id
                switchSchedule(targetId)
            }
            scheduleNextAlarm()
            ReminderEngine.scheduleNextWidgetUpdate(getApplication(), appDao)
        }
    }

    private suspend fun ensureBuiltInTimetablesExist() {
        val existingGroups = appDao.getAllTimetableGroups().firstOrNull() ?: emptyList()
        val existingIds = existingGroups.map { it.id }

        if (!existingIds.contains("tt_lzjtu")) {
            appDao.insertTimetableGroup(TimetableGroup("tt_lzjtu", "兰州交通大学"))
            appDao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 1, startTime = "08:00", endTime = "08:50"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 2, startTime = "09:00", endTime = "09:50"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 3, startTime = "10:10", endTime = "11:00"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 4, startTime = "11:10", endTime = "12:00"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 5, startTime = "14:30", endTime = "15:20"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 6, startTime = "15:30", endTime = "16:20"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 7, startTime = "16:40", endTime = "17:30"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 8, startTime = "17:40", endTime = "18:30"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 9, startTime = "19:30", endTime = "20:20"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 10, startTime = "20:30", endTime = "21:20")
            ))
        }

        if (!existingIds.contains("tt_cjlu")) {
            appDao.insertTimetableGroup(TimetableGroup("tt_cjlu", "中国计量大学"))
            appDao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 1, startTime = "08:00", endTime = "08:45"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 2, startTime = "08:50", endTime = "09:35"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 3, startTime = "09:55", endTime = "10:40"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 4, startTime = "10:45", endTime = "11:30"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 5, startTime = "11:35", endTime = "12:20"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 6, startTime = "13:30", endTime = "14:15"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 7, startTime = "14:20", endTime = "15:05"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 8, startTime = "15:15", endTime = "16:00"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 9, startTime = "16:05", endTime = "16:50"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 10, startTime = "18:00", endTime = "18:45"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 11, startTime = "18:50", endTime = "19:35"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 12, startTime = "19:40", endTime = "20:25"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 13, startTime = "21:35", endTime = "22:20"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 14, startTime = "21:45", endTime = "22:30"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 15, startTime = "21:55", endTime = "22:40"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 16, startTime = "22:05", endTime = "22:50"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 17, startTime = "22:15", endTime = "23:00")
            ))
        }

        if (!existingIds.contains("tt_zjhu_summer")) {
            appDao.insertTimetableGroup(TimetableGroup("tt_zjhu_summer", "湖州师范大学(夏)"))
            appDao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 1, startTime = "08:00", endTime = "08:40"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 2, startTime = "08:50", endTime = "09:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 3, startTime = "09:50", endTime = "10:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 4, startTime = "10:40", endTime = "11:20"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 5, startTime = "11:30", endTime = "12:10"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 6, startTime = "14:00", endTime = "14:40"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 7, startTime = "14:50", endTime = "15:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 8, startTime = "15:50", endTime = "16:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 9, startTime = "16:40", endTime = "17:20"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 10, startTime = "18:30", endTime = "19:10"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 11, startTime = "19:20", endTime = "20:00"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 12, startTime = "20:10", endTime = "20:50")
            ))
        }

        if (!existingIds.contains("tt_zjhu_winter")) {
            appDao.insertTimetableGroup(TimetableGroup("tt_zjhu_winter", "湖州师范大学(冬)"))
            appDao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 1, startTime = "08:00", endTime = "08:40"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 2, startTime = "08:50", endTime = "09:30"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 3, startTime = "09:50", endTime = "10:30"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 4, startTime = "10:40", endTime = "11:20"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 5, startTime = "11:30", endTime = "12:10"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 6, startTime = "13:30", endTime = "14:10"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 7, startTime = "14:20", endTime = "15:00"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 8, startTime = "15:20", endTime = "16:00"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 9, startTime = "16:10", endTime = "16:50"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 10, startTime = "18:00", endTime = "18:40"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 11, startTime = "18:50", endTime = "19:30"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 12, startTime = "19:40", endTime = "20:20")
            ))
        }
    }

    private suspend fun initDefaultData() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -8 * 7)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val defaultGroup = ScheduleGroup("default_id", "空白课表", "tt_cjlu", sdf.format(cal.time))
        appDao.insertScheduleGroup(defaultGroup)
        _currentScheduleId.value = defaultGroup.id
        _currentWeek.value = 9
    }

    fun notifyWidgetUpdate() {
        viewModelScope.launch {
            CourseWidget().updateAll(getApplication())
            ReminderEngine.scheduleNextWidgetUpdate(getApplication(), appDao)
        }
    }

    fun scheduleNextAlarm() {
        viewModelScope.launch {
            ReminderEngine.calculateAndScheduleNext(getApplication(), appDao)
        }
    }

    fun toggleTheme(isDark: Boolean) { updateSetting(SettingsKeys.IS_DARK_THEME, isDark) }

    fun changeWeek(week: Int) {
        updateWeekAndReverseCalculateStartDate(_currentScheduleId.value, week)
    }

    private fun calculateWeekFromStartDate(startDateStr: String): Int {
        if (startDateStr.isEmpty()) return 1
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startCal = Calendar.getInstance().apply {
                time = sdf.parse(startDateStr) ?: Date()
                set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val currentCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }

            while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                startCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (currentCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                currentCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            val diffMillis = currentCal.timeInMillis - startCal.timeInMillis
            val diffDays = diffMillis / (1000 * 60 * 60 * 24)
            val diffWeeks = (diffDays / 7).toInt() + 1

            if (diffWeeks < 1) 1 else diffWeeks
        } catch (e: Exception) { 1 }
    }

    fun refreshCurrentWeek() {
        viewModelScope.launch {
            val group = appDao.getScheduleGroupFlow(_currentScheduleId.value).firstOrNull()
            group?.let {
                val calculatedWeek = calculateWeekFromStartDate(it.startDate)
                if (_currentWeek.value != calculatedWeek) {
                    _currentWeek.value = calculatedWeek
                }
            }
        }
    }

    fun updateScheduleStartDate(id: String, dateStr: String) {
        viewModelScope.launch {
            appDao.getScheduleGroupFlow(id).firstOrNull()?.let {
                appDao.updateScheduleGroup(it.copy(startDate = dateStr))
                _currentWeek.value = calculateWeekFromStartDate(dateStr)
                notifyWidgetUpdate()
                scheduleNextAlarm()
            }
        }
    }

    fun updateWeekAndReverseCalculateStartDate(id: String, week: Int) {
        _currentWeek.value = week
        viewModelScope.launch {
            appDao.getScheduleGroupFlow(id).firstOrNull()?.let {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -(week - 1) * 7)
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                appDao.updateScheduleGroup(it.copy(startDate = sdf.format(cal.time)))
                notifyWidgetUpdate()
                scheduleNextAlarm()
            }
        }
    }

    fun switchSchedule(id: String) {
        _currentScheduleId.value = id
        updateSetting(SettingsKeys.CURRENT_SCHEDULE_ID, id)
        viewModelScope.launch {
            val group = appDao.getScheduleGroupFlow(id).firstOrNull()
            _currentWeek.value = if (group != null && group.startDate.isNotEmpty()) calculateWeekFromStartDate(group.startDate) else 1
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun createNewSchedule(name: String) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            appDao.insertScheduleGroup(ScheduleGroup(newId, name, "tt_cjlu", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())))
            switchSchedule(newId)
        }
    }

    fun renameSchedule(id: String, newName: String) {
        viewModelScope.launch {
            appDao.getScheduleGroupFlow(id).firstOrNull()?.let {
                appDao.updateScheduleGroup(it.copy(name = newName))
                notifyWidgetUpdate()
            }
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            val groups = appDao.getAllScheduleGroups().firstOrNull() ?: emptyList()
            if (groups.size > 1) {
                appDao.deleteScheduleGroup(id)
                if (_currentScheduleId.value == id) {
                    switchSchedule(groups.first { it.id != id }.id)
                } else {
                    notifyWidgetUpdate()
                    scheduleNextAlarm()
                }
            }
        }
    }

    fun linkTimetableToCurrentSchedule(timetableId: String) {
        viewModelScope.launch {
            appDao.getScheduleGroupFlow(_currentScheduleId.value).firstOrNull()?.let {
                appDao.updateScheduleGroup(it.copy(timetableId = timetableId))
                notifyWidgetUpdate()
                scheduleNextAlarm()
            }
        }
    }

    fun saveTimetable(id: String?, name: String, nodes: List<TimeNode>) {
        viewModelScope.launch {
            val tid = id ?: UUID.randomUUID().toString()
            appDao.insertTimetableGroup(TimetableGroup(tid, name))
            appDao.deleteTimeNodes(tid)
            appDao.insertTimeNodes(nodes.map { it.copy(timetableId = tid, id = UUID.randomUUID().toString()) })
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun deleteTimetable(id: String) {
        val isBuiltIn = id == "tt_cjlu" || id == "tt_zjhu_summer" || id == "tt_zjhu_winter"
        if (!isBuiltIn) {
            viewModelScope.launch {
                appDao.deleteTimeNodes(id)
                appDao.deleteTimetableGroup(id)
                notifyWidgetUpdate()
                scheduleNextAlarm()
            }
        }
    }

    suspend fun getTimeNodesForEdit(id: String): List<TimeNode> = appDao.getTimeNodes(id)

    fun updateCoursePosition(courseId: String, deltaCols: Int, deltaRows: Int, currentCourse: Course) {
        viewModelScope.launch {
            val oldOverride = appDao.getAllOverrides().firstOrNull()?.find { it.courseId == courseId }
            val currentDay = oldOverride?.newDayOfWeek ?: currentCourse.dayOfWeek
            val currentStart = oldOverride?.newStartNode ?: currentCourse.startNode
            val currentEnd = oldOverride?.newEndNode ?: currentCourse.endNode
            val span = currentEnd - currentStart
            val newDay = (currentDay + deltaCols).coerceIn(1, 7)
            val newStart = (currentStart + deltaRows).coerceIn(1, 12)
            val newEnd = (newStart + span).coerceIn(1, 12)
            appDao.insertOverride(CourseOverride(courseId, newDay, newStart, newEnd))
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun addCustomCourse(name: String, location: String, teacher: String, day: Int, start: Int, end: Int, color: String, credits: String? = null) {
        viewModelScope.launch {
            appDao.insertCourse(Course(UUID.randomUUID().toString(), _currentScheduleId.value, name, location, teacher, day, start, end, "[8,9,10,11,12]", color, credits))
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun updateCustomCourse(id: String, name: String, location: String, teacher: String, day: Int, start: Int, end: Int, color: String, weeks: String, credits: String? = null) {
        viewModelScope.launch {
            appDao.updateCourse(Course(id, _currentScheduleId.value, name, location, teacher, day, start, end, weeks, color, credits))
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            appDao.deleteCourse(courseId)
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun getCourseStatistic(courseName: String): Flow<CourseStatistic?> {
        return appDao.getStatisticFlow(courseName)
    }

    suspend fun getCourseStatisticDirect(courseName: String): CourseStatistic? {
        return appDao.getStatisticDirect(courseName)
    }

    fun incrementAbsenceCount(courseName: String) {
        viewModelScope.launch {
            val stats = appDao.getStatisticDirect(courseName) ?: CourseStatistic(courseName, 0, 0)
            appDao.insertStatistic(stats.copy(absenceCount = stats.absenceCount + 1))
        }
    }

    fun incrementCalledCount(courseName: String) {
        viewModelScope.launch {
            val stats = appDao.getStatisticDirect(courseName) ?: CourseStatistic(courseName, 0, 0)
            appDao.insertStatistic(stats.copy(calledCount = stats.calledCount + 1))
        }
    }

    fun updateCourseStatistic(courseName: String, absence: Int, called: Int) {
        viewModelScope.launch {
            appDao.insertStatistic(CourseStatistic(courseName, absence, called))
        }
    }

    fun importFromJson(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonString)
                val targetScheduleId = _currentScheduleId.value
                var maxWeekInImport = totalWeeks.value
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val weeksStr = obj.optString("weeks", "[]")
                    try {
                        val weeksArr = JSONArray(weeksStr)
                        for (j in 0 until weeksArr.length()) {
                            val w = weeksArr.getInt(j)
                            if (w > maxWeekInImport) {
                                maxWeekInImport = w
                            }
                        }
                    } catch (e: Exception) {}

                    appDao.insertCourse(
                        Course(
                            id = UUID.randomUUID().toString(),
                            scheduleId = targetScheduleId,
                            name = obj.optString("name", "未知课程"),
                            location = obj.optString("location", "未排地点"),
                            teacher = obj.optString("teacher", ""),
                            dayOfWeek = obj.optInt("dayOfWeek", 1),
                            startNode = obj.optInt("startNode", 1),
                            endNode = obj.optInt("endNode", 2),
                            weeks = weeksStr,
                            colorTheme = obj.optString("colorTheme", "blue")
                        )
                    )
                }
                if (maxWeekInImport > totalWeeks.value) {
                    updateSetting(SettingsKeys.TOTAL_WEEKS, maxWeekInImport)
                }
                notifyWidgetUpdate()
                scheduleNextAlarm()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun importFromShareCode(code: String, onResult: (Boolean) -> Unit) {
        try {
            val jsonString = String(Base64.decode(code, Base64.DEFAULT), Charsets.UTF_8)
            importFromJson(jsonString, onResult)
        } catch (e: Exception) {
            onResult(false)
        }
    }


}

