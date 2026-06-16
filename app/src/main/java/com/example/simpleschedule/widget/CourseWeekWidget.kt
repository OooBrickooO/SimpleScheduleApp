package com.example.simpleschedule.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background as glanceBackground
import androidx.glance.layout.*
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.padding as glancePadding
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.TextAlign as GlanceTextAlign
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable as glanceClickable
import com.example.simpleschedule.MainActivity
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.*
import com.example.simpleschedule.ui.theme.getPalette
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

data class WeekWidgetData(
    val coursesByDay: Map<Int, List<DisplayCourse>>,
    val timeNodeMap: Map<Int, TimeNode>,
    val currentWeek: Int,
    val isTranslucent: Boolean
)

suspend fun loadWeekWidgetData(context: Context): WeekWidgetData {
    val appDao = AppDatabase.getDatabase(context).appDao()
    val prefs = context.dataStore.data.firstOrNull()
    val savedId = prefs?.get(SettingsKeys.CURRENT_SCHEDULE_ID)
    val isTranslucent = prefs?.get(SettingsKeys.WIDGET_TRANSLUCENT) ?: false

    val scheduleGroups = appDao.getAllScheduleGroups().firstOrNull() ?: emptyList()
    val currentSchedule = scheduleGroups.find { it.id == savedId } ?: scheduleGroups.firstOrNull()

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

    val coursesByDay = (1..7).associateWith { day ->
        rawCourses.mapNotNull { course ->
            val over = overrideMap[course.id]
            val effectiveDay = over?.newDayOfWeek ?: course.dayOfWeek
            val effectiveStart = over?.newStartNode ?: course.startNode
            val effectiveEnd = over?.newEndNode ?: course.endNode

            if (effectiveDay != day) return@mapNotNull null

            val weeksList = course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }
            if (!weeksList.contains(currentWeek)) return@mapNotNull null

            DisplayCourse(course, effectiveDay, effectiveStart, effectiveEnd)
        }.sortedBy { it.displayStartNode }
    }

    return WeekWidgetData(coursesByDay, timeNodeMap, currentWeek, isTranslucent)
}

class CourseWeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWeekWidgetData(context)
        provideContent {
            CourseWeekWidgetContent(context, data)
        }
    }
}

@Composable
fun CourseWeekWidgetContent(context: Context, data: WeekWidgetData) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    
    GlanceColumn(
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .glanceBackground(ColorProvider(if (data.isTranslucent) Color(0xCC18181B) else Color(0xFF18181B)))
            .glancePadding(12.dp)
            .glanceClickable(actionStartActivity(intent))
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlanceText(
                text = "第 ${data.currentWeek} 周课程表",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = GlanceFontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            GlanceText(
                text = "全周视图",
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.5f)), fontSize = 11.sp)
            )
        }
        
        Spacer(modifier = GlanceModifier.height(10.dp))
        
        Row(modifier = GlanceModifier.fillMaxSize().defaultWeight()) {
            (1..7).forEach { day ->
                DayColumn(
                    dayName = dayNames[day - 1],
                    courses = data.coursesByDay[day] ?: emptyList(),
                    isLast = day == 7
                )
            }
        }
    }
}

@Composable
fun RowScope.DayColumn(dayName: String, courses: List<DisplayCourse>, isLast: Boolean) {
    GlanceColumn(
        modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlanceText(
            text = dayName,
            style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)), fontSize = 12.sp, fontWeight = GlanceFontWeight.Medium)
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        
        Box(modifier = GlanceModifier.fillMaxSize().defaultWeight()) {
            GlanceColumn(modifier = GlanceModifier.fillMaxSize()) {
                (1..12).forEach { node ->
                    val course = courses.find { node >= it.displayStartNode && node <= it.displayEndNode }
                    if (course != null) {
                        val palette = getPalette(course.course.colorTheme, isDark = true)
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .glanceBackground(ColorProvider(palette.accent.copy(alpha = 0.9f)))
                                .cornerRadius(2.dp)
                        ) {}
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .glanceBackground(ColorProvider(Color.White.copy(alpha = 0.05f)))
                        ) {}
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
        }
    }
    if (!isLast) {
        Spacer(modifier = GlanceModifier.width(4.dp))
    }
}

class CourseWeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWeekWidget()
}
