package com.example.simpleschedule.viewmodel

import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.*
import com.example.simpleschedule.receiver.ReminderEngine
import com.example.simpleschedule.utils.SmbHelper
import com.example.simpleschedule.utils.WebDavHelper
import com.example.simpleschedule.utils.SecureSecretStore
import com.example.simpleschedule.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
    val dynamicIslandEnabled = application.dataStore.data.map { it[SettingsKeys.DYNAMIC_ISLAND_ENABLED] ?: false }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val predictiveBackEnabled = application.dataStore.data.map { it[SettingsKeys.PREDICTIVE_BACK_ENABLED] ?: true }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val floatingBottomBar = application.dataStore.data.map { it[SettingsKeys.FLOATING_BOTTOM_BAR] ?: false }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val tabAnimationType = application.dataStore.data.map { it[SettingsKeys.TAB_ANIMATION_TYPE] ?: "Slide" }.stateIn(viewModelScope, SharingStarted.Lazily, "Slide")
    val hasSeenMakeUpGuide: StateFlow<Boolean?> = application.dataStore.data.map { 
        val seen: Boolean? = it[SettingsKeys.HAS_SEEN_MAKEUP_GUIDE] ?: false
        seen
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val accentColor = application.dataStore.data.map { it[SettingsKeys.ACCENT_COLOR] ?: -1L }.stateIn(viewModelScope, SharingStarted.Lazily, -1L)
    val courseColorPool = application.dataStore.data.map { it[SettingsKeys.COURSE_COLOR_POOL] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun markMakeUpGuideSeen() {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[SettingsKeys.HAS_SEEN_MAKEUP_GUIDE] = true }
        }
    }

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
    fun updateSetting(key: Preferences.Key<Long>, value: Long) = viewModelScope.launch {
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

    val makeUpRules = _currentScheduleId.flatMapLatest { sId ->
        appDao.getMakeUpRulesBySchedule(sId)
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
            CourseWidget2x2().updateAll(getApplication())
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

    fun createNewSchedule(name: String, onCreated: (() -> Unit)? = null) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            appDao.insertScheduleGroup(ScheduleGroup(newId, name, "tt_cjlu", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())))
            switchSchedule(newId)
            onCreated?.invoke()
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
            val existingCourse = appDao.getCoursesBySchedule(_currentScheduleId.value)
                .firstOrNull()
                ?.firstOrNull { it.id == id }
            val existingOverride = appDao.getAllOverrides()
                .firstOrNull()
                ?.firstOrNull { it.courseId == id }
            val originalCourse = existingCourse ?: Course(
                id = id,
                scheduleId = _currentScheduleId.value,
                name = name,
                location = location,
                teacher = teacher,
                dayOfWeek = day,
                startNode = start,
                endNode = end,
                weeks = weeks,
                colorTheme = color,
                credits = credits
            )
            appDao.updateCourse(
                originalCourse.copy(
                    name = name,
                    location = location,
                    teacher = teacher,
                    dayOfWeek = day,
                    startNode = start,
                    endNode = end,
                    weeks = weeks,
                    colorTheme = color,
                    credits = credits
                )
            )
            if (existingOverride != null) {
                appDao.insertOverride(
                    existingOverride.copy(
                        newDayOfWeek = day,
                        newStartNode = start,
                        newEndNode = end
                    )
                )
            }
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

    fun addMakeUpRule(sourceDate: String, targetDate: String) {
        viewModelScope.launch {
            appDao.insertMakeUpRule(
                MakeUpRule(
                    scheduleId = _currentScheduleId.value,
                    sourceDate = sourceDate,
                    targetDate = targetDate
                )
            )
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun deleteMakeUpRule(id: String) {
        viewModelScope.launch {
            appDao.deleteMakeUpRule(id)
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

    fun importFromJson(jsonString: String, replaceCurrent: Boolean = false, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonString)
                val targetScheduleId = _currentScheduleId.value
                if (replaceCurrent) {
                    appDao.deleteCoursesBySchedule(targetScheduleId)
                }
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
                            colorTheme = obj.optString("colorTheme", "blue"),
                            credits = obj.optString("credits").takeIf { it.isNotBlank() }
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
            importFromJson(jsonString, onResult = onResult)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    // --- 云备份 StateFlow ---
    val webdavUrl = application.dataStore.data.map { it[SettingsKeys.WEBDAV_URL] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val webdavUsername = application.dataStore.data.map { it[SettingsKeys.WEBDAV_USERNAME] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val webdavPassword = application.dataStore.data.map { prefs -> SecureSecretStore.readSync(prefs, SettingsKeys.WEBDAV_PASSWORD_ENCRYPTED, SettingsKeys.WEBDAV_PASSWORD) }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val webdavRemotePath = application.dataStore.data.map { it[SettingsKeys.WEBDAV_REMOTE_PATH] ?: "SimpleSchedule/backup.json" }.stateIn(viewModelScope, SharingStarted.Lazily, "SimpleSchedule/backup.json")

    val smbHost = application.dataStore.data.map { it[SettingsKeys.SMB_HOST] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val smbPort = application.dataStore.data.map { it[SettingsKeys.SMB_PORT] ?: 445 }.stateIn(viewModelScope, SharingStarted.Lazily, 445)
    val smbShare = application.dataStore.data.map { it[SettingsKeys.SMB_SHARE] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val smbUsername = application.dataStore.data.map { it[SettingsKeys.SMB_USERNAME] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val smbPassword = application.dataStore.data.map { prefs ->
        SecureSecretStore.readSync(prefs, SettingsKeys.SMB_PASSWORD_ENCRYPTED, SettingsKeys.SMB_PASSWORD)
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val smbDomain = application.dataStore.data.map { it[SettingsKeys.SMB_DOMAIN] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val smbRemotePath = application.dataStore.data.map { it[SettingsKeys.SMB_REMOTE_PATH] ?: "SimpleSchedule/backup.json" }.stateIn(viewModelScope, SharingStarted.Lazily, "SimpleSchedule/backup.json")

    val lastBackupTime = application.dataStore.data.map { it[SettingsKeys.LAST_BACKUP_TIME] ?: "" }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    // --- 云备份与数据导入导出核心方法 ---

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val root = JSONObject().apply {
            put("version", 1)
            put("exportTime", sdf.format(Date()))
            put("appName", "SimpleSchedule")

            val scheduleArray = JSONArray()
            appDao.getAllScheduleGroupsDirect().forEach { sg ->
                scheduleArray.put(JSONObject().apply {
                    put("id", sg.id)
                    put("name", sg.name)
                    put("timetableId", sg.timetableId)
                    put("startDate", sg.startDate)
                })
            }
            put("schedules", scheduleArray)

            val timetableArray = JSONArray()
            appDao.getAllTimetableGroupsDirect().forEach { tg ->
                timetableArray.put(JSONObject().apply {
                    put("id", tg.id)
                    put("name", tg.name)
                })
            }
            put("timetables", timetableArray)

            val timeNodeArray = JSONArray()
            appDao.getAllTimeNodesDirect().forEach { tn ->
                timeNodeArray.put(JSONObject().apply {
                    put("id", tn.id)
                    put("timetableId", tn.timetableId)
                    put("nodeIndex", tn.nodeIndex)
                    put("startTime", tn.startTime)
                    put("endTime", tn.endTime)
                })
            }
            put("timeNodes", timeNodeArray)

            val courseArray = JSONArray()
            appDao.getAllCoursesDirect().forEach { c ->
                courseArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("scheduleId", c.scheduleId)
                    put("name", c.name)
                    put("location", c.location)
                    put("teacher", c.teacher)
                    put("dayOfWeek", c.dayOfWeek)
                    put("startNode", c.startNode)
                    put("endNode", c.endNode)
                    put("weeks", c.weeks)
                    put("colorTheme", c.colorTheme)
                    put("credits", c.credits ?: "")
                })
            }
            put("courses", courseArray)

            val overrideArray = JSONArray()
            appDao.getAllOverridesDirect().forEach { co ->
                overrideArray.put(JSONObject().apply {
                    put("courseId", co.courseId)
                    put("newDayOfWeek", co.newDayOfWeek)
                    put("newStartNode", co.newStartNode)
                    put("newEndNode", co.newEndNode)
                })
            }
            put("courseOverrides", overrideArray)

            val statArray = JSONArray()
            appDao.getAllStatisticsDirect().forEach { cs ->
                statArray.put(JSONObject().apply {
                    put("courseName", cs.courseName)
                    put("absenceCount", cs.absenceCount)
                    put("calledCount", cs.calledCount)
                })
            }
            put("courseStatistics", statArray)

            val makeupArray = JSONArray()
            appDao.getAllMakeUpRulesDirect().forEach { mr ->
                makeupArray.put(JSONObject().apply {
                    put("id", mr.id)
                    put("scheduleId", mr.scheduleId)
                    put("sourceDate", mr.sourceDate)
                    put("targetDate", mr.targetDate)
                })
            }
            put("makeUpRules", makeupArray)
        }
        root.toString(2)
    }

    suspend fun exportCurrentScheduleJson(): String = withContext(Dispatchers.IO) {
        val sId = _currentScheduleId.value
        val courses = appDao.getAllCoursesDirect().filter { it.scheduleId == sId }
        val courseArray = JSONArray()
        courses.forEach { c ->
            courseArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("location", c.location)
                put("teacher", c.teacher)
                put("dayOfWeek", c.dayOfWeek)
                put("startNode", c.startNode)
                put("endNode", c.endNode)
                put("weeks", c.weeks)
                put("colorTheme", c.colorTheme)
                put("credits", c.credits ?: "")
            })
        }
        courseArray.toString(2)
    }

    fun restoreFullBackupJson(jsonString: String, mode: String = "FULL", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmed = jsonString.trim()
                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    if (mode == "FULL") {
                        appDao.deleteAllCourses()
                        appDao.deleteAllScheduleGroups()
                        appDao.deleteAllTimetableGroups()
                        appDao.deleteAllTimeNodes()
                        appDao.deleteAllOverrides()
                        appDao.deleteAllStatistics()
                        appDao.deleteAllMakeUpRules()
                    }

                    root.optJSONArray("schedules")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            appDao.insertScheduleGroup(
                                ScheduleGroup(
                                    id = obj.getString("id"),
                                    name = obj.getString("name"),
                                    timetableId = obj.optString("timetableId", "default_timetable"),
                                    startDate = obj.optString("startDate", "")
                                )
                            )
                        }
                    }

                    root.optJSONArray("timetables")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            appDao.insertTimetableGroup(
                                TimetableGroup(
                                    id = obj.getString("id"),
                                    name = obj.getString("name")
                                )
                            )
                        }
                    }

                    root.optJSONArray("timeNodes")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            appDao.insertTimeNodes(
                                listOf(
                                    TimeNode(
                                        id = obj.optString("id", UUID.randomUUID().toString()),
                                        timetableId = obj.getString("timetableId"),
                                        nodeIndex = obj.getInt("nodeIndex"),
                                        startTime = obj.getString("startTime"),
                                        endTime = obj.getString("endTime")
                                    )
                                )
                            )
                        }
                    }

                    root.optJSONArray("courses")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            appDao.insertCourse(
                                Course(
                                    id = obj.optString("id", UUID.randomUUID().toString()),
                                    scheduleId = obj.optString("scheduleId", _currentScheduleId.value),
                                    name = obj.optString("name", "未知课程"),
                                    location = obj.optString("location", ""),
                                    teacher = obj.optString("teacher", ""),
                                    dayOfWeek = obj.optInt("dayOfWeek", 1),
                                    startNode = obj.optInt("startNode", 1),
                                    endNode = obj.optInt("endNode", 2),
                                    weeks = obj.optString("weeks", "[]"),
                                    colorTheme = obj.optString("colorTheme", "blue"),
                                    credits = obj.optString("credits").takeIf { !it.isNullOrBlank() }
                                )
                            )
                        }
                    }

                    root.optJSONArray("courseOverrides")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            appDao.insertOverride(
                                CourseOverride(
                                    courseId = obj.getString("courseId"),
                                    newDayOfWeek = obj.getInt("newDayOfWeek"),
                                    newStartNode = obj.getInt("newStartNode"),
                                    newEndNode = obj.getInt("newEndNode")
                                )
                            )
                        }
                    }

                    root.optJSONArray("courseStatistics")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            appDao.insertStatistic(
                                CourseStatistic(
                                    courseName = obj.getString("courseName"),
                                    absenceCount = obj.optInt("absenceCount", 0),
                                    calledCount = obj.optInt("calledCount", 0)
                                )
                            )
                        }
                    }

                    root.optJSONArray("makeUpRules")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            appDao.insertMakeUpRule(
                                MakeUpRule(
                                    id = obj.optString("id", UUID.randomUUID().toString()),
                                    scheduleId = obj.getString("scheduleId"),
                                    sourceDate = obj.getString("sourceDate"),
                                    targetDate = obj.getString("targetDate")
                                )
                            )
                        }
                    }

                    val groups = appDao.getAllScheduleGroupsDirect()
                    if (groups.isNotEmpty()) {
                        switchSchedule(groups.first().id)
                    }

                    notifyWidgetUpdate()
                    scheduleNextAlarm()
                    onResult(true, "备份数据恢复成功！")
                } else if (trimmed.startsWith("[")) {
                    importFromJson(trimmed, replaceCurrent = (mode == "FULL")) { success ->
                        if (success) {
                            onResult(true, "课程列表导入成功！")
                        } else {
                            onResult(false, "课程数据解析失败！")
                        }
                    }
                } else {
                    onResult(false, "无效的 JSON 数据格式")
                }
            } catch (e: Exception) {
                onResult(false, "导入失败: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun saveWebDavSettings(url: String, user: String, pass: String, remotePath: String) {
        updateSetting(SettingsKeys.WEBDAV_URL, url)
        updateSetting(SettingsKeys.WEBDAV_USERNAME, user)
        viewModelScope.launch { SecureSecretStore.write(getApplication<Application>(), SettingsKeys.WEBDAV_PASSWORD_ENCRYPTED, SettingsKeys.WEBDAV_PASSWORD, pass) }
        updateSetting(SettingsKeys.WEBDAV_REMOTE_PATH, remotePath)
    }

    fun testWebDavConnection(url: String, user: String, pass: String, remotePath: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = WebDavHelper.testConnection(url, user, pass, remotePath)
            res.fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message ?: "连接失败") }
            )
        }
    }

    fun backupToWebDav(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val url = webdavUrl.value
                val user = webdavUsername.value
                val pass = webdavPassword.value
                val path = webdavRemotePath.value
                if (url.isBlank()) {
                    onResult(false, "请先填写 WebDAV 服务器地址")
                    return@launch
                }
                val content = exportFullBackupJson()
                val res = WebDavHelper.uploadFile(url, user, pass, path, content)
                res.fold(
                    onSuccess = {
                        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        updateSetting(SettingsKeys.LAST_BACKUP_TIME, now)
                        onResult(true, "WebDAV 云备份成功！")
                    },
                    onFailure = { onResult(false, it.message ?: "WebDAV 备份失败") }
                )
            } catch (e: Exception) {
                onResult(false, "备份失败: ${e.localizedMessage}")
            }
        }
    }

    fun restoreFromWebDav(mode: String = "FULL", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val url = webdavUrl.value
                val user = webdavUsername.value
                val pass = webdavPassword.value
                val path = webdavRemotePath.value
                if (url.isBlank()) {
                    onResult(false, "请先填写 WebDAV 服务器地址")
                    return@launch
                }
                val res = WebDavHelper.downloadFile(url, user, pass, path)
                res.fold(
                    onSuccess = { content ->
                        restoreFullBackupJson(content, mode = mode, onResult = onResult)
                    },
                    onFailure = { onResult(false, it.message ?: "WebDAV 下载失败") }
                )
            } catch (e: Exception) {
                onResult(false, "从 WebDAV 恢复失败: ${e.localizedMessage}")
            }
        }
    }

    fun saveSmbSettings(host: String, port: Int, share: String, user: String, pass: String, domain: String, remotePath: String) {
        updateSetting(SettingsKeys.SMB_HOST, host)
        updateSetting(SettingsKeys.SMB_PORT, port)
        updateSetting(SettingsKeys.SMB_SHARE, share)
        updateSetting(SettingsKeys.SMB_USERNAME, user)
        viewModelScope.launch { SecureSecretStore.write(getApplication<Application>(), SettingsKeys.SMB_PASSWORD_ENCRYPTED, SettingsKeys.SMB_PASSWORD, pass) }
        updateSetting(SettingsKeys.SMB_DOMAIN, domain)
        updateSetting(SettingsKeys.SMB_REMOTE_PATH, remotePath)
    }

    fun testSmbConnection(host: String, port: Int, share: String, user: String, pass: String, domain: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = SmbHelper.testConnection(host, port, share, user, pass, domain)
            res.fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message ?: "连接失败") }
            )
        }
    }

    fun backupToSmb(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val host = smbHost.value
                val port = smbPort.value
                val share = smbShare.value
                val user = smbUsername.value
                val pass = smbPassword.value
                val domain = smbDomain.value
                val path = smbRemotePath.value

                if (host.isBlank() || share.isBlank()) {
                    onResult(false, "请先填写 SMB 服务器 IP 和共享名称")
                    return@launch
                }

                val content = exportFullBackupJson()
                val res = SmbHelper.uploadFile(host, port, share, user, pass, domain, path, content)
                res.fold(
                    onSuccess = {
                        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        updateSetting(SettingsKeys.LAST_BACKUP_TIME, now)
                        onResult(true, "SMB 云备份成功！")
                    },
                    onFailure = { onResult(false, it.message ?: "SMB 备份失败") }
                )
            } catch (e: Exception) {
                onResult(false, "SMB 备份异常: ${e.localizedMessage}")
            }
        }
    }

    fun restoreFromSmb(mode: String = "FULL", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val host = smbHost.value
                val port = smbPort.value
                val share = smbShare.value
                val user = smbUsername.value
                val pass = smbPassword.value
                val domain = smbDomain.value
                val path = smbRemotePath.value

                if (host.isBlank() || share.isBlank()) {
                    onResult(false, "请先填写 SMB 服务器 IP 和共享名称")
                    return@launch
                }

                val res = SmbHelper.downloadFile(host, port, share, user, pass, domain, path)
                res.fold(
                    onSuccess = { content ->
                        restoreFullBackupJson(content, mode = mode, onResult = onResult)
                    },
                    onFailure = { onResult(false, it.message ?: "SMB 下载失败") }
                )
            } catch (e: Exception) {
                onResult(false, "从 SMB 恢复失败: ${e.localizedMessage}")
            }
        }
    }
}


