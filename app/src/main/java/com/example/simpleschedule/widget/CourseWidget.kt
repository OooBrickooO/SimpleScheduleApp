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
        val distinctCourseCount = todayCourses.distinctBy { it.course.name }.size
        GlanceText(
            text = if (todayCourses.isEmpty()) "今天已经没有课啦，好好休息喵~" else "今日还有 $distinctCourseCount 门课要上",
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

class CourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget()
}

// --- 9. 后台守护任务 (WorkManager) ---

