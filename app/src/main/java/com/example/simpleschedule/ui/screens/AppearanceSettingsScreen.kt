package com.example.simpleschedule.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.ui.theme.*
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AppearanceSettingsScreen(viewModel: ScheduleViewModel, isDark: Boolean, onBack: () -> Unit) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)

    val hideTime by viewModel.hideTime.collectAsState()
    val cellHeightDp by viewModel.cellHeight.collectAsState()
    val cornerRadiusDp by viewModel.cornerRadius.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val courseColorPoolString by viewModel.courseColorPool.collectAsState()

    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val backModifier = AppBackHandler(predictiveBackEnabled) { onBack() }

    Column(modifier = Modifier.fillMaxSize().then(backModifier).background(if(isDark) BgDark else BgLight)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("更多外观设置", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            
            // Global Accent Color Module
            item {
                Text("全局主色调 (Accent Color)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val presetColors = listOf(
                            -1L to Color.Transparent, // Default
                            0xFFE63946 to Color(0xFFE63946), // Red
                            0xFF457B9D to Color(0xFF457B9D), // Blue
                            0xFF2A9D8F to Color(0xFF2A9D8F), // Green
                            0xFF9B5DE5 to Color(0xFF9B5DE5)  // Purple
                        )
                        presetColors.forEach { (value, color) ->
                            val isSelected = accentColor == value
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (value == -1L) (if(isDark) Color.DarkGray else Color.LightGray) else color)
                                    .border(if (isSelected) 2.dp else 0.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                                    .clickable { viewModel.updateSetting(SettingsKeys.ACCENT_COLOR, value) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (value == -1L) {
                                    Text("默认", fontSize = 10.sp, color = if(isDark) Color.White else Color.Black)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Gamut Color Selection Module
            item {
                Text("课程颜色分配池 (Gamut Pool)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Column {
                        Text("在下方色域图中拖动光圈，实时圈选生成属于你的 10 种课表随机颜色！", fontSize = 12.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 16.dp))
                        
                        ColorGamutSelector(
                            isDark = isDark,
                            onColorsGenerated = { colors ->
                                val joined = colors.joinToString(",") { it.value.toString() }
                                viewModel.updateSetting(SettingsKeys.COURSE_COLOR_POOL, joined)
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("当前池子预览:", fontSize = 12.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                        
                        // Parse current pool
                        val currentPool = if (courseColorPoolString.isBlank()) emptyList() else courseColorPoolString.split(",").mapNotNull { it.toULongOrNull()?.let { v -> Color(v) } }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (currentPool.isEmpty()) {
                                Text("默认主题色", fontSize = 12.sp, color = textColor)
                            } else {
                                currentPool.forEach { color ->
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(color))
                                }
                            }
                        }
                        
                        if (currentPool.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "恢复默认课表颜色", 
                                fontSize = 12.sp, 
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { viewModel.updateSetting(SettingsKeys.COURSE_COLOR_POOL, "") }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Existing Grid Settings
            item {
                Text("网格属性 (Grid Settings)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).border(0.5.dp, borderColor, RoundedCornerShape(12.dp))) {
                    Column {
                        SettingCheckboxItem(title = "隐藏格子内的时间显示", checked = hideTime, onCheckedChange = { viewModel.updateSetting(SettingsKeys.HIDE_TIME, it) }, textColor = textColor, borderColor = borderColor, isDark = isDark)

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("课程格子高度 (${cellHeightDp.roundToInt()}dp)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Slider(
                                value = cellHeightDp,
                                onValueChange = { viewModel.updateSetting(SettingsKeys.CELL_HEIGHT, it) },
                                valueRange = 40f..100f,
                                colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f))
                            )
                        }
                        HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("格子圆角半径 (${cornerRadiusDp.roundToInt()}dp)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Slider(
                                value = cornerRadiusDp,
                                onValueChange = { viewModel.updateSetting(SettingsKeys.CORNER_RADIUS, it) },
                                valueRange = 0f..24f,
                                colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
fun ColorGamutSelector(isDark: Boolean, onColorsGenerated: (List<Color>) -> Unit) {
    val borderColor = if (isDark) BorderDark else BorderLight
    var cursorPosition by remember { mutableStateOf(Offset(100f, 100f)) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val cursorRadius = 60f

    val hueColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    
    fun generateColorsFromCircle() {
        if (canvasSize.width == 0f || canvasSize.height == 0f) return
        
        val colors = mutableListOf<Color>()
        val centerX = cursorPosition.x
        val centerY = cursorPosition.y
        
        // Generate 10 evenly distributed points inside the circle using golden angle
        val goldenAngle = PI * (3.0 - kotlin.math.sqrt(5.0))
        for (i in 0 until 10) {
            val r = cursorRadius * kotlin.math.sqrt(i / 9f) // sqrt for even area distribution
            val theta = i * goldenAngle
            
            val px = (centerX + r * cos(theta).toFloat()).coerceIn(0f, canvasSize.width)
            val py = (centerY + r * sin(theta).toFloat()).coerceIn(0f, canvasSize.height)
            
            val hue = (px / canvasSize.width) * 360f
            val saturation = 1f - (py / canvasSize.height) // Top is full saturation, bottom is white
            val value = if (isDark) 0.8f else 0.95f
            
            val androidColor = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
            colors.add(Color(androidColor))
        }
        onColorsGenerated(colors)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { generateColorsFromCircle() }
                ) { change, dragAmount ->
                    change.consume()
                    val newX = (cursorPosition.x + dragAmount.x).coerceIn(0f, size.width.toFloat())
                    val newY = (cursorPosition.y + dragAmount.y).coerceIn(0f, size.height.toFloat())
                    cursorPosition = Offset(newX, newY)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            // 1. Draw Hue gradient (Horizontal)
            drawRect(
                brush = Brush.horizontalGradient(hueColors),
                size = size
            )
            // 2. Draw Saturation overlay (Vertical, White to Transparent)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color.Transparent)
                ),
                size = size
            )
            
            // Draw Cursor
            drawCircle(
                color = Color.White,
                radius = cursorRadius,
                center = cursorPosition,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = cursorRadius,
                center = cursorPosition,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
