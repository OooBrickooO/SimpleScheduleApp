package com.example.simpleschedule.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val SHOW_SAT = booleanPreferencesKey("show_sat")
    val SHOW_SUN = booleanPreferencesKey("show_sun")
    val SHOW_NOT_THIS_WEEK = booleanPreferencesKey("show_not_this_week")
    val CELL_HEIGHT = floatPreferencesKey("cell_height")
    val CORNER_RADIUS = floatPreferencesKey("corner_radius")
    val HIDE_TIME = booleanPreferencesKey("hide_time")
    val BOTTOM_BLANK = booleanPreferencesKey("bottom_blank")
    val VIBRATION = booleanPreferencesKey("vibration")

    val TOTAL_WEEKS = intPreferencesKey("total_weeks")
    val MATERIAL_YOU = booleanPreferencesKey("material_you")
    val WARN_TIMETABLE_ERROR = booleanPreferencesKey("warn_timetable_error")
    val AUTO_UPDATE = booleanPreferencesKey("auto_update")

    val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    val CURRENT_SCHEDULE_ID = stringPreferencesKey("current_schedule_id")

    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_VOICE_ENABLED = booleanPreferencesKey("reminder_voice_enabled")
    val REMINDER_NOTIFY_ENABLED = booleanPreferencesKey("reminder_notify_enabled")
    val REMINDER_ADVANCE_MINS = intPreferencesKey("reminder_advance_mins")
    val LAST_TRIGGERED_CLASS_START_MILLIS = longPreferencesKey("last_triggered_class_start_millis")
    val LAST_DISMISSED_CLASS_START_MILLIS = longPreferencesKey("last_dismissed_class_start_millis")

    val WIDGET_TRANSLUCENT = booleanPreferencesKey("widget_translucent")
    val LAST_SEEN_ANNOUNCEMENT_VERSION = stringPreferencesKey("last_seen_announcement_version")
    val LAST_CHECKED_UPDATE_DATE = stringPreferencesKey("last_checked_update_date")
    val SILENCE_UPDATE_NOTIFICATION = booleanPreferencesKey("silence_update_notification")
    val LAST_SILENCED_VERSION_CODE = intPreferencesKey("last_silenced_version_code")
    val DYNAMIC_ISLAND_ENABLED = booleanPreferencesKey("dynamic_island_enabled")
    val LAST_SEEN_ANNOUNCEMENT_ID = stringPreferencesKey("last_seen_announcement_id")
    val PREDICTIVE_BACK_ENABLED = booleanPreferencesKey("predictive_back_enabled")
    val FLOATING_BOTTOM_BAR = booleanPreferencesKey("floating_bottom_bar")
    val TAB_ANIMATION_TYPE = stringPreferencesKey("tab_animation_type")
    val HAS_SEEN_MAKEUP_GUIDE = booleanPreferencesKey("has_seen_makeup_guide")
    val HAS_SEEN_DATA_MANAGEMENT_GUIDE = booleanPreferencesKey("has_seen_data_management_guide")
    val ACCENT_COLOR = longPreferencesKey("accent_color")
    val COURSE_COLOR_POOL = stringPreferencesKey("course_color_pool")

    // 云备份 - WebDAV
    val WEBDAV_URL = stringPreferencesKey("webdav_url")
    val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
    val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
    val WEBDAV_PASSWORD_ENCRYPTED = stringPreferencesKey("webdav_password_encrypted")
    val WEBDAV_REMOTE_PATH = stringPreferencesKey("webdav_remote_path")

    // 云备份 - SMB
    val SMB_HOST = stringPreferencesKey("smb_host")
    val SMB_PORT = intPreferencesKey("smb_port")
    val SMB_SHARE = stringPreferencesKey("smb_share")
    val SMB_USERNAME = stringPreferencesKey("smb_username")
    val SMB_PASSWORD = stringPreferencesKey("smb_password")
    val SMB_PASSWORD_ENCRYPTED = stringPreferencesKey("smb_password_encrypted")
    val SMB_DOMAIN = stringPreferencesKey("smb_domain")
    val SMB_REMOTE_PATH = stringPreferencesKey("smb_remote_path")

    val LAST_BACKUP_TIME = stringPreferencesKey("last_backup_time")
}
