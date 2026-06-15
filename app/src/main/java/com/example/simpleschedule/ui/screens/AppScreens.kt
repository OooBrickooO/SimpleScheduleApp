package com.example.simpleschedule.ui.screens

import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.room.*
import com.example.simpleschedule.ui.theme.*
import com.example.simpleschedule.utils.*
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import com.example.simpleschedule.receiver.ReminderEngine

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
import android.window.BackEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectDragGestures
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

@Composable
fun AppBackHandler(enabled: Boolean, onBack: () -> Unit): Modifier {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        var scale by remember { mutableStateOf(1f) }
        var translateX by remember { mutableStateOf(0f) }
        var cornerRadius by remember { mutableStateOf(0.dp) }

        PredictiveBackHandler { progress ->
            try {
                progress.collect { event ->
                    scale = 1f - (event.progress * 0.08f)
                    translateX = if (event.swipeEdge == BackEvent.EDGE_LEFT) {
                        event.progress * 56f
                    } else {
                        -event.progress * 56f
                    }
                    cornerRadius = (event.progress * 24).dp
                }
                onBack()
                scale = 1f
                translateX = 0f
                cornerRadius = 0.dp
            } catch (e: Exception) {
                scale = 1f
                translateX = 0f
                cornerRadius = 0.dp
            }
        }

        return Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = translateX
                if (cornerRadius > 0.dp) {
                    clip = true
                    shape = RoundedCornerShape(cornerRadius)
                }
            }
    } else {
        BackHandler(enabled = true, onBack = onBack)
        return Modifier
    }
}

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
            .navigationBarsPadding()
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
    var scheduleIdToDelete by remember { mutableStateOf<String?>(null) }
    var scheduleNameToDelete by remember { mutableStateOf<String?>(null) }


    val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }
    val currentScheduleName = currentSchedule?.name ?: "课表"
    val startDate = currentSchedule?.startDate ?: ""
    val pagerState = rememberPagerState(initialPage = (currentWeek - 1).coerceAtLeast(0), pageCount = { totalWeeks.coerceAtLeast(1) })

    val showNotThisWeek by viewModel.showNotThisWeek.collectAsState()
    val bottomBlank by viewModel.bottomBlank.collectAsState()
    val materialYou by viewModel.materialYou.collectAsState()

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
                        DropdownMenuItem(text = { Text("从教务导入课表/考试", fontWeight = FontWeight.Bold, color = textColor) }, leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null, tint = textColor) }, onClick = {
                            showAddMenu = false
                            onWebViewImportClick()
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
                materialYou = materialYou,
                onCourseClick = { course, isFuture -> selectedCourseWithWeek = Pair(course, if (isFuture) 0 else weekForThisPage) }
            )
        }
    }



    if (selectedCourseWithWeek != null) {
        val (course, clickedWeek) = selectedCourseWithWeek!!
        CourseDetailDialog(
            displayCourse = course,
            clickedWeek = clickedWeek,
            isDark = isDark,
            materialYou = materialYou,
            viewModel = viewModel,
            onDismiss = { selectedCourseWithWeek = null },
            onDelete = { viewModel.deleteCourse(course.course.id); selectedCourseWithWeek = null },
            onEdit = { onEditCourse(course.course); selectedCourseWithWeek = null }
        )
    }

    if (scheduleIdToDelete != null) {
        DeleteScheduleDialog(
            isDark = isDark,
            scheduleName = scheduleNameToDelete ?: "课表",
            onDismiss = { scheduleIdToDelete = null; scheduleNameToDelete = null },
            onConfirm = {
                val targetId = scheduleIdToDelete!!
                if (scheduleGroups.size <= 1) {
                    Toast.makeText(context, "必须至少保留一个课表喵！", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.deleteSchedule(targetId)
                }
                scheduleIdToDelete = null; scheduleNameToDelete = null
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
            onDeleteScheduleClick = { id, name -> scheduleIdToDelete = id; scheduleNameToDelete = name }
        )
    }
}

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

    val allDays = remember { listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }

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

    val (dateList, currentMonth) = remember(startDate, displayedWeek) {
        val dates = mutableListOf<Int>()
        var monthVal = 1
        val calendar = Calendar.getInstance()
        try {
            if (startDate.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.time = sdf.parse(startDate) ?: Date()
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
        } catch (e: Exception) {
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
                        val palette = getCoursePalette(course.colorTheme, isDark, materialYou)
                        var dragOffset by remember { mutableStateOf(Offset.Zero) }
                        var isDragging by remember { mutableStateOf(false) }

                        val baseOffsetX = timeColWidthDp + colWidthDp * mappedCol
                        val baseOffsetY = cellHeightDp.dp * (displayCourse.displayStartNode - 1)
                        val cardHeight = cellHeightDp.dp * (displayCourse.displayEndNode - displayCourse.displayStartNode + 1)

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
                                                try { vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (e: Exception) {}
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
    val predictiveBack by viewModel.predictiveBack.collectAsState()
    val backModifier = AppBackHandler(predictiveBack) { onBack() }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
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
    val predictiveBack by viewModel.predictiveBack.collectAsState()
    val backModifier = AppBackHandler(predictiveBack) { onBack() }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
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
    val dynamicIslandEnabled by viewModel.dynamicIslandEnabled.collectAsState()
    val liveUpdateEnabled by viewModel.liveUpdateEnabled.collectAsState()
    val predictiveBack by viewModel.predictiveBack.collectAsState()
    val backModifier = AppBackHandler(predictiveBack) { onBack() }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
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
                            subtext = "在系统后台自动计算下一节课的时间，并在上课前通过闹钟准时触发提醒。需确保已授予后台允许及精确闹钟权限",
                            checked = reminderEnabled,
                            onCheckedChange = { checked ->
                                viewModel.updateSetting(SettingsKeys.REMINDER_ENABLED, checked)
                                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                                    if (!alarmManager.canScheduleExactAlarms()) {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            })
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                        }
                                    }
                                }
                            },
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
                                    "提示：如果遇到上一门课上完了而接下来还有课，且课间休息短于您设置的提前时间，系统将在上节课刚下课时立刻为您播报",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.6f),
                                    lineHeight = 14.sp
                                )
                            }

                            SettingCheckboxItem(
                                title = "显示通知栏提醒",
                                checked = reminderNotifyEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.updateSetting(SettingsKeys.REMINDER_NOTIFY_ENABLED, checked)
                                    if (checked) {
                                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
                                        } else {
                                            notificationManager.areNotificationsEnabled()
                                        }
                                        if (!isGranted) {
                                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                }
                                            } else {
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.parse("package:${context.packageName}")
                                                }
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                },
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

                            SettingCheckboxItem(
                                title = "开启课前提醒通知",
                                checked = dynamicIslandEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.updateSetting(SettingsKeys.DYNAMIC_ISLAND_ENABLED, checked)
                                },
                                textColor = textColor,
                                borderColor = borderColor,
                                isDark = isDark,
                                showBottomBorder = dynamicIslandEnabled
                            )

                            if (dynamicIslandEnabled) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .background(
                                            if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(4.dp)
                                ) {
                                    SettingCheckboxItemWithSubtext(
                                        title = "使用实时通知",
                                        subtext = "需要授予通知权限。实时通知仅在Android 16+ 设备上生效，低于Android16的设备开启后无效果。若不开启实时知，则使用传统的通知栏常驻通知",
                                        checked = liveUpdateEnabled,
                                        onCheckedChange = { viewModel.updateSetting(SettingsKeys.LIVE_UPDATE_ENABLED, it) },
                                        textColor = textColor,
                                        borderColor = Color.Transparent,
                                        isDark = isDark,
                                        showBottomBorder = false
                                    )
                                }
                            }

                            SettingValueItem(title = "发送一条测试提醒", value = "5秒后触发", showBottomBorder = false, textColor = textColor, borderColor = borderColor, onClick = {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    Toast.makeText(context, "请先允许精确闹钟权限喵", Toast.LENGTH_SHORT).show()
                                } else {
                                    val triggerTime = System.currentTimeMillis() + 5000
                                    val classStartTime = triggerTime + reminderAdvanceMins * 60000
                                    ReminderEngine.scheduleAlarmByParams(context, "【测试】课A", "地球", "Start-End", triggerTime, classStartTime, reminderNotifyEnabled, reminderVoiceEnabled, "slate")
                                    Toast.makeText(context, "已设置 5 秒后的测试提醒喵！请切出应用等待", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }


    // 悬浮窗权限引导已因原生状态栏胶囊方案而移除

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
    val predictiveBack by viewModel.predictiveBack.collectAsState()
    val backModifier = AppBackHandler(predictiveBack) { onBack() }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
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
                        SettingCheckboxItemWithSubtext(
                            title = "预见式返回",
                            subtext = " 仅在Android 14 以上的设备生效，在支持的设备上侧滑返回时可预览上一个界面",
                            checked = predictiveBack,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.PREDICTIVE_BACK, it) },
                            textColor = textColor, borderColor = borderColor, isDark = isDark
                        )
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

