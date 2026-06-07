package com.example.simpleschedule.utils

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

// --- 辅助函数 ---
fun addMinutes(timeStr: String, mins: Int): String {
    val parts = timeStr.split(":")
    if (parts.size != 2) return timeStr
    val total = (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) + mins
    return String.format(Locale.getDefault(), "%02d:%02d", (total / 60) % 24, total % 60)
}

fun showTimePicker(context: Context, initialTime: String, onTimeSelected: (String) -> Unit) {
    val parts = initialTime.split(":")
    TimePickerDialog(
        context,
        { _, h, m -> onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", h, m)) },
        parts.getOrNull(0)?.toIntOrNull() ?: 8,
        parts.getOrNull(1)?.toIntOrNull() ?: 0,
        true
    ).show()
}

fun showDatePicker(context: Context, initialDateStr: String, onDateSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
    try {
        if (initialDateStr.isNotEmpty()) {
            cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(initialDateStr) ?: Date()
        }
    } catch (e: Exception) {}

    DatePickerDialog(
        context,
        { _, y, m, d -> onDateSelected(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)) },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun formatDateForDisplay(dateStr: String): String {
    if (dateStr.isEmpty()) return "未设置"
    return try {
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: return dateStr
        SimpleDateFormat("yyyy-M-d EEEE", Locale.CHINESE).format(parsedDate)
    } catch (e: Exception) {
        dateStr
    }
}

// --- 3. UI 主题与调色板 (Theme & Palette) ---
