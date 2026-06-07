package com.example.simpleschedule.ui.theme

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


val BgLight = Color(0xFFFAFAFA)
val BorderLight = Color(0xFFE4E4E7)
val TextLight = Color(0xFF09090B)
val BgDark = Color(0xFF09090B)
val BorderDark = Color(0xFF27272A)
val TextDark = Color(0xFFFAFAFA)

data class CoursePalette(val bg: Color, val border: Color, val text: Color, val accent: Color)

fun getPalette(theme: String, isDark: Boolean): CoursePalette {
    return when (theme) {
        "blue" -> if (isDark) CoursePalette(Color(0xFF151B23), Color(0xFF2A4365), Color(0xFF90CDF4), Color(0xFF3182CE)) else CoursePalette(Color(0xFFEEF2F6), Color(0xFFD0DBE7), Color(0xFF2A4365), Color(0xFF3182CE))
        "pink" -> if (isDark) CoursePalette(Color(0xFF381A2A), Color(0xFF831843), Color(0xFFFBCFE8), Color(0xFFBE185D)) else CoursePalette(Color(0xFFFDF2F8), Color(0xFFFBCFE8), Color(0xFF9D174D), Color(0xFFEC4899))
        "indigo" -> if (isDark) CoursePalette(Color(0xFF1E1E2D), Color(0xFF3730A3), Color(0xFFC7D2FE), Color(0xFF4F46E5)) else CoursePalette(Color(0xFFEEF2FF), Color(0xFFC7D2FE), Color(0xFF3730A3), Color(0xFF4F46E5))
        "rose" -> if (isDark) CoursePalette(Color(0xFF331B22), Color(0xFF881337), Color(0xFFFECDD3), Color(0xFFE11D48)) else CoursePalette(Color(0xFFFFF1F2), Color(0xFFFECDD3), Color(0xFF9F1239), Color(0xFFE11D48))
        "purple" -> if (isDark) CoursePalette(Color(0xFF1C1228), Color(0xFF581C87), Color(0xFFD8B4FE), Color(0xFF9333EA)) else CoursePalette(Color(0xFFFAF5FF), Color(0xFFE9D5FF), Color(0xFF581C87), Color(0xFF9333EA))
        "slate" -> if (isDark) CoursePalette(Color(0xFF0F1218), Color(0xFF334155), Color(0xFFCBD5E1), Color(0xFF64748B)) else CoursePalette(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFF334155), Color(0xFF64748B))
        "green" -> if (isDark) CoursePalette(Color(0xFF121C17), Color(0xFF166534), Color(0xFF86EFAC), Color(0xFF16A34A)) else CoursePalette(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF166534), Color(0xFF16A34A))
        "orange" -> if (isDark) CoursePalette(Color(0xFF241711), Color(0xFF9A3412), Color(0xFFFDBA74), Color(0xFFEA580C)) else CoursePalette(Color(0xFFFFF7ED), Color(0xFFFED7AA), Color(0xFF9A3412), Color(0xFFEA580C))
        "red" -> if (isDark) CoursePalette(Color(0xFF261414), Color(0xFF991B1B), Color(0xFFFCA5A5), Color(0xFFDC2626)) else CoursePalette(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFF991B1B), Color(0xFFDC2626))
        else -> if (isDark) CoursePalette(Color(0xFF151B23), Color(0xFF2A4365), Color(0xFF90CDF4), Color(0xFF3182CE)) else CoursePalette(Color(0xFFEEF2F6), Color(0xFFD0DBE7), Color(0xFF2A4365), Color(0xFF3182CE))
    }
}

// --- 4. 主入口与全局结构 ---
