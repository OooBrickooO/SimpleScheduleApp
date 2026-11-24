package com.example.simpleschedule.di

import android.app.Application
import com.example.simpleschedule.data.local.room.AppDatabase
import com.example.simpleschedule.data.repository.BackupRepository
import com.example.simpleschedule.data.repository.ScheduleRepository
import com.example.simpleschedule.data.repository.SettingsRepository
import com.example.simpleschedule.data.repository.UpdateRepository

class AppContainer(application: Application) {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(application) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }
    val scheduleRepository: ScheduleRepository by lazy { ScheduleRepository(database) }
    val backupRepository: BackupRepository by lazy {
        BackupRepository(scheduleRepository, settingsRepository)
    }
    val updateRepository: UpdateRepository by lazy {
        UpdateRepository(application, settingsRepository)
    }
}
