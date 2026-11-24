package com.example.simpleschedule.ui.course

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.domain.ScheduleResolver
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.CourseColorThemeOptions
import com.example.simpleschedule.ui.theme.getPalette
import com.example.simpleschedule.viewmodel.ScheduleViewModel

@Composable
fun CourseEditDialog(
    isDark: Boolean,
    initialCourse: Course?,
    maxNodeCount: Int = 17,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String?, String, String, String, Int, Int, Int, String, String, String?, Int, Int) -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) BorderDark else BorderLight
    val nodeLimit = maxNodeCount.coerceAtLeast(12)

    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var loc by remember { mutableStateOf(initialCourse?.location ?: "") }
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var credits by remember { mutableStateOf(initialCourse?.credits ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(initialCourse?.dayOfWeek ?: 1) }
    var startNode by remember { mutableIntStateOf((initialCourse?.startNode ?: 1).coerceIn(1, nodeLimit)) }
    var endNode by remember { mutableIntStateOf((initialCourse?.endNode ?: 2).coerceIn(startNode, nodeLimit)) }
    var colorTheme by remember { mutableStateOf(initialCourse?.colorTheme ?: CourseColorThemeOptions.random()) }

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
        val parsed = initialCourse?.let { ScheduleResolver.parseWeeks(it.weeks).sorted() }
        if (parsed.isNullOrEmpty()) (1..16).toList() else parsed
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
        mutableIntStateOf(
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
            val startW = (startWeekStr.toIntOrNull() ?: 1).coerceIn(1, 50)
            val endW = (endWeekStr.toIntOrNull() ?: 16).coerceIn(startW, 50)
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

                    // 卡片配色选择
                    Column {
                        Text("卡片颜色", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CourseColorThemeOptions.forEach { option ->
                                val palette = getPalette(option, isDark)
                                val selected = colorTheme == option
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent)
                                        .border(if (selected) 2.5.dp else 0.dp, if (selected) textColor else Color.Transparent, CircleShape)
                                        .clickable { colorTheme = option },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
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
                                IconButton(onClick = { if (absenceCount > 0) absenceCount-- }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = textColor, modifier = Modifier.size(16.dp))
                                }
                                Text("$absenceCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                IconButton(onClick = { absenceCount++ }, modifier = Modifier.size(28.dp)) {
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
                                IconButton(onClick = { if (calledCount > 0) calledCount-- }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = textColor, modifier = Modifier.size(16.dp))
                                }
                                Text("$calledCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                                IconButton(onClick = { calledCount++ }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = textColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // 星期
                    Column {
                        Text("星期", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val days = listOf("一", "二", "三", "四", "五", "六", "日")
                            days.forEachIndexed { index, dName ->
                                val isSelected = dayOfWeek == (index + 1)
                                val activeThemeColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { dayOfWeek = index + 1 },
                                    color = if (isSelected) activeThemeColor else (if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
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

                    // 节次
                    Column {
                        Text("节次", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                DropdownMenu(expanded = startExpanded, onDismissRequest = { startExpanded = false }) {
                                    (1..nodeLimit).forEach { n ->
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
                                DropdownMenu(expanded = endExpanded, onDismissRequest = { endExpanded = false }) {
                                    (1..nodeLimit).forEach { n ->
                                        DropdownMenuItem(
                                            text = { Text("第 $n 节", color = textColor) },
                                            onClick = {
                                                if (n >= startNode) endNode = n
                                                endExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 上课周数
                    Column {
                        Text("上课周数", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val activeTabColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)
                            Surface(
                                modifier = Modifier.weight(1f).clickable { useShortcutMode = true },
                                color = if (useShortcutMode) activeTabColor.copy(alpha = 0.15f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (useShortcutMode) activeTabColor else textColor.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text("快捷输入 (始末周)", color = if (useShortcutMode) activeTabColor else textColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f).clickable { useShortcutMode = false },
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
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val types = listOf("每周", "单周", "双周")
                                    types.forEachIndexed { index, tName ->
                                        val isTypeSelected = weekType == index
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { weekType = index },
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
                            if (name.isNotBlank() && activeWeeksList.isNotEmpty()) {
                                onConfirm(
                                    initialCourse?.id,
                                    name.trim(),
                                    loc.trim(),
                                    teacher.trim(),
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
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor),
                        shape = RoundedCornerShape(4.dp)
                    ) { Text("保存") }
                }
            }
        }
    }
}
