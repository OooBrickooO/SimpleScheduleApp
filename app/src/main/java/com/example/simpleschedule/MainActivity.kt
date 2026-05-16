package com.example.simpleschedule // 请修改为你真实的包名

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
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

// Glance 相关导入
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

// --- 0. DataStore 全局偏好设置 ---
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val SHOW_SAT = booleanPreferencesKey("show_sat")
    val SHOW_SUN = booleanPreferencesKey("show_sun")
    val SHOW_NOT_THIS_WEEK = booleanPreferencesKey("show_not_this_week")
    val CELL_HEIGHT = floatPreferencesKey("cell_height")
    val CORNER_RADIUS = floatPreferencesKey("corner_radius")
    val HIDE_TIME = booleanPreferencesKey("hide_time")
    val BOTTOM_BLANK = booleanPreferencesKey("bottom_blank")
    val VIBRATION = booleanPreferencesKey("vibration")

    val TOTAL_WEEKS = intPreferencesKey("total_weeks")
    val MATERIAL_YOU = booleanPreferencesKey("material_you")
    val WARN_TIMETABLE_ERROR = booleanPreferencesKey("warn_timetable_error")
    val AUTO_UPDATE = booleanPreferencesKey("auto_update")

    val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    val CURRENT_SCHEDULE_ID = stringPreferencesKey("current_schedule_id")

    // 课前提醒相关设置
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_VOICE_ENABLED = booleanPreferencesKey("reminder_voice_enabled")
    val REMINDER_NOTIFY_ENABLED = booleanPreferencesKey("reminder_notify_enabled")
    val REMINDER_ADVANCE_MINS = intPreferencesKey("reminder_advance_mins")

    // 小组件毛玻璃
    val WIDGET_TRANSLUCENT = booleanPreferencesKey("widget_translucent")
}

// --- 1. 数据层 (Data Layer: Room Entities) ---

@Entity(tableName = "schedule_groups")
data class ScheduleGroup(
    @PrimaryKey val id: String,
    val name: String,
    val timetableId: String = "default_timetable",
    val startDate: String = ""
)

@Entity(tableName = "timetable_groups")
data class TimetableGroup(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "time_nodes")
data class TimeNode(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timetableId: String,
    val nodeIndex: Int,
    val startTime: String,
    val endTime: String
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val name: String,
    val location: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startNode: Int,
    val endNode: Int,
    val weeks: String,
    val colorTheme: String
)

@Entity(tableName = "course_overrides")
data class CourseOverride(
    @PrimaryKey val courseId: String,
    val newDayOfWeek: Int,
    val newStartNode: Int,
    val newEndNode: Int
)

data class DisplayCourse(
    val course: Course,
    val displayDay: Int,
    val displayStartNode: Int,
    val displayEndNode: Int
)

@Dao
interface AppDao {
    @Query("SELECT * FROM schedule_groups")
    fun getAllScheduleGroups(): Flow<List<ScheduleGroup>>

    @Query("SELECT * FROM schedule_groups WHERE id = :id")
    fun getScheduleGroupFlow(id: String): Flow<ScheduleGroup?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleGroup(scheduleGroup: ScheduleGroup)

    @Update
    suspend fun updateScheduleGroup(scheduleGroup: ScheduleGroup)

    @Query("DELETE FROM schedule_groups WHERE id = :id")
    suspend fun deleteScheduleGroup(id: String)

    @Query("SELECT * FROM courses WHERE scheduleId = :scheduleId")
    fun getCoursesBySchedule(scheduleId: String): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCourses(courses: List<Course>)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: String)

    @Query("SELECT * FROM course_overrides")
    fun getAllOverrides(): Flow<List<CourseOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: CourseOverride)

    @Query("SELECT * FROM timetable_groups")
    fun getAllTimetableGroups(): Flow<List<TimetableGroup>>

    @Query("SELECT * FROM time_nodes WHERE timetableId = :tid ORDER BY nodeIndex ASC")
    fun getTimeNodesFlow(tid: String): Flow<List<TimeNode>>

    @Query("SELECT * FROM time_nodes WHERE timetableId = :tid ORDER BY nodeIndex ASC")
    suspend fun getTimeNodes(tid: String): List<TimeNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableGroup(group: TimetableGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeNodes(nodes: List<TimeNode>)

    @Query("DELETE FROM time_nodes WHERE timetableId = :tid")
    suspend fun deleteTimeNodes(tid: String)

    @Query("DELETE FROM timetable_groups WHERE id = :tid")
    suspend fun deleteTimetableGroup(tid: String)
}

@Database(entities = [ScheduleGroup::class, TimetableGroup::class, TimeNode::class, Course::class, CourseOverride::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "simpleschedule_db")
                .fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}

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

    // DataStore 绑定变量
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
            scheduleNextAlarm() // 重新计算闹钟
        }
    }
    fun updateSetting(key: Preferences.Key<Float>, value: Float) = viewModelScope.launch {
        getApplication<Application>().dataStore.edit { it[key] = value }
    }
    fun updateSetting(key: Preferences.Key<Int>, value: Int) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { it[key] = value }
            scheduleNextAlarm() // 重新计算闹钟
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
            // 在每次启动应用时也确保注册下一次小组件更新的闹钟
            ReminderEngine.scheduleNextWidgetUpdate(getApplication(), appDao)
        }
    }

    private suspend fun ensureBuiltInTimetablesExist() {
        val existingGroups = appDao.getAllTimetableGroups().firstOrNull() ?: emptyList()
        val existingIds = existingGroups.map { it.id }

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

        val defaultGroup = ScheduleGroup("default_id", "默认课表", "tt_cjlu", sdf.format(cal.time))
        appDao.insertScheduleGroup(defaultGroup)
        _currentScheduleId.value = defaultGroup.id
        _currentWeek.value = 9
        insertMockData()
    }

    fun notifyWidgetUpdate() {
        viewModelScope.launch {
            CourseWidget().updateAll(getApplication())
            ReminderEngine.scheduleNextWidgetUpdate(getApplication(), appDao)
        }
    }

    // 手动触发计算下一次课程闹钟
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

    fun addCustomCourse(name: String, location: String, teacher: String, day: Int, start: Int, end: Int, color: String) {
        viewModelScope.launch {
            appDao.insertCourse(Course(UUID.randomUUID().toString(), _currentScheduleId.value, name, location, teacher, day, start, end, "[8,9,10,11,12]", color))
            notifyWidgetUpdate()
            scheduleNextAlarm()
        }
    }

    fun updateCustomCourse(id: String, name: String, location: String, teacher: String, day: Int, start: Int, end: Int, color: String, weeks: String) {
        viewModelScope.launch {
            appDao.updateCourse(Course(id, _currentScheduleId.value, name, location, teacher, day, start, end, weeks, color))
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

    fun importFromJson(jsonString: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonString)
                val targetScheduleId = _currentScheduleId.value
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
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
                            weeks = obj.optString("weeks", "[8,9,10,11,12]"),
                            colorTheme = obj.optString("colorTheme", "blue")
                        )
                    )
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

    private suspend fun insertMockData() {
        val mockCourses = listOf(
            Course("1", "default_id", "高等数学A2", "翔宇楼206", "缪周倩", 1, 1, 2, "[8,9,10]", "blue"),
            Course("2", "default_id", "大学物理A1", "环宇楼A201", "尚曼玉", 3, 1, 2, "[8,9]", "pink"),
            Course("3", "default_id", "模拟电子线路", "环宇楼A505", "潘晨", 4, 1, 2, "[8,9,10]", "indigo"),
            Course("4", "default_id", "概率论与数理统计", "环宇楼A301", "朱文静", 5, 1, 2, "[8,9,10]", "purple"),
            Course("5", "default_id", "大学英语5", "环宇楼D505", "陆崔崔", 2, 3, 4, "[8,9]", "slate"),
            Course("6", "default_id", "物理实验A", "未排地点", "张海岛", 5, 6, 8, "[10]", "rose"),
            Course("7", "default_id", "习近平新时代中国特色社会主义思想概论", "环宇楼A504", "刘世吾", 1, 3, 5, "[8,9]", "rose"),
            Course("8", "default_id", "超长课程测试", "测试楼101", "系统", 2, 6, 6, "[8]", "purple")
        )
        appDao.insertAllCourses(mockCourses)
    }
}

// --- 辅助函数 ---
private fun addMinutes(timeStr: String, mins: Int): String {
    val parts = timeStr.split(":")
    if (parts.size != 2) return timeStr
    val total = (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) + mins
    return String.format(Locale.getDefault(), "%02d:%02d", (total / 60) % 24, total % 60)
}

private fun showTimePicker(context: Context, initialTime: String, onTimeSelected: (String) -> Unit) {
    val parts = initialTime.split(":")
    TimePickerDialog(
        context,
        { _, h, m -> onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", h, m)) },
        parts.getOrNull(0)?.toIntOrNull() ?: 8,
        parts.getOrNull(1)?.toIntOrNull() ?: 0,
        true
    ).show()
}

private fun showDatePicker(context: Context, initialDateStr: String, onDateSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
    try {
        if (initialDateStr.isNotEmpty()) {
            cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(initialDateStr) ?: Date()
        }
    } catch (e: Exception) {}

    DatePickerDialog(
        context,
        { _, y, m, d -> onDateSelected(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)) },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatDateForDisplay(dateStr: String): String {
    if (dateStr.isEmpty()) return "未设置"
    return try {
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: return dateStr
        SimpleDateFormat("yyyy-M-d EEEE", Locale.CHINESE).format(parsedDate)
    } catch (e: Exception) {
        dateStr
    }
}

// --- 3. UI 主题与调色板 (Theme & Palette) ---

val BgLight = Color(0xFFFAFAFA)
val BorderLight = Color(0xFFE4E4E7)
val TextLight = Color(0xFF09090B)
val BgDark = Color(0xFF09090B)
val BorderDark = Color(0xFF27272A)
val TextDark = Color(0xFFFAFAFA)

data class CoursePalette(val bg: Color, val border: Color, val text: Color, val accent: Color)

fun getPalette(theme: String, isDark: Boolean): CoursePalette {
    return when (theme) {
        "blue" -> if (isDark) CoursePalette(Color(0xFF151B23), Color(0xFF2A4365), Color(0xFF90CDF4), Color(0xFF3182CE)) else CoursePalette(Color(0xFFEEF2F6), Color(0xFFD0DBE7), Color(0xFF2A4365), Color(0xFF3182CE))
        "pink" -> if (isDark) CoursePalette(Color(0xFF381A2A), Color(0xFF831843), Color(0xFFFBCFE8), Color(0xFFBE185D)) else CoursePalette(Color(0xFFFDF2F8), Color(0xFFFBCFE8), Color(0xFF9D174D), Color(0xFFEC4899))
        "indigo" -> if (isDark) CoursePalette(Color(0xFF1E1E2D), Color(0xFF3730A3), Color(0xFFC7D2FE), Color(0xFF4F46E5)) else CoursePalette(Color(0xFFEEF2FF), Color(0xFFC7D2FE), Color(0xFF3730A3), Color(0xFF4F46E5))
        "rose" -> if (isDark) CoursePalette(Color(0xFF331B22), Color(0xFF881337), Color(0xFFFECDD3), Color(0xFFE11D48)) else CoursePalette(Color(0xFFFFF1F2), Color(0xFFFECDD3), Color(0xFF9F1239), Color(0xFFE11D48))
        "purple" -> if (isDark) CoursePalette(Color(0xFF1C1228), Color(0xFF581C87), Color(0xFFD8B4FE), Color(0xFF9333EA)) else CoursePalette(Color(0xFFFAF5FF), Color(0xFFE9D5FF), Color(0xFF581C87), Color(0xFF9333EA))
        "slate" -> if (isDark) CoursePalette(Color(0xFF0F1218), Color(0xFF334155), Color(0xFFCBD5E1), Color(0xFF64748B)) else CoursePalette(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFF334155), Color(0xFF64748B))
        // 保留原有的颜色以兼容已有数据
        "green" -> if (isDark) CoursePalette(Color(0xFF121C17), Color(0xFF166534), Color(0xFF86EFAC), Color(0xFF16A34A)) else CoursePalette(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF166534), Color(0xFF16A34A))
        "orange" -> if (isDark) CoursePalette(Color(0xFF241711), Color(0xFF9A3412), Color(0xFFFDBA74), Color(0xFFEA580C)) else CoursePalette(Color(0xFFFFF7ED), Color(0xFFFED7AA), Color(0xFF9A3412), Color(0xFFEA580C))
        "red" -> if (isDark) CoursePalette(Color(0xFF261414), Color(0xFF991B1B), Color(0xFFFCA5A5), Color(0xFFDC2626)) else CoursePalette(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFF991B1B), Color(0xFFDC2626))
        else -> if (isDark) CoursePalette(Color(0xFF151B23), Color(0xFF2A4365), Color(0xFF90CDF4), Color(0xFF3182CE)) else CoursePalette(Color(0xFFEEF2F6), Color(0xFFD0DBE7), Color(0xFF2A4365), Color(0xFF3182CE))
    }
}

