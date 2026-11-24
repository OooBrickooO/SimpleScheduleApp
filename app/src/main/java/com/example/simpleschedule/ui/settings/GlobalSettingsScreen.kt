package com.example.simpleschedule.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.ui.common.AppBackHandler
import com.example.simpleschedule.ui.common.SettingCheckboxItem
import com.example.simpleschedule.ui.common.SettingCheckboxItemWithSubtext
import com.example.simpleschedule.ui.common.SettingItemWithSubtext
import com.example.simpleschedule.ui.common.SettingValueItem
import com.example.simpleschedule.ui.components.MakeUpManagerBottomSheet
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import com.example.simpleschedule.widget.CourseWeekWidgetReceiver
import com.example.simpleschedule.widget.CourseWidget2x2Receiver
import com.example.simpleschedule.widget.CourseWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun GlobalSettingsScreen(
    viewModel: ScheduleViewModel,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val bottomBlank by viewModel.bottomBlank.collectAsState()
    val vibration by viewModel.vibration.collectAsState()
    val materialYou by viewModel.materialYou.collectAsState()
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val widgetTranslucent by viewModel.widgetTranslucent.collectAsState()
    val floatingBottomBar by viewModel.floatingBottomBar.collectAsState()
    val tabAnimationType by viewModel.tabAnimationType.collectAsState()
    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val scheduleGroups by viewModel.scheduleGroups.collectAsState()
    val currentScheduleId by viewModel.currentScheduleId.collectAsState()
    val makeUpRules by viewModel.makeUpRules.collectAsState()

    var showWidgetPinDialog by remember { mutableStateOf(false) }
    var showScheduleSelectDialog by remember { mutableStateOf(false) }
    var showMakeUpSheet by remember { mutableStateOf(false) }

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
            Text("全局设置", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingItemWithSubtext(
                            title = "将课表添加到桌面",
                            subtext = "支持今日课表 4x2、紧凑 2x2 与全周课表 4x4 小组件（若启动器拦截请到桌面长按手动添加）",
                            textColor = textColor,
                            borderColor = borderColor,
                            onClick = { showWidgetPinDialog = true }
                        )
                        SettingCheckboxItemWithSubtext(
                            title = "桌面小部件半透明/毛玻璃化",
                            subtext = "开启后小部件背景将变为半透明（具体的毛玻璃模糊效果取决于系统桌面启动器支持）",
                            checked = widgetTranslucent,
                            onCheckedChange = {
                                viewModel.updateSetting(SettingsKeys.WIDGET_TRANSLUCENT, it)
                                viewModel.notifyWidgetUpdate()
                            },
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark
                        )
                        SettingCheckboxItem(
                            title = "根据桌面壁纸更改主题色",
                            checked = materialYou,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.MATERIAL_YOU, it) },
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark
                        )
                        SettingCheckboxItemWithSubtext(
                            title = "预测式返回效果",
                            subtext = "在支持的 Android 14+ 设备上侧滑返回将有缩放预览效果",
                            checked = predictiveBackEnabled,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.PREDICTIVE_BACK_ENABLED, it) },
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark
                        )
                        SettingCheckboxItemWithSubtext(
                            title = "悬浮底栏样式",
                            subtext = "开启后底栏将变为圆角悬浮样式",
                            checked = floatingBottomBar,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.FLOATING_BOTTOM_BAR, it) },
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark
                        )

                        var showAnimMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SettingValueItem(
                                title = "页面切换动画",
                                value = if (tabAnimationType == "Slide") "左右滑动" else "淡入淡出",
                                textColor = textColor,
                                borderColor = borderColor,
                                onClick = { showAnimMenu = true }
                            )
                            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)) {
                                DropdownMenu(
                                    expanded = showAnimMenu,
                                    onDismissRequest = { showAnimMenu = false },
                                    modifier = Modifier.background(if (isDark) Color(0xFF18181B) else Color.White).border(0.5.dp, borderColor)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("左右滑动", color = textColor, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.updateSetting(SettingsKeys.TAB_ANIMATION_TYPE, "Slide")
                                            showAnimMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("淡入淡出", color = textColor, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.updateSetting(SettingsKeys.TAB_ANIMATION_TYPE, "Fade")
                                            showAnimMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        SettingCheckboxItemWithSubtext(
                            title = "课表下方增加留白区域",
                            subtext = "开启后，课表下方会多出一段空白区域，便于将底部的课程滑动至屏幕中间查看",
                            checked = bottomBlank,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.BOTTOM_BLANK, it) },
                            showBottomBorder = false,
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text("课程设置", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        val currentScheduleName = scheduleGroups.find { it.id == currentScheduleId }?.name ?: ""
                        SettingValueItem(
                            title = "切换当前课表",
                            value = currentScheduleName,
                            textColor = textColor,
                            borderColor = borderColor,
                            onClick = { showScheduleSelectDialog = true }
                        )
                        SettingItemWithSubtext(
                            title = "节假日调休管理",
                            subtext = "将某天的课表临时对调到另一天，支持高亮提醒与小组件联动",
                            showBottomBorder = false,
                            textColor = textColor,
                            borderColor = borderColor,
                            onClick = { showMakeUpSheet = true }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text("其他", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingItemWithSubtext(
                            title = "允许后台运行和自启",
                            subtext = "点击后，一般在「耗电管理」或者跟电池相关的选项中进行设置",
                            textColor = textColor,
                            borderColor = borderColor,
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        SettingItemWithSubtext(
                            title = "忽略电池优化",
                            subtext = "忽略后，桌面小部件的刷新也许会更稳定一些哦，课前提醒也能更准时",
                            textColor = textColor,
                            borderColor = borderColor,
                            onClick = {
                                try {
                                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                            Toast.makeText(context, "已经忽略电池优化啦", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "无法请求电池优化权限", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        SettingCheckboxItem(
                            title = "启动时自动检查更新",
                            checked = autoUpdate,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.AUTO_UPDATE, it) },
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark
                        )
                        SettingValueItem(
                            title = "清除内置浏览器缓存与登录态",
                            value = "",
                            textColor = textColor,
                            borderColor = borderColor,
                            onClick = {
                                coroutineScope.launch {
                                    clearBuiltInBrowserCache(context)
                                    Toast.makeText(context, "内置浏览器 Cookie 与缓存已彻底清除", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        SettingCheckboxItemWithSubtext(
                            title = "振动反馈",
                            subtext = "点击、滑动等操作时有振动反馈",
                            checked = vibration,
                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.VIBRATION, it) },
                            showBottomBorder = false,
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp).navigationBarsPadding())
            }
        }
    }

    if (showWidgetPinDialog) {
        val options = listOf(
            "今日课表 (4×2)" to CourseWidgetReceiver::class.java,
            "今日课表紧凑版 (2×2)" to CourseWidget2x2Receiver::class.java,
            "全周课程表 (4×4)" to CourseWeekWidgetReceiver::class.java
        )
        AlertDialog(
            onDismissRequest = { showWidgetPinDialog = false },
            title = { Text("选择要添加的小组件", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { (label, cls) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showWidgetPinDialog = false
                                    requestPinWidget(context, cls)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWidgetPinDialog = false }) { Text("取消", color = textColor) }
            }
        )
    }

    if (showScheduleSelectDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleSelectDialog = false },
            title = { Text("切换当前课表", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    scheduleGroups.forEach { group ->
                        val selected = group.id == currentScheduleId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchSchedule(group.id)
                                    showScheduleSelectDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(group.name, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = textColor, modifier = Modifier.weight(1f))
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = textColor)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScheduleSelectDialog = false }) { Text("关闭", color = textColor) }
            }
        )
    }

    if (showMakeUpSheet) {
        MakeUpManagerBottomSheet(
            isDark = isDark,
            makeUpRules = makeUpRules,
            onDismiss = { showMakeUpSheet = false },
            onAddRule = { src, tgt -> viewModel.addMakeUpRule(src, tgt) },
            onDeleteRule = { id -> viewModel.deleteMakeUpRule(id) }
        )
    }
}

private fun requestPinWidget(context: Context, receiverClass: Class<*>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(context, receiverClass)
            val ok = manager.requestPinAppWidget(provider, null, null)
            Toast.makeText(
                context,
                if (ok) "已向桌面发起添加请求，请按系统提示确认" else "当前桌面暂不支持直接添加，请长按桌面空白处手动添加",
                Toast.LENGTH_LONG
            ).show()
            return
        }
    }
    Toast.makeText(context, "请返回手机桌面长按空白处，选择小组件添加 SimpleSchedule", Toast.LENGTH_LONG).show()
}

private suspend fun clearBuiltInBrowserCache(context: Context) {
    withContext(Dispatchers.Main) {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
    }
    withContext(Dispatchers.IO) {
        runCatching {
            File(context.cacheDir.parentFile, "app_webview").deleteRecursively()
        }
    }
}
