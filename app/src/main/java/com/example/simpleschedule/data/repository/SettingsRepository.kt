package com.example.simpleschedule.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.simpleschedule.data.local.datastore.AppSettings
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.SmbConfig
import com.example.simpleschedule.data.local.datastore.WebDavConfig
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.security.EncryptedSecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isDarkTheme = prefs[SettingsKeys.IS_DARK_THEME],
            showSat = prefs[SettingsKeys.SHOW_SAT] ?: true,
            showSun = prefs[SettingsKeys.SHOW_SUN] ?: true,
            showNotThisWeek = prefs[SettingsKeys.SHOW_NOT_THIS_WEEK] ?: true,
            cellHeight = prefs[SettingsKeys.CELL_HEIGHT] ?: 64f,
            cornerRadius = prefs[SettingsKeys.CORNER_RADIUS] ?: 4f,
            hideTime = prefs[SettingsKeys.HIDE_TIME] ?: false,
            bottomBlank = prefs[SettingsKeys.BOTTOM_BLANK] ?: true,
            vibration = prefs[SettingsKeys.VIBRATION] ?: true,
            totalWeeks = prefs[SettingsKeys.TOTAL_WEEKS] ?: 20,
            materialYou = prefs[SettingsKeys.MATERIAL_YOU] ?: false,
            warnTimetableError = prefs[SettingsKeys.WARN_TIMETABLE_ERROR] ?: true,
            autoUpdate = prefs[SettingsKeys.AUTO_UPDATE] ?: false,
            currentScheduleId = prefs[SettingsKeys.CURRENT_SCHEDULE_ID] ?: "",
            reminderEnabled = prefs[SettingsKeys.REMINDER_ENABLED] ?: false,
            reminderVoiceEnabled = prefs[SettingsKeys.REMINDER_VOICE_ENABLED] ?: true,
            reminderNotifyEnabled = prefs[SettingsKeys.REMINDER_NOTIFY_ENABLED] ?: true,
            reminderAdvanceMins = prefs[SettingsKeys.REMINDER_ADVANCE_MINS] ?: 20,
            widgetTranslucent = prefs[SettingsKeys.WIDGET_TRANSLUCENT] ?: false,
            dynamicIslandEnabled = prefs[SettingsKeys.DYNAMIC_ISLAND_ENABLED] ?: false,
            predictiveBackEnabled = prefs[SettingsKeys.PREDICTIVE_BACK_ENABLED] ?: true,
            floatingBottomBar = prefs[SettingsKeys.FLOATING_BOTTOM_BAR] ?: false,
            tabAnimationType = prefs[SettingsKeys.TAB_ANIMATION_TYPE] ?: "Slide",
            hasSeenMakeUpGuide = prefs[SettingsKeys.HAS_SEEN_MAKEUP_GUIDE] ?: false,
            hasSeenDataManagementGuide = prefs[SettingsKeys.HAS_SEEN_DATA_MANAGEMENT_GUIDE] ?: false,
            accentColor = prefs[SettingsKeys.ACCENT_COLOR] ?: -1L,
            courseColorPool = prefs[SettingsKeys.COURSE_COLOR_POOL] ?: "",
            webDav = WebDavConfig(
                url = prefs[SettingsKeys.WEBDAV_URL] ?: "",
                username = prefs[SettingsKeys.WEBDAV_USERNAME] ?: "",
                password = EncryptedSecretStore.readSync(
                    prefs,
                    SettingsKeys.WEBDAV_PASSWORD_ENCRYPTED,
                    SettingsKeys.WEBDAV_PASSWORD
                ),
                remotePath = prefs[SettingsKeys.WEBDAV_REMOTE_PATH] ?: "SimpleSchedule/backup.json"
            ),
            smb = SmbConfig(
                host = prefs[SettingsKeys.SMB_HOST] ?: "",
                port = prefs[SettingsKeys.SMB_PORT] ?: 445,
                share = prefs[SettingsKeys.SMB_SHARE] ?: "",
                username = prefs[SettingsKeys.SMB_USERNAME] ?: "",
                password = EncryptedSecretStore.readSync(
                    prefs,
                    SettingsKeys.SMB_PASSWORD_ENCRYPTED,
                    SettingsKeys.SMB_PASSWORD
                ),
                domain = prefs[SettingsKeys.SMB_DOMAIN] ?: "",
                remotePath = prefs[SettingsKeys.SMB_REMOTE_PATH] ?: "SimpleSchedule/backup.json"
            ),
            lastBackupTime = prefs[SettingsKeys.LAST_BACKUP_TIME] ?: ""
        )
    }

    suspend fun <T> getPreference(key: Preferences.Key<T>): T? {
        return context.dataStore.data.map { it[key] }.first()
    }

    suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun clearDarkTheme() {
        context.dataStore.edit { it.remove(SettingsKeys.IS_DARK_THEME) }
    }

    suspend fun saveWebDavConfig(config: WebDavConfig) {
        val trimmedUrl = config.url.trim()
        val trimmedUser = config.username.trim()
        val trimmedPath = config.remotePath.trim().ifBlank { "SimpleSchedule/backup.json" }
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.WEBDAV_URL] = trimmedUrl
            prefs[SettingsKeys.WEBDAV_USERNAME] = trimmedUser
            prefs[SettingsKeys.WEBDAV_REMOTE_PATH] = trimmedPath
        }
        EncryptedSecretStore.write(
            context,
            SettingsKeys.WEBDAV_PASSWORD_ENCRYPTED,
            SettingsKeys.WEBDAV_PASSWORD,
            config.password
        )
    }

    suspend fun saveSmbConfig(config: SmbConfig) {
        val trimmedHost = config.host.trim()
        val trimmedShare = config.share.trim()
        val trimmedUser = config.username.trim()
        val trimmedDomain = config.domain.trim()
        val trimmedPath = config.remotePath.trim().ifBlank { "SimpleSchedule/backup.json" }
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.SMB_HOST] = trimmedHost
            prefs[SettingsKeys.SMB_PORT] = config.port
            prefs[SettingsKeys.SMB_SHARE] = trimmedShare
            prefs[SettingsKeys.SMB_USERNAME] = trimmedUser
            prefs[SettingsKeys.SMB_DOMAIN] = trimmedDomain
            prefs[SettingsKeys.SMB_REMOTE_PATH] = trimmedPath
        }
        EncryptedSecretStore.write(
            context,
            SettingsKeys.SMB_PASSWORD_ENCRYPTED,
            SettingsKeys.SMB_PASSWORD,
            config.password
        )
    }
}
