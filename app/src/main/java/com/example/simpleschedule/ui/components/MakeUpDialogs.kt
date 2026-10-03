package com.example.simpleschedule.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.utils.formatDateForDisplay
import com.example.simpleschedule.utils.showDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeUpManagerBottomSheet(
    isDark: Boolean,
    makeUpRules: List<MakeUpRule>,
    onDismiss: () -> Unit,
    onAddRule: (sourceDate: String, targetDate: String) -> Unit,
    onDeleteRule: (ruleId: String) -> Unit,
    onShowGuide: (() -> Unit)? = null
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val primaryColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val deleteColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    var showAddDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF121212) else Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = borderColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = "节假日调休管理",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "临时移动课表，并高亮框选调休上课日",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                    if (onShowGuide != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onShowGuide, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = "使用指南",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = primaryColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { showAddDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增调休", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (makeUpRules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.25f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "暂无调休设置",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "可在上方点击「新增调休」，或在主界面长按日期表头快速设置",
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.4f)
                        )
                        if (onShowGuide != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = primaryColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable { onShowGuide() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.HelpOutline,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "查看调休换课使用指南",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(makeUpRules, key = { it.id }) { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(surfaceColor)
                                .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = primaryColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "调休补课",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatDateForDisplay(rule.targetDate),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "被调原课",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatDateForDisplay(rule.sourceDate),
                                        fontSize = 13.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteRule(rule.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = deleteColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMakeUpRuleDialog(
            isDark = isDark,
            initialTargetDate = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { sourceDate, targetDate ->
                onAddRule(sourceDate, targetDate)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddMakeUpRuleDialog(
    isDark: Boolean,
    initialTargetDate: String = "",
    initialSourceDate: String = "",
    onDismiss: () -> Unit,
    onConfirm: (sourceDate: String, targetDate: String) -> Unit
) {
    val context = LocalContext.current
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val primaryColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var targetDate by remember { mutableStateOf(if (initialTargetDate.isNotEmpty()) initialTargetDate else todayStr) }
    var sourceDate by remember { mutableStateOf(if (initialSourceDate.isNotEmpty()) initialSourceDate else todayStr) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF18181B) else Color.White,
            border = BorderStroke(0.5.dp, borderColor),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "添加调休换课",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 目标日期（补课日）
                Text("1. 目标日期（原放假，改上课的日子）", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceColor)
                        .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            showDatePicker(context, targetDate) { selected ->
                                targetDate = selected
                                errorMessage = null
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDateForDisplay(targetDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 被调日期（原上课日）
                Text("2. 被调日期（原上课，因事改放假的日子）", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceColor)
                        .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            showDatePicker(context, sourceDate) { selected ->
                                sourceDate = selected
                                errorMessage = null
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDateForDisplay(sourceDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "提示：被调日期的课表将临时移动至目标日期；被调日期自身将被标为放假。",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.5f),
                    lineHeight = 15.sp
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, fontSize = 12.sp, color = Color(0xFFDC2626))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = textColor.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (targetDate == sourceDate) {
                                errorMessage = "目标日期和被调日期不能相同喵！"
                                return@Button
                            }
                            onConfirm(sourceDate, targetDate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("确认调休")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickMakeUpDialog(
    isDark: Boolean,
    dateStr: String,
    existingTargetRule: MakeUpRule?,
    existingSourceRule: MakeUpRule?,
    onDismiss: () -> Unit,
    onSaveRule: (sourceDate: String, targetDate: String) -> Unit,
    onDeleteRule: (ruleId: String) -> Unit,
    onOpenFullManager: () -> Unit
) {
    val context = LocalContext.current
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val primaryColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val deleteColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    var sourceDate by remember {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            cal.time = sdf.parse(dateStr) ?: Date()
            // 如果长按的是周六或周日，默认猜上一个周五
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SATURDAY) cal.add(Calendar.DAY_OF_YEAR, -1)
            else if (dow == Calendar.SUNDAY) cal.add(Calendar.DAY_OF_YEAR, -2)
            else cal.add(Calendar.DAY_OF_YEAR, -1)
            mutableStateOf(sdf.format(cal.time))
        } catch (e: Exception) {
            mutableStateOf(dateStr)
        }
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF18181B) else Color.White,
            border = BorderStroke(0.5.dp, borderColor),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (existingTargetRule != null) {
                    // 当前日期已被设置为调休补课日
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = primaryColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "调休中",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "调休补课日",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "该日期 (${formatDateForDisplay(dateStr)}) 当前安排的是「${formatDateForDisplay(existingTargetRule.sourceDate)}」的课程。",
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "管理全部调休 >",
                            fontSize = 12.sp,
                            color = primaryColor,
                            modifier = Modifier.clickable { onDismiss(); onOpenFullManager() }
                        )

                        Row {
                            TextButton(onClick = onDismiss) {
                                Text("关闭", color = textColor.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    onDeleteRule(existingTargetRule.id)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = deleteColor, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("取消调休")
                            }
                        }
                    }
                } else if (existingSourceRule != null) {
                    // 当前日期是被调走的放假日
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "已放假",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "已调休放假",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "该日期 (${formatDateForDisplay(dateStr)}) 原有课程已调至「${formatDateForDisplay(existingSourceRule.targetDate)}」上课。",
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "管理全部调休 >",
                            fontSize = 12.sp,
                            color = primaryColor,
                            modifier = Modifier.clickable { onDismiss(); onOpenFullManager() }
                        )

                        Row {
                            TextButton(onClick = onDismiss) {
                                Text("关闭", color = textColor.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    onDeleteRule(existingSourceRule.id)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("恢复原课表")
                            }
                        }
                    }
                } else {
                    // 新增调休：以当前长按的日期作为目标补课日
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "设为调休上课日",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "目标补课日：${formatDateForDisplay(dateStr)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "选择要调过来的课程日期（原上课日）：",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaceColor)
                            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                showDatePicker(context, sourceDate) { selected ->
                                    sourceDate = selected
                                    errorMessage = null
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDateForDisplay(sourceDate),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "选定日期的课表将临时移动至本日；原上课日将被设为放假。",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f),
                        lineHeight = 15.sp
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage!!, fontSize = 12.sp, color = Color(0xFFDC2626))
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "管理全部调休 >",
                            fontSize = 12.sp,
                            color = primaryColor,
                            modifier = Modifier.clickable { onDismiss(); onOpenFullManager() }
                        )

                        Row {
                            TextButton(onClick = onDismiss) {
                                Text("取消", color = textColor.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (sourceDate == dateStr) {
                                        errorMessage = "被调日期不能和目标日期相同喵！"
                                        return@Button
                                    }
                                    onSaveRule(sourceDate, dateStr)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("确定调休")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MakeUpFeatureGuideDialog(
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val primaryColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF18181B) else Color.White,
            border = BorderStroke(0.5.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 顶部标题区
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = primaryColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "新功能上线",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "调休快捷换课 · 使用指南",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "法定节假日遇上调休补课？课表随心对调，闹钟小组件全自动同步！",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.6f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 功能卡片列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuideItemCard(
                        isDark = isDark,
                        icon = Icons.Rounded.TouchApp,
                        iconTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        title = "1. 长按表头，极速设调休",
                        desc = "在课表主界面长按任何一天（例如调为上课的周日），即可直接唤起快捷弹窗，选择需要补上的原课程日期。",
                        tag = "快捷操作"
                    )

                    GuideItemCard(
                        isDark = isDark,
                        icon = Icons.Rounded.Tune,
                        iconTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                        title = "2. 顶部「+」菜单集中管理",
                        desc = "点击主界面顶部右上角「+」菜单选择「调休快捷换课」，可一览当前所有的调休安排，并支持随时删除与恢复。",
                        tag = "全面管理"
                    )

                    GuideItemCard(
                        isDark = isDark,
                        icon = Icons.Rounded.CropFree,
                        iconTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        title = "3. 醒目细边框高亮提醒",
                        desc = "调休补课日整列将以专属高亮细边框与柔和底色强调，表头附有「调休」角标；被调放假日则自动清空并标注「放假」。",
                        tag = "界面强调"
                    )

                    GuideItemCard(
                        isDark = isDark,
                        icon = Icons.Rounded.Widgets,
                        iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                        title = "4. 小组件与闹钟提醒全联动",
                        desc = "桌面日小组件、周小组件及后台上课闹钟、TTS 语音播报均会自动识别调休，补课日准时提醒！",
                        tag = "系统联动"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "我知道啦，去试试",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideItemCard(
    isDark: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String,
    tag: String
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF222227) else Color(0xFFF8FAFC)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = iconTint.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.65f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