data class School(
    val name: String,
    val type: String, // "zf" or "qg"
    val initial: String, // "A"-"Z"
    val url: String
)

fun loadSchoolsFromAssets(context: Context): List<School> {
    return try {
        val jsonStr = context.assets.open("schools.json").bufferedReader().use { it.readText() }
        val jsonArray = org.json.JSONArray(jsonStr)
        val list = mutableListOf<School>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                School(
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    initial = obj.getString("initial"),
                    url = obj.optString("url", "")
                )
            )
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

enum class ImportStage {
    SELECT_MODE,
    WEB_IMPORT,
    XLS_IMPORT
}

@Composable
fun SystemCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun SchoolItem(
    school: School,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(width = 0.5.dp, color = borderColor.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = school.name,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        val badgeBg = if (school.type == "zf") Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
        val badgeText = if (school.type == "zf") Color(0xFF3B82F6) else Color(0xFFD97706)
        val badgeLabel = if (school.type == "zf") "正方" else "青果"
        
        Box(
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = badgeLabel,
                color = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun executeZhengfangImportJs(loadUrl: String, webViewRef: WebView?) {
    val jsCode = if (loadUrl.contains("lzjtu")) {
        """
        javascript:(function() {
            try {
                var courses = [];
                var colorList = ['blue', 'pink', 'purple', 'slate', 'indigo', 'rose'];
                var nodes = document.querySelectorAll('.kbcontent, .kbcontent1');

                if (nodes.length === 0) {
                    window.Android.passData("ERROR:未找到课程节点，请确保已进入课表页面！");
                    return;
                }

                nodes.forEach(function(node) {
                    var idStr = node.id;
                    if (!idStr) return;
                    var parts = idStr.split('-');
                    if (parts.length < 3) return;

                    var nameNode = node.querySelector('a');
                    if (!nameNode) return;
                    var name = nameNode.innerText.trim();
                    if (!name) return;

                    var teacherNode = node.querySelector('font[title="教师"]');
                    var teacher = teacherNode ? teacherNode.innerText.trim() : '';

                    var locationNode = node.querySelector('font[title="教室"]');
                    var location = locationNode ? locationNode.innerText.trim() : '未排地点';

                    var timeNode = node.querySelector('font[title="周次(节次)"]');
                    if (!timeNode) return;
                    var timeText = timeNode.innerText.trim();

                    var weekStr = timeText.split('(周)')[0];
                    var sectionStr = timeText.match(/\[(\d+)-(\d+)节\]/);
                    var startNode = 1, endNode = 2;
                    if (sectionStr) {
                        startNode = parseInt(sectionStr[1], 10);
                        endNode = parseInt(sectionStr[2], 10);
                    } else {
                        var tr = node.closest('tr');
                        if(tr) {
                            var th = tr.querySelector('th');
                            if(th && th.innerText.match(/\((\d+),(\d+)小节\)/)) {
                                var m = th.innerText.match(/\((\d+),(\d+)小节\)/);
                                startNode = parseInt(m[1], 10);
                                endNode = parseInt(m[2], 10);
                            }
                        }
                    }
                    
                    var dayOfWeek = parseInt(parts[1], 10);

                    var weeksArr = [];
                    var weekParts = weekStr.split(',');
                    weekParts.forEach(function(p) {
                        var rangeMatch = p.match(/(\d+)-(\d+)/);
                        if (rangeMatch) {
                            for(var i = parseInt(rangeMatch[1], 10); i <= parseInt(rangeMatch[2], 10); i++) {
                                weeksArr.push(i);
                            }
                        } else {
                            var singleMatch = p.match(/(\d+)/);
                            if (singleMatch) {
                                weeksArr.push(parseInt(singleMatch[1], 10));
                            }
                        }
                    });

                    if (weeksArr.length === 0) return;

                    var nameHash = 0;
                    for(var i=0; i<name.length; i++) nameHash += name.charCodeAt(i);
                    var colorTheme = colorList[nameHash % colorList.length];

                    courses.push({
                        name: name,
                        location: location,
                        teacher: teacher,
                        dayOfWeek: dayOfWeek,
                        startNode: startNode,
                        endNode: endNode,
                        weeks: JSON.stringify(weeksArr),
                        colorTheme: colorTheme
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
    } else {
        """
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
    }
    webViewRef?.evaluateJavascript(jsCode, null)
}

fun executeZhengfangExamJs(webViewRef: WebView?) {
    val jsCode = """
    javascript:(function() {
        try {
            var grid = document.getElementById("tabGrid") || document.querySelector("table.ui-jqgrid-btable");
            if (!grid) {
                window.Android.passData("ERROR:未找到考试数据表格，请确保已进入「考试信息查询」页面且已加载完成！");
                return;
            }
            window.Android.passExamData(grid.outerHTML);
        } catch (e) {
            window.Android.passData("ERROR:提取异常:" + e.message);
        }
    })();
    """.trimIndent()
    webViewRef?.evaluateJavascript(jsCode, null)
}

@Composable
fun WebViewImportScreen(
    viewModel: ScheduleViewModel,
    isDark: Boolean,
    activeTimeNodes: List<TimeNode>,
    startDate: String,
    hasCourses: Boolean,
    onBack: () -> Unit,
    onImport: (String) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val predictiveBack by viewModel.predictiveBack.collectAsState()

    var stage by remember { mutableStateOf(ImportStage.SELECT_MODE) }
    var selectedSchool by remember { mutableStateOf<School?>(null) }
    var selectedSystem by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: 按教务分类, 1: 按学校
    var searchQuery by remember { mutableStateOf("") }

    var url by remember { mutableStateOf("https://jwxt.cjlu.edu.cn/") }
    var loadUrl by remember { mutableStateOf("https://jwxt.cjlu.edu.cn/") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    var showBlockDialog by remember { mutableStateOf(false) }
    var showConfirmExamDialog by remember { mutableStateOf(false) }

    val schools = remember { loadSchoolsFromAssets(context) }
    val listState = rememberLazyListState()

    val xlsPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val jsonData = parseQingGuoXls(inputStream)
                    if (jsonData != null && jsonData != "[]") {
                        onImport(jsonData)
                    } else {
                        Toast.makeText(context, "未提取到课程，请检查XLS文件格式", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取或解析文件失败: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    val backModifier = AppBackHandler(predictiveBack) {
        if (stage != ImportStage.SELECT_MODE) {
            stage = ImportStage.SELECT_MODE
        } else if (selectedSystem != null) {
            selectedSystem = null
        } else {
            onBack()
        }
    }

    val filteredSchools = remember(schools, searchQuery, activeTab, selectedSystem) {
        val filtered = schools.filter { school ->
            val matchesSearch = school.name.contains(searchQuery, ignoreCase = true)
            val matchesTab = if (activeTab == 0) {
                if (selectedSystem == "zf") school.type == "zf"
                else if (selectedSystem == "qg") school.type == "qg"
                else false
            } else {
                true
            }
            matchesSearch && matchesTab
        }
        filtered.sortedWith(compareBy({ it.initial }, { it.name }))
    }

    val listItems = remember(filteredSchools) {
        val items = mutableListOf<Any>()
        filteredSchools.groupBy { it.initial }.forEach { (initial, schoolsForInitial) ->
            items.add(initial)
            items.addAll(schoolsForInitial)
        }
        items
    }

    val letterToFirstIndex = remember(listItems) {
        val map = mutableMapOf<Char, Int>()
        listItems.forEachIndexed { index, item ->
            if (item is String && item.length == 1) {
                val char = item[0].uppercaseChar()
                if (!map.containsKey(char)) {
                    map[char] = index
                }
            }
        }
        map
    }

    val alphabet = ('A'..'Z').toList()

    Column(modifier = Modifier.fillMaxSize().imePadding().then(backModifier)) {
        when (stage) {
            ImportStage.SELECT_MODE -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier
                            .clickable {
                                if (selectedSystem != null) {
                                    selectedSystem = null
                                } else {
                                    onBack()
                                }
                            }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedSystem == "zf") "正方教务学校" else if (selectedSystem == "qg") "青果教务学校" else "选择导入方式",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedSystem == null) {
                    Text(
                        text = "常用学校",
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val commonSchoolsList = listOf(
                            School("中国计量大学", "zf", "Z", "https://jwxt.cjlu.edu.cn/"),
                            School("湖州师范大学", "zf", "H", "http://syjw.zjhu.edu.cn/"),
                            School("兰州交通大学", "qg", "L", "")
                        )
                        commonSchoolsList.forEach { s ->
                            Button(
                                onClick = {
                                    selectedSchool = s
                                    if (s.type == "qg") {
                                        stage = ImportStage.XLS_IMPORT
                                    } else {
                                        url = s.url
                                        loadUrl = s.url
                                        stage = ImportStage.WEB_IMPORT
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                                    contentColor = textColor
                                )
                            ) {
                                Text(
                                    text = s.name.replace("大学", ""),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("按教务分类", "按学校").forEachIndexed { index, title ->
                            val selected = activeTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) {
                                            if (isDark) Color(0xFF2C2C2E) else Color.White
                                        } else Color.Transparent
                                    )
                                    .clickable { activeTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (selected) textColor else textColor.copy(alpha = 0.6f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (activeTab == 0 && selectedSystem == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SystemCard(
                            title = "正方教务系统",
                            description = "最常见的教务系统，支持在线登录并直接提取课表与考试安排。",
                            icon = Icons.Rounded.Language,
                            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                            onClick = { selectedSystem = "zf" }
                        )
                        SystemCard(
                            title = "青果教务系统",
                            description = "青果系统暂不支持网页端一键提取，统一通过在电脑端导出为 .xls 格式文件后导入。",
                            icon = Icons.Rounded.Description,
                            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                            onClick = {
                                selectedSchool = School("青果教务", "qg", "Q", "")
                                stage = ImportStage.XLS_IMPORT
                            }
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (activeTab == 0 && selectedSystem == "zf") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "手动输入正方教务网址：",
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = url,
                                            onValueChange = { url = it },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            singleLine = true,
                                            placeholder = { Text("https://...", fontSize = 13.sp) },
                                            textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 13.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                                                focusedTextColor = textColor,
                                                unfocusedTextColor = textColor
                                            )
                                        )
                                        Button(
                                            onClick = {
                                                loadUrl = url
                                                selectedSchool = null
                                                stage = ImportStage.WEB_IMPORT
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) BgDark else BgLight),
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Text("进入", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("搜索学校名称...", color = textColor.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = textColor.copy(alpha = 0.5f)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = null, tint = textColor.copy(alpha = 0.5f))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = borderColor,
                                unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = textColor
                            )
                        )

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                items(listItems) { item ->
                                    if (item is String && item.length == 1) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = item,
                                                color = textColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else if (item is School) {
                                        SchoolItem(
                                            school = item,
                                            isDark = isDark,
                                            onClick = {
                                                selectedSchool = item
                                                if (item.type == "qg") {
                                                    stage = ImportStage.XLS_IMPORT
                                                } else {
                                                    if (item.url.isNotEmpty()) {
                                                        url = item.url
                                                        loadUrl = item.url
                                                    } else {
                                                        val query = android.net.Uri.encode("${item.name}教务系统")
                                                        val searchUrl = "https://m.baidu.com/s?word=$query"
                                                        url = searchUrl
                                                        loadUrl = searchUrl
                                                        Toast.makeText(context, "已自动搜索该校教务系统链接，请在页面中点击进入", Toast.LENGTH_LONG).show()
                                                    }
                                                    stage = ImportStage.WEB_IMPORT
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            var alphabetHeight by remember { mutableStateOf(1) }
                            Column(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .onGloballyPositioned { coordinates ->
                                        alphabetHeight = coordinates.size.height
                                    }
                                    .pointerInput(letterToFirstIndex) {
                                        detectDragGestures { change, _ ->
                                            val y = change.position.y
                                            val index = ((y / alphabetHeight) * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                                            val letter = alphabet[index]
                                            val targetIndex = alphabet.filter { it >= letter }.mapNotNull { letterToFirstIndex[it] }.firstOrNull()
                                            if (targetIndex != null) {
                                                coroutineScope.launch { listState.scrollToItem(targetIndex) }
                                            }
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                alphabet.forEach { letter ->
                                    val hasSchools = letterToFirstIndex.containsKey(letter)
                                    Text(
                                        text = letter.toString(),
                                        color = if (hasSchools) textColor else textColor.copy(alpha = 0.25f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable(enabled = hasSchools) {
                                                letterToFirstIndex[letter]?.let { index ->
                                                    coroutineScope.launch { listState.scrollToItem(index) }
                                                }
                                            }
                                            .padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ImportStage.WEB_IMPORT -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier
                            .clickable { stage = ImportStage.SELECT_MODE }
                            .padding(8.dp)
                    )
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            url = "https://jwxt.cjlu.edu.cn/"
                            loadUrl = url
                        },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("计量大学", fontSize = 12.sp, color = textColor)
                    }
                    OutlinedButton(
                        onClick = {
                            url = "http://syjw.zjhu.edu.cn/"
                            loadUrl = url
                        },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("湖州师范", fontSize = 12.sp, color = textColor)
                    }
                    OutlinedButton(
                        onClick = {
                            stage = ImportStage.XLS_IMPORT
                        },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("兰州交大", fontSize = 12.sp, color = textColor)
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
                    AndroidView(
                        factory = { ctx ->
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

                                    @JavascriptInterface
                                    fun passExamData(html: String) {
                                        (ctx as? ComponentActivity)?.runOnUiThread {
                                            val (jsonData, errorMsg) = parseZhengfangExamHtml(
                                                html.byteInputStream(),
                                                activeTimeNodes,
                                                startDate
                                            )
                                            if (errorMsg != null) {
                                                Toast.makeText(ctx, errorMsg, Toast.LENGTH_LONG).show()
                                            } else if (jsonData != null && jsonData != "[]") {
                                                onImport(jsonData)
                                            } else {
                                                Toast.makeText(ctx, "未提取到任何有效的考试安排", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }, "Android")
                                webViewRef = this
                            }
                        },
                        update = { it.loadUrl(loadUrl) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (hasCourses) {
                                showBlockDialog = true
                            } else {
                                executeZhengfangImportJs(loadUrl, webViewRef)
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) BgDark else BgLight)
                    ) {
                        Text("提取教务课表", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (hasCourses) {
                                showConfirmExamDialog = true
                            } else {
                                executeZhengfangExamJs(webViewRef)
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF3F4F6),
                            contentColor = textColor
                        ),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text("提取考试安排", fontWeight = FontWeight.Bold)
                    }
                }
            }

            ImportStage.XLS_IMPORT -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor,
                        modifier = Modifier
                            .clickable { stage = ImportStage.SELECT_MODE }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedSchool != null) "${selectedSchool!!.name}导入" else "本地 XLS 导入",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val promptText = if (selectedSchool != null) {
                        "由于「${selectedSchool!!.name}」使用的是青果教务系统，受环境限制，请先在电脑端或浏览器中导出课表为 .xls 文件，发送到手机后点击下方按钮导入。"
                    } else {
                        "由于该校教务系统环境限制，请在电脑端或浏览器自行导出课表为 .xls 文件，发送到手机后通过下方按钮导入。"
                    }
                    Text(
                        text = promptText,
                        color = textColor,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Button(
                        onClick = {
                            if (hasCourses) {
                                showBlockDialog = true
                            } else {
                                xlsPickerLauncher.launch("application/vnd.ms-excel")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = if (isDark) BgDark else BgLight)
                    ) {
                        Text("选择 XLS 文件并导入", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showBlockDialog) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text("无法导入", fontWeight = FontWeight.Bold) },
                text = { Text("当前课表中已经有数据啦！\n为了避免数据混乱叠加在一起，请先去【新建一个空白的课表】，然后再进行教务导入哦喵！") },
                confirmButton = {
                    TextButton(onClick = { showBlockDialog = false }) {
                        Text("知道啦", color = textColor, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                titleContentColor = textColor,
                textContentColor = textColor.copy(alpha = 0.8f)
            )
        }

        if (showConfirmExamDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmExamDialog = false },
                title = { Text("确认导入考试", fontWeight = FontWeight.Bold) },
                text = { Text("检测到您当前课表中已经有课程。导入考试安排将作为日程叠加在现有课表上，是否确认继续导入？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmExamDialog = false
                            executeZhengfangExamJs(webViewRef)
                        }
                    ) {
                        Text("确认导入", color = textColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmExamDialog = false }) {
                        Text("取消", color = textColor.copy(alpha = 0.6f))
                    }
                },
                containerColor = if (isDark) Color(0xFF18181B) else Color.White,
                titleContentColor = textColor,
                textContentColor = textColor.copy(alpha = 0.8f)
            )
        }
    }
}


fun parseQingGuoXls(inputStream: InputStream): String? {
    try {
        val workbook = Workbook.getWorkbook(inputStream)
        val sheet = workbook.getSheet(0)
        val courses = mutableListOf<Map<String, Any>>()
        val colorList = listOf("blue", "pink", "purple", "slate", "indigo", "rose")

        for (row in 0 until sheet.rows) {
            val timeHeader = sheet.getCell(0, row).contents ?: ""
            if (timeHeader.isBlank() && row > 0) continue

            for (col in 1..7) {
                if (col >= sheet.columns) break
                val cellContent = sheet.getCell(col, row).contents ?: continue
                if (cellContent.isBlank() || cellContent.length < 5) continue

                val lines = cellContent.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                var i = 0
                while (i < lines.size) {
                    val name = lines[i]
                    var teacher = ""
                    var timeStr = ""
                    var location = "未排地点"

                    var timeIndex = -1
                    for (j in i + 1 until lines.size) {
                        if (lines[j].contains("周") && Regex("\\d").containsMatchIn(lines[j])) {
                            timeIndex = j
                            break
                        }
                    }

                    if (timeIndex != -1) {
                        timeStr = lines[timeIndex]
                        if (timeIndex > i + 1) teacher = lines[timeIndex - 1]
                        if (timeIndex + 1 < lines.size && !lines[timeIndex + 1].contains("周")) {
                            location = lines[timeIndex + 1]
                            i = timeIndex + 2
                        } else {
                            i = timeIndex + 1
                        }

                        var startNode = 1
                        var endNode = 2
                        val weeksArr = mutableListOf<Int>()

                        val weekStr = timeStr.substringBefore("(周)").substringBefore("([")
                        val parts = weekStr.split(",")
                        for (p in parts) {
                            val rangeMatch = Regex("(\\d+)-(\\d+)").find(p)
                            if (rangeMatch != null) {
                                val (s, e) = rangeMatch.destructured
                                for (w in s.toInt()..e.toInt()) weeksArr.add(w)
                            } else {
                                val singleMatch = Regex("(\\d+)").find(p)
                                if (singleMatch != null) {
                                    weeksArr.add(singleMatch.groupValues[1].toInt())
                                }
                            }
                        }

                        val sectionMatch = Regex("\\[(\\d+)-(\\d+)").find(timeStr)
                        if (sectionMatch != null) {
                            val (s, e) = sectionMatch.destructured
                            startNode = s.toInt()
                            endNode = e.toInt()
                        }

                        if (weeksArr.isNotEmpty() && name.isNotBlank()) {
                            var nameHash = 0
                            for (char in name) nameHash += char.code
                            val colorTheme = colorList[nameHash % colorList.size]

                            courses.add(mapOf(
                                "name" to name,
                                "location" to location,
                                "teacher" to teacher,
                                "dayOfWeek" to col,
                                "startNode" to startNode,
                                "endNode" to endNode,
                                "weeks" to weeksArr.toString(),
                                "colorTheme" to colorTheme
                            ))
                        }
                    } else {
                        i++
                    }
                }
            }
        }
        workbook.close()

        val sb = java.lang.StringBuilder()
        sb.append("[")
        for ((index, c) in courses.withIndex()) {
            sb.append("{")
            sb.append("\"name\":\"${c["name"].toString().replace("\"", "\\\"")}\",")
            sb.append("\"location\":\"${c["location"].toString().replace("\"", "\\\"")}\",")
            sb.append("\"teacher\":\"${c["teacher"].toString().replace("\"", "\\\"")}\",")
            sb.append("\"dayOfWeek\":${c["dayOfWeek"]},")
            sb.append("\"startNode\":${c["startNode"]},")
            sb.append("\"endNode\":${c["endNode"]},")
            sb.append("\"weeks\":\"${c["weeks"]}\",")
            sb.append("\"colorTheme\":\"${c["colorTheme"]}\"")
            sb.append("}")
            if (index < courses.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
fun TimetableListScreen(timetables: List<TimetableGroup>, currentLinkedId: String, isDark: Boolean, predictiveBack: Boolean, onBack: () -> Unit, onSelect: (String) -> Unit, onEdit: (String?) -> Unit, onDelete: (String) -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White

    val backModifier = AppBackHandler(predictiveBack) { onBack() }
    var timetableIdToDelete by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().then(backModifier)) {
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
                                    DropdownMenuItem(text = { Text("🗑️ 删除时间表", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) }, onClick = { showMenu = false; timetableIdToDelete = tGroup.id })
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp).navigationBarsPadding()) }
            }
        }

        if (timetableIdToDelete != null) {
            val targetGroup = timetables.find { it.id == timetableIdToDelete }
            AlertDialog(
                onDismissRequest = { timetableIdToDelete = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除时间表「${targetGroup?.name ?: ""}」吗？删除后将无法恢复，且可能影响与之关联的课表喵。", color = textColor) },
                confirmButton = {
                    Button(
                        onClick = {
                            val tid = timetableIdToDelete!!
                            timetableIdToDelete = null
                            onDelete(tid)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                    ) {
                        Text("确认删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { timetableIdToDelete = null }) {
                        Text("取消", color = textColor.copy(alpha = 0.6f))
                    }
                }
            )
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

    val predictiveBack by viewModel.predictiveBack.collectAsState()
    val backModifier = AppBackHandler(predictiveBack) { onBack() }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
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
fun CourseManagementScreen(courses: List<Course>, isDark: Boolean, materialYou: Boolean, predictiveBack: Boolean, onBack: () -> Unit, onEditCourse: (Course) -> Unit, onDeleteCourse: (String) -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val backModifier = AppBackHandler(predictiveBack) { onBack() }
    var courseIdToDelete by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
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
                val palette = getCoursePalette(course.colorTheme, isDark, materialYou)
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).animateContentSize().clip(RoundedCornerShape(8.dp)).background(palette.bg).border(0.5.dp, palette.border, RoundedCornerShape(8.dp)).pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { courseIdToDelete = course.id },
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

    if (courseIdToDelete != null) {
        val targetCourse = courses.find { it.id == courseIdToDelete }
        AlertDialog(
            onDismissRequest = { courseIdToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除课程「${targetCourse?.name ?: ""}」吗？删除后将无法恢复喵。", color = textColor) },
            confirmButton = {
                Button(
                    onClick = {
                        val cid = courseIdToDelete!!
                        courseIdToDelete = null
                        onDeleteCourse(cid)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { courseIdToDelete = null }) {
                    Text("取消", color = textColor.copy(alpha = 0.6f))
                }
            }
        )
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

@Composable
fun CourseDetailDialog(
    displayCourse: DisplayCourse,
    clickedWeek: Int,
    isDark: Boolean,
    materialYou: Boolean,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) BorderDark else BorderLight
    val course = displayCourse.course
    val palette = getCoursePalette(course.colorTheme, isDark, materialYou)
    val weeksList = course.weeks.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toIntOrNull() }

    val statistic by viewModel.getCourseStatistic(course.name).collectAsState(initial = null)
    val absenceCount = statistic?.absenceCount ?: 0
    val calledCount = statistic?.calledCount ?: 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier.fillMaxWidth().padding(16.dp).border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            var showLocationAlert by remember { mutableStateOf(false) }
            var showTeacherAlert by remember { mutableStateOf(false) }
            var showDeleteConfirm by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(24.dp)) {
                // Header with Color bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(palette.accent))
                    Spacer(modifier = Modifier.width(8.dp))

                    val isOddOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 != 0 } && weeksList.size > 1
                    val isEvenOnly = weeksList.isNotEmpty() && weeksList.all { it % 2 == 0 } && weeksList.size > 1
                    val modeText = if (isOddOnly) " (单周)" else if (isEvenOnly) " (双周)" else ""

                    Text(
                        text = if (clickedWeek > 0) "第 $clickedWeek 周$modeText" else "从第 ${weeksList.firstOrNull() ?: 1} 周开始$modeText",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(course.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor, lineHeight = 28.sp)
                Spacer(modifier = Modifier.height(20.dp))

                // Location and Teacher Info Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showLocationAlert = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5), contentColor = textColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = "Loc", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("上课地点", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { showTeacherAlert = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5), contentColor = textColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, contentDescription = "Teacher", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("授课教师", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showLocationAlert) {
                    AlertDialog(
                        onDismissRequest = { showLocationAlert = false },
                        title = { Text("上课地点") },
                        text = { Text(course.location.ifBlank { "未排地点" }, fontSize = 15.sp, color = textColor) },
                        confirmButton = {
                            TextButton(onClick = { showLocationAlert = false }) { Text("确定", color = textColor) }
                        }
                    )
                }

                if (showTeacherAlert) {
                    AlertDialog(
                        onDismissRequest = { showTeacherAlert = false },
                        title = { Text("授课教师") },
                        text = { Text(course.teacher.ifBlank { "无" }, fontSize = 15.sp, color = textColor) },
                        confirmButton = {
                            TextButton(onClick = { showTeacherAlert = false }) { Text("确定", color = textColor) }
                        }
                    )
                }

                if (!course.credits.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, borderColor).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "Credits", tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("学分: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), letterSpacing = 1.sp)
                        Text(course.credits, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(borderColor))
                Spacer(modifier = Modifier.height(16.dp))

                // Stats / Counters Section
                Text("课堂表现统计 (科目内共享)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Absence Increment Button
                    Button(
                        onClick = { viewModel.incrementAbsenceCount(course.name) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5), contentColor = textColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("旷课: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("$absenceCount 次", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Rounded.Add, contentDescription = "+1", modifier = Modifier.size(14.dp), tint = textColor.copy(alpha = 0.6f))
                        }
                    }
                    // Called Increment Button
                    Button(
                        onClick = { viewModel.incrementCalledCount(course.name) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5), contentColor = textColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("被点名: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("$calledCount 次", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Rounded.Add, contentDescription = "+1", modifier = Modifier.size(14.dp), tint = textColor.copy(alpha = 0.6f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom actions (EDIT / REMOVE)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor)
                    ) {
                        Text("编辑", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(0.5.dp, borderColor)
                    ) {
                        Text("删除", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("确认删除") },
                        text = { Text("确定要删除这门课程吗？删除后将无法恢复喵。", color = textColor) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                            ) {
                                Text("确认删除")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("取消", color = textColor.copy(alpha = 0.6f))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnnouncementDialog(title: String, content: String, isDark: Boolean, onDismiss: () -> Unit) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) BorderDark else BorderLight

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier.fillMaxWidth().padding(16.dp).border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = "Notification",
                        tint = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val lines = content.split("\n")
                    lines.forEach { line ->
                        if (line.trim().isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            val isHighlight = line.contains("官网") || line.contains("http") || 
                                              line.contains("https") || line.contains("重要") || 
                                              line.contains("⚠️") || line.contains("下载")
                            if (isHighlight) {
                                Surface(
                                    color = if (isDark) Color(0xFF3F2D0B) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = line,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = line,
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5),
                            contentColor = textColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp).fillMaxWidth()
                    ) {
                        Text("我知道了", fontWeight = FontWeight.Bold)
                    }
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
fun CourseEditDialog(
    isDark: Boolean,
    initialCourse: Course?,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String?, String, String, String, Int, Int, Int, String, String, String?, Int, Int) -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) BorderDark else BorderLight

    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var loc by remember { mutableStateOf(initialCourse?.location ?: "") }
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var credits by remember { mutableStateOf(initialCourse?.credits ?: "") }
    var dayOfWeek by remember { mutableStateOf(initialCourse?.dayOfWeek ?: 1) }
    var startNode by remember { mutableStateOf(initialCourse?.startNode ?: 1) }
    var endNode by remember { mutableStateOf(initialCourse?.endNode ?: 2) }
    val colorTheme by remember { mutableStateOf(initialCourse?.colorTheme ?: listOf("blue", "pink", "purple", "slate", "indigo", "rose").random()) }

    var absenceCount by remember { mutableIntStateOf(0) }
    var calledCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(name) {
        if (name.isNotBlank()) {
            val stats = viewModel.getCourseStatisticDirect(name)
            absenceCount = stats?.absenceCount ?: 0
            calledCount = stats?.calledCount ?: 0
        } else {
            absenceCount = 0
            calledCount = 0
        }
    }

    val initialParsedWeeks = remember(initialCourse) {
        initialCourse?.weeks?.removeSurrounding("[", "]")?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.sorted() ?: (1..16).toList()
    }

    var useShortcutMode by remember {
        mutableStateOf(
            initialCourse == null || run {
                val min = initialParsedWeeks.firstOrNull() ?: 1
                val max = initialParsedWeeks.lastOrNull() ?: 16
                val isAll = (min..max).toList() == initialParsedWeeks
                val isOdd = (min..max).filter { it % 2 != 0 } == initialParsedWeeks
                val isEven = (min..max).filter { it % 2 == 0 } == initialParsedWeeks
                isAll || isOdd || isEven
            }
        )
    }

    var startWeekStr by remember { mutableStateOf((initialParsedWeeks.firstOrNull() ?: 1).toString()) }
    var endWeekStr by remember { mutableStateOf((initialParsedWeeks.lastOrNull() ?: 16).toString()) }
    var weekType by remember {
        mutableStateOf(
            run {
                val min = initialParsedWeeks.firstOrNull() ?: 1
                val max = initialParsedWeeks.lastOrNull() ?: 16
                val isOdd = (min..max).filter { it % 2 != 0 } == initialParsedWeeks
                val isEven = (min..max).filter { it % 2 == 0 } == initialParsedWeeks
                if (isOdd) 1 else if (isEven) 2 else 0
            }
        )
    }
    var customWeeksStr by remember { mutableStateOf(initialParsedWeeks.joinToString(", ")) }

    val activeWeeksList = remember(useShortcutMode, startWeekStr, endWeekStr, weekType, customWeeksStr) {
        if (useShortcutMode) {
            val startW = startWeekStr.toIntOrNull() ?: 1
            val endW = endWeekStr.toIntOrNull() ?: 16
            val range = startW..endW
            when (weekType) {
                1 -> range.filter { it % 2 != 0 }
                2 -> range.filter { it % 2 == 0 }
                else -> range.toList()
            }
        } else {
            customWeeksStr.split(Regex("[,，\\s]+"))
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..50 }
                .distinct()
                .sorted()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = bgColor, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if (initialCourse == null) "添加课程" else "编辑课程", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, label = { Text("课程名称", color = textColor.copy(alpha = 0.6f)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                    )
                    OutlinedTextField(
                        value = loc, onValueChange = { loc = it }, label = { Text("上课地点", color = textColor.copy(alpha = 0.6f)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                    )
                    OutlinedTextField(
                        value = teacher, onValueChange = { teacher = it }, label = { Text("授课教师", color = textColor.copy(alpha = 0.6f)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                    )
                    OutlinedTextField(
                        value = credits, onValueChange = { credits = it }, label = { Text("学分 (选择性输入)", color = textColor.copy(alpha = 0.6f)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("课堂表现统计 (科目内共享)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("旷课次数", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, borderColor, RoundedCornerShape(8.dp)).padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { if (absenceCount > 0) absenceCount-- },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = textColor, modifier = Modifier.size(16.dp))
                                }
                                Text("$absenceCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                IconButton(
                                    onClick = { absenceCount++ },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = textColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("被点名次数", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, borderColor, RoundedCornerShape(8.dp)).padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { if (calledCount > 0) calledCount-- },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = textColor, modifier = Modifier.size(16.dp))
                                }
                                Text("$calledCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                IconButton(
                                    onClick = { calledCount++ },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = textColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Day of Week
                    Column {
                        Text("星期", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val days = listOf("一", "二", "三", "四", "五", "六", "日")
                            days.forEachIndexed { index, dName ->
                                val isSelected = dayOfWeek == (index + 1)
                                val activeThemeColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { dayOfWeek = index + 1 },
                                    color = if (isSelected) activeThemeColor else (if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dName,
                                            color = if (isSelected) Color.White else textColor.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Start/End Node
                    Column {
                        Text("节次", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var startExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { startExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                                ) {
                                    Text("起始: 第 $startNode 节")
                                }
                                DropdownMenu(
                                    expanded = startExpanded,
                                    onDismissRequest = { startExpanded = false }
                                ) {
                                    (1..17).forEach { n ->
                                        DropdownMenuItem(
                                            text = { Text("第 $n 节", color = textColor) },
                                            onClick = {
                                                startNode = n
                                                if (endNode < startNode) endNode = startNode
                                                startExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            var endExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { endExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                                ) {
                                    Text("结束: 第 $endNode 节")
                                }
                                DropdownMenu(
                                    expanded = endExpanded,
                                    onDismissRequest = { endExpanded = false }
                                ) {
                                    (1..17).forEach { n ->
                                        DropdownMenuItem(
                                            text = { Text("第 $n 节", color = textColor) },
                                            onClick = {
                                                if (n >= startNode) {
                                                    endNode = n
                                                }
                                                endExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Weeks selection
                    Column {
                        Text("上课周数", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeTabColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { useShortcutMode = true },
                                color = if (useShortcutMode) activeTabColor.copy(alpha = 0.15f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (useShortcutMode) activeTabColor else textColor.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text("快捷输入 (始末周)", color = if (useShortcutMode) activeTabColor else textColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { useShortcutMode = false },
                                color = if (!useShortcutMode) activeTabColor.copy(alpha = 0.15f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (!useShortcutMode) activeTabColor else textColor.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text("自定义输入 (列出周数)", color = if (!useShortcutMode) activeTabColor else textColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (useShortcutMode) {
                            val activeTabColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = startWeekStr,
                                        onValueChange = { startWeekStr = it },
                                        label = { Text("起始周", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                                    )
                                    OutlinedTextField(
                                        value = endWeekStr,
                                        onValueChange = { endWeekStr = it },
                                        label = { Text("结束周", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f)) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val types = listOf("每周", "单周", "双周")
                                    types.forEachIndexed { index, tName ->
                                        val isTypeSelected = weekType == index
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { weekType = index },
                                            color = if (isTypeSelected) activeTabColor else (if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = tName,
                                                    color = if (isTypeSelected) Color.White else textColor.copy(alpha = 0.8f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = customWeeksStr,
                                onValueChange = { customWeeksStr = it },
                                label = { Text("上课周数 (如 1, 2, 3, 5)", color = textColor.copy(alpha = 0.6f)) },
                                placeholder = { Text("例如: 1, 2, 3, 5", color = textColor.copy(alpha = 0.4f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f), cursorColor = textColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "已选周数预览: ${if (activeWeeksList.isEmpty()) "无" else activeWeeksList.joinToString(", ") { "${it}周" }}",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    val finalWeeksStr = remember(activeWeeksList) {
                        activeWeeksList.joinToString(prefix = "[", postfix = "]") { it.toString() }
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    initialCourse?.id,
                                    name,
                                    loc,
                                    teacher,
                                    dayOfWeek,
                                    startNode,
                                    endNode,
                                    colorTheme,
                                    finalWeeksStr,
                                    credits.trim().ifEmpty { null },
                                    absenceCount,
                                    calledCount
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor), shape = RoundedCornerShape(4.dp)
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
fun AdjustCourseScreen(isDark: Boolean, predictiveBack: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)

    val backModifier = AppBackHandler(predictiveBack) { onBack() }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoreMenuBottomSheet(
    isDark: Boolean, currentPagerWeek: Int, totalWeeks: Int,
    scheduleGroups: List<ScheduleGroup>, currentScheduleId: String,
    onBrowseWeek: (Int) -> Unit, onSwitchSchedule: (String) -> Unit,
    onCreateScheduleClick: () -> Unit,
    onDismiss: () -> Unit, onManageCoursesClick: () -> Unit, onTimetableManageClick: () -> Unit,
    onScheduleSettingsClick: () -> Unit, onGlobalSettingsClick: () -> Unit, onReminderSettingsClick: () -> Unit,
    onDeleteScheduleClick: (String, String) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val primaryColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) BgDark else BgLight,
        dragHandle = { BottomSheetDefaults.DragHandle(color = borderColor) }
    ) {
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

            Spacer(modifier = Modifier.height(16.dp))

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
                            .combinedClickable(
                                onClick = { onSwitchSchedule(group.id) },
                                onLongClick = {
                                    onDismiss()
                                    onDeleteScheduleClick(group.id, group.name)
                                }
                            )
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

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    val currentName = scheduleGroups.find { it.id == currentScheduleId }?.name ?: "课表"
                    onDismiss()
                    onDeleteScheduleClick(currentScheduleId, currentName)
                },
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
fun ProfileScreen(
    isDark: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onCheckUpdateClick: () -> Unit = {},
    onShowAnnouncementClick: () -> Unit = {}
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            "v${packageInfo.versionName}"
        } catch (e: Exception) {
            "v2.6.3.0615"
        }
    }

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
                Text("S", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isDark) BgDark else BgLight)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text("Made By 视界Seekai ", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("QQ交流群: 817954315", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = textColor.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text("亮暗模式切换(默认跟随系统)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.4f), letterSpacing = 1.sp)
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
                SettingValueItem(title = "版本", value = versionName, textColor = textColor, borderColor = borderColor, onClick = onCheckUpdateClick)
                SettingItemWithSubtext(
                    title = "更新说明",
                    subtext = "点击查看最近版本的更新公告说明",
                    showBottomBorder = true,
                    textColor = textColor,
                    borderColor = borderColor,
                    onClick = onShowAnnouncementClick
                )
                SettingItemWithSubtext(
                    title = "开源与反馈",
                    subtext = "点击访问 GitHub 仓库获取源码或提交建议",
                    showBottomBorder = false,
                    textColor = textColor,
                    borderColor = borderColor,
                    onClick = { uriHandler.openUri("https://github.com/OooBrickooO/SimpleScheduleApp") }
                )
            }
        }
    }
}

// --- 7. 自动提醒服务与调度引擎 ---

