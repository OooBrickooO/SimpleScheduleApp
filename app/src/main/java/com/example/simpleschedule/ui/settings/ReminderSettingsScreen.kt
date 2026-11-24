package com.example.simpleschedule.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.receiver.ReminderEngine
import com.example.simpleschedule.ui.common.AppBackHandler
import com.example.simpleschedule.ui.common.SettingCheckboxItem
import com.example.simpleschedule.ui.common.SettingCheckboxItemWithSubtext
import com.example.simpleschedule.ui.common.SettingValueItem
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import kotlin.math.roundToInt

@Composable
fun ReminderSettingsScreen(viewModel: ScheduleViewModel, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val context = LocalContext.current

    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderNotifyEnabled by viewModel.reminderNotifyEnabled.collectAsState()
    val reminderVoiceEnabled by viewModel.reminderVoiceEnabled.collectAsState()
    val reminderAdvanceMins by viewModel.reminderAdvanceMins.collectAsState()
    val dynamicIslandEnabled by viewModel.dynamicIslandEnabled.collectAsState()

    var sliderAdvanceMins by remember(reminderAdvanceMins) { mutableIntStateOf(reminderAdvanceMins) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var hasExactAlarmPermission by remember { mutableStateOf(true) }
    var hasBatteryExemption by remember { mutableStateOf(true) }
    var hasPromotionPermission by remember { mutableStateOf(true) }

    val checkAllPermissions = {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.areNotificationsEnabled()
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        hasBatteryExemption = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true

        hasPromotionPermission = if (Build.VERSION.SDK_INT >= 36) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.canPostPromotedNotifications()
        } else true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            Text("课前提醒", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingCheckboxItemWithSubtext(
                            title = "启用上课提醒",
                            subtext = "在系统后台自动计算下一节课的时间，并在上课前通过闹钟准时触发提醒。需确保已授予后台允许及精确闹钟权限",
                            checked = reminderEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                            Toast.makeText(context, "请先授予精确闹钟权限喵！", Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                        }
                                        return@SettingCheckboxItemWithSubtext
                                    }
                                }
                                viewModel.updateSetting(SettingsKeys.REMINDER_ENABLED, checked)
                            },
                            textColor = textColor,
                            borderColor = borderColor,
                            isDark = isDark,
                            showBottomBorder = reminderEnabled
                        )

                        AnimatedVisibility(
                            visible = reminderEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                ) {
                                    Text("提前提醒时间: $sliderAdvanceMins 分钟", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    Slider(
                                        value = sliderAdvanceMins.toFloat(),
                                        onValueChange = { sliderAdvanceMins = it.roundToInt() },
                                        onValueChangeFinished = {
                                            viewModel.updateSetting(SettingsKeys.REMINDER_ADVANCE_MINS, sliderAdvanceMins)
                                        },
                                        valueRange = 1f..30f,
                                        steps = 28,
                                        colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "提示：如果遇到上一门课上完了而接下来还有课，且课间休息短于您设置的提前时间，系统将在上节课刚下课时立刻为您播报",
                                        fontSize = 10.sp,
                                        color = textColor.copy(alpha = 0.6f),
                                        lineHeight = 14.sp
                                    )
                                }

                                SettingCheckboxItem(
                                    title = "显示通知栏提醒",
                                    checked = reminderNotifyEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                            val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                            } else {
                                                notificationManager.areNotificationsEnabled()
                                            }

                                            if (!isGranted) {
                                                openNotificationSettings(context)
                                                Toast.makeText(context, "请先授予通知权限喵！", Toast.LENGTH_SHORT).show()
                                                return@SettingCheckboxItem
                                            }
                                        }
                                        viewModel.updateSetting(SettingsKeys.REMINDER_NOTIFY_ENABLED, checked)
                                    },
                                    textColor = textColor,
                                    borderColor = borderColor,
                                    isDark = isDark,
                                    showBottomBorder = true
                                )

                                AnimatedVisibility(
                                    visible = reminderNotifyEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                            .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                                    ) {
                                        SettingCheckboxItemWithSubtext(
                                            title = "使用实时通知",
                                            subtext = "在支持的系统上显示实时通知，需要Android 16+",
                                            checked = dynamicIslandEnabled,
                                            onCheckedChange = { viewModel.updateSetting(SettingsKeys.DYNAMIC_ISLAND_ENABLED, it) },
                                            textColor = textColor,
                                            borderColor = if (dynamicIslandEnabled) borderColor.copy(alpha = 0.2f) else Color.Transparent,
                                            isDark = isDark,
                                            showBottomBorder = false
                                        )
                                    }
                                }

                                SettingCheckboxItem(
                                    title = "开启语音播报",
                                    checked = reminderVoiceEnabled,
                                    onCheckedChange = { viewModel.updateSetting(SettingsKeys.REMINDER_VOICE_ENABLED, it) },
                                    textColor = textColor,
                                    borderColor = borderColor,
                                    isDark = isDark,
                                    showBottomBorder = true
                                )

                                SettingValueItem(
                                    title = "发送一条测试提醒",
                                    value = "5秒后触发",
                                    showBottomBorder = false,
                                    textColor = textColor,
                                    borderColor = borderColor,
                                    onClick = {
                                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                            Toast.makeText(context, "请先允许精确闹钟权限喵", Toast.LENGTH_SHORT).show()
                                            return@SettingValueItem
                                        }
                                        val triggerTime = System.currentTimeMillis() + 5000
                                        val classStartTime = triggerTime + 20000
                                        val classEndTime = classStartTime + 35000
                                        ReminderEngine.scheduleAlarmByParams(
                                            context,
                                            "【测试】课A",
                                            "地球",
                                            "测试",
                                            triggerTime,
                                            classStartTime,
                                            classEndTime,
                                            reminderNotifyEnabled,
                                            reminderVoiceEnabled,
                                            "slate"
                                        )
                                        Toast.makeText(context, "已设置测试提醒：5秒后触发，25秒后上课，60秒后下课。请锁定屏幕或退回桌面测试喵！", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("权限自检与后台兼容", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        PermissionCheckRow(
                            title = "通知栏通知权限",
                            description = "接收上课课程详情通知的必要前提",
                            granted = hasNotificationPermission,
                            isDark = isDark,
                            textColor = textColor,
                            borderColor = borderColor,
                            onGrantClick = { openNotificationSettings(context) }
                        )

                        PermissionCheckRow(
                            title = "精确闹钟权限",
                            description = "允许系统后台计算闹钟并准时发出课前通知",
                            granted = hasExactAlarmPermission,
                            isDark = isDark,
                            textColor = textColor,
                            borderColor = borderColor,
                            onGrantClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        })
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    }
                                }
                            }
                        )

                        PermissionCheckRow(
                            title = "忽略电池优化 (防杀后台)",
                            description = "避免系统因休眠清理后台进程导致提醒失效",
                            granted = hasBatteryExemption,
                            isDark = isDark,
                            textColor = textColor,
                            borderColor = borderColor,
                            showBottomBorder = Build.VERSION.SDK_INT >= 36,
                            onGrantClick = {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        })
                                    }
                                } catch (_: Exception) {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                                        .onFailure { Toast.makeText(context, "无法打开电池优化设置", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        )

                        if (Build.VERSION.SDK_INT >= 36) {
                            PermissionCheckRow(
                                title = "实况通知权限 (安卓16+)",
                                description = "允许在状态栏展示实时通知/Live Update倒计时",
                                granted = hasPromotionPermission,
                                isDark = isDark,
                                textColor = textColor,
                                borderColor = borderColor,
                                showBottomBorder = false,
                                onGrantClick = {
                                    try {
                                        val intent = Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").apply {
                                            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "无法打开实况通知设置，请前往系统通知设置手动开启", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF291E1A) else Color(0xFFFFF7ED), RoundedCornerShape(12.dp))
                        .border(0.5.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFED7AA), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = "OEM Tips", tint = if (isDark) Color(0xFFFDBA74) else Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("后台兼容与实时通知适配建议", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "【重要提醒】\n" +
                                "部分系统（如 ColorOS、OriginOS、HyperOS、HarmonyOS）在执行「一键清理/优化内存」时会强制注销本应用的所有后台提醒闹钟。若想保证提醒 100% 准时，请务必在「最近任务/多任务卡片」页面对本应用卡片下拉加锁，并确保开启「自启动」与电池「无限制」权限喵！\n\n" +
                                "对于国产定制系统，为保证提醒准时与实时通知正常显示：\n" +
                                "1. ColorOS 16 流体云需要进入「设置-通知与控制中心-流体云」，拉到最下方为应用单独勾选「允许显示流体云」。\n" +
                                "2. ColorOS 16 需要进入「设置-小布助手-小布建议」，开启通勤等对应的场景开关，否则胶囊会被静默丢弃喵。\n" +
                                "3. 通用设置：请开启应用的「自启动」或「允许后台启动」权限，并将省电策略设为「无限制」或「允许高耗电」。\n" +
                                "4. 如有异常，可尝试在开发者选项中开启「流体云调试模式」排查拦截原因，或清除「智慧决策服务」等系统组件缓存喵。\n" +
                                "5. 小米系列设备发送实时通知需要 HyperOS 3.0.300 以上版本，并且需要让 APP 在后台运行喵。",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

@Composable
fun PermissionCheckRow(
    title: String,
    description: String,
    granted: Boolean,
    isDark: Boolean,
    textColor: Color,
    borderColor: Color,
    showBottomBorder: Boolean = true,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 11.sp, color = textColor.copy(alpha = 0.5f), lineHeight = 14.sp)
        }

        Button(
            onClick = { if (!granted) onGrantClick() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) {
                    if (isDark) Color(0xFF065F46) else Color(0xFFD1FAE5)
                } else {
                    if (isDark) Color(0xFF991B1B) else Color(0xFFFEE2E2)
                },
                contentColor = if (granted) {
                    if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
                } else {
                    if (isDark) Color(0xFFF87171) else Color(0xFF991B1B)
                }
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(text = if (granted) "已授予" else "去授予", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    if (showBottomBorder) {
        HorizontalDivider(color = borderColor, thickness = 0.5.dp)
    }
}
