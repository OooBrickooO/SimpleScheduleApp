package com.example.simpleschedule.ui.course

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simpleschedule.data.local.room.DisplayCourse
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.domain.ScheduleResolver
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.getCoursePalette
import com.example.simpleschedule.viewmodel.ScheduleViewModel

@Composable
fun CourseDetailDialog(
    displayCourse: DisplayCourse,
    clickedWeek: Int,
    timeNodes: List<TimeNode>,
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
    val chipBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)
    val course = displayCourse.course
    val courseColorPool by viewModel.courseColorPool.collectAsState()
    val palette = getCoursePalette(course.colorTheme, isDark, materialYou, course.name, courseColorPool)
    val weeksList = ScheduleResolver.parseWeeks(course.weeks)

    val statistic by viewModel.getCourseStatistic(course.name).collectAsState(initial = null)
    val absenceCount = statistic?.absenceCount ?: 0
    val calledCount = statistic?.calledCount ?: 0

    val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val dayStr = dayLabels.getOrElse(displayCourse.displayDay - 1) { "周${displayCourse.displayDay}" }
    val startTime = timeNodes.find { it.nodeIndex == displayCourse.displayStartNode }?.startTime
    val endTime = timeNodes.find { it.nodeIndex == displayCourse.displayEndNode }?.endTime
    val nodeStr = if (displayCourse.displayStartNode == displayCourse.displayEndNode) {
        "第${displayCourse.displayStartNode}节"
    } else {
        "第${displayCourse.displayStartNode}-${displayCourse.displayEndNode}节"
    }
    val timeDetail = if (startTime != null && endTime != null) {
        "$dayStr $nodeStr ($startTime-$endTime)"
    } else {
        "$dayStr $nodeStr"
    }
    val weeksDetail = if (weeksList.isEmpty()) "未设置" else formatWeeksSummary(weeksList)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            var showDeleteConfirm by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(palette.accent, RoundedCornerShape(3.dp)))
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
                Spacer(modifier = Modifier.height(10.dp))
                Text(course.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor, lineHeight = 28.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = chipBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailInfoRow(Icons.Rounded.Schedule, "时间", timeDetail, textColor)
                        DetailInfoRow(Icons.Rounded.LocationOn, "地点", course.location.ifBlank { "未排地点" }, textColor)
                        DetailInfoRow(Icons.Rounded.Person, "教师", course.teacher.ifBlank { "无" }, textColor)
                        DetailInfoRow(Icons.Rounded.CalendarMonth, "周次", weeksDetail, textColor)
                        if (!course.credits.isNullOrBlank()) {
                            DetailInfoRow(Icons.Rounded.Info, "学分", course.credits, textColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text("课堂表现统计 (科目内共享)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.incrementAbsenceCount(course.name) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = chipBg, contentColor = textColor),
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
                    Button(
                        onClick = { viewModel.incrementCalledCount(course.name) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = chipBg, contentColor = textColor),
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

                Spacer(modifier = Modifier.height(22.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor)
                    ) {
                        Text("编辑", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f).height(46.dp),
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
                                onClick = { showDeleteConfirm = false; onDelete() },
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
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    textColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = label, tint = textColor.copy(alpha = 0.55f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label：",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.55f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatWeeksSummary(weeks: List<Int>): String {
    if (weeks.isEmpty()) return ""
    val sorted = weeks.distinct().sorted()
    val ranges = mutableListOf<String>()
    var start = sorted.first()
    var prev = start
    for (w in sorted.drop(1)) {
        if (w == prev + 1) {
            prev = w
        } else {
            ranges.add(if (start == prev) "$start" else "$start-$prev")
            start = w
            prev = w
        }
    }
    ranges.add(if (start == prev) "$start" else "$start-$prev")
    return "第 ${ranges.joinToString("、")} 周"
}
