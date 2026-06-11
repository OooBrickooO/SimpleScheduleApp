package com.example.simpleschedule.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import java.util.Locale

class DynamicIslandService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var courseName = "下一门课"
    private var location = "教室"
    private var classStartMillis = 0L
    private var classEndMillis = 0L
    private var useLiveUpdate = true

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        intent?.let {
            courseName = it.getStringExtra("COURSE_NAME") ?: courseName
            location = it.getStringExtra("LOCATION") ?: location
            classStartMillis = it.getLongExtra("CLASS_START_MILLIS", 0L)
            classEndMillis = it.getLongExtra("CLASS_END_MILLIS", 0L)
            useLiveUpdate = it.getBooleanExtra("USE_LIVE_UPDATE", true)
        }

        // 统一通过此方法管理前台通知
        showForegroundNotification()

        // 核心判断：仅在非 Live Update 模式下启动自定义悬浮窗
        val isAndroid16Plus = Build.VERSION.SDK_INT >= 36 || Build.VERSION.CODENAME == "Baklava"
        val showFloating = !isAndroid16Plus || !useLiveUpdate
        
        if (showFloating && composeView == null) {
            setupOverlayWindow()
        } else if (!showFloating && composeView != null) {
            removeOverlayWindow()
        }

        return START_NOT_STICKY
    }

    private fun showForegroundNotification() {
        val channelId = "DynamicIslandServiceChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isAndroid16Plus = Build.VERSION.SDK_INT >= 36 || Build.VERSION.CODENAME == "Baklava"
        val isPromoted = isAndroid16Plus && useLiveUpdate

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (isPromoted) "上课提醒（实时通知）" else "灵动岛后台服务"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        if (isPromoted) {
            // --- Android 16+ Live Update 模式 ---
            builder.setContentTitle("正在上课：$courseName")
                .setContentText("地点：$location")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setOngoing(true)
                .addExtras(Bundle().apply {
                    putBoolean("android.requestPromotedOngoing", true)
                    putString("android.shortCriticalText", courseName)
                })
        } else {
            // --- 传统悬浮窗模式 ---
            builder.setContentTitle("正在显示灵动岛提醒")
                .setContentText("课程：$courseName @ $location")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setPriority(Notification.PRIORITY_LOW)
        }

        // 添加点击跳转
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
            builder.setContentIntent(pendingIntent)
        }

        startForeground(2001, builder.build())
    }

    private fun removeOverlayWindow() {
        composeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
            composeView = null
        }
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 12
        }

        val context = this
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(context)
            setViewTreeViewModelStoreOwner(context)
            setViewTreeSavedStateRegistryOwner(context)
        }

        view.setContent {
            var isExpanded by remember { mutableStateOf(false) }
            var remainingMinutes by remember { mutableStateOf(0) }
            var remainingSeconds by remember { mutableStateOf(0) }

            LaunchedEffect(classStartMillis) {
                while (isActive) {
                    val diff = classStartMillis - System.currentTimeMillis()
                    if (diff <= 0) {
                        stopSelf()
                        break
                    }
                    val totalSecs = (diff / 1000).toInt()
                    remainingMinutes = totalSecs / 60
                    remainingSeconds = totalSecs % 60
                    delay(1000)
                }
            }

            LaunchedEffect(isExpanded) {
                if (isExpanded) {
                    params.width = WindowManager.LayoutParams.MATCH_PARENT
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                } else {
                    params.width = WindowManager.LayoutParams.WRAP_CONTENT
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                }
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            DynamicIslandContent(
                courseName = courseName,
                location = location,
                mins = remainingMinutes,
                secs = remainingSeconds,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onDismiss = {
                    val intent = Intent(context, com.example.simpleschedule.receiver.CourseAlarmReceiver::class.java).apply {
                        action = "ACTION_DISMISS_REMINDER"
                    }
                    context.sendBroadcast(intent)
                }
            )
        }

        try {
            windowManager?.addView(view, params)
            composeView = view
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        composeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onDestroy()
    }
}

@Composable
fun DynamicIslandContent(
    courseName: String,
    location: String,
    mins: Int,
    secs: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF000000))
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (courseName.length > 5) courseName.substring(0, 5) + ".." else courseName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(10.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF10B981))
                )

                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = "${mins}m${secs}s",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF000000))
                    .clickable { onToggleExpand() }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "即将上课",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1F2937))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${mins}分${secs}秒",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = courseName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "地点: ${location.replace("楼", "")}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "一键清扫清除",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
