package com.example.simpleschedule.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight

enum class HelpFeatureType {
    TEXT_ONLY,
    INTERACTIVE
}

data class HelpFeatureItem(
    val title: String,
    val description: String,
    val detailedGuide: String = "",
    val keywords: List<String> = emptyList(),
    val type: HelpFeatureType = HelpFeatureType.TEXT_ONLY,
    val icon: ImageVector = Icons.AutoMirrored.Rounded.Article,
    val tint: Color = Color.Gray,
    val actionText: String = "",
    val onActionClick: (() -> Unit)? = null
)

@Composable
fun HelpSearchScreen(
    isDark: Boolean,
    onBack: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToReminderSettings: () -> Unit,
    onNavigateToAppearanceSettings: () -> Unit,
    onNavigateToDataManagement: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val bgColor = if (isDark) BgDark else BgLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White
    val borderColor = if (isDark) BorderDark else BorderLight

    var searchQuery by remember { mutableStateOf("") }

    val features = remember(isDark) {
        listOf(
            HelpFeatureItem(
                title = "数据管理与云备份 (WebDAV / SMB / JSON)",
                description = "支持本地 JSON 导入导出，以及坚果云等 WebDAV 与局域网 SMB 云备份恢复。",
                detailedGuide = "你可以导出全部课表与调休规则为 JSON，或配置 WebDAV / SMB 局域网共享进行云端加密同步，恢复时支持全量覆盖或增量合并。",
                keywords = listOf("备份", "恢复", "导出", "导入", "json", "webdav", "smb", "坚果云", "云同步", "换机"),
                type = HelpFeatureType.INTERACTIVE,
                icon = Icons.Rounded.CloudSync,
                tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                actionText = "前往数据管理",
                onActionClick = onNavigateToDataManagement
            ),
            HelpFeatureItem(
                title = "全局主色调与课程颜色池",
                description = "支持色域盘自定义全局 Accent 主题色，以及圈选 10 种课表色池。",
                detailedGuide = "在外观设置中点击或拖动色域盘即可实时预览并生成属于你的课表配色方案。",
                keywords = listOf("配色", "颜色", "主题色", "色域", "外观", "自定义"),
                type = HelpFeatureType.INTERACTIVE,
                icon = Icons.Rounded.Palette,
                tint = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                actionText = "前往外观设置",
                onActionClick = onNavigateToAppearanceSettings
            ),
            HelpFeatureItem(
                title = "自动上课闹钟与提醒",
                description = "利用系统闹钟在上课前指定时间自动响铃，支持 TTS 语音播报与实时通知。",
                keywords = listOf("闹钟", "响铃", "上课提醒", "忘了上课", "语音", "播报", "铃声", "流体云"),
                type = HelpFeatureType.TEXT_ONLY,
                icon = Icons.Rounded.Alarm,
                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
            ),
            HelpFeatureItem(
                title = "长按调休 (快捷换课)",
                description = "将原定的放假日与上课日临时对调，并自动同步系统小组件和闹钟。",
                detailedGuide = "在主界面课表中，长按你需要将课程「挪过去」的那天表头（例如被调成工作日的周日），即可唤出快捷调休面板。选择原课程日期后，该日整列将被高亮显示，并标注「调休」角标。",
                keywords = listOf("周末", "放假", "补课", "节假日", "挪课", "换课", "调休"),
                type = HelpFeatureType.INTERACTIVE,
                icon = Icons.Rounded.TouchApp,
                tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                actionText = "回到课表试试看",
                onActionClick = onNavigateToTimetable
            ),
            HelpFeatureItem(
                title = "长按拖拽调课",
                description = "在日历格子里直接按住某节课，并将其拖动到其他日期或时间。",
                detailedGuide = "主界面中，长按任意课程卡片进入拖拽状态，将其拖动至目标节次松手，即可直接完成临时调课！",
                keywords = listOf("换课", "挪课", "换时间", "拖动", "移课", "移动"),
                type = HelpFeatureType.INTERACTIVE,
                icon = Icons.Rounded.Swipe,
                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                actionText = "回到课表试试看",
                onActionClick = onNavigateToTimetable
            ),
            HelpFeatureItem(
                title = "启用课前提醒功能",
                description = "在后台自动识别即将开始的课程，并通过系统闹钟唤醒你。",
                detailedGuide = "你需要进入「课前提醒」开启此功能，并授予应用精准闹钟与后台运行权限，以确保提醒能够准时触发。",
                keywords = listOf("权限", "后台", "不响铃", "闹钟不响", "无法提醒", "精确闹钟", "杀后台"),
                type = HelpFeatureType.INTERACTIVE,
                icon = Icons.Rounded.NotificationsActive,
                tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                actionText = "前往课前提醒设置",
                onActionClick = onNavigateToReminderSettings
            ),
            HelpFeatureItem(
                title = "明暗模式切换",
                description = "在「我的」界面下方可直接切换跟随系统、浅色或深色模式。",
                keywords = listOf("夜间模式", "黑夜", "白天", "深色", "浅色", "主题", "暗黑", "跟随系统"),
                type = HelpFeatureType.TEXT_ONLY,
                icon = Icons.Rounded.DarkMode,
                tint = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
            ),
            HelpFeatureItem(
                title = "课表导入 (教务系统 / CSV / XLS)",
                description = "点击主界面右上角「+」，可导入 CSV 表格、青果 XLS 或通过内置浏览器提取教务系统课表与考试。",
                keywords = listOf("导入", "添加课表", "抓取", "教务处", "正方", "青果", "Excel", "表格", "解析"),
                type = HelpFeatureType.TEXT_ONLY,
                icon = Icons.Rounded.Download,
                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            ),
            HelpFeatureItem(
                title = "桌面与锁屏小组件",
                description = "可在「全局设置 -> 将课表添加到桌面」一键发起添加，或长按桌面空白处添加日/周视图小组件。",
                keywords = listOf("桌面", "挂件", "日视图", "周视图", "卡片", "快捷方式", "小组件"),
                type = HelpFeatureType.TEXT_ONLY,
                icon = Icons.Rounded.Widgets,
                tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
            )
        ).sortedBy { it.title }
    }

    val filteredFeatures = remember(searchQuery, features) {
        if (searchQuery.isBlank()) features else features.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.keywords.any { keyword -> keyword.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Text("功能查找与使用帮助", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            placeholder = { Text("搜索功能...", color = textColor.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = textColor.copy(alpha = 0.5f)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderColor,
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredFeatures, key = { it.title }) { feature ->
                HelpFeatureCard(feature = feature, isDark = isDark, onBack = onBack)
            }
        }
    }
}

@Composable
fun HelpFeatureCard(feature: HelpFeatureItem, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val surfaceColor = if (isDark) Color(0xFF222227) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) BorderDark else BorderLight

    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "help_arrow")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = feature.tint.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = feature.icon, contentDescription = null, tint = feature.tint, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = feature.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = feature.description, fontSize = 12.sp, color = textColor.copy(alpha = 0.65f), lineHeight = 16.sp)
                }

                if (feature.type == HelpFeatureType.INTERACTIVE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp).rotate(arrowRotation)
                    )
                }
            }

            if (feature.type == HelpFeatureType.INTERACTIVE) {
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        HorizontalDivider(color = borderColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = feature.detailedGuide, fontSize = 13.sp, color = textColor.copy(alpha = 0.8f), lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { feature.onActionClick?.invoke() ?: onBack() },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                        ) {
                            Text(text = feature.actionText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
