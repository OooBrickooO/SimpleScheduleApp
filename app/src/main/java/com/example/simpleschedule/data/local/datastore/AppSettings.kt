package com.example.simpleschedule.data.local.datastore

data class WebDavConfig(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "SimpleSchedule/backup.json"
)

data class SmbConfig(
    val host: String = "",
    val port: Int = 445,
    val share: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val remotePath: String = "SimpleSchedule/backup.json"
)

data class AppSettings(
    val isDarkTheme: Boolean? = null,
    val showSat: Boolean = true,
    val showSun: Boolean = true,
    val showNotThisWeek: Boolean = true,
    val cellHeight: Float = 64f,
    val cornerRadius: Float = 4f,
    val hideTime: Boolean = false,
    val bottomBlank: Boolean = true,
    val vibration: Boolean = true,
    val totalWeeks: Int = 20,
    val materialYou: Boolean = false,
    val warnTimetableError: Boolean = true,
    val autoUpdate: Boolean = false,
    val currentScheduleId: String = "",
    val reminderEnabled: Boolean = false,
    val reminderVoiceEnabled: Boolean = true,
    val reminderNotifyEnabled: Boolean = true,
    val reminderAdvanceMins: Int = 20,
    val widgetTranslucent: Boolean = false,
    val dynamicIslandEnabled: Boolean = false,
    val predictiveBackEnabled: Boolean = true,
    val floatingBottomBar: Boolean = false,
    val tabAnimationType: String = "Slide",
    val hasSeenMakeUpGuide: Boolean? = null,
    val hasSeenDataManagementGuide: Boolean? = null,
    val accentColor: Long = -1L,
    val courseColorPool: String = "",
    val webDav: WebDavConfig = WebDavConfig(),
    val smb: SmbConfig = SmbConfig(),
    val lastBackupTime: String = ""
)
