package com.example.simpleschedule.data.local.datastore

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

// --- 0. DataStore 全局偏好设置 ---
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

    val WIDGET_TRANSLUCENT = booleanPreferencesKey("widget_translucent")
    val LAST_SEEN_ANNOUNCEMENT_VERSION = stringPreferencesKey("last_seen_announcement_version")
    val LAST_CHECKED_UPDATE_DATE = stringPreferencesKey("last_checked_update_date")
    val SILENCE_UPDATE_NOTIFICATION = booleanPreferencesKey("silence_update_notification")
    val LAST_SILENCED_VERSION_CODE = intPreferencesKey("last_silenced_version_code")
    val DYNAMIC_ISLAND_ENABLED = booleanPreferencesKey("dynamic_island_enabled")
    val LAST_SEEN_ANNOUNCEMENT_ID = stringPreferencesKey("last_seen_announcement_id")
    val PREDICTIVE_BACK_ENABLED = booleanPreferencesKey("predictive_back_enabled")
}