// --- 4. 主入口与全局结构 ---

class MainActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // 开启边缘到边缘渲染，适配底部虚拟按键
        setContent {
            // Android 13+ 运行时通知权限申请
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                // 1. 请求通知权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                // 2. 主动检查精确闹钟权限，如果没有则弹出强警告（防断连关键）
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "注意：请前往设置允许应用的「精确闹钟」权限，否则桌面卡片将无法准时刷新喵！", Toast.LENGTH_LONG).show()
                }

                // 3. 注册 WorkManager 的兜底循环任务（每30分钟执行一次，防止被荣耀杀后台后彻底失联）
                val fallbackRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "WidgetFallback",
                    ExistingPeriodicWorkPolicy.KEEP,
                    fallbackRequest
                )
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDarkSetting by viewModel.isDarkTheme.collectAsState()
            val isDark = isDarkSetting ?: isSystemDark

            val currentWeek by viewModel.currentWeek.collectAsState()
            val displayCourses by viewModel.displayCourses.collectAsState()
            val scheduleGroups by viewModel.scheduleGroups.collectAsState()
            val currentScheduleId by viewModel.currentScheduleId.collectAsState()
            val activeTimeNodes by viewModel.activeTimeNodes.collectAsState()
            val timetableGroups by viewModel.timetableGroups.collectAsState()
            val totalWeeks by viewModel.totalWeeks.collectAsState()

            val animatedBgColor by animateColorAsState(if (isDark) BgDark else BgLight, tween(500), label = "bg")

            // 生命周期绑定：每次回到应用都会精准刷新周数
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshCurrentWeek()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            var currentRoute by remember { mutableStateOf("main") }
            var currentTab by remember { mutableIntStateOf(0) }
            var courseToEdit by remember { mutableStateOf<Course?>(null) }
            var editingTimetableId by remember { mutableStateOf<String?>(null) }
            var showAddDialog by remember { mutableStateOf(false) }
            var showShareCodeDialog by remember { mutableStateOf(false) }
            var showCreateScheduleDialog by remember { mutableStateOf(false) }

            Surface(modifier = Modifier.fillMaxSize(), color = animatedBgColor) {
                DotMatrixBackground(isDark = isDark)

                // 替换原有的 Box 为 AnimatedContent 实现全局路由动画
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        val springSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                        (fadeIn(animationSpec = springSpec) + scaleIn(initialScale = 0.95f, animationSpec = springSpec)) togetherWith fadeOut(animationSpec = springSpec)
                    },
                    label = "route_anim",
                    modifier = Modifier.fillMaxSize()
                ) { route ->
                    when (route) {
                        "course_management" -> {
                            CourseManagementScreen(
                                courses = displayCourses.map { it.course }.distinctBy { it.id },
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onEditCourse = { courseToEdit = it; showAddDialog = true },
                                onDeleteCourse = { viewModel.deleteCourse(it) }
                            )
                        }
                        "timetable_list" -> {
                            val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }
                            TimetableListScreen(
                                timetables = timetableGroups,
                                currentLinkedId = currentSchedule?.timetableId ?: "tt_cjlu",
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onSelect = { viewModel.linkTimetableToCurrentSchedule(it) },
                                onEdit = { id -> editingTimetableId = id; currentRoute = "timetable_edit" },
                                onDelete = { viewModel.deleteTimetable(it) }
                            )
                        }
                        "timetable_edit" -> {
                            TimetableEditScreen(
                                timetableId = editingTimetableId,
                                timetables = timetableGroups,
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "timetable_list" },
                                onSave = { id, name, nodes ->
                                    viewModel.saveTimetable(id, name, nodes)
                                    currentRoute = "timetable_list"
                                }
                            )
                        }
                        "schedule_settings" -> {
                            ScheduleSettingsScreen(
                                viewModel = viewModel,
                                scheduleGroups = scheduleGroups,
                                currentScheduleId = currentScheduleId,
                                currentWeek = currentWeek,
                                timeNodeCount = activeTimeNodes.size,
                                totalWeeks = totalWeeks,
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onRenameSchedule = { id, newName -> viewModel.renameSchedule(id, newName) },
                                onWeekChange = { viewModel.updateWeekAndReverseCalculateStartDate(currentScheduleId, it) },
                                onStartDateChange = { viewModel.updateScheduleStartDate(currentScheduleId, it) },
                                onTotalWeeksChange = { viewModel.updateSetting(SettingsKeys.TOTAL_WEEKS, it) },
                                onManageTimetableClick = { currentRoute = "timetable_list" },
                                onManageCoursesClick = { currentRoute = "course_management" },
                                onMoreAppearanceClick = { currentRoute = "appearance_settings" }
                            )
                        }
                        "appearance_settings" -> {
                            AppearanceSettingsScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "schedule_settings" }
                            )
                        }
                        "global_settings" -> {
                            GlobalSettingsScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onAdjustCourseClick = { currentRoute = "adjust_course" }
                            )
                        }
                        "adjust_course" -> {
                            AdjustCourseScreen(isDark = isDark, onBack = { currentRoute = "global_settings" })
                        }
                        "webview_import" -> {
                            WebViewImportScreen(
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onImport = { json ->
                                    viewModel.importFromJson(json) { success ->
                                        Toast.makeText(context, if (success) "导入成功" else "解析失败", Toast.LENGTH_SHORT).show()
                                        if (success) currentRoute = "main"
                                    }
                                }
                            )
                        }
                        "reminder_settings" -> {
                            ReminderSettingsScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "main" }
                            )
                        }
                        else -> {
                            Scaffold(
                                containerColor = Color.Transparent,
                                bottomBar = { BottomNavBar(isDark = isDark, currentTab = currentTab, onTabSelected = { currentTab = it }) }
                            ) { paddingValues ->
                                Crossfade(
                                    targetState = currentTab,
                                    label = "tab_anim",
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) { tab ->
                                    Box(modifier = Modifier.padding(paddingValues)) {
                                        if (tab == 0) {
                                            TimetableScreen(
                                                viewModel = viewModel,
                                                courses = displayCourses,
                                                timeNodes = activeTimeNodes,
                                                currentWeek = currentWeek,
                                                totalWeeks = totalWeeks,
                                                scheduleGroups = scheduleGroups,
                                                currentScheduleId = currentScheduleId,
                                                isDark = isDark,
                                                onAddClick = { courseToEdit = null; showAddDialog = true },
                                                onManageCoursesClick = { currentRoute = "course_management" },
                                                onManageTimetablesClick = { currentRoute = "timetable_list" },
                                                onScheduleSettingsClick = { currentRoute = "schedule_settings" },
                                                onGlobalSettingsClick = { currentRoute = "global_settings" },
                                                onEditCourse = { courseToEdit = it; showAddDialog = true },
                                                onWebViewImportClick = { currentRoute = "webview_import" },
                                                onShareCodeClick = { showShareCodeDialog = true },
                                                onCreateScheduleClick = { showCreateScheduleDialog = true },
                                                onReminderSettingsClick = { currentRoute = "reminder_settings" }
                                            )
                                        } else {
                                            ProfileScreen(isDark = isDark, onThemeToggle = { viewModel.toggleTheme(it) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddDialog) {
                    CourseEditDialog(
                        isDark = isDark,
                        initialCourse = courseToEdit,
                        onDismiss = { showAddDialog = false; courseToEdit = null },
                        onConfirm = { id, name, loc, t, d, s, e, c, w ->
                            if (id == null) {
                                viewModel.addCustomCourse(name, loc, t, d, s, e, c)
                            } else {
                                viewModel.updateCustomCourse(id, name, loc, t, d, s, e, c, w)
                            }
                            showAddDialog = false
                            courseToEdit = null
                        }
                    )
                }

                if (showShareCodeDialog) {
                    ShareCodeDialog(
                        isDark = isDark,
                        onDismiss = { showShareCodeDialog = false },
                        onConfirm = { code ->
                            viewModel.importFromShareCode(code) { success ->
                                Toast.makeText(context, if (success) "口令解析成功" else "无效口令", Toast.LENGTH_SHORT).show()
                                if(success) showShareCodeDialog = false
                            }
                        }
                    )
                }

                if (showCreateScheduleDialog) {
                    CreateScheduleDialog(
                        isDark = isDark,
                        onDismiss = { showCreateScheduleDialog = false },
                        onConfirm = { name ->
                            viewModel.createNewSchedule(name)
                            showCreateScheduleDialog = false
                        }
                    )
                }
            }
        }
    }
}

// --- 组件部分 ---

@Composable
fun DotMatrixBackground(isDark: Boolean) {
    val dotColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 16.dp.toPx()
        val dotRadius = 1.dp.toPx()
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
fun BottomNavBar(isDark: Boolean, currentTab: Int, onTabSelected: (Int) -> Unit) {
    val borderColor = if (isDark) BorderDark else BorderLight
    val activeColor = if (isDark) Color.White else Color.Black
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) BgDark.copy(alpha = 0.9f) else BgLight.copy(alpha = 0.9f))
            .border(0.5.dp, borderColor)
            .navigationBarsPadding() // 处理底部的虚拟按键避让
            .padding(vertical = 12.dp, horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        NavBarItem(icon = Icons.Rounded.DateRange, label = "TIMETABLE", isActive = currentTab == 0, color = if (currentTab == 0) activeColor else inactiveColor, onClick = { onTabSelected(0) })
        NavBarItem(icon = Icons.Rounded.Person, label = "PROFILE", isActive = currentTab == 1, color = if (currentTab == 1) activeColor else inactiveColor, onClick = { onTabSelected(1) })
    }
}

@Composable
fun NavBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = color, letterSpacing = 1.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: ScheduleViewModel, courses: List<DisplayCourse>, timeNodes: List<TimeNode>, currentWeek: Int, totalWeeks: Int, scheduleGroups: List<ScheduleGroup>, currentScheduleId: String, isDark: Boolean,
    onAddClick: () -> Unit, onManageCoursesClick: () -> Unit, onManageTimetablesClick: () -> Unit, onScheduleSettingsClick: () -> Unit, onGlobalSettingsClick: () -> Unit,
    onEditCourse: (Course) -> Unit, onWebViewImportClick: () -> Unit, onShareCodeClick: () -> Unit, onCreateScheduleClick: () -> Unit, onReminderSettingsClick: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedCourseWithWeek by remember { mutableStateOf<Pair<DisplayCourse, Int>?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showDeleteScheduleDialog by remember { mutableStateOf(false) }
    var showImportBlockDialog by remember { mutableStateOf(false) }

    val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }
    val currentScheduleName = currentSchedule?.name ?: "课表"
    val startDate = currentSchedule?.startDate ?: ""
    val pagerState = rememberPagerState(initialPage = (currentWeek - 1).coerceAtLeast(0), pageCount = { totalWeeks.coerceAtLeast(1) })

    val showNotThisWeek by viewModel.showNotThisWeek.collectAsState()
    val bottomBlank by viewModel.bottomBlank.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Column(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { showMoreSheet = true })) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentScheduleName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Menu", tint = textColor)
                    }
                    Text("第 ${pagerState.currentPage + 1} 周", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box {
                    Icon(Icons.Rounded.Add, contentDescription = "Add", tint = textColor, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showAddMenu = true })
                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }, modifier = Modifier.background(if (isDark) Color(0xFF18181B) else Color.White).border(0.5.dp, borderColor)) {
                        DropdownMenuItem(text = { Text("手动添加课程", fontWeight = FontWeight.Bold, color = textColor) }, leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = textColor) }, onClick = { showAddMenu = false; onAddClick() })
                        Divider(color = borderColor, thickness = 0.5.dp)
                        DropdownMenuItem(text = { Text("从教务导入", fontWeight = FontWeight.Bold, color = textColor) }, leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null, tint = textColor) }, onClick = {
                            showAddMenu = false
                            if (courses.isNotEmpty()) {
                                showImportBlockDialog = true
                            } else {
                                onWebViewImportClick()
                            }
                        })
                        DropdownMenuItem(text = { Text("分享口令导入", fontWeight = FontWeight.Bold, color = textColor) }, leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = textColor) }, onClick = { showAddMenu = false; onShareCodeClick() })
                    }
                }
                Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = textColor, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showMoreSheet = true })
            }
        }

        LaunchedEffect(currentWeek) {
            val safePage = (currentWeek - 1).coerceIn(0, totalWeeks - 1)
            if (pagerState.currentPage != safePage) pagerState.animateScrollToPage(safePage)
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val weekForThisPage = page + 1
            val coursesForThisPage = remember(courses, weekForThisPage, showNotThisWeek) {
                val occupiedSlots = mutableSetOf<String>()

                val currentCourses = courses.filter { dc ->
                    val weeksList = dc.course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }
                    weeksList.contains(weekForThisPage)
                }.map { dc ->
                    for (node in dc.displayStartNode..dc.displayEndNode) {
                        occupiedSlots.add("${dc.displayDay}_$node")
                    }
                    Pair(dc, false)
                }

                val futureCourses = mutableListOf<Pair<DisplayCourse, Boolean>>()
                if (showNotThisWeek) {
                    val potentialFutures = courses.mapNotNull { dc ->
                        val weeksList = dc.course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }
                        if (!weeksList.contains(weekForThisPage)) {
                            val futureWeeks = weeksList.filter { it > weekForThisPage }.sorted()
                            if (futureWeeks.isNotEmpty() && (futureWeeks.first() - weekForThisPage <= 3)) {
                                return@mapNotNull Pair(dc, futureWeeks.first())
                            }
                        }
                        null
                    }.sortedBy { it.second }

                    for ((dc, _) in potentialFutures) {
                        var hasConflict = false
                        for (node in dc.displayStartNode..dc.displayEndNode) {
                            if (occupiedSlots.contains("${dc.displayDay}_$node")) {
                                hasConflict = true
                                break
                            }
                        }
                        if (!hasConflict) {
                            for (node in dc.displayStartNode..dc.displayEndNode) {
                                occupiedSlots.add("${dc.displayDay}_$node")
                            }
                            futureCourses.add(Pair(dc, true))
                        }
                    }
                }
                currentCourses + futureCourses
            }
            TimetableGrid(
                viewModel = viewModel,
                coursesForThisPage = coursesForThisPage,
                timeNodes = timeNodes,
                isDark = isDark,
                bottomBlank = bottomBlank,
                startDate = startDate,
                displayedWeek = weekForThisPage,
                onCourseClick = { course, isFuture -> selectedCourseWithWeek = Pair(course, if (isFuture) 0 else weekForThisPage) }
            )
        }
    }

    if (showImportBlockDialog) {
        AlertDialog(
            onDismissRequest = { showImportBlockDialog = false },
            title = { Text("无法导入", fontWeight = FontWeight.Bold) },
            text = { Text("当前课表中已经有数据啦！\n为了避免数据混乱叠加在一起，请先去【新建一个空白的课表】，然后再进行教务导入哦喵！") },
            confirmButton = {
                TextButton(onClick = { showImportBlockDialog = false }) { Text("知道啦", color = textColor, fontWeight = FontWeight.Bold) }
            },
            containerColor = if (isDark) Color(0xFF18181B) else Color.White,
            titleContentColor = textColor,
            textContentColor = textColor.copy(alpha = 0.8f)
        )
    }

    if (selectedCourseWithWeek != null) {
        val (course, clickedWeek) = selectedCourseWithWeek!!
        CourseDetailSheet(
            displayCourse = course,
            clickedWeek = clickedWeek,
            isDark = isDark,
            onDismiss = { selectedCourseWithWeek = null },
            onDelete = { viewModel.deleteCourse(course.course.id); selectedCourseWithWeek = null },
            onEdit = { onEditCourse(course.course); selectedCourseWithWeek = null }
        )
    }

    if (showDeleteScheduleDialog) {
        DeleteScheduleDialog(
            isDark = isDark,
            scheduleName = currentScheduleName,
            onDismiss = { showDeleteScheduleDialog = false },
            onConfirm = {
                if (scheduleGroups.size <= 1) {
                    Toast.makeText(context, "必须至少保留一个课表喵！", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.deleteSchedule(currentScheduleId)
                }
                showDeleteScheduleDialog = false
            }
        )
    }

    if (showMoreSheet) {
        MoreMenuBottomSheet(
            isDark = isDark,
            currentPagerWeek = pagerState.currentPage + 1,
            totalWeeks = totalWeeks,
            scheduleGroups = scheduleGroups,
            currentScheduleId = currentScheduleId,
            onBrowseWeek = { targetWeek -> coroutineScope.launch { pagerState.animateScrollToPage(targetWeek - 1) } },
            onSwitchSchedule = { viewModel.switchSchedule(it); showMoreSheet = false },
            onCreateScheduleClick = { showMoreSheet = false; onCreateScheduleClick() },
            onDismiss = { showMoreSheet = false },
            onManageCoursesClick = onManageCoursesClick,
            onTimetableManageClick = onManageTimetablesClick,
            onScheduleSettingsClick = onScheduleSettingsClick,
            onGlobalSettingsClick = onGlobalSettingsClick,
            onReminderSettingsClick = onReminderSettingsClick,
            onDeleteScheduleClick = { showDeleteScheduleDialog = true }
        )
    }
}

