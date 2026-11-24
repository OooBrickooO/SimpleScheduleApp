package com.example.simpleschedule.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.room.ScheduleGroup
import com.example.simpleschedule.ui.common.AppBackHandler
import com.example.simpleschedule.ui.common.SettingCheckboxItem
import com.example.simpleschedule.ui.common.SettingValueItem
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.utils.formatDateForDisplay
import com.example.simpleschedule.utils.showDatePicker
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ScheduleSettingsScreen(
    viewModel: ScheduleViewModel,
    scheduleGroups: List<ScheduleGroup>,
    currentScheduleId: String,
    currentWeek: Int,
    timeNodeCount: Int,
    totalWeeks: Int,
    isDark: Boolean,
    onBack: () -> Unit,
    onRenameSchedule: (String, String) -> Unit,
    onWeekChange: (Int) -> Unit,
    onStartDateChange: (String) -> Unit,
    onTotalWeeksChange: (Int) -> Unit,
    onManageTimetableClick: () -> Unit,
    onManageCoursesClick: () -> Unit,
    onMoreAppearanceClick: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val context = LocalContext.current

    val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }
    var scheduleName by remember(currentScheduleId) { mutableStateOf(currentSchedule?.name ?: "") }
    var showChangeWeekDialog by remember { mutableStateOf(false) }
    var showTotalWeeksDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentSchedule?.name) {
        val remoteName = currentSchedule?.name
        if (remoteName != null && scheduleName.isEmpty()) {
            scheduleName = remoteName
        }
    }

    // Debounce rename persistence instead of writing Room & widgets on every keystroke
    LaunchedEffect(scheduleName, currentScheduleId) {
        val trimmed = scheduleName.trim()
        if (trimmed.isNotEmpty() && trimmed != (currentSchedule?.name ?: "")) {
            delay(500)
            onRenameSchedule(currentScheduleId, trimmed)
        }
    }

    val showSat by viewModel.showSat.collectAsState()
    val showSun by viewModel.showSun.collectAsState()
    val showNotThisWeek by viewModel.showNotThisWeek.collectAsState()
    val cellHeightDp by viewModel.cellHeight.collectAsState()
    val cornerRadiusDp by viewModel.cornerRadius.collectAsState()

    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val backModifier = AppBackHandler(predictiveBackEnabled) { onBack() }

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
            Text("课表设置", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("课表名称", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        OutlinedTextField(
                            value = scheduleName,
                            onValueChange = { scheduleName = it },
                            textStyle = TextStyle(textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f)),
                            singleLine = true,
                            modifier = Modifier.width(160.dp),
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
                        SettingValueItem(title = "一天课程节数", value = "$timeNodeCount 节", textColor = textColor, borderColor = borderColor, onClick = onManageTimetableClick)
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
                        SettingValueItem(title = "课程格子高度", value = "${cellHeightDp.roundToInt()} dp", textColor = textColor, borderColor = borderColor, onClick = onMoreAppearanceClick)
                        SettingValueItem(title = "格子圆角半径", value = "${cornerRadiusDp.roundToInt()} dp", textColor = textColor, borderColor = borderColor, onClick = onMoreAppearanceClick)
                        Row(modifier = Modifier.fillMaxWidth().clickable { onMoreAppearanceClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("更多外观设置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("全局主色调、课程颜色池、格子高度与圆角", fontSize = 10.sp, color = textColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }

    if (showChangeWeekDialog) {
        ChangeWeekDialog(
            isDark = isDark,
            title = "设置当前周",
            currentValue = currentWeek,
            maxValue = totalWeeks,
            onDismiss = { showChangeWeekDialog = false },
            onConfirm = { onWeekChange(it); showChangeWeekDialog = false }
        )
    }
    if (showTotalWeeksDialog) {
        ChangeWeekDialog(
            isDark = isDark,
            title = "设置学期周数",
            currentValue = totalWeeks,
            maxValue = 30,
            onDismiss = { showTotalWeeksDialog = false },
            onConfirm = { onTotalWeeksChange(it); showTotalWeeksDialog = false }
        )
    }
}

@Composable
fun ChangeWeekDialog(
    isDark: Boolean,
    title: String,
    currentValue: Int,
    maxValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val safeMax = maxValue.coerceAtLeast(2)
    var value by remember { mutableFloatStateOf(currentValue.coerceIn(1, safeMax).toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(24.dp))
                Text("${value.roundToInt()}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 1f..safeMax.toFloat(),
                    steps = (safeMax - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = textColor.copy(alpha = 0.5f)) }
                    Button(
                        onClick = { onConfirm(value.roundToInt()) },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
