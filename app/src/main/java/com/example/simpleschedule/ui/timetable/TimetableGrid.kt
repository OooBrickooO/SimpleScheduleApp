package com.example.simpleschedule.ui.timetable

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.data.local.room.DisplayCourse
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.domain.ScheduleResolver
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.ui.theme.getCoursePalette
import com.example.simpleschedule.utils.getDateByWeekAndDay
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("MissingPermission")
@Composable
fun TimetableGrid(
    viewModel: ScheduleViewModel,
    coursesForThisPage: List<Pair<DisplayCourse, Boolean>>,
    timeNodes: List<TimeNode>,
    isDark: Boolean,
    bottomBlank: Boolean,
    startDate: String,
    displayedWeek: Int,
    materialYou: Boolean,
    makeUpDayMap: Map<Int, MakeUpRule> = emptyMap(),
    holidayDayMap: Map<Int, MakeUpRule> = emptyMap(),
    onDayLongPress: (Int, String) -> Unit = { _, _ -> },
    onCourseClick: (DisplayCourse, Boolean) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val accent = LocalAccentColor.current ?: MaterialTheme.colorScheme.primary
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
    val courseColorPool by viewModel.courseColorPool.collectAsState()

    val allDays = remember { listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }
    val todayDateStr = remember { ScheduleResolver.formatDate(Calendar.getInstance()) }

    val visualColMap = remember(showSat, showSun) {
        mutableMapOf<Int, Int>().apply {
            var colIdx = 0
            for (day in 1..7) {
                if ((day == 6 && !showSat) || (day == 7 && !showSun)) continue
                put(day, colIdx++)
            }
        }
    }
    val daysCount = remember(visualColMap) { visualColMap.size.coerceAtLeast(1) }

    val timeColWidthDp = 45.dp
    val maxRow = timeNodes.size.coerceAtLeast(1)
    val scrollState = rememberScrollState()
    var draggingCourseId by remember { mutableStateOf<String?>(null) }

    val (dateList, currentMonth) = remember(startDate, displayedWeek) {
        val dates = mutableListOf<Int>()
        var monthVal = 1
        val calendar = Calendar.getInstance()
        try {
            if (startDate.isNotEmpty()) {
                calendar.time = ScheduleResolver.parseDate(startDate) ?: Date()
                while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                calendar.add(Calendar.DAY_OF_YEAR, (displayedWeek - 1) * 7)
                monthVal = calendar.get(Calendar.MONTH) + 1
                for (i in 0..6) {
                    dates.add(calendar.get(Calendar.DAY_OF_MONTH))
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                repeat(7) { dates.add(1) }
            }
        } catch (_: Exception) {
            dates.clear()
            repeat(7) { dates.add(1) }
        }
        Pair(dates, monthVal)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(timeColWidthDp).padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "$currentMonth\n月",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            for (day in 1..7) {
                if (visualColMap.containsKey(day)) {
                    val isMakeUp = makeUpDayMap.containsKey(day)
                    val isHoliday = holidayDayMap.containsKey(day)
                    val dayDateStr = getDateByWeekAndDay(displayedWeek, day, startDate) ?: ""
                    val isToday = dayDateStr.isNotEmpty() && dayDateStr == todayDateStr

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (dayDateStr.isNotEmpty()) {
                                        if (vibration) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                runCatching { vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)) }
                                            } else {
                                                @Suppress("DEPRECATION")
                                                runCatching { vibrator.vibrate(50) }
                                            }
                                        }
                                        onDayLongPress(day, dayDateStr)
                                    }
                                }
                            )
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            allDays[day - 1],
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) accent else textColor.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val displayDate = if (dateList.size == 7) dateList[day - 1].toString() else "-"
                        Text(
                            displayDate,
                            fontSize = 14.sp,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isToday) accent else textColor.copy(alpha = 0.8f)
                        )
                        if (isMakeUp) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, accent)
                            ) {
                                Text(
                                    text = "调休",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isHoliday) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626))
                            ) {
                                Text(
                                    text = "放假",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalHeight = cellHeightDp.dp * maxRow + if (bottomBlank) 150.dp else 0.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState, enabled = draggingCourseId == null)
        ) {
            Column {
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

                    makeUpDayMap.keys.forEach { makeUpDay ->
                        val mappedCol = visualColMap[makeUpDay]
                        if (mappedCol != null) {
                            val colX = timeColWidthDp + colWidthDp * mappedCol
                            Box(
                                modifier = Modifier
                                    .offset(x = colX, y = 0.dp)
                                    .size(width = colWidthDp, height = cellHeightDp.dp * maxRow)
                                    .background(accent.copy(alpha = 0.04f), RoundedCornerShape(cornerRadiusDp.dp))
                                    .border(
                                        width = 1.dp,
                                        color = accent.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(cornerRadiusDp.dp)
                                    )
                            )
                        }
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

                    val latestColWidthPx by rememberUpdatedState(colWidthPx)
                    val latestCellHeightPx by rememberUpdatedState(cellHeightPx)
                    val latestDaysCount by rememberUpdatedState(daysCount)
                    val latestVisualColMap by rememberUpdatedState(visualColMap)
                    val latestVibration by rememberUpdatedState(vibration)

                    coursesForThisPage.forEach { (displayCourse, isFuture) ->
                        val mappedCol = visualColMap[displayCourse.displayDay]
                        if (mappedCol != null) {
                            key(displayCourse.course.id, displayCourse.displayDay, isFuture) {
                                val course = displayCourse.course
                                val palette = getCoursePalette(course.colorTheme, isDark, materialYou, course.name, courseColorPool)
                                var dragOffset by remember { mutableStateOf(Offset.Zero) }
                                var isDragging by remember { mutableStateOf(false) }

                                val isMakeUpCourse = makeUpDayMap.containsKey(displayCourse.displayDay)
                                val latestMappedCol by rememberUpdatedState(mappedCol)
                                val latestDisplayDay by rememberUpdatedState(displayCourse.displayDay)
                                val latestCourse by rememberUpdatedState(course)
                                val latestIsMakeUpCourse by rememberUpdatedState(isMakeUpCourse)

                                val baseOffsetX = timeColWidthDp + colWidthDp * mappedCol
                                val baseOffsetY = cellHeightDp.dp * (displayCourse.displayStartNode - 1)
                                val cardHeight = cellHeightDp.dp * (displayCourse.displayEndNode - displayCourse.displayStartNode + 1)

                                var isVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(course.id) { isVisible = true }
                                val alphaAnim by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(320), label = "alpha")
                                val scaleAnim by animateFloatAsState(targetValue = if (isVisible) 1f else 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

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
                                                    if (latestIsMakeUpCourse) {
                                                        Toast.makeText(context, "调休补课日的课程由原日期映射，请在原日期调整或取消调休喵", Toast.LENGTH_SHORT).show()
                                                        return@detectDragGesturesAfterLongPress
                                                    }
                                                    isDragging = true
                                                    draggingCourseId = latestCourse.id
                                                    if (latestVibration && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        runCatching { vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)) }
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (!isDragging) return@detectDragGesturesAfterLongPress
                                                    change.consume()
                                                    dragOffset += dragAmount
                                                },
                                                onDragEnd = {
                                                    if (!isDragging) return@detectDragGesturesAfterLongPress
                                                    isDragging = false
                                                    draggingCourseId = null
                                                    val deltaCols = (dragOffset.x / latestColWidthPx).roundToInt()
                                                    val deltaRows = (dragOffset.y / latestCellHeightPx).roundToInt()
                                                    if (deltaCols != 0 || deltaRows != 0) {
                                                        val targetCol = (latestMappedCol + deltaCols).coerceIn(0, latestDaysCount - 1)
                                                        val newDay = latestVisualColMap.entries.find { it.value == targetCol }?.key ?: latestDisplayDay
                                                        viewModel.updateCoursePosition(latestCourse.id, newDay - latestDisplayDay, deltaRows, latestCourse)
                                                    }
                                                    dragOffset = Offset.Zero
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    draggingCourseId = null
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
                                            val weeksList = ScheduleResolver.parseWeeks(course.weeks)
                                            if (isFuture) {
                                                val isOddOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 != 0 } && weeksList.size > 1
                                                val isEvenOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 == 0 } && weeksList.size > 1
                                                val badgeText = if (isOddOnly) "[非本周(单)]" else if (isEvenOnly) "[非本周(双)]" else "[非本周]"
                                                Text(badgeText, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 2.dp))
                                            }
                                            Text(
                                                text = course.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFuture) textColor.copy(alpha = 0.5f) else palette.text,
                                                lineHeight = 14.sp,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp).heightIn(max = cardHeight * 0.5f)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.LocationOn,
                                                    contentDescription = "Loc",
                                                    tint = if (isFuture) textColor.copy(alpha = 0.4f) else palette.text.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(10.dp).padding(top = 1.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = course.location,
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
                val bottomNavHeight = 96.dp
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(modifier = Modifier.height(bottomNavHeight))
            }
        }
    }
}
