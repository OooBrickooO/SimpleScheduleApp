package com.example.simpleschedule.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable as glanceClickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background as glanceBackground
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize as glanceFillMaxSize
import androidx.glance.layout.fillMaxWidth as glanceFillMaxWidth
import androidx.glance.layout.height as glanceHeight
import androidx.glance.layout.padding as glancePadding
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextAlign as GlanceTextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.simpleschedule.MainActivity
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.AppDatabase
import com.example.simpleschedule.data.local.room.DisplayCourse
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.domain.ScheduleResolver
import com.example.simpleschedule.ui.theme.getPalette
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Locale

data class WidgetData(
    val todayCourses: List<DisplayCourse>,
    val timeNodeMap: Map<Int, TimeNode>,
    val isTranslucent: Boolean
)

suspend fun loadWidgetData(context: Context): WidgetData {
    val appDao = AppDatabase.getDatabase(context).appDao()
    val prefs = context.dataStore.data.firstOrNull()
    val savedId = prefs?.get(SettingsKeys.CURRENT_SCHEDULE_ID)
    val isTranslucent = prefs?.get(SettingsKeys.WIDGET_TRANSLUCENT) ?: false

    val scheduleGroups = appDao.getAllScheduleGroups().firstOrNull() ?: emptyList()
    val currentSchedule = scheduleGroups.find { it.id == savedId } ?: scheduleGroups.firstOrNull()

    val cal = Calendar.getInstance()
    val todayDayOfWeek = ScheduleResolver.calendarDayToIsoDay(cal)
    val currentWeek = if (currentSchedule != null) {
        ScheduleResolver.calculateWeekFromStartDate(currentSchedule.startDate, cal)
    } else 1

    val rawCourses = if (currentSchedule != null) appDao.getCoursesBySchedule(currentSchedule.id).firstOrNull() ?: emptyList() else emptyList()
    val overrides = appDao.getAllOverrides().firstOrNull() ?: emptyList()
    val overrideMap = overrides.associateBy { it.courseId }
    val makeUpRules = if (currentSchedule != null) appDao.getMakeUpRulesBySchedule(currentSchedule.id).firstOrNull() ?: emptyList() else emptyList()
    val timeNodes = if (currentSchedule != null) appDao.getTimeNodes(currentSchedule.timetableId) else emptyList()
    val timeNodeMap = timeNodes.associateBy { it.nodeIndex }

    val displayCourses = ScheduleResolver.applyOverrides(rawCourses, overrideMap)
    val todayDateStr = ScheduleResolver.formatDate(cal)
    val currentTimeStr = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

    val daySchedule = if (currentSchedule != null) {
        ScheduleResolver.resolveDayCourses(
            dateStr = todayDateStr,
            week = currentWeek,
            dayOfWeek = todayDayOfWeek,
            startDateStr = currentSchedule.startDate,
            displayCourses = displayCourses,
            makeUpRules = makeUpRules
        )
    } else null

    val todayCourses = (daySchedule?.courses ?: emptyList()).filter { dc ->
        val endNodeInfo = timeNodeMap[dc.displayEndNode]
        if (endNodeInfo != null) {
            val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val endHm = ScheduleResolver.parseTimeMinutes(endNodeInfo.endTime)
            if (endHm != null && endHm.first * 60 + endHm.second <= nowMins) {
                return@filter false
            } else if (endHm == null && endNodeInfo.endTime <= currentTimeStr) {
                return@filter false
            }
        }
        true
    }.sortedBy { it.displayStartNode }

    return WidgetData(todayCourses, timeNodeMap, isTranslucent)
}

class CourseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            CourseWidgetContent4x2(context, data.todayCourses, data.timeNodeMap, data.isTranslucent)
        }
    }
}