@Composable
fun TimetableGrid(
    viewModel: ScheduleViewModel,
    coursesForThisPage: List<Pair<DisplayCourse, Boolean>>,
    timeNodes: List<TimeNode>,
    isDark: Boolean,
    bottomBlank: Boolean,
    startDate: String,
    displayedWeek: Int,
    onCourseClick: (DisplayCourse, Boolean) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val showSat by viewModel.showSat.collectAsState()
    val showSun by viewModel.showSun.collectAsState()
    val cellHeightDp by viewModel.cellHeight.collectAsState()
    val cornerRadiusDp by viewModel.cornerRadius.collectAsState()
    val hideTime by viewModel.hideTime.collectAsState()
    val vibration by viewModel.vibration.collectAsState()

    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val visualColMap = mutableMapOf<Int, Int>()
    var colIdx = 0
    for (day in 1..7) {
        if ((day == 6 && !showSat) || (day == 7 && !showSun)) continue
        visualColMap[day] = colIdx++
    }
    val daysCount = colIdx.coerceAtLeast(1)

    val timeColWidthDp = 45.dp
    val maxRow = timeNodes.size.coerceAtLeast(1)
    val scrollState = rememberScrollState()

    val calendar = Calendar.getInstance()
    var currentMonth = 1
    val dateList = mutableListOf<Int>()
    try {
        if (startDate.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            calendar.time = sdf.parse(startDate) ?: Date()

            // 先重置回这一周的周一
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            // 加上正确的偏离周次（基于天数加减防止夏令时周越界错误喵）
            calendar.add(Calendar.DAY_OF_YEAR, (displayedWeek - 1) * 7)

            currentMonth = calendar.get(Calendar.MONTH) + 1
            for (i in 0..6) {
                dateList.add(calendar.get(Calendar.DAY_OF_MONTH))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            for (i in 0..6) { dateList.add(1) }
        }
    } catch (e: Exception) {
        for (i in 0..6) { dateList.add(1) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(timeColWidthDp).padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("$currentMonth\n月", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = textColor.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }

            for (day in 1..7) {
                if (visualColMap.containsKey(day)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(allDays[day-1], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(4.dp))
                        val displayDate = if (dateList.size == 7) dateList[day-1].toString() else "-"
                        Text(displayDate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.8f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalHeight = cellHeightDp.dp * maxRow + if (bottomBlank) 150.dp else 0.dp

        Box(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(totalHeight).padding(end = 12.dp)) {
                val colWidthDp = (maxWidth - timeColWidthDp) / daysCount
                val cellHeightPx = with(LocalDensity.current) { cellHeightDp.dp.toPx() }
                val colWidthPx = with(LocalDensity.current) { colWidthDp.toPx() }

                for (i in 1..maxRow) {
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).offset(y = cellHeightDp.dp * i).background(borderColor))
                }
                for (i in 1..daysCount) {
                    Box(modifier = Modifier.fillMaxHeight().width(0.5.dp).offset(x = timeColWidthDp + colWidthDp * i).background(borderColor))
                }

                timeNodes.forEachIndexed { index, node ->
                    Column(
                        modifier = Modifier.width(timeColWidthDp).height(cellHeightDp.dp).offset(y = cellHeightDp.dp * index).padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${node.nodeIndex}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f))
                        if (!hideTime) {
                            Text("${node.startTime}\n${node.endTime}", fontSize = 8.sp, color = textColor.copy(alpha = 0.4f), textAlign = TextAlign.Center, lineHeight = 10.sp)
                        }
                    }
                }

                coursesForThisPage.forEach { (displayCourse, isFuture) ->
                    val mappedCol = visualColMap[displayCourse.displayDay]
                    if (mappedCol != null) {
                        val course = displayCourse.course
                        val palette = getPalette(course.colorTheme, isDark)
                        var dragOffset by remember { mutableStateOf(Offset.Zero) }
                        var isDragging by remember { mutableStateOf(false) }

                        val baseOffsetX = timeColWidthDp + colWidthDp * mappedCol
                        val baseOffsetY = cellHeightDp.dp * (displayCourse.displayStartNode - 1)
                        val cardHeight = cellHeightDp.dp * (displayCourse.displayEndNode - displayCourse.displayStartNode + 1)

                        // 课程卡片入场动画 (弹性缩放+透明度)
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(displayCourse.course.id) { isVisible = true }
                        val alphaAnim by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(400), label = "alpha")
                        val scaleAnim by animateFloatAsState(targetValue = if (isVisible) 1f else 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

                        Box(
                            modifier = Modifier
                                .offset {
                                    if (isDragging) {
                                        IntOffset((baseOffsetX.toPx() + dragOffset.x).roundToInt(), (baseOffsetY.toPx() + dragOffset.y).roundToInt())
                                    } else {
                                        IntOffset(baseOffsetX.toPx().roundToInt(), baseOffsetY.toPx().roundToInt())
                                    }
                                }
                                .size(width = colWidthDp, height = cardHeight)
                                .graphicsLayer {
                                    alpha = alphaAnim
                                    scaleX = scaleAnim
                                    scaleY = scaleAnim
                                }
                                .padding(2.5.dp)
                                .let { if (isDragging) it.shadow(12.dp, RoundedCornerShape(cornerRadiusDp.dp)) else it }
                                .pointerInput(course.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            isDragging = true
                                            if (vibration && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            val deltaCols = (dragOffset.x / colWidthPx).roundToInt()
                                            val deltaRows = (dragOffset.y / cellHeightPx).roundToInt()
                                            if (deltaCols != 0 || deltaRows != 0) {
                                                val targetCol = (mappedCol + deltaCols).coerceIn(0, daysCount - 1)
                                                val newDay = visualColMap.entries.find { it.value == targetCol }?.key ?: course.dayOfWeek
                                                viewModel.updateCoursePosition(course.id, newDay - course.dayOfWeek, deltaRows, course)
                                            }
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                                .clickable { if (!isDragging) onCourseClick(displayCourse, isFuture) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(cornerRadiusDp.dp))
                                    .background(if (isFuture) Color.Transparent else palette.bg)
                                    .border(
                                        width = if (isFuture) 1.dp else 0.5.dp,
                                        color = palette.border,
                                        shape = RoundedCornerShape(cornerRadiusDp.dp)
                                    )
                            ) {
                                if (!isFuture) {
                                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(palette.accent))
                                }
                                Column(modifier = Modifier.fillMaxSize().padding(start = 6.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)) {
                                    val weeksList = course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }
                                    if (isFuture) {
                                        val isOddOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 != 0 } && weeksList.size > 1
                                        val isEvenOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 == 0 } && weeksList.size > 1
                                        val badgeText = if (isOddOnly) "[非本周(单)]" else if (isEvenOnly) "[非本周(双)]" else "[非本周]"
                                        Text(badgeText, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 2.dp))
                                    }
                                    // 课程名获取剩余的弹性空间（但不会强制占满把 Row 挤掉）
                                    Text(
                                        text = course.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFuture) textColor.copy(alpha = 0.5f) else palette.text,
                                        lineHeight = 14.sp,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    // 限制高度最大为卡片的高度的 50%，如果内容很长最多显示4行，不会再因为 55dp 绝对判断导致消失了喵
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp).heightIn(max = cardHeight * 0.5f)
                                    ) {
                                        Icon(Icons.Rounded.LocationOn, contentDescription = "Loc", tint = if (isFuture) textColor.copy(alpha = 0.4f) else palette.text.copy(alpha = 0.7f), modifier = Modifier.size(10.dp).padding(top = 1.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = course.location.replace("楼", ""),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isFuture) textColor.copy(alpha = 0.4f) else palette.text.copy(alpha = 0.7f),
                                            lineHeight = 11.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleSettingsScreen(
    viewModel: ScheduleViewModel, scheduleGroups: List<ScheduleGroup>, currentScheduleId: String, currentWeek: Int, timeNodeCount: Int, totalWeeks: Int, isDark: Boolean,
    onBack: () -> Unit, onRenameSchedule: (String, String) -> Unit, onWeekChange: (Int) -> Unit, onStartDateChange: (String) -> Unit, onTotalWeeksChange: (Int) -> Unit,
    onManageTimetableClick: () -> Unit, onManageCoursesClick: () -> Unit, onMoreAppearanceClick: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val context = LocalContext.current

    val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }
    var scheduleName by remember(currentScheduleId) { mutableStateOf(currentSchedule?.name ?: "") }
    var showChangeWeekDialog by remember { mutableStateOf(false) }
    var showTotalWeeksDialog by remember { mutableStateOf(false) }

    val showSat by viewModel.showSat.collectAsState()
    val showSun by viewModel.showSun.collectAsState()
    val showNotThisWeek by viewModel.showNotThisWeek.collectAsState()
    val cellHeightDp by viewModel.cellHeight.collectAsState()
    val cornerRadiusDp by viewModel.cornerRadius.collectAsState()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("课表设置", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("课表名称", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        OutlinedTextField(
                            value = scheduleName,
                            onValueChange = { scheduleName = it; onRenameSchedule(currentScheduleId, it) },
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f)),
                            singleLine = true,
                            modifier = Modifier.width(150.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor.copy(alpha = 0.6f),
                                cursorColor = textColor
                            )
                        )
                    }
                }
            }
            item {
                Text("课表数据", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingValueItem(title = "上课时间", value = "点击此处更改", textColor = textColor, borderColor = borderColor, onClick = onManageTimetableClick)
                        SettingValueItem(title = "第一周的第一天", value = formatDateForDisplay(currentSchedule?.startDate ?: ""), textColor = textColor, borderColor = borderColor, onClick = {
                            showDatePicker(context, currentSchedule?.startDate ?: "") { newDate ->
                                onStartDateChange(newDate)
                            }
                        })
                        SettingValueItem(title = "当前周", value = "第 $currentWeek 周", textColor = textColor, borderColor = borderColor, onClick = { showChangeWeekDialog = true })
                        SettingValueItem(title = "一天课程节数", value = "$timeNodeCount 节", textColor = textColor, borderColor = borderColor)
                        SettingValueItem(title = "学期周数", value = "$totalWeeks 周", textColor = textColor, borderColor = borderColor, onClick = { showTotalWeeksDialog = true })
                        SettingValueItem(title = "管理已添加课程", value = "", showBottomBorder = false, textColor = textColor, borderColor = borderColor, onClick = onManageCoursesClick)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            item {
                Text("课表外观", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingCheckboxItem(title = "显示周六", checked = showSat, onCheckedChange = { viewModel.updateSetting(SettingsKeys.SHOW_SAT, it) }, textColor = textColor, borderColor = borderColor, isDark = isDark)
                        SettingCheckboxItem(title = "显示周日", checked = showSun, onCheckedChange = { viewModel.updateSetting(SettingsKeys.SHOW_SUN, it) }, textColor = textColor, borderColor = borderColor, isDark = isDark)
                        SettingCheckboxItem(title = "显示非本周课程", checked = showNotThisWeek, onCheckedChange = { viewModel.updateSetting(SettingsKeys.SHOW_NOT_THIS_WEEK, it) }, textColor = textColor, borderColor = borderColor, isDark = isDark)
                        SettingValueItem(title = "课程格子高度", value = "${cellHeightDp.roundToInt()} dp", textColor = textColor, borderColor = borderColor)
                        SettingValueItem(title = "格子圆角半径", value = "${cornerRadiusDp.roundToInt()} dp", textColor = textColor, borderColor = borderColor)
                        Row(modifier = Modifier.fillMaxWidth().clickable { onMoreAppearanceClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("更多外观设置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("背景、文字颜色和大小、不透明度等", fontSize = 10.sp, color = textColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().clickable {
                    Toast.makeText(context, "已设为默认配置", Toast.LENGTH_SHORT).show()
                }.background(if (isDark) Color(0xFF1C1228) else Color(0xFFFAF5FF), RoundedCornerShape(12.dp)).border(0.5.dp, if (isDark) Color(0xFF581C87) else Color(0xFFD8B4FE), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("将此课表配置用作默认配置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF581C87))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("应用此项后，新建的课表会使用此课表中除了上课时间、课表名称、开学日期以外的配置，后续对该课表的修改并不会同步到新的课表中。", fontSize = 10.sp, color = if (isDark) Color(0xFFD8B4FE).copy(alpha = 0.7f) else Color(0xFF581C87).copy(alpha = 0.7f), lineHeight = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }

    if (showChangeWeekDialog) {
        ChangeWeekDialog(isDark = isDark, title = "设置当前周", currentValue = currentWeek, maxValue = totalWeeks, onDismiss = { showChangeWeekDialog = false }, onConfirm = { onWeekChange(it); showChangeWeekDialog = false })
    }
    if (showTotalWeeksDialog) {
        ChangeWeekDialog(isDark = isDark, title = "设置学期周数", currentValue = totalWeeks, maxValue = 30, onDismiss = { showTotalWeeksDialog = false }, onConfirm = { onTotalWeeksChange(it); showTotalWeeksDialog = false })
    }
}

@Composable
fun AppearanceSettingsScreen(viewModel: ScheduleViewModel, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)

    val hideTime by viewModel.hideTime.collectAsState()
    val cellHeightDp by viewModel.cellHeight.collectAsState()
    val cornerRadiusDp by viewModel.cornerRadius.collectAsState()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("更多外观设置", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingValueItem(title = "纯色背景颜色", value = "默认偏好", textColor = textColor, borderColor = borderColor)
                        SettingCheckboxItem(title = "隐藏格子内的时间显示", checked = hideTime, onCheckedChange = { viewModel.updateSetting(SettingsKeys.HIDE_TIME, it) }, textColor = textColor, borderColor = borderColor, isDark = isDark)

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("课程格子高度 (${cellHeightDp.roundToInt()}dp)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Slider(
                                value = cellHeightDp,
                                onValueChange = { viewModel.updateSetting(SettingsKeys.CELL_HEIGHT, it) },
                                valueRange = 40f..100f,
                                colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f))
                            )
                        }
                        Divider(color = borderColor, thickness = 0.5.dp)
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("格子圆角半径 (${cornerRadiusDp.roundToInt()}dp)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Slider(
                                value = cornerRadiusDp,
                                onValueChange = { viewModel.updateSetting(SettingsKeys.CORNER_RADIUS, it) },
                                valueRange = 0f..24f,
                                colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
fun ReminderSettingsScreen(viewModel: ScheduleViewModel, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val context = LocalContext.current

    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderNotifyEnabled by viewModel.reminderNotifyEnabled.collectAsState()
    val reminderVoiceEnabled by viewModel.reminderVoiceEnabled.collectAsState()
    val reminderAdvanceMins by viewModel.reminderAdvanceMins.collectAsState()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("课前提醒", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingCheckboxItemWithSubtext(
                            title = "启用上课提醒",
                            subtext = "在系统后台自动计算下一节课的时间，并在上课前通过闹钟准时触发提醒。需确保已授予后台允许及精确闹钟权限。",
                            checked = reminderEnabled,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.REMINDER_ENABLED, it) },
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark,
                            showBottomBorder = reminderEnabled
                        )

                        if (reminderEnabled) {
                            Column(modifier = Modifier.padding(16.dp).background(if(isDark) Color(0xFF27272A) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp)).padding(16.dp)) {
                                Text("提前提醒时间: $reminderAdvanceMins 分钟", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Slider(
                                    value = reminderAdvanceMins.toFloat(),
                                    onValueChange = { viewModel.updateSetting(SettingsKeys.REMINDER_ADVANCE_MINS, it.roundToInt()) },
                                    valueRange = 1f..30f,
                                    steps = 28,
                                    colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "提示：如果遇到上一门课上完了而接下来还有课，且课间休息短于您设置的提前时间，系统将在上节课刚下课时立刻为您播报。",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    lineHeight = 14.sp
                                )
                            }

                            SettingCheckboxItem(
                                title = "显示漂亮的通知栏卡片",
                                checked = reminderNotifyEnabled,
                                onCheckedChange = { viewModel.updateSetting(SettingsKeys.REMINDER_NOTIFY_ENABLED, it) },
                                textColor = textColor,
                                borderColor = borderColor,
                                isDark = isDark,
                                showBottomBorder = true
                            )

                            SettingCheckboxItem(
                                title = "开启语音播报",
                                checked = reminderVoiceEnabled,
                                onCheckedChange = { viewModel.updateSetting(SettingsKeys.REMINDER_VOICE_ENABLED, it) },
                                textColor = textColor,
                                borderColor = borderColor,
                                isDark = isDark,
                                showBottomBorder = true
                            )

                            SettingValueItem(title = "发送一条测试提醒", value = "10秒后触发", showBottomBorder = false, textColor = textColor, borderColor = borderColor, onClick = {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    Toast.makeText(context, "请先允许精确闹钟权限喵", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 注册一个供测试的假动作
                                    val triggerTime = System.currentTimeMillis() + 10000
                                    val classStartTime = triggerTime + reminderAdvanceMins * 60000
                                    ReminderEngine.scheduleAlarmByParams(context, "【测试】习近平新时代中国特色...", "环宇楼A504", "09:55-12:20", triggerTime, classStartTime, reminderNotifyEnabled, reminderVoiceEnabled)
                                    Toast.makeText(context, "已设置 10 秒后的测试提醒喵！请切出应用等待", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
fun GlobalSettingsScreen(viewModel: ScheduleViewModel, isDark: Boolean, onBack: () -> Unit, onAdjustCourseClick: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val context = LocalContext.current

    val bottomBlank by viewModel.bottomBlank.collectAsState()
    val vibration by viewModel.vibration.collectAsState()
    val materialYou by viewModel.materialYou.collectAsState()
    val warnTimetableError by viewModel.warnTimetableError.collectAsState()
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val widgetTranslucent by viewModel.widgetTranslucent.collectAsState()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("全局设置", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingItemWithSubtext(title = "将课表添加到桌面", subtext = "有日视图和周视图可选哦，能否添加成功取决于系统，如果添加不了可以去桌面手动添加。添加成功后，可以左右滑动桌面看看系统把课表放到哪一页了", textColor = textColor, borderColor = borderColor)
                        SettingCheckboxItemWithSubtext(
                            title = "桌面小部件半透明/毛玻璃化",
                            subtext = "开启后小部件背景将变为半透明（具体的毛玻璃模糊效果取决于系统桌面启动器支持）",
                            checked = widgetTranslucent,
                            onCheckedChange = {
                                viewModel.updateSetting(SettingsKeys.WIDGET_TRANSLUCENT, it)
                                viewModel.notifyWidgetUpdate()
                            },
                            textColor = textColor, borderColor = borderColor, isDark = isDark
                        )
                        SettingItemWithSubtext(title = "自定义空视图图片", subtext = "这个是空视图图片！就是没有课的时候显示的图片！目前仅在日视图小组件和周视图小组件生效。长按可以关闭~", textColor = textColor, borderColor = borderColor)
                        SettingCheckboxItem(title = "根据桌面壁纸更改主题色", checked = materialYou, onCheckedChange = { viewModel.updateSetting(SettingsKeys.MATERIAL_YOU, it) }, textColor = textColor, borderColor = borderColor, isDark = isDark)
                        SettingCheckboxItemWithSubtext(title = "课表下方增加留白区域", subtext = "开启后，课表下方会多出一段空白区域，便于将底部的课程滑动至屏幕中间查看", checked = bottomBlank, onCheckedChange = { viewModel.updateSetting(SettingsKeys.BOTTOM_BLANK, it) }, showBottomBorder = false, textColor = textColor, borderColor = borderColor, isDark = isDark)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            item {
                Text("课程设置", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingValueItem(title = "设置当前课表", value = "", textColor = textColor, borderColor = borderColor, onClick = { /* Navigate to schedule selection if needed */ })
                        SettingItemWithSubtext(title = "日期之间调课", subtext = "将某天的课移动到另一天", showBottomBorder = false, textColor = textColor, borderColor = borderColor, onClick = onAdjustCourseClick)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            item {
                Text("其他", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingItemWithSubtext(title = "允许后台运行和自启", subtext = "点击后，一般在「耗电管理」或者跟电池相关的选项中进行设置", textColor = textColor, borderColor = borderColor, onClick = {
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show()
                            }
                        })
                        SettingItemWithSubtext(title = "忽略电池优化", subtext = "忽略后，桌面小部件的刷新也许会更稳定一些哦，课前提醒也能更准时", textColor = textColor, borderColor = borderColor, onClick = {
                            try {
                                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                        Toast.makeText(context, "已经忽略电池优化啦", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = android.net.Uri.parse("package:${context.packageName}") }
                                        context.startActivity(intent)
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法请求电池优化权限", Toast.LENGTH_SHORT).show()
                            }
                        })
                        SettingCheckboxItem(title = "自动检查更新", checked = autoUpdate, onCheckedChange = { viewModel.updateSetting(SettingsKeys.AUTO_UPDATE, it) }, textColor = textColor, borderColor = borderColor, isDark = isDark)
                        SettingValueItem(title = "清除内置浏览器缓存", value = "", textColor = textColor, borderColor = borderColor, onClick = { Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show() })
                        SettingCheckboxItemWithSubtext(title = "振动反馈", subtext = "点击、滑动等操作时有振动反馈", checked = vibration, onCheckedChange = { viewModel.updateSetting(SettingsKeys.VIBRATION, it) }, showBottomBorder = false, textColor = textColor, borderColor = borderColor, isDark = isDark)
                    }
                }
                Spacer(modifier = Modifier.height(100.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
fun SettingValueItem(title: String, value: String, showBottomBorder: Boolean = true, textColor: Color, borderColor: Color, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
        if (value.isNotEmpty()) {
            Text(value, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
        } else {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.4f))
        }
    }
    if (showBottomBorder) Divider(color = borderColor, thickness = 0.5.dp)
}

@Composable
fun SettingCheckboxItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, showBottomBorder: Boolean = true, textColor: Color, borderColor: Color, isDark: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = textColor, uncheckedColor = textColor.copy(alpha = 0.4f), checkmarkColor = if (isDark) BgDark else BgLight))
    }
    if (showBottomBorder) Divider(color = borderColor, thickness = 0.5.dp)
}

@Composable
fun SettingItemWithSubtext(title: String, subtext: String, showBottomBorder: Boolean = true, textColor: Color, borderColor: Color, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtext, fontSize = 10.sp, color = textColor.copy(alpha = 0.5f), lineHeight = 14.sp)
        }
    }
    if (showBottomBorder) Divider(color = borderColor, thickness = 0.5.dp)
}

@Composable
fun SettingCheckboxItemWithSubtext(title: String, subtext: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, showBottomBorder: Boolean = true, textColor: Color, borderColor: Color, isDark: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtext, fontSize = 10.sp, color = textColor.copy(alpha = 0.5f), lineHeight = 14.sp)
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = textColor, uncheckedColor = textColor.copy(alpha = 0.4f), checkmarkColor = if (isDark) BgDark else BgLight))
    }
    if (showBottomBorder) Divider(color = borderColor, thickness = 0.5.dp)
}

@Composable
fun DeleteScheduleDialog(isDark: Boolean, scheduleName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("删除课表", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                Spacer(modifier = Modifier.height(16.dp))
                Text("确定要删除「$scheduleName」吗？此操作无法恢复。", fontSize = 14.sp, color = textColor.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White), shape = RoundedCornerShape(4.dp)) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
fun ShareCodeDialog(isDark: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    var code by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("分享口令导入", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("在此粘贴口令", color = textColor.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        cursorColor = textColor
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(onClick = { if(code.isNotBlank()) onConfirm(code) }, colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor), shape = RoundedCornerShape(4.dp)) {
                        Text("导入")
                    }
                }
            }
        }
    }
}

@Composable
fun WebViewImportScreen(isDark: Boolean, onBack: () -> Unit, onImport: (String) -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val context = LocalContext.current
    var url by remember { mutableStateOf("https://jwxt.cjlu.edu.cn/") }
    var loadUrl by remember { mutableStateOf("https://jwxt.cjlu.edu.cn/") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.weight(1f).height(50.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = textColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = textColor
                )
            )
            TextButton(onClick = { loadUrl = url }) {
                Text("访问", color = textColor, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { url = "https://jwxt.cjlu.edu.cn/"; loadUrl = url }, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("计量大学", fontSize = 12.sp, color = textColor)
            }
            OutlinedButton(onClick = { url = "http://syjw.zjhu.edu.cn/"; loadUrl = url }, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("湖州师范", fontSize = 12.sp, color = textColor)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webViewClient = WebViewClient()
                    addJavascriptInterface(object : Any() {
                        @JavascriptInterface
                        fun passData(data: String) {
                            if (data.startsWith("ERROR:")) {
                                (ctx as? ComponentActivity)?.runOnUiThread {
                                    Toast.makeText(ctx, data.removePrefix("ERROR:"), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                onImport(data)
                            }
                        }
                    }, "Android")
                    webViewRef = this
                }
            }, update = { it.loadUrl(loadUrl) }, modifier = Modifier.fillMaxSize())
        }
        Button(
            onClick = {
                val jsCode = """
                    javascript:(function() {
                        try {
                            var courses = [];
                            var colorList = ['blue', 'pink', 'purple', 'slate', 'indigo', 'rose'];
                            var nodes = document.querySelectorAll('.kbcontent, .timetable_con');
                            
                            if (nodes.length === 0) {
                                window.Android.passData("ERROR:未找到课程节点，请确保已进入「个人课表查询」页面且已显示课表！");
                                return;
                            }

                            nodes.forEach(function(node) {
                                var td = node.closest('td');
                                if (!td || !td.id) return;
                                var dayOfWeek = parseInt(td.id.split('-')[0]);
                                
                                var htmlBlocks = node.innerHTML.split(/<hr[^>]*>/i);
                                
                                htmlBlocks.forEach(function(html) {
                                    var tempDiv = document.createElement('div');
                                    tempDiv.innerHTML = html;
                                    
                                    var textContent = tempDiv.innerText.trim();
                                    if (!textContent) return;
                                    
                                    var titleNode = tempDiv.querySelector('.title font, font[title="课程"], font.color-font');
                                    var name = titleNode ? titleNode.innerText.replace(/[★○●◇]/g, '').trim() : '';
                                    if (!name) {
                                         var lines = textContent.split('\n').filter(l => l.trim() !== '');
                                         if(lines.length > 0) name = lines[0].replace(/[★○●◇]/g, '').trim();
                                    }
                                    
                                    var timeText = '', location = '未排地点', teacher = '';
                                    
                                    var ps = tempDiv.querySelectorAll('p, font, span');
                                    if (ps.length > 0) {
                                        ps.forEach(function(p) {
                                            var text = p.innerText.trim();
                                            if (text.includes('节/周') || text.includes('节)') || text.match(/\d+-\d+节/)) timeText = text;
                                            if (text.includes('校区') || text.includes('楼') || text.includes('教室') || p.getAttribute('title') === '教室') location = text;
                                            if (p.getAttribute('title') === '老师' || text.match(/[\u4e00-\u9fa5]{2,4}/)) {
                                                if(!teacher && text !== name && !text.includes('周') && !text.includes('楼')) {
                                                    teacher = text;
                                                }
                                            }
                                        });
                                    } else {
                                        var lines = textContent.split('\n').map(l => l.trim()).filter(l => l !== '');
                                        lines.forEach(function(line) {
                                            if (line.match(/\d+-\d+节/)) timeText = line;
                                            else if (line.match(/楼|教室|校区/)) location = line;
                                        });
                                    }
                                    
                                    var startNode = 1, endNode = 2;
                                    var weeksArr = [];
                                    
                                    var timeMatch = timeText.match(/\((\d+)-(\d+)节\)(.*)/) || timeText.match(/(\d+)-(\d+)节(.*)/);
                                    if (timeMatch) {
                                        startNode = parseInt(timeMatch[1]);
                                        endNode = parseInt(timeMatch[2]);
                                        var weekStr = timeMatch[3].replace(/\s+/g, '');
                                        
                                        var parts = weekStr.split(/[,，、]+/);
                                        parts.forEach(function(p) {
                                            var isOdd = p.indexOf('单') !== -1;
                                            var isEven = p.indexOf('双') !== -1;
                                            var rangeMatch = p.match(/(\d+)-(\d+)/);
                                            if (rangeMatch) {
                                                for(var i = parseInt(rangeMatch[1]); i <= parseInt(rangeMatch[2]); i++) {
                                                    if (isOdd && i % 2 === 0) continue;
                                                    if (isEven && i % 2 !== 0) continue;
                                                    weeksArr.push(i);
                                                }
                                            } else {
                                                var singleMatch = p.match(/(\d+)/);
                                                if (singleMatch) {
                                                    var wNum = parseInt(singleMatch[1]);
                                                    if (isOdd && wNum % 2 === 0) return;
                                                    if (isEven && wNum % 2 !== 0) return;
                                                    weeksArr.push(wNum);
                                                }
                                            }
                                        });
                                    }
                                    
                                    if (!name || weeksArr.length === 0) return;
                                    
                                    var nameHash = 0;
                                    for(var i=0; i<name.length; i++) nameHash += name.charCodeAt(i);
                                    var colorTheme = colorList[nameHash % colorList.length];
                                    
                                    courses.push({
                                        name: name,
                                        location: location.replace('上课地点：', '').trim(),
                                        teacher: teacher.replace('教师：', '').trim(),
                                        dayOfWeek: dayOfWeek,
                                        startNode: startNode,
                                        endNode: endNode,
                                        weeks: JSON.stringify(weeksArr),
                                        colorTheme: colorTheme
                                    });
                                });
                            });
                            
                            var uniqueCourses = [];
                            var seen = new Set();
                            courses.forEach(function(c) {
                                var key = c.name + c.dayOfWeek + c.startNode + c.endNode + c.weeks;
                                if(!seen.has(key)) {
                                    seen.add(key);
                                    uniqueCourses.push(c);
                                }
                            });
                            
                            window.Android.passData(JSON.stringify(uniqueCourses));
                        } catch (e) {
                            window.Android.passData("ERROR:提取异常:" + e.message);
                        }
                    })();
                """.trimIndent()
                webViewRef?.evaluateJavascript(jsCode, null)
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp).navigationBarsPadding(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if(isDark) BgDark else BgLight)
        ) {
            Text("提取当前正方教务课表", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimetableListScreen(timetables: List<TimetableGroup>, currentLinkedId: String, isDark: Boolean, onBack: () -> Unit, onSelect: (String) -> Unit, onEdit: (String?) -> Unit, onDelete: (String) -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("时间表", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                item {
                    Text("当前课表关联的时间表", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))
                    timetables.find { it.id == currentLinkedId }?.let {
                        Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(20.dp)) {
                            Text(it.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = textColor, modifier = Modifier.align(Alignment.CenterEnd))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("点击应用，操作菜单可编辑", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))
                }
                items(timetables) { tGroup ->
                    var showMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().animateContentSize().padding(bottom = 12.dp).background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).clickable { onSelect(tGroup.id) }.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tGroup.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Box {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = textColor.copy(alpha = 0.5f), modifier = Modifier.clickable { showMenu = true })
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(if (isDark) Color(0xFF18181B) else Color.White).border(0.5.dp, borderColor)) {
                                DropdownMenuItem(text = { Text("✏️ 编辑内容", fontWeight = FontWeight.Bold, color = textColor) }, onClick = { showMenu = false; onEdit(tGroup.id) })
                                val isBuiltIn = tGroup.id == "tt_cjlu" || tGroup.id == "tt_zjhu_summer" || tGroup.id == "tt_zjhu_winter"
                                if (!isBuiltIn) {
                                    Divider(color = borderColor, thickness = 0.5.dp)
                                    DropdownMenuItem(text = { Text("🗑️ 删除时间表", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) }, onClick = { showMenu = false; onDelete(tGroup.id) })
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp).navigationBarsPadding()) }
            }
        }
        FloatingActionButton(
            onClick = { onEdit(null) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp),
            containerColor = textColor,
            contentColor = if (isDark) BgDark else BgLight,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Add, "Add Timetable")
        }
    }
}

