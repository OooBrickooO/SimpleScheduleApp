package com.example.simpleschedule.ui.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.data.local.room.ScheduleGroup
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoreMenuBottomSheet(
    isDark: Boolean,
    currentPagerWeek: Int,
    totalWeeks: Int,
    scheduleGroups: List<ScheduleGroup>,
    currentScheduleId: String,
    onBrowseWeek: (Int) -> Unit,
    onSwitchSchedule: (String) -> Unit,
    onCreateScheduleClick: () -> Unit,
    onDismiss: () -> Unit,
    onManageCoursesClick: () -> Unit,
    onTimetableManageClick: () -> Unit,
    onScheduleSettingsClick: () -> Unit,
    onGlobalSettingsClick: () -> Unit,
    onReminderSettingsClick: () -> Unit,
    onDataManagementClick: () -> Unit,
    onDeleteScheduleClick: (String, String) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val primaryColor = LocalAccentColor.current ?: if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) BgDark else BgLight,
        dragHandle = { BottomSheetDefaults.DragHandle(color = borderColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("预览周次（第 $currentPagerWeek 周）", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                Surface(
                    color = primaryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onDismiss(); onScheduleSettingsClick() }
                ) {
                    Text(
                        text = "修改当前周 >",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = currentPagerWeek.toFloat().coerceIn(1f, totalWeeks.toFloat().coerceAtLeast(2f)),
                onValueChange = { onBrowseWeek(it.roundToInt()) },
                valueRange = 1f..totalWeeks.toFloat().coerceAtLeast(2f),
                steps = (totalWeeks - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = primaryColor.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("课表", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.weight(1f))
                Text("新建课表", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.clickable { onDismiss(); onCreateScheduleClick() })
                Spacer(modifier = Modifier.width(16.dp))
                Text("管理", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.clickable { onDismiss(); onScheduleSettingsClick() })
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                scheduleGroups.forEach { group ->
                    val isSelected = group.id == currentScheduleId
                    Box(
                        modifier = Modifier
                            .size(84.dp)
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
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = if (isDark) BgDark else BgLight, modifier = Modifier.size(30.dp))
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

            Spacer(modifier = Modifier.height(22.dp))

            data class MenuItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)
            val menuItems = listOf(
                MenuItem("上课时间", Icons.Rounded.Schedule) { onDismiss(); onTimetableManageClick() },
                MenuItem("已添课程", Icons.Rounded.EventNote) { onDismiss(); onManageCoursesClick() },
                MenuItem("课表设置", Icons.Rounded.Tune) { onDismiss(); onScheduleSettingsClick() },
                MenuItem("课前提醒", Icons.Rounded.Campaign) { onDismiss(); onReminderSettingsClick() },
                MenuItem("全局配置", Icons.Rounded.Settings) { onDismiss(); onGlobalSettingsClick() },
                MenuItem("数据管理", Icons.Rounded.FolderSpecial) { onDismiss(); onDataManagementClick() }
            )

            // 2 x 3 clean grid
            menuItems.chunked(3).forEachIndexed { rowIdx, rowItems ->
                if (rowIdx > 0) Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = item.onClick
                                )
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(surfaceColor, RoundedCornerShape(14.dp))
                                    .border(0.5.dp, borderColor, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = item.label, tint = textColor, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

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
