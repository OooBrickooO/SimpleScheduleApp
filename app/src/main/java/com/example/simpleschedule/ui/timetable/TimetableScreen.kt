package com.example.simpleschedule.ui.timetable

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.local.room.DisplayCourse
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.data.local.room.ScheduleGroup
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.domain.ScheduleResolver
import com.example.simpleschedule.ui.backup.DataManagementFeatureGuideDialog
import com.example.simpleschedule.ui.components.MakeUpFeatureGuideDialog
import com.example.simpleschedule.ui.components.MakeUpManagerBottomSheet
import com.example.simpleschedule.ui.components.QuickMakeUpDialog
import com.example.simpleschedule.ui.course.CourseDetailDialog
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.utils.getDateByWeekAndDay
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch

@Composable
fun TimetableScreen(
    viewModel: ScheduleViewModel,
    courses: List<DisplayCourse>,
    timeNodes: List<TimeNode>,
    currentWeek: Int,
    totalWeeks: Int,
    scheduleGroups: List<ScheduleGroup>,
    currentScheduleId: String,
    isDark: Boolean,
    onAddClick: () -> Unit,
    onManageCoursesClick: () -> Unit,
    onManageTimetablesClick: () -> Unit,
    onScheduleSettingsClick: () -> Unit,
    onGlobalSettingsClick: () -> Unit,
    onEditCourse: (Course) -> Unit,
    onWebViewImportClick: () -> Unit,
    onCsvImportClick: () -> Unit,
    onShareCodeClick: () -> Unit,
    onCreateScheduleClick: () -> Unit,
    onReminderSettingsClick: () -> Unit,
    onDataManagementClick: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val accent = LocalAccentColor.current ?: MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedCourseWithWeek by remember { mutableStateOf<Pair<DisplayCourse, Int>?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var scheduleIdToDelete by remember { mutableStateOf<String?>(null) }
    var scheduleNameToDelete by remember { mutableStateOf<String?>(null) }
    val makeUpRules by viewModel.makeUpRules.collectAsState()
    var showMakeUpManagerSheet by remember { mutableStateOf(false) }
    var quickMakeUpTargetDate by remember { mutableStateOf<String?>(null) }
    val hasSeenMakeUpGuide by viewModel.hasSeenMakeUpGuide.collectAsState()
    val hasSeenDataManagementGuide by viewModel.hasSeenDataManagementGuide.collectAsState()
    var showMakeUpGuideDialog by remember { mutableStateOf(false) }
    var showDataGuideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hasSeenMakeUpGuide, hasSeenDataManagementGuide) {
        if (hasSeenMakeUpGuide == false) {
            showMakeUpGuideDialog = true
        } else if (hasSeenMakeUpGuide == true && hasSeenDataManagementGuide == false && !showMakeUpGuideDialog) {
            showDataGuideDialog = true
        }
    }

    val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }
    val currentScheduleName = currentSchedule?.name ?: "课表"
    val startDate = currentSchedule?.startDate ?: ""
    val pagerState = rememberPagerState(
        initialPage = (currentWeek - 1).coerceAtLeast(0),
        pageCount = { totalWeeks.coerceAtLeast(1) }
    )

    val showNotThisWeek by viewModel.showNotThisWeek.collectAsState()
    val bottomBlank by viewModel.bottomBlank.collectAsState()
    val materialYou by viewModel.materialYou.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showMoreSheet = true }
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentScheduleName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Menu", tint = textColor)
                }
                val displayedPageWeek = pagerState.currentPage + 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "第 $displayedPageWeek 周",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    if (displayedPageWeek != currentWeek) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = accent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, accent.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                val targetPage = (currentWeek - 1).coerceIn(0, (totalWeeks - 1).coerceAtLeast(0))
                                coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        ) {
                            Text(
                                text = "非本周 · 回第${currentWeek}周",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add",
                        tint = textColor,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showAddMenu = true }
                    )
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF18181B) else Color.White)
                            .border(0.5.dp, borderColor)
                    ) {
                        DropdownMenuItem(
                            text = { Text("手动添加课程", fontWeight = FontWeight.Bold, color = textColor) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = textColor) },
                            onClick = { showAddMenu = false; onAddClick() }
                        )
                        HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = { Text("从教务导入课表/考试", fontWeight = FontWeight.Bold, color = textColor) },
                            leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null, tint = textColor) },
                            onClick = { showAddMenu = false; onWebViewImportClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("从 CSV 文件导入", fontWeight = FontWeight.Bold, color = textColor) },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = textColor) },
                            onClick = { showAddMenu = false; onCsvImportClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("分享口令导入", fontWeight = FontWeight.Bold, color = textColor) },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = textColor) },
                            onClick = { showAddMenu = false; onShareCodeClick() }
                        )
                        HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = { Text("调休快捷换课", fontWeight = FontWeight.Bold, color = textColor) },
                            leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = textColor) },
                            onClick = { showAddMenu = false; showMakeUpManagerSheet = true }
                        )
                    }
                }
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = textColor,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showMoreSheet = true }
                )
            }
        }

        LaunchedEffect(currentWeek, totalWeeks) {
            val safePage = (currentWeek - 1).coerceIn(0, (totalWeeks - 1).coerceAtLeast(0))
            if (pagerState.currentPage != safePage) {
                pagerState.animateScrollToPage(safePage)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val weekForThisPage = page + 1
            val coursesForThisPage = remember(courses, weekForThisPage, showNotThisWeek, makeUpRules, startDate) {
                ScheduleResolver.resolveWeekGridCourses(
                    week = weekForThisPage,
                    startDateStr = startDate,
                    displayCourses = courses,
                    makeUpRules = makeUpRules,
                    showNotThisWeek = showNotThisWeek
                )
            }

            val makeUpDayMap = remember(makeUpRules, weekForThisPage, startDate) {
                val map = mutableMapOf<Int, MakeUpRule>()
                for (day in 1..7) {
                    val dayDate = getDateByWeekAndDay(weekForThisPage, day, startDate) ?: ""
                    val rule = makeUpRules.find { it.targetDate.isNotEmpty() && it.targetDate == dayDate }
                    if (rule != null) map[day] = rule
                }
                map
            }

            val holidayDayMap = remember(makeUpRules, weekForThisPage, startDate) {
                val map = mutableMapOf<Int, MakeUpRule>()
                for (day in 1..7) {
                    val dayDate = getDateByWeekAndDay(weekForThisPage, day, startDate) ?: ""
                    val rule = makeUpRules.find { it.sourceDate.isNotEmpty() && it.sourceDate == dayDate }
                    if (rule != null) map[day] = rule
                }
                map
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
                makeUpDayMap = makeUpDayMap,
                holidayDayMap = holidayDayMap,
                onDayLongPress = { _, dateStr -> quickMakeUpTargetDate = dateStr },
                onCourseClick = { course, isFuture ->
                    selectedCourseWithWeek = Pair(course, if (isFuture) 0 else weekForThisPage)
                }
            )
        }
    }

    if (selectedCourseWithWeek != null) {
        val (course, clickedWeek) = selectedCourseWithWeek!!
        CourseDetailDialog(
            displayCourse = course,
            clickedWeek = clickedWeek,
            timeNodes = timeNodes,
            isDark = isDark,
            materialYou = materialYou,
            viewModel = viewModel,
            onDismiss = { selectedCourseWithWeek = null },
            onDelete = { viewModel.deleteCourse(course.course.id); selectedCourseWithWeek = null },
            onEdit = {
                selectedCourseWithWeek = null
                onEditCourse(
                    course.course.copy(
                        dayOfWeek = course.displayDay,
                        startNode = course.displayStartNode,
                        endNode = course.displayEndNode
                    )
                )
            }
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
                scheduleIdToDelete = null
                scheduleNameToDelete = null
            }
        )
    }

    if (quickMakeUpTargetDate != null) {
        val targetDate = quickMakeUpTargetDate!!
        val existingTargetRule = makeUpRules.find { it.targetDate == targetDate }
        val existingSourceRule = makeUpRules.find { it.sourceDate == targetDate }

        QuickMakeUpDialog(
            isDark = isDark,
            dateStr = targetDate,
            existingTargetRule = existingTargetRule,
            existingSourceRule = existingSourceRule,
            onDismiss = { quickMakeUpTargetDate = null },
            onSaveRule = { sourceDate, tDate ->
                viewModel.addMakeUpRule(sourceDate, tDate)
                quickMakeUpTargetDate = null
            },
            onDeleteRule = { ruleId ->
                viewModel.deleteMakeUpRule(ruleId)
                quickMakeUpTargetDate = null
            },
            onOpenFullManager = { showMakeUpManagerSheet = true }
        )
    }

    if (showMakeUpManagerSheet) {
        MakeUpManagerBottomSheet(
            isDark = isDark,
            makeUpRules = makeUpRules,
            onDismiss = { showMakeUpManagerSheet = false },
            onAddRule = { sourceDate, targetDate -> viewModel.addMakeUpRule(sourceDate, targetDate) },
            onDeleteRule = { ruleId -> viewModel.deleteMakeUpRule(ruleId) },
            onShowGuide = { showMakeUpGuideDialog = true }
        )
    }

    if (showMakeUpGuideDialog) {
        MakeUpFeatureGuideDialog(
            isDark = isDark,
            onDismiss = {
                viewModel.markMakeUpGuideSeen()
                showMakeUpGuideDialog = false
            }
        )
    }

    if (showDataGuideDialog) {
        DataManagementFeatureGuideDialog(
            isDark = isDark,
            onDismiss = {
                viewModel.markDataManagementGuideSeen()
                showDataGuideDialog = false
            },
            onGoNow = {
                viewModel.markDataManagementGuideSeen()
                showDataGuideDialog = false
                onDataManagementClick()
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
            onBrowseWeek = { targetWeek ->
                coroutineScope.launch { pagerState.animateScrollToPage((targetWeek - 1).coerceAtLeast(0)) }
            },
            onSwitchSchedule = { viewModel.switchSchedule(it); showMoreSheet = false },
            onCreateScheduleClick = { showMoreSheet = false; onCreateScheduleClick() },
            onDismiss = { showMoreSheet = false },
            onManageCoursesClick = onManageCoursesClick,
            onTimetableManageClick = onManageTimetablesClick,
            onScheduleSettingsClick = onScheduleSettingsClick,
            onGlobalSettingsClick = onGlobalSettingsClick,
            onReminderSettingsClick = onReminderSettingsClick,
            onDataManagementClick = onDataManagementClick,
            onDeleteScheduleClick = { id, name -> scheduleIdToDelete = id; scheduleNameToDelete = name }
        )
    }
}