@Composable
fun TimetableEditScreen(timetableId: String?, timetables: List<TimetableGroup>, viewModel: ScheduleViewModel, isDark: Boolean, onBack: () -> Unit, onSave: (String?, String, List<TimeNode>) -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var isSameDuration by remember { mutableStateOf(true) }
    var classDuration by remember { mutableStateOf("45") }
    var nodes by remember { mutableStateOf<List<TimeNode>>(emptyList()) }

    LaunchedEffect(timetableId) {
        if (timetableId != null) {
            name = timetables.find { it.id == timetableId }?.name ?: ""
            nodes = viewModel.getTimeNodesForEdit(timetableId)
        } else {
            name = "新时间表"
            var currentStart = "08:00"
            nodes = (1..12).map { i ->
                val end = addMinutes(currentStart, 45)
                val node = TimeNode(timetableId = "temp", nodeIndex = i, startTime = currentStart, endTime = end)
                currentStart = addMinutes(end, 10)
                node
            }
        }
    }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (timetableId == null) "新建时间表" else "编辑时间表", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Spacer(modifier = Modifier.weight(1f))
            Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.clickable { onSave(timetableId, name, nodes) }.padding(8.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("时间表名称", color = textColor.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        cursorColor = textColor
                    )
                )
                Row(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("每节课时长相同", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Switch(checked = isSameDuration, onCheckedChange = { isSameDuration = it }, colors = SwitchDefaults.colors(checkedThumbColor = if (isDark) BgDark else BgLight, checkedTrackColor = textColor))
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (isSameDuration) {
                    Row(modifier = Modifier.fillMaxWidth().animateContentSize().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("一节课时长 (分钟)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        OutlinedTextField(
                            value = classDuration,
                            onValueChange = { classDuration = it },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = textColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                                cursorColor = textColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("具体时间节点", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))
            }
            itemsIndexed(nodes) { index, node ->
                Row(modifier = Modifier.fillMaxWidth().animateContentSize().padding(bottom = 12.dp).background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("第 ${node.nodeIndex} 节", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.width(60.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Text(node.startTime, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.clickable {
                        showTimePicker(context, node.startTime) { newStart ->
                            val dur = classDuration.toIntOrNull() ?: 45
                            val newEnd = if (isSameDuration) addMinutes(newStart, dur) else node.endTime
                            val newList = nodes.toMutableList()
                            newList[index] = node.copy(startTime = newStart, endTime = newEnd)
                            nodes = newList
                        }
                    }.padding(8.dp))
                    Text(" - ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f))
                    Text(node.endTime, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.clickable {
                        showTimePicker(context, node.endTime) { newEnd ->
                            val newList = nodes.toMutableList()
                            newList[index] = node.copy(endTime = newEnd)
                            nodes = newList
                        }
                    }.padding(8.dp))
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        val lastEnd = nodes.lastOrNull()?.endTime ?: "08:00"
                        val dur = classDuration.toIntOrNull() ?: 45
                        val newStart = addMinutes(lastEnd, 10)
                        val newEnd = addMinutes(newStart, dur)
                        nodes = nodes + TimeNode(timetableId = "temp", nodeIndex = nodes.size + 1, startTime = newStart, endTime = newEnd)
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp).navigationBarsPadding(),
                    shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
                ) {
                    Text("➕ 增加一节课", color = textColor)
                }
            }
        }
    }
}

@Composable
fun CourseManagementScreen(courses: List<Course>, isDark: Boolean, onBack: () -> Unit, onEditCourse: (Course) -> Unit, onDeleteCourse: (String) -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("课程管理", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Spacer(modifier = Modifier.weight(1f))
            Text("长按删除", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            modifier = Modifier.navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(courses) { course ->
                val palette = getPalette(course.colorTheme, isDark)
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).animateContentSize().clip(RoundedCornerShape(8.dp)).background(palette.bg).border(0.5.dp, palette.border, RoundedCornerShape(8.dp)).pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDeleteCourse(course.id) },
                            onDrag = { _, _ -> }, onDragEnd = {}, onDragCancel = {}
                        )
                    }.clickable { onEditCourse(course) }.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = course.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.text, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ChangeWeekDialog(isDark: Boolean, title: String, currentValue: Int, maxValue: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    var value by remember { mutableFloatStateOf(currentValue.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(24.dp))
                Text("${value.roundToInt()}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(value = value, onValueChange = { value = it }, valueRange = 1f..maxValue.toFloat(), steps = (maxValue - 2).coerceAtLeast(0), colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(onClick = { onConfirm(value.roundToInt()) }, colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor), shape = RoundedCornerShape(4.dp)) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(displayCourse: DisplayCourse, clickedWeek: Int, isDark: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val course = displayCourse.course
    val palette = getPalette(course.colorTheme, isDark)
    val weeksList = course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = if (isDark) BgDark else BgLight, dragHandle = { BottomSheetDefaults.DragHandle(color = borderColor) }) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(palette.accent))
                Spacer(modifier = Modifier.width(8.dp))

                val isOddOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 != 0 } && weeksList.size > 1
                val isEvenOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 == 0 } && weeksList.size > 1
                val modeText = if (isOddOnly) " (单周)" else if (isEvenOnly) " (双周)" else ""

                Text(text = if (clickedWeek > 0) "WEEK $clickedWeek$modeText" else "STARTS W${weeksList.firstOrNull() ?: 1}$modeText", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f), letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(course.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textColor, lineHeight = 32.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f).border(0.5.dp, borderColor).padding(16.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = "Loc", tint = textColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LOCATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    Text(course.location, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Column(modifier = Modifier.weight(1f).border(0.5.dp, borderColor).padding(16.dp)) {
                    Icon(Icons.Rounded.Person, contentDescription = "Teacher", tint = textColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("INSTRUCTOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    Text(course.teacher, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(0.dp), colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) BgDark else BgLight)) {
                    Text("EDIT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(0.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)), border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)) {
                    Text("REMOVE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun CreateScheduleDialog(isDark: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("新建课表", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("例如：大二上学期", color = textColor.copy(alpha = 0.6f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = textColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f),
                        cursorColor = textColor
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(onClick = { if(name.isNotBlank()) onConfirm(name) }, colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor), shape = RoundedCornerShape(4.dp)) {
                        Text("创建")
                    }
                }
            }
        }
    }
}

@Composable
fun CourseEditDialog(isDark: Boolean, initialCourse: Course?, onDismiss: () -> Unit, onConfirm: (String?, String, String, String, Int, Int, Int, String, String) -> Unit) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var loc by remember { mutableStateOf(initialCourse?.location ?: "") }
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var day by remember { mutableStateOf(initialCourse?.dayOfWeek?.toString() ?: "1") }
    var start by remember { mutableStateOf(initialCourse?.startNode?.toString() ?: "1") }
    var end by remember { mutableStateOf(initialCourse?.endNode?.toString() ?: "2") }
    val colorTheme by remember { mutableStateOf(initialCourse?.colorTheme ?: listOf("blue", "pink", "purple", "slate", "indigo", "rose").random()) }
    val weeks by remember { mutableStateOf(initialCourse?.weeks ?: "[8,9,10,11,12]") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if (initialCourse == null) "添加课程" else "编辑课程", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("课程名称", color = textColor.copy(alpha = 0.6f)) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = loc, onValueChange = { loc = it }, label = { Text("上课地点", color = textColor.copy(alpha = 0.6f)) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = teacher, onValueChange = { teacher = it }, label = { Text("授课教师", color = textColor.copy(alpha = 0.6f)) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = day, onValueChange = { day = it }, label = { Text("星期(1-7)", color = textColor.copy(alpha = 0.6f)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                    )
                    OutlinedTextField(
                        value = start, onValueChange = { start = it }, label = { Text("起始节", color = textColor.copy(alpha = 0.6f)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                    )
                    OutlinedTextField(
                        value = end, onValueChange = { end = it }, label = { Text("结束节", color = textColor.copy(alpha = 0.6f)) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(
                        onClick = { onConfirm(initialCourse?.id, name, loc, teacher, day.toIntOrNull()?:1, start.toIntOrNull()?:1, end.toIntOrNull()?:2, colorTheme, weeks) },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor), shape = RoundedCornerShape(4.dp)
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
fun AdjustCourseScreen(isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("日期之间调课", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp).background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Swipe, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("你已经可以直接拖拽啦！", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text("请返回主页，长按你想要调整的课程卡片，直接拖动到新的日期和节次即可完成调课。\n此页面后续将作为【批量复杂调课】功能区。", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), lineHeight = 20.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuBottomSheet(
    isDark: Boolean, currentPagerWeek: Int, totalWeeks: Int,
    scheduleGroups: List<ScheduleGroup>, currentScheduleId: String,
    onBrowseWeek: (Int) -> Unit, onSwitchSchedule: (String) -> Unit,
    onCreateScheduleClick: () -> Unit,
    onDismiss: () -> Unit, onManageCoursesClick: () -> Unit, onTimetableManageClick: () -> Unit,
    onScheduleSettingsClick: () -> Unit, onGlobalSettingsClick: () -> Unit, onReminderSettingsClick: () -> Unit,
    onDeleteScheduleClick: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val primaryColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = if (isDark) BgDark else BgLight, dragHandle = { BottomSheetDefaults.DragHandle(color = borderColor) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("修改当前周", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                Surface(
                    color = primaryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onDismiss(); onScheduleSettingsClick() }
                ) {
                    Text(
                        text = "前往设置 >",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = currentPagerWeek.toFloat(),
                onValueChange = { onBrowseWeek(it.roundToInt()) },
                valueRange = 1f..totalWeeks.toFloat().coerceAtLeast(2f),
                steps = (totalWeeks - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor, inactiveTrackColor = primaryColor.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("课表", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.weight(1f))
                Text("新建课表", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.clickable { onDismiss(); onCreateScheduleClick() })
                Spacer(modifier = Modifier.width(16.dp))
                Text("管理", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.clickable { onDismiss(); onScheduleSettingsClick() })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                scheduleGroups.forEach { group ->
                    val isSelected = group.id == currentScheduleId
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) primaryColor else surfaceColor)
                            .border(0.5.dp, if (isSelected) primaryColor else borderColor, RoundedCornerShape(12.dp))
                            .clickable { onSwitchSchedule(group.id) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = if (isDark) BgDark else BgLight, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            text = group.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) (if (isDark) BgDark else BgLight) else textColor,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            data class MenuItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)
            val menuItems = listOf(
                MenuItem("上课时间", Icons.Rounded.Schedule) { onDismiss(); onTimetableManageClick() },
                MenuItem("已添课程", Icons.Rounded.EventNote) { onDismiss(); onManageCoursesClick() },
                MenuItem("课表设置", Icons.Rounded.Tune) { onDismiss(); onScheduleSettingsClick() },
                MenuItem("课前提醒", Icons.Rounded.Campaign) { onDismiss(); onReminderSettingsClick() },
                MenuItem("全局配置", Icons.Rounded.Settings) { onDismiss(); onGlobalSettingsClick() }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                menuItems.forEach { item ->
                    Column(
                        modifier = Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = item.onClick),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(surfaceColor, RoundedCornerShape(14.dp)).border(0.5.dp, borderColor, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = textColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { onDismiss(); onDeleteScheduleClick() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = BorderStroke(0.5.dp, Color(0xFFDC2626)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("删除当前课表", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileScreen(isDark: Boolean, onThemeToggle: (Boolean) -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Text("管理", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth().background(surfaceColor).border(0.5.dp, borderColor).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).background(textColor), contentAlignment = Alignment.Center) {
                Text("G", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isDark) BgDark else BgLight)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text("Gemi User", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("ID: 849201", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = textColor.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text("APPEARANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.4f), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.weight(1f).clickable { onThemeToggle(false) }.background(if (!isDark) textColor else Color.Transparent).border(0.5.dp, if (!isDark) Color.Transparent else borderColor).padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Light", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (!isDark) BgLight else textColor)
            }
            Row(modifier = Modifier.weight(1f).clickable { onThemeToggle(true) }.background(if (isDark) textColor else Color.Transparent).border(0.5.dp, if (isDark) Color.Transparent else borderColor).padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Dark", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) BgDark else textColor)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text("关于", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
        Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
            Column {
                SettingValueItem(title = "版本", value = "v1.2.0", textColor = textColor, borderColor = borderColor)
                SettingItemWithSubtext(
                    title = "开源与反馈",
                    subtext = "点击访问 GitHub 仓库获取源码或提交建议",
                    showBottomBorder = false,
                    textColor = textColor,
                    borderColor = borderColor,
                    onClick = { uriHandler.openUri("https://github.com/yourname/SimpleSchedule") }
                )
            }
        }
    }
}

// --- 7. 自动提醒服务与调度引擎 ---

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

        // 计算当前周次与时间
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

        // 简易预测：最多推算未来3天的课，找出最近的一个未开课的
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

    fun scheduleAlarmByParams(context: Context, courseName: String, location: String, timeStr: String, triggerMillis: Long, classStartMillis: Long, showNotify: Boolean, playVoice: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

        // 1. 注册提醒弹出的闹钟
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

        // 2. 注册自动取消(上课时)的闹钟
        val cancelIntent = Intent(context, CourseAlarmReceiver::class.java).apply { action = "ACTION_DISMISS_REMINDER" }
        val cancelPending = PendingIntent.getBroadcast(context, REQUEST_CODE_CANCEL, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerMillis, showPending)
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, classStartMillis, cancelPending)
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    // 精确计算并注册下一次桌面小组件刷新（下课时或午夜）
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
                set(Calendar.SECOND, 2) // 下课后2秒准时刷新小组件
                set(Calendar.MILLISECOND, 0)
            }
            val millis = endCal.timeInMillis
            if (millis > System.currentTimeMillis()) millis else null
        }

        var nextUpdateMillis = todayCoursesEndMillis.minOrNull()

        // 如果今天没课了或者课都上完了，设置到第二天凌晨 00:00:01 更新
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

class CourseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, "ACTION_UPDATE_WIDGET" -> {
                // 开机或者闹钟触发时，利用 WorkManager 执行高优先级的一次性更新任务，彻底解决协程中途被杀的问题
                val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
            "ACTION_SHOW_REMINDER" -> {
                val playVoice = intent.getBooleanExtra("PLAY_VOICE", true)
                val showNotify = intent.getBooleanExtra("SHOW_NOTIFY", true)

                val serviceIntent = Intent(context, CourseForegroundService::class.java).apply {
                    putExtra("COURSE_NAME", intent.getStringExtra("COURSE_NAME"))
                    putExtra("LOCATION", intent.getStringExtra("LOCATION"))
                    putExtra("TIME_STR", intent.getStringExtra("TIME_STR"))
                    putExtra("CLASS_START_MILLIS", intent.getLongExtra("CLASS_START_MILLIS", 0L))
                    putExtra("SHOW_NOTIFY", showNotify)
                    putExtra("PLAY_VOICE", playVoice)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            "ACTION_DISMISS_REMINDER" -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(1001)

                // 下课时同样交由守护 Worker 重新计算下一节课
                val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}

class CourseForegroundService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val CHANNEL_ID = "CourseAlarmChannel"
    private var pendingSpeakText: String? = null
    private var playVoice = true
    private var showNotify = true
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "上课提醒", NotificationManager.IMPORTANCE_HIGH))
        }
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val courseName = intent?.getStringExtra("COURSE_NAME") ?: "未知课程"
        val location = intent?.getStringExtra("LOCATION") ?: "未知地点"
        val timeStr = intent?.getStringExtra("TIME_STR") ?: "00:00"
        val classStartMillis = intent?.getLongExtra("CLASS_START_MILLIS", 0L) ?: 0L
        showNotify = intent?.getBooleanExtra("SHOW_NOTIFY", true) ?: true
        playVoice = intent?.getBooleanExtra("PLAY_VOICE", true) ?: true

        val remoteViews = RemoteViews(packageName, R.layout.notification_course)
        remoteViews.setTextViewText(R.id.tv_time, timeStr)
        remoteViews.setTextViewText(R.id.tv_location, location.replace("楼", ""))
        remoteViews.setTextViewText(R.id.tv_course_name, courseName)

        if (classStartMillis > 0) {
            remoteViews.setChronometer(R.id.chronometer, classStartMillis, "%s 后上课", true)
            remoteViews.setChronometerCountDown(R.id.chronometer, true)
        }

        val cancelIntent = Intent(this, CourseAlarmReceiver::class.java).apply { action = "ACTION_DISMISS_REMINDER" }
        val cancelPending = PendingIntent.getBroadcast(this, SettingsKeys.REMINDER_ADVANCE_MINS.hashCode(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        remoteViews.setOnClickPendingIntent(R.id.btn_mute, cancelPending)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(1001, notification)
        }

        if (!showNotify) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }

        if (playVoice) {
            val textToSpeak = "您接下来在 ${location.replace("楼", "")} 有一节 $courseName 课。请准备。"
            if (isTtsReady && tts != null) {
                tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                waitAndFinish(showNotify)
            } else {
                pendingSpeakText = textToSpeak
                // 防止 TTS 引擎一直不就绪导致服务挂起
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(6000)
                    if (pendingSpeakText != null) finishServiceSafely(showNotify)
                }
            }
        } else {
            finishServiceSafely(showNotify)
        }

        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && playVoice) {
            val result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = false
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "语音播报失败：系统缺少中文TTS引擎或语音包，请前往系统设置下载", Toast.LENGTH_LONG).show()
                }
                finishServiceSafely(showNotify)
            } else {
                isTtsReady = true
                pendingSpeakText?.let {
                    tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
                    pendingSpeakText = null
                    waitAndFinish(showNotify)
                }
            }
        } else {
            finishServiceSafely(showNotify)
        }
    }

    private fun waitAndFinish(keepNotification: Boolean) {
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(6000) // 等待语音说完再解绑服务
            finishServiceSafely(keepNotification)
        }
    }

    private fun finishServiceSafely(keepNotification: Boolean) {
        if (keepNotification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// --- 8. 桌面小组件 (Jetpack Glance) ---

class CourseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appDao = AppDatabase.getDatabase(context).appDao()
        val prefs = context.dataStore.data.firstOrNull()
        val savedId = prefs?.get(SettingsKeys.CURRENT_SCHEDULE_ID)
        val isTranslucent = prefs?.get(SettingsKeys.WIDGET_TRANSLUCENT) ?: false

        val scheduleGroups = appDao.getAllScheduleGroups().firstOrNull() ?: emptyList()
        val currentSchedule = scheduleGroups.find { it.id == savedId } ?: scheduleGroups.firstOrNull()

        val cal = Calendar.getInstance()
        val todayDayOfWeek = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1

        var currentWeek = 1
        if (currentSchedule != null && currentSchedule.startDate.isNotEmpty()) {
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

        val rawCourses = if (currentSchedule != null) appDao.getCoursesBySchedule(currentSchedule.id).firstOrNull() ?: emptyList() else emptyList()
        val overrides = appDao.getAllOverrides().firstOrNull() ?: emptyList()
        val overrideMap = overrides.associateBy { it.courseId }

        val timeNodes = if (currentSchedule != null) appDao.getTimeNodes(currentSchedule.timetableId) else emptyList()
        val timeNodeMap = timeNodes.associateBy { it.nodeIndex }

        val currentTimeStr = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

        val todayCourses = rawCourses.mapNotNull { course ->
            val over = overrideMap[course.id]
            val effectiveDay = over?.newDayOfWeek ?: course.dayOfWeek
            val effectiveStart = over?.newStartNode ?: course.startNode
            val effectiveEnd = over?.newEndNode ?: course.endNode

            if (effectiveDay != todayDayOfWeek) return@mapNotNull null

            val weeksList = course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }
            if (!weeksList.contains(currentWeek)) return@mapNotNull null

            val endNodeInfo = timeNodeMap[effectiveEnd]
            if (endNodeInfo != null && endNodeInfo.endTime <= currentTimeStr) {
                return@mapNotNull null
            }

            DisplayCourse(course, effectiveDay, effectiveStart, effectiveEnd)
        }.sortedBy { it.displayStartNode }

        provideContent {
            CourseWidgetContent(context, todayCourses, timeNodeMap, isTranslucent)
        }
    }
}

@Suppress("RestrictedApi")
@Composable
fun CourseWidgetContent(context: Context, todayCourses: List<DisplayCourse>, timeNodeMap: Map<Int, TimeNode>, isTranslucent: Boolean) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    GlanceColumn(
        modifier = GlanceModifier.glanceFillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .glanceBackground(ColorProvider(if (isTranslucent) Color(0x9918181B) else Color(0xFF18181B)))
            .glancePadding(16.dp)
            .glanceClickable(actionStartActivity(intent))
    ) {
        GlanceText(
            text = if (todayCourses.isEmpty()) "今天已经没有课啦，好好休息喵~" else "今日还有 ${todayCourses.size} 门课要上",
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = GlanceFontWeight.Bold)
        )
        GlanceSpacer(modifier = GlanceModifier.glanceHeight(12.dp))

        val showCourses = todayCourses.take(4)

        showCourses.forEach { displayCourse ->
            val course = displayCourse.course
            val startNode = timeNodeMap[displayCourse.displayStartNode]
            val endNode = timeNodeMap[displayCourse.displayEndNode]
            val timeStr = "${startNode?.startTime ?: ""} - ${endNode?.endTime ?: ""}"

            val palette = getPalette(course.colorTheme, isDark = true)
            val cardAlpha = if (isTranslucent) 0.7f else 1.0f

            GlanceColumn(
                modifier = GlanceModifier
                    .glanceFillMaxWidth()
                    .cornerRadius(12.dp)
                    .glanceBackground(ColorProvider(palette.bg.copy(alpha = cardAlpha)))
                    .glancePadding(12.dp)
                    .glanceClickable(actionStartActivity(intent))
            ) {
                GlanceText(course.name, style = TextStyle(color = ColorProvider(palette.text), fontSize = 14.sp, fontWeight = GlanceFontWeight.Bold))
                GlanceSpacer(modifier = GlanceModifier.glanceHeight(4.dp))
                GlanceText("$timeStr ${course.location}", style = TextStyle(color = ColorProvider(palette.text.copy(alpha = 0.8f)), fontSize = 11.sp))
            }
            GlanceSpacer(modifier = GlanceModifier.glanceHeight(8.dp))
        }

        if (todayCourses.size > 4) {
            GlanceText(
                text = "... 还有 ${todayCourses.size - 4} 门课被折叠了喵",
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.6f)), fontSize = 12.sp)
            )
        }

        if (todayCourses.isEmpty()) {
            GlanceSpacer(modifier = GlanceModifier.defaultWeight())
            GlanceText(
                text = "(๑•̀ㅂ•́)و✧\n今天没有课啦！",
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.5f)), fontSize = 16.sp, textAlign = GlanceTextAlign.Center),
                modifier = GlanceModifier.glanceFillMaxWidth()
            )
        } else if (todayCourses.size <= 4) {
            GlanceSpacer(modifier = GlanceModifier.defaultWeight())
            GlanceText(
                text = "(ฅ´ω`ฅ) 加油喵~",
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.3f)), fontSize = 14.sp, textAlign = GlanceTextAlign.Center),
                modifier = GlanceModifier.glanceFillMaxWidth().glancePadding(top = 8.dp)
            )
        }
    }
}

// --- 9. 后台守护任务 (WorkManager) ---

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val appDao = AppDatabase.getDatabase(applicationContext).appDao()
            // 1. 强制刷新小组件 UI
            CourseWidget().updateAll(applicationContext)
            // 2. 重新注册下一个精确闹钟
            ReminderEngine.scheduleNextWidgetUpdate(applicationContext, appDao)
            // 3. 重新检查和注册课前提醒
            ReminderEngine.calculateAndScheduleNext(applicationContext, appDao)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class CourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget()
}