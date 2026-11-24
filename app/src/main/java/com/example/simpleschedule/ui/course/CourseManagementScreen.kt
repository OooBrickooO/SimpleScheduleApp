package com.example.simpleschedule.ui.course

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.ui.common.AppBackHandler
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.ui.theme.getCoursePalette
import com.example.simpleschedule.viewmodel.ScheduleViewModel

@Composable
fun CourseManagementScreen(
    viewModel: ScheduleViewModel,
    courses: List<Course>,
    isDark: Boolean,
    materialYou: Boolean,
    onBack: () -> Unit,
    onEditCourse: (Course) -> Unit,
    onDeleteCourse: (String) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight

    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val courseColorPool by viewModel.courseColorPool.collectAsState()
    val backModifier = AppBackHandler(predictiveBackEnabled) { onBack() }

    var courseIdToDelete by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val dayLabels = remember { listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日") }

    val filteredCourses = remember(courses, searchQuery) {
        if (searchQuery.isBlank()) courses
        else courses.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true) ||
                it.teacher.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().then(backModifier)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = textColor,
                modifier = Modifier.clickable { onBack() }.padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("课程管理 (${courses.size})", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("搜索课程名 / 教室 / 教师...", color = textColor.copy(alpha = 0.45f), fontSize = 13.sp) },
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
                unfocusedBorderColor = borderColor.copy(alpha = 0.6f),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = textColor
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
            modifier = Modifier.navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredCourses, key = { it.id }) { course ->
                val palette = getCoursePalette(course.colorTheme, isDark, materialYou, course.name, courseColorPool)
                val dayLabel = dayLabels.getOrElse(course.dayOfWeek - 1) { "周${course.dayOfWeek}" }
                val slotLabel = "$dayLabel ${course.startNode}-${course.endNode}节"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.45f)
                        .animateContentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.bg)
                        .border(0.5.dp, palette.border, RoundedCornerShape(10.dp))
                        .clickable { onEditCourse(course) }
                        .padding(12.dp)
                ) {
                    IconButton(
                        onClick = { courseIdToDelete = course.id },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "删除课程",
                            tint = palette.text.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = course.name + if (!course.credits.isNullOrBlank()) "（${course.credits}）" else "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.text,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = slotLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.text.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (course.location.isNotBlank()) {
                            Text(
                                text = course.location,
                                fontSize = 10.sp,
                                color = palette.text.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
