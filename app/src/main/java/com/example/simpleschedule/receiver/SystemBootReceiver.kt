package com.example.simpleschedule.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.simpleschedule.utils.LocalLogger
import com.example.simpleschedule.widget.WidgetUpdateWorker

/**
 * Handles system-level lifecycle broadcasts to restore widget updates and course reminder alarms.
 */
class SystemBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val supported = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_SET,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (action !in supported) return

        LocalLogger.log(context, "SystemBootReceiver", "Rescheduling widgets and reminders for action=$action")
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SystemRestoreWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
