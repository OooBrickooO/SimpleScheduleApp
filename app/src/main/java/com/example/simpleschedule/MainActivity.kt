package com.example.simpleschedule

import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.room.*
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import com.example.simpleschedule.ui.theme.*
import com.example.simpleschedule.ui.screens.*
import com.example.simpleschedule.widget.WidgetUpdateWorker

import android.annotation.SuppressLint
import java.io.InputStream
import jxl.Workbook
import android.app.Application
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable as glanceClickable
import androidx.glance.background as glanceBackground
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize as glanceFillMaxSize
import androidx.glance.layout.fillMaxWidth as glanceFillMaxWidth
import androidx.glance.layout.height as glanceHeight
import androidx.glance.layout.padding as glancePadding
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.TextAlign as GlanceTextAlign
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.simpleschedule.data.local.datastore.dataStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            var updateInfoToShow by remember { mutableStateOf<UpdateInfo?>(null) }
            var isCheckingManualUpdate by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "注意：请前往设置允许应用的「精确闹钟」权限，否则桌面卡片将无法准时刷新喵！", Toast.LENGTH_LONG).show()
                }

                val fallbackRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "WidgetFallback",
                    ExistingPeriodicWorkPolicy.KEEP,
                    fallbackRequest
                )

                // 启动时自动检查更新
                launch {
                    val enabled = try {
                        context.dataStore.data.map { it[SettingsKeys.AUTO_UPDATE] ?: false }.first()
                    } catch (e: Exception) {
                        false
                    }
                    if (enabled) {
                        val update = checkUpdate(UPDATE_URL)
                        if (update != null) {
                            val currentVersionCode = try {
                                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    packageInfo.longVersionCode.toInt()
                                } else {
                                    @Suppress("DEPRECATION")
                                    packageInfo.versionCode
                                }
                            } catch (e: Exception) {
                                1
                            }
                            if (update.versionCode > currentVersionCode) {
                                updateInfoToShow = update
                            }
                        }
                    }
                }
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDarkSetting by viewModel.isDarkTheme.collectAsState()
            val isDark = isDarkSetting ?: isSystemDark
            val materialYou by viewModel.materialYou.collectAsState()

            val currentWeek by viewModel.currentWeek.collectAsState()
            val displayCourses by viewModel.displayCourses.collectAsState()
            val scheduleGroups by viewModel.scheduleGroups.collectAsState()
            val currentScheduleId by viewModel.currentScheduleId.collectAsState()
            val activeTimeNodes by viewModel.activeTimeNodes.collectAsState()
            val timetableGroups by viewModel.timetableGroups.collectAsState()
            val totalWeeks by viewModel.totalWeeks.collectAsState()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshCurrentWeek()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            var currentRoute by remember { mutableStateOf("main") }
            var currentTab by remember { mutableIntStateOf(0) }
            var courseToEdit by remember { mutableStateOf<Course?>(null) }
            var editingTimetableId by remember { mutableStateOf<String?>(null) }
            var showAddDialog by remember { mutableStateOf(false) }
            var showShareCodeDialog by remember { mutableStateOf(false) }
            var showCreateScheduleDialog by remember { mutableStateOf(false) }

            SimpleScheduleTheme(
                darkTheme = isDark,
                dynamicColor = materialYou
            ) {
                val animatedBgColor by animateColorAsState(
                    targetValue = if (isDark) {
                        if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MaterialTheme.colorScheme.background else BgDark
                    } else {
                        if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MaterialTheme.colorScheme.background else BgLight
                    },
                    animationSpec = tween(500),
                    label = "bg"
                )

                Surface(modifier = Modifier.fillMaxSize(), color = animatedBgColor) {
                    DotMatrixBackground(isDark = isDark)

                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        val springSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                        (fadeIn(animationSpec = springSpec) + scaleIn(initialScale = 0.95f, animationSpec = springSpec)) togetherWith fadeOut(animationSpec = springSpec)
                    },
                    label = "route_anim",
                    modifier = Modifier.fillMaxSize()
                ) { route ->
                    when (route) {
                        "course_management" -> {
                            CourseManagementScreen(
                                courses = displayCourses.map { it.course }.distinctBy { it.id },
                                isDark = isDark,
                                materialYou = materialYou,
                                onBack = { currentRoute = "main" },
                                onEditCourse = { courseToEdit = it; showAddDialog = true },
                                onDeleteCourse = { viewModel.deleteCourse(it) }
                            )
                        }
                        "timetable_list" -> {
                            val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }
                            TimetableListScreen(
                                timetables = timetableGroups,
                                currentLinkedId = currentSchedule?.timetableId ?: "tt_cjlu",
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onSelect = { viewModel.linkTimetableToCurrentSchedule(it) },
                                onEdit = { id -> editingTimetableId = id; currentRoute = "timetable_edit" },
                                onDelete = { viewModel.deleteTimetable(it) }
                            )
                        }
                        "timetable_edit" -> {
                            TimetableEditScreen(
                                timetableId = editingTimetableId,
                                timetables = timetableGroups,
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "timetable_list" },
                                onSave = { id, name, nodes ->
                                    viewModel.saveTimetable(id, name, nodes)
                                    currentRoute = "timetable_list"
                                }
                            )
                        }
                        "schedule_settings" -> {
                            ScheduleSettingsScreen(
                                viewModel = viewModel,
                                scheduleGroups = scheduleGroups,
                                currentScheduleId = currentScheduleId,
                                currentWeek = currentWeek,
                                timeNodeCount = activeTimeNodes.size,
                                totalWeeks = totalWeeks,
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onRenameSchedule = { id, newName -> viewModel.renameSchedule(id, newName) },
                                onWeekChange = { viewModel.updateWeekAndReverseCalculateStartDate(currentScheduleId, it) },
                                onStartDateChange = { viewModel.updateScheduleStartDate(currentScheduleId, it) },
                                onTotalWeeksChange = { viewModel.updateSetting(SettingsKeys.TOTAL_WEEKS, it) },
                                onManageTimetableClick = { currentRoute = "timetable_list" },
                                onManageCoursesClick = { currentRoute = "course_management" },
                                onMoreAppearanceClick = { currentRoute = "appearance_settings" }
                            )
                        }
                        "appearance_settings" -> {
                            AppearanceSettingsScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "schedule_settings" }
                            )
                        }
                        "global_settings" -> {
                            GlobalSettingsScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "main" },
                                onAdjustCourseClick = { currentRoute = "adjust_course" }
                            )
                        }
                        "adjust_course" -> {
                            AdjustCourseScreen(isDark = isDark, onBack = { currentRoute = "global_settings" })
                        }
                        "webview_import" -> {
                            WebViewImportScreen(
                                isDark = isDark,
                                activeTimeNodes = activeTimeNodes,
                                startDate = scheduleGroups.find { it.id == currentScheduleId }?.startDate ?: "",
                                hasCourses = displayCourses.isNotEmpty(),
                                onBack = { currentRoute = "main" },
                                onImport = { json ->
                                    viewModel.importFromJson(json) { success ->
                                        Toast.makeText(context, if (success) "导入成功" else "解析失败", Toast.LENGTH_SHORT).show()
                                        if (success) currentRoute = "main"
                                    }
                                }
                            )
                        }
                        "reminder_settings" -> {
                            ReminderSettingsScreen(
                                viewModel = viewModel,
                                isDark = isDark,
                                onBack = { currentRoute = "main" }
                            )
                        }
                        else -> {
                            Scaffold(
                                containerColor = Color.Transparent,
                                bottomBar = { BottomNavBar(isDark = isDark, currentTab = currentTab, onTabSelected = { currentTab = it }) }
                            ) { paddingValues ->
                                Crossfade(
                                    targetState = currentTab,
                                    label = "tab_anim",
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) { tab ->
                                    Box(modifier = Modifier.padding(paddingValues)) {
                                        if (tab == 0) {
                                            TimetableScreen(
                                                viewModel = viewModel,
                                                courses = displayCourses,
                                                timeNodes = activeTimeNodes,
                                                currentWeek = currentWeek,
                                                totalWeeks = totalWeeks,
                                                scheduleGroups = scheduleGroups,
                                                currentScheduleId = currentScheduleId,
                                                isDark = isDark,
                                                onAddClick = { courseToEdit = null; showAddDialog = true },
                                                onManageCoursesClick = { currentRoute = "course_management" },
                                                onManageTimetablesClick = { currentRoute = "timetable_list" },
                                                onScheduleSettingsClick = { currentRoute = "schedule_settings" },
                                                onGlobalSettingsClick = { currentRoute = "global_settings" },
                                                onEditCourse = { courseToEdit = it; showAddDialog = true },
                                                onWebViewImportClick = { currentRoute = "webview_import" },
                                                onShareCodeClick = { showShareCodeDialog = true },
                                                onCreateScheduleClick = { showCreateScheduleDialog = true },
                                                onReminderSettingsClick = { currentRoute = "reminder_settings" }
                                            )
                                        } else {
                                            ProfileScreen(
                                                isDark = isDark,
                                                onThemeToggle = { viewModel.toggleTheme(it) },
                                                onCheckUpdateClick = {
                                                    if (!isCheckingManualUpdate) {
                                                        isCheckingManualUpdate = true
                                                        Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
                                                        coroutineScope.launch {
                                                            val update = checkUpdate(UPDATE_URL)
                                                            isCheckingManualUpdate = false
                                                            if (update != null) {
                                                                val currentVersionCode = try {
                                                                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                                        packageInfo.longVersionCode.toInt()
                                                                    } else {
                                                                        @Suppress("DEPRECATION")
                                                                        packageInfo.versionCode
                                                                    }
                                                                } catch (e: Exception) {
                                                                    1
                                                                }
                                                                if (update.versionCode > currentVersionCode) {
                                                                    updateInfoToShow = update
                                                                } else {
                                                                    Toast.makeText(context, "当前已经是最新版本喵！", Toast.LENGTH_SHORT).show()
                                                                }
                                                            } else {
                                                                Toast.makeText(context, "检查更新失败，请稍后重试", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddDialog) {
                    CourseEditDialog(
                        isDark = isDark,
                        initialCourse = courseToEdit,
                        onDismiss = { showAddDialog = false; courseToEdit = null },
                        onConfirm = { id, name, loc, t, d, s, e, c, w, credits ->
                            if (id == null) {
                                viewModel.addCustomCourse(name, loc, t, d, s, e, c, credits)
                            } else {
                                viewModel.updateCustomCourse(id, name, loc, t, d, s, e, c, w, credits)
                            }
                            showAddDialog = false
                            courseToEdit = null
                        }
                    )
                }

                if (showShareCodeDialog) {
                    ShareCodeDialog(
                        isDark = isDark,
                        onDismiss = { showShareCodeDialog = false },
                        onConfirm = { code ->
                            viewModel.importFromShareCode(code) { success ->
                                Toast.makeText(context, if (success) "口令解析成功" else "无效口令", Toast.LENGTH_SHORT).show()
                                if(success) showShareCodeDialog = false
                            }
                        }
                    )
                }

                if (showCreateScheduleDialog) {
                    CreateScheduleDialog(
                        isDark = isDark,
                        onDismiss = { showCreateScheduleDialog = false },
                        onConfirm = { name ->
                            viewModel.createNewSchedule(name)
                            showCreateScheduleDialog = false
                        }
                    )
                }

                if (updateInfoToShow != null) {
                    UpdateDialog(
                        updateInfo = updateInfoToShow!!,
                        isDark = isDark,
                        onDismiss = { updateInfoToShow = null },
                        onConfirm = {
                            startDownloadApk(context, updateInfoToShow!!.apkUrl)
                            updateInfoToShow = null
                        }
                    )
                }
            }
        }
    }
}

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val UPDATE_URL = "https://www.lingflame.cn/update.json"
    }

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val apkUrl: String,
        val forceUpdate: Boolean
    )

    private var downloadId: Long = -1

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId && id != -1L) {
                installDownloadedApk(context)
            }
        }
    }

    private fun startDownloadApk(context: Context, url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("正在下载课表更新...")
                setDescription("SimpleSchedule 最新版本")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "app-release.apk")
                setMimeType("application/vnd.android.package-archive")
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)
            Toast.makeText(context, "开始下载更新喵，请查看通知栏", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "启动下载失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun installDownloadedApk(context: Context) {
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app-release.apk")
        if (!apkFile.exists()) {
            Toast.makeText(context, "未找到下载的更新包喵", Toast.LENGTH_SHORT).show()
            return
        }
        val authority = "${context.packageName}.fileprovider"
        try {
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "拉起安装程序失败，请尝试在文件管理器中手动安装: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun checkUpdate(updateUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(updateUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                
                val jsonObject = JSONObject(response.toString())
                UpdateInfo(
                    versionCode = jsonObject.getInt("versionCode"),
                    versionName = jsonObject.getString("versionName"),
                    changelog = jsonObject.getString("changelog"),
                    apkUrl = jsonObject.getString("apkUrl"),
                    forceUpdate = jsonObject.optBoolean("forceUpdate", false)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }
}

@Composable
fun UpdateDialog(
    updateInfo: MainActivity.UpdateInfo,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Dialog(onDismissRequest = { if (!updateInfo.forceUpdate) onDismiss() }) {
        Surface(shape = RoundedCornerShape(12.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("发现新版本: ${updateInfo.versionName}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                Text(updateInfo.changelog, fontSize = 14.sp, color = textColor.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (!updateInfo.forceUpdate) {
                        TextButton(onClick = onDismiss) { Text("以后再说", color = textColor.copy(alpha = 0.5f)) }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("立即更新")
                    }
                }
            }
        }
    }
}

// --- 组件部分 ---

