package com.example.simpleschedule.receiver

import android.app.NotificationChannel
import com.example.simpleschedule.utils.LocalLogger
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.compose.ui.graphics.toArgb
import com.example.simpleschedule.MainActivity
import com.example.simpleschedule.R
import com.example.simpleschedule.data.local.datastore.SettingsKeys

class CourseAlarmService : Service() {
    private var handler: Handler? = null
    private var stopRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val courseName = intent.getStringExtra("COURSE_NAME") ?: ""
        val location = intent.getStringExtra("LOCATION") ?: ""
        val timeStr = intent.getStringExtra("TIME_STR") ?: ""
        val classStartMillis = intent.getLongExtra("CLASS_START_MILLIS", 0L)
        val classEndMillis = intent.getLongExtra("CLASS_END_MILLIS", 0L)
        val colorTheme = intent.getStringExtra("COLOR_THEME") ?: "slate"
        val stateStr = intent.getStringExtra("STATE") ?: "UPCOMING"
        val state = if (stateStr == "IN_CLASS") NotificationState.IN_CLASS else NotificationState.UPCOMING

        LocalLogger.log(this, "Service", "onStartCommand: 课程=$courseName, 地点=$location, 状态=$stateStr")

        // 使用 CourseAlarmReceiver 中的统一构建函数构建通知
        val notification = buildCourseNotification(
            this,
            state,
            courseName,
            location,
            timeStr,
            classStartMillis,
            classEndMillis,
            colorTheme,
            true
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
        LocalLogger.log(this, "Service", "成功调用 startForeground 并启动前台通知")

        // 设置安全定时器：在下课 5 分钟后自动停止服务（兜底防止电池优化拦截 ACTION_DISMISS_REMINDER 导致服务常驻）
        handler?.let { h ->
            stopRunnable?.let { r -> h.removeCallbacks(r) }
        }
        
        val targetSafetyStopMillis = if (classEndMillis > 0) {
            classEndMillis + 5 * 60 * 1000L
        } else if (classStartMillis > 0) {
            classStartMillis + 120 * 60 * 1000L // 默认最多持续2小时
        } else {
            System.currentTimeMillis() + 60 * 60 * 1000L // 默认1小时
        }

        val delay = targetSafetyStopMillis - System.currentTimeMillis()
        if (delay > 0) {
            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            val r = Runnable {
                LocalLogger.log(this, "Service", "前台服务安全定时器触发：主动调用 stopSelf() 释放服务")
                stopSelf()
            }
            stopRunnable = r
            handler?.postDelayed(r, delay)
            LocalLogger.log(this, "Service", "已注册服务安全保护定时器：将于 ${delay / 1000} 秒后自动清理释放")
        } else {
            LocalLogger.log(this, "Service", "安全保护延迟小于等于0，立即停止服务")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        LocalLogger.log(this, "Service", "onDestroy: 服务销毁并清理定时器")
        super.onDestroy()
        handler?.let { h ->
            stopRunnable?.let { r -> h.removeCallbacks(r) }
        }
    }
}
