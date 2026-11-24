package com.example.simpleschedule.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.simpleschedule.data.local.room.AppDatabase
import com.example.simpleschedule.receiver.ReminderEngine

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val appDao = AppDatabase.getDatabase(applicationContext).appDao()
            CourseWidget().updateAll(applicationContext)
            CourseWidget2x2().updateAll(applicationContext)
            CourseWeekWidget().updateAll(applicationContext)
            ReminderEngine.scheduleNextWidgetUpdate(applicationContext, appDao)
            ReminderEngine.calculateAndScheduleNext(applicationContext, appDao)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
