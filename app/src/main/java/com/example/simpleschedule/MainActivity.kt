package com.example.simpleschedule

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.repository.UpdateInfo
import com.example.simpleschedule.ui.backup.DataManagementScreen
import com.example.simpleschedule.ui.common.BottomNavBar
import com.example.simpleschedule.ui.common.DotMatrixBackground
import com.example.simpleschedule.ui.course.CourseEditDialog
import com.example.simpleschedule.ui.course.CourseManagementScreen
import com.example.simpleschedule.ui.importing.ImportStage
import com.example.simpleschedule.ui.importing.ShareCodeDialog
import com.example.simpleschedule.ui.importing.WebViewImportScreen
import com.example.simpleschedule.ui.profile.HelpSearchScreen
import com.example.simpleschedule.ui.profile.ProfileScreen
import com.example.simpleschedule.ui.settings.AppearanceSettingsScreen
import com.example.simpleschedule.ui.settings.GlobalSettingsScreen
import com.example.simpleschedule.ui.settings.ReminderSettingsScreen
import com.example.simpleschedule.ui.settings.ScheduleSettingsScreen
import com.example.simpleschedule.ui.settings.TimetableEditScreen
import com.example.simpleschedule.ui.settings.TimetableListScreen
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.SimpleScheduleTheme
import com.example.simpleschedule.ui.timetable.CreateScheduleDialog
import com.example.simpleschedule.ui.timetable.TimetableScreen
import com.example.simpleschedule.ui.update.AnnouncementDialog
import com.example.simpleschedule.ui.update.DownloadProgressDialog
import com.example.simpleschedule.ui.update.UpdateDialog
import com.example.simpleschedule.viewmodel.BackupViewModel
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import com.example.simpleschedule.widget.WidgetUpdateWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels()
    private val backupViewModel: BackupViewModel by viewModels()
    private val updateRepo by lazy { (application as SimpleScheduleApplication).container.updateRepository }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        super.onCreate(savedInstanceState)

        updateRepo.cleanupOldApk()
        updateRepo.registerReceiver()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            var updateInfoToShow by remember { mutableStateOf<UpdateInfo?>(null) }
            var isCheckingManualUpdate by remember { mutableStateOf(false) }
            var announcementDialogTitle by remember { mutableStateOf("") }
            var announcementDialogContent by remember { mutableStateOf("") }
            var showAnnouncementDialog by remember { mutableStateOf(false) }

            val downloadState by updateRepo.downloadState.collectAsState()

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "注意：请前往设置允许应用的「精确闹钟」权限，否则桌面卡片将无法准时刷新喵！", Toast.LENGTH_LONG).show()
                }

                val fallbackRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "WidgetFallback",
                    ExistingPeriodicWorkPolicy.KEEP,
                    fallbackRequest
                )

                launch {
                    updateRepo.checkUnreadAnnouncement()?.let { info ->
                        announcementDialogTitle = info.title
                        announcementDialogContent = info.content
                        showAnnouncementDialog = true
                    }
                }

                launch {
                    updateRepo.checkForAutoUpdate()?.let { update ->
                        updateInfoToShow = update
                    }
                }
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDarkSetting by viewModel.isDarkTheme.collectAsState()
            val isDark = isDarkSetting ?: isSystemDark
            val materialYou by viewModel.materialYou.collectAsState()
            val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
            val accentColor by viewModel.accentColor.collectAsState()
            val parsedAccent = if (accentColor != -1L) Color(accentColor) else null
            val activeTimeNodes by viewModel.activeTimeNodes.collectAsState()

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

            val navController = rememberNavController()
            var currentTab by remember { mutableIntStateOf(0) }
            var courseToEdit by remember { mutableStateOf<Course?>(null) }
            var editingTimetableId by remember { mutableStateOf<String?>(null) }
            var showAddDialog by remember { mutableStateOf(false) }
            var showShareCodeDialog by remember { mutableStateOf(false) }
            var showCreateScheduleDialog by remember { mutableStateOf(false) }

            SimpleScheduleTheme(
                darkTheme = isDark,
                dynamicColor = materialYou,
                accentColor = accentColor
            ) {
                CompositionLocalProvider(LocalAccentColor provides parsedAccent) {
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isDark) {
                            if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MaterialTheme.colorScheme.background else BgDark
                        } else {
                            if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MaterialTheme.colorScheme.background else BgLight
                        },
                        animationSpec = tween(500),
                        label = "bg"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(animatedBgColor)
                    ) {
                        DotMatrixBackground(isDark = isDark)

                        NavHost(
                            navController = navController,
                            startDestination = "main",
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                                    scaleIn(initialScale = 0.9f, animationSpec = tween(300))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(300)) +
                                    scaleOut(targetScale = 0.9f, animationSpec = tween(300))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                    scaleIn(initialScale = 0.9f, animationSpec = tween(300))
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(300)) +
                                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                                    scaleOut(targetScale = 0.9f, animationSpec = tween(300))
                            }
                        ) {
                            composable("main") {
                                val currentWeek by viewModel.currentWeek.collectAsState()
                                val displayCourses by viewModel.displayCourses.collectAsState()
                                val scheduleGroups by viewModel.scheduleGroups.collectAsState()
                                val currentScheduleId by viewModel.currentScheduleId.collectAsState()
                                val totalWeeks by viewModel.totalWeeks.collectAsState()
                                val tabAnimationType by viewModel.tabAnimationType.collectAsState()

                                AnimatedContent(
                                    targetState = currentTab,
                                    label = "tab_anim",
                                    transitionSpec = {
                                        if (tabAnimationType == "Slide") {
                                            if (targetState > initialState) {
                                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                                            } else {
                                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                                            }
                                        } else {
                                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                        }
                                    }
                                ) { tab ->
                                    when (tab) {
                                        0 -> {
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
                                                onManageCoursesClick = { navController.navigate("course_management") },
                                                onManageTimetablesClick = { navController.navigate("timetable_list") },
                                                onScheduleSettingsClick = { navController.navigate("schedule_settings") },
                                                onGlobalSettingsClick = { navController.navigate("global_settings") },
                                                onEditCourse = { courseToEdit = it; showAddDialog = true },
                                                onWebViewImportClick = { navController.navigate("webview_import") },
                                                onCsvImportClick = { navController.navigate("webview_import?stage=csv") },
                                                onShareCodeClick = { showShareCodeDialog = true },
                                                onCreateScheduleClick = { showCreateScheduleDialog = true },
                                                onReminderSettingsClick = { navController.navigate("reminder_settings") },
                                                onDataManagementClick = { navController.navigate("data_management") }
                                            )
                                        }
                                        else -> {
                                            ProfileScreen(
                                                isDark = isDark,
                                                themeSetting = isDarkSetting,
                                                onThemeSelect = { viewModel.toggleTheme(it) },
                                                onShowAnnouncementClick = {
                                                    if (announcementDialogTitle.isNotEmpty()) {
                                                        showAnnouncementDialog = true
                                                    } else {
                                                        Toast.makeText(context, "正在获取公告...", Toast.LENGTH_SHORT).show()
                                                        coroutineScope.launch {
                                                            val result = updateRepo.fetchAnnouncement()
                                                            if (result != null) {
                                                                announcementDialogTitle = result.title
                                                                announcementDialogContent = result.content
                                                                showAnnouncementDialog = true
                                                            } else {
                                                                Toast.makeText(context, "无法获取公告，请稍后再试", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                },
                                                onHelpSearchClick = { navController.navigate("help_search") },
                                                onAppearanceSettingsClick = { navController.navigate("appearance_settings") },
                                                onCheckUpdateClick = {
                                                    if (!isCheckingManualUpdate) {
                                                        isCheckingManualUpdate = true
                                                        Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
                                                        coroutineScope.launch {
                                                            val update = updateRepo.checkUpdate()
                                                            isCheckingManualUpdate = false
                                                            if (update != null) {
                                                                if (update.versionCode > updateRepo.getCurrentVersionCode()) {
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

                            composable("course_management") {
                                val displayCourses by viewModel.displayCourses.collectAsState()
                                CourseManagementScreen(
                                    viewModel = viewModel,
                                    courses = displayCourses.map { it.course }.distinctBy { it.id },
                                    isDark = isDark,
                                    materialYou = materialYou,
                                    onBack = { navController.popBackStack() },
                                    onEditCourse = { courseToEdit = it; showAddDialog = true },
                                    onDeleteCourse = { viewModel.deleteCourse(it) }
                                )
                            }

                            composable("timetable_list") {
                                val scheduleGroups by viewModel.scheduleGroups.collectAsState()
                                val currentScheduleId by viewModel.currentScheduleId.collectAsState()
                                val timetableGroups by viewModel.timetableGroups.collectAsState()
                                val currentSchedule = scheduleGroups.find { it.id == currentScheduleId }

                                TimetableListScreen(
                                    viewModel = viewModel,
                                    timetables = timetableGroups,
                                    currentLinkedId = currentSchedule?.timetableId ?: "tt_cjlu",
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() },
                                    onSelect = { viewModel.linkTimetableToCurrentSchedule(it) },
                                    onEdit = { id -> editingTimetableId = id; navController.navigate("timetable_edit") },
                                    onDelete = { viewModel.deleteTimetable(it) }
                                )
                            }

                            composable("timetable_edit") {
                                val timetableGroups by viewModel.timetableGroups.collectAsState()
                                TimetableEditScreen(
                                    timetableId = editingTimetableId,
                                    timetables = timetableGroups,
                                    viewModel = viewModel,
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() },
                                    onSave = { id, name, nodes ->
                                        viewModel.saveTimetable(id, name, nodes)
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("schedule_settings") {
                                val currentWeek by viewModel.currentWeek.collectAsState()
                                val scheduleGroups by viewModel.scheduleGroups.collectAsState()
                                val currentScheduleId by viewModel.currentScheduleId.collectAsState()
                                val totalWeeks by viewModel.totalWeeks.collectAsState()

                                ScheduleSettingsScreen(
                                    viewModel = viewModel,
                                    scheduleGroups = scheduleGroups,
                                    currentScheduleId = currentScheduleId,
                                    currentWeek = currentWeek,
                                    timeNodeCount = activeTimeNodes.size,
                                    totalWeeks = totalWeeks,
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() },
                                    onRenameSchedule = { id, newName -> viewModel.renameSchedule(id, newName) },
                                    onWeekChange = { viewModel.updateWeekAndReverseCalculateStartDate(currentScheduleId, it) },
                                    onStartDateChange = { viewModel.updateScheduleStartDate(currentScheduleId, it) },
                                    onTotalWeeksChange = { viewModel.updateSetting(com.example.simpleschedule.data.local.datastore.SettingsKeys.TOTAL_WEEKS, it) },
                                    onManageTimetableClick = { navController.navigate("timetable_list") },
                                    onManageCoursesClick = { navController.navigate("course_management") },
                                    onMoreAppearanceClick = { navController.navigate("appearance_settings") }
                                )
                            }

                            composable("appearance_settings") {
                                AppearanceSettingsScreen(
                                    viewModel = viewModel,
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("data_management") {
                                DataManagementScreen(
                                    scheduleViewModel = viewModel,
                                    backupViewModel = backupViewModel,
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("global_settings") {
                                GlobalSettingsScreen(
                                    viewModel = viewModel,
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "webview_import?stage={stage}",
                                arguments = listOf(navArgument("stage") {
                                    type = NavType.StringType
                                    defaultValue = "select"
                                })
                            ) { backStackEntry ->
                                val displayCourses by viewModel.displayCourses.collectAsState()
                                val scheduleGroups by viewModel.scheduleGroups.collectAsState()
                                val currentScheduleId by viewModel.currentScheduleId.collectAsState()
                                val importStage = if (backStackEntry.arguments?.getString("stage") == "csv") {
                                    ImportStage.CSV_IMPORT
                                } else {
                                    ImportStage.SELECT_MODE
                                }

                                WebViewImportScreen(
                                    isDark = isDark,
                                    activeTimeNodes = activeTimeNodes,
                                    startDate = scheduleGroups.find { it.id == currentScheduleId }?.startDate ?: "",
                                    hasCourses = displayCourses.isNotEmpty(),
                                    predictiveBackEnabled = predictiveBackEnabled,
                                    initialStage = importStage,
                                    onBack = { navController.popBackStack() },
                                    onImport = { json ->
                                        viewModel.importFromJson(json) { success ->
                                            Toast.makeText(context, if (success) "导入成功" else "解析失败", Toast.LENGTH_SHORT).show()
                                            if (success) navController.popBackStack("main", inclusive = false)
                                        }
                                    },
                                    onOverwriteImport = { json ->
                                        viewModel.importFromJson(json, replaceCurrent = true) { success ->
                                            Toast.makeText(context, if (success) "已覆盖当前课表并导入" else "导入失败", Toast.LENGTH_SHORT).show()
                                            if (success) navController.popBackStack("main", inclusive = false)
                                        }
                                    },
                                    onCreateScheduleAndImport = { json, newScheduleName ->
                                        viewModel.createNewSchedule(newScheduleName) {
                                            viewModel.importFromJson(json) { success ->
                                                Toast.makeText(context, if (success) "已新建课表并导入" else "导入失败", Toast.LENGTH_SHORT).show()
                                                if (success) navController.popBackStack("main", inclusive = false)
                                            }
                                        }
                                    }
                                )
                            }

                            composable("help_search") {
                                HelpSearchScreen(
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToTimetable = {
                                        currentTab = 0
                                        navController.popBackStack("main", inclusive = false)
                                    },
                                    onNavigateToReminderSettings = { navController.navigate("reminder_settings") },
                                    onNavigateToAppearanceSettings = { navController.navigate("appearance_settings") },
                                    onNavigateToDataManagement = { navController.navigate("data_management") }
                                )
                            }

                            composable("reminder_settings") {
                                ReminderSettingsScreen(
                                    viewModel = viewModel,
                                    isDark = isDark,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        val floatingBottomBar by viewModel.floatingBottomBar.collectAsState()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        if (navBackStackEntry?.destination?.route == "main") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Transparent)
                            ) {
                                BottomNavBar(
                                    isDark = isDark,
                                    isFloating = floatingBottomBar,
                                    currentTab = currentTab,
                                    onTabSelected = { currentTab = it }
                                )
                            }
                        }
                    }
                }

                if (showAddDialog) {
                    androidx.compose.runtime.key(courseToEdit?.id ?: "new-course") {
                        CourseEditDialog(
                            isDark = isDark,
                            initialCourse = courseToEdit,
                            maxNodeCount = activeTimeNodes.size.coerceAtLeast(12),
                            viewModel = viewModel,
                            onDismiss = { showAddDialog = false; courseToEdit = null },
                            onConfirm = { id, name, loc, t, d, s, e, c, w, credits, abs, call ->
                                if (id == null) {
                                    viewModel.addCustomCourse(name, loc, t, d, s, e, c, w, credits)
                                } else {
                                    viewModel.updateCustomCourse(id, name, loc, t, d, s, e, c, w, credits)
                                }
                                viewModel.updateCourseStatistic(name, abs, call)
                                showAddDialog = false
                                courseToEdit = null
                            }
                        )
                    }
                }

                if (showShareCodeDialog) {
                    ShareCodeDialog(
                        isDark = isDark,
                        onDismiss = { showShareCodeDialog = false },
                        onConfirm = { code ->
                            viewModel.importFromShareCode(code) { success ->
                                Toast.makeText(context, if (success) "口令解析成功" else "无效口令", Toast.LENGTH_SHORT).show()
                                if (success) showShareCodeDialog = false
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

                if (showAnnouncementDialog) {
                    AnnouncementDialog(
                        title = announcementDialogTitle,
                        content = announcementDialogContent,
                        isDark = isDark,
                        onDismiss = { showAnnouncementDialog = false }
                    )
                }

                if (updateInfoToShow != null) {
                    val info = updateInfoToShow!!
                    UpdateDialog(
                        updateInfo = info,
                        isDark = isDark,
                        onDismiss = { updateInfoToShow = null },
                        onConfirm = {
                            updateRepo.startDownloadApk(info.apkUrl)
                            updateInfoToShow = null
                        },
                        onSilence = {
                            val silencedCode = info.versionCode
                            coroutineScope.launch { updateRepo.silenceVersion(silencedCode) }
                            updateInfoToShow = null
                        }
                    )
                }

                if (downloadState.status != null) {
                    DownloadProgressDialog(
                        progress = downloadState.progress,
                        sizeLabel = downloadState.sizeLabel,
                        status = downloadState.status,
                        isDark = isDark,
                        onCancel = { updateRepo.cancelDownload() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRepo.unregisterReceiver()
    }
}
