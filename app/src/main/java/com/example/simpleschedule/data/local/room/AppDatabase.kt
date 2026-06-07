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