class CourseWidget2x2 : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            CourseWidgetContentNarrow(context, data.todayCourses, data.timeNodeMap, data.isTranslucent, maxShow = 2)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun CourseWidgetContent4x2(context: Context, todayCourses: List<DisplayCourse>, timeNodeMap: Map<Int, TimeNode>, isTranslucent: Boolean) {
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
        val totalSlots = todayCourses.size
        GlanceText(
            text = if (todayCourses.isEmpty()) "今天已经没有课啦，好好休息喵~" else "今日还有 $totalSlots 节课程安排",
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
                val displayName = course.name + if (!course.credits.isNullOrBlank()) "（${course.credits}）" else ""
                GlanceText(displayName, style = TextStyle(color = ColorProvider(palette.text), fontSize = 14.sp, fontWeight = GlanceFontWeight.Bold))
                GlanceSpacer(modifier = GlanceModifier.glanceHeight(4.dp))
                GlanceText("$timeStr ${course.location}", style = TextStyle(color = ColorProvider(palette.text.copy(alpha = 0.8f)), fontSize = 11.sp))
            }
            GlanceSpacer(modifier = GlanceModifier.glanceHeight(8.dp))
        }

        if (todayCourses.size > 4) {
            GlanceText(
                text = "... 还有 ${todayCourses.size - 4} 节课被折叠了喵",
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

@SuppressLint("RestrictedApi")
@Composable
fun CourseWidgetContentNarrow(context: Context, todayCourses: List<DisplayCourse>, timeNodeMap: Map<Int, TimeNode>, isTranslucent: Boolean, maxShow: Int) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    GlanceColumn(
        modifier = GlanceModifier.glanceFillMaxSize()
            .appWidgetBackground()
            .cornerRadius(12.dp)
            .glanceBackground(ColorProvider(if (isTranslucent) Color(0x9918181B) else Color(0xFF18181B)))
            .glancePadding(8.dp)
            .glanceClickable(actionStartActivity(intent))
    ) {
        GlanceText(
            text = if (todayCourses.isEmpty()) "今日无课" else "今日 ${todayCourses.size} 节课",
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = GlanceFontWeight.Bold)
        )
        GlanceSpacer(modifier = GlanceModifier.glanceHeight(6.dp))

        val showCourses = todayCourses.take(maxShow)

        showCourses.forEach { displayCourse ->
            val course = displayCourse.course
            val startNode = timeNodeMap[displayCourse.displayStartNode]
            val startTime = startNode?.startTime ?: ""
            val loc = course.location.trim()
            val infoText = if (loc.isNotEmpty()) "$startTime @$loc" else startTime

            val palette = getPalette(course.colorTheme, isDark = true)
            val cardAlpha = if (isTranslucent) 0.7f else 1.0f

            GlanceColumn(
                modifier = GlanceModifier
                    .glanceFillMaxWidth()
                    .cornerRadius(8.dp)
                    .glanceBackground(ColorProvider(palette.bg.copy(alpha = cardAlpha)))
                    .glancePadding(6.dp)
                    .glanceClickable(actionStartActivity(intent))
            ) {
                GlanceText(
                    text = course.name + if (!course.credits.isNullOrBlank()) "（${course.credits}）" else "",
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(palette.text), fontSize = 11.sp, fontWeight = GlanceFontWeight.Bold)
                )
                GlanceSpacer(modifier = GlanceModifier.glanceHeight(2.dp))
                GlanceText(
                    text = infoText,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(palette.text.copy(alpha = 0.8f)), fontSize = 9.sp)
                )
            }
            GlanceSpacer(modifier = GlanceModifier.glanceHeight(4.dp))
        }

        if (todayCourses.size > maxShow) {
            GlanceText(
                text = "... 还有 ${todayCourses.size - maxShow} 节",
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.5f)), fontSize = 9.sp)
            )
        }

        if (todayCourses.isEmpty()) {
            GlanceSpacer(modifier = GlanceModifier.defaultWeight())
            GlanceText(
                text = "好好休息喵~",
                style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.4f)), fontSize = 11.sp, textAlign = GlanceTextAlign.Center),
                modifier = GlanceModifier.glanceFillMaxWidth()
            )
        }
    }
}

class CourseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget()
}

class CourseWidget2x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CourseWidget2x2()
}
