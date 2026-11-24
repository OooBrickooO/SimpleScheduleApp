package com.example.simpleschedule.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.data.local.room.TimetableGroup
import com.example.simpleschedule.data.repository.ScheduleRepository
import com.example.simpleschedule.ui.common.AppBackHandler
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.utils.addMinutes
import com.example.simpleschedule.utils.showTimePicker
import com.example.simpleschedule.viewmodel.ScheduleViewModel

@Composable
fun TimetableListScreen(
    viewModel: ScheduleViewModel,
    timetables: List<TimetableGroup>,
    currentLinkedId: String,
    isDark: Boolean,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (String?) -> Unit,
    onDelete: (String) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White

    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val backModifier = AppBackHandler(predictiveBackEnabled) { onBack() }

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
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.clickable { onBack() }.padding(8.dp)
                )
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
                items(timetables, key = { it.id }) { tGroup ->
                    var showMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(bottom = 12.dp)
                            .background(surfaceColor, RoundedCornerShape(12.dp))
                            .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { onSelect(tGroup.id) }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tGroup.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Box {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = textColor.copy(alpha = 0.5f), modifier = Modifier.clickable { showMenu = true })
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(if (isDark) Color(0xFF18181B) else Color.White).border(0.5.dp, borderColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✏️ 编辑内容", fontWeight = FontWeight.Bold, color = textColor) },
                                    onClick = { showMenu = false; onEdit(tGroup.id) }
                                )
                                if (tGroup.id !in ScheduleRepository.BUILT_IN_TIMETABLE_IDS) {
                                    HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                                    DropdownMenuItem(
                                        text = { Text("🗑️ 删除时间表", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
                                        onClick = { showMenu = false; timetableIdToDelete = tGroup.id }
                                    )
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
                text = { Text("确定要删除时间表「${targetGroup?.name ?: ""}」吗？删除后原关联此时间表的课表将自动回退到默认时间表喵。", color = textColor) },
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

        val accent = LocalAccentColor.current
        FloatingActionButton(
            onClick = { onEdit(null) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp),
            containerColor = accent ?: textColor,
            contentColor = if (accent != null) (if (isDark) Color.Black else Color.White) else (if (isDark) BgDark else BgLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Add, "Add Timetable")
        }
    }
}

@Composable
fun TimetableEditScreen(
    timetableId: String?,
    timetables: List<TimetableGroup>,
    viewModel: ScheduleViewModel,
    isDark: Boolean,
    onBack: () -> Unit,
    onSave: (String?, String, List<TimeNode>) -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White
    val context = LocalContext.current

    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val backModifier = AppBackHandler(predictiveBackEnabled) { onBack() }

    var name by remember { mutableStateOf("") }
    var isSameDuration by remember { mutableStateOf(true) }
    var classDuration by remember { mutableStateOf("45") }
    var nodes by remember { mutableStateOf<List<TimeNode>>(emptyList()) }
    var showDeleteNodeDialog by remember { mutableStateOf(false) }
    var selectedNodeIds by remember { mutableStateOf<Set<String>>(emptySet()) }

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
            Text(if (timetableId == null) "新建时间表" else "编辑时间表", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    if (nodes.size > 1) {
                        selectedNodeIds = emptySet()
                        showDeleteNodeDialog = true
                    }
                },
                enabled = nodes.size > 1
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除时间节点",
                    tint = if (nodes.size > 1) Color(0xFFDC2626) else textColor.copy(alpha = 0.25f)
                )
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("每节课时长相同", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    val accent = LocalAccentColor.current ?: textColor
                    Switch(checked = isSameDuration, onCheckedChange = { isSameDuration = it }, colors = SwitchDefaults.colors(checkedThumbColor = if (isDark) BgDark else BgLight, checkedTrackColor = accent))
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (isSameDuration) {
                    Row(
                        modifier = Modifier.fillMaxWidth().animateContentSize().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth().animateContentSize().padding(bottom = 12.dp).background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, borderColor)
                ) {
                    Text("➕ 增加一节课", color = textColor)
                }
            }
        }
    }

    if (showDeleteNodeDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteNodeDialog = false },
            title = { Text("删除时间节点", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    Text("可批量选择要删除的节次，删除后后面的节次会自动前移。", color = textColor.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    val selectableNodeIds = nodes.drop(1).map { it.id }.toSet()
                    val allSelected = selectableNodeIds.isNotEmpty() && selectedNodeIds.containsAll(selectableNodeIds)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedNodeIds = if (allSelected) emptySet() else selectableNodeIds
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = allSelected, onCheckedChange = {
                            selectedNodeIds = if (it) selectableNodeIds else emptySet()
                        })
                        Text("全选（保留第 1 节）", color = textColor, fontWeight = FontWeight.Bold)
                    }
                    nodes.forEach { node ->
                        val isFirstNode = node.id == nodes.firstOrNull()?.id
                        val isSelected = selectedNodeIds.contains(node.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isFirstNode) {
                                    selectedNodeIds = if (isSelected) selectedNodeIds - node.id else selectedNodeIds + node.id
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isSelected, enabled = !isFirstNode, onCheckedChange = { checked ->
                                    selectedNodeIds = if (checked) selectedNodeIds + node.id else selectedNodeIds - node.id
                                })
                                Column {
                                    Text("第 ${node.nodeIndex} 节", color = if (isFirstNode) textColor.copy(alpha = 0.45f) else textColor, fontWeight = FontWeight.Bold)
                                    Text("${node.startTime} - ${node.endTime}", color = textColor.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedNodeIds.isNotEmpty() && selectedNodeIds.size < nodes.size,
                    onClick = {
                        nodes = nodes
                            .filterNot { selectedNodeIds.contains(it.id) }
                            .mapIndexed { index, remaining -> remaining.copy(nodeIndex = index + 1) }
                        selectedNodeIds = emptySet()
                        showDeleteNodeDialog = false
                    }
                ) {
                    Text("删除所选", color = if (selectedNodeIds.isNotEmpty() && selectedNodeIds.size < nodes.size) Color(0xFFDC2626) else textColor.copy(alpha = 0.35f))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedNodeIds = emptySet()
                    showDeleteNodeDialog = false
                }) {
                    Text("取消", color = textColor.copy(alpha = 0.6f))
                }
            },
            containerColor = if (isDark) Color(0xFF18181B) else Color.White,
            titleContentColor = textColor,
            textContentColor = textColor.copy(alpha = 0.8f)
        )
    }
}
