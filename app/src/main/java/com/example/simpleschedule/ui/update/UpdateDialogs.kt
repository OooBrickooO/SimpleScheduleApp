package com.example.simpleschedule.ui.update

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simpleschedule.data.repository.UpdateInfo
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import kotlin.math.roundToInt

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSilence: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Dialog(onDismissRequest = { if (!updateInfo.forceUpdate) onDismiss() }) {
        Surface(shape = RoundedCornerShape(12.dp), color = bgColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("发现新版本: ${updateInfo.versionName}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val lines = updateInfo.changelog.split("\n")
                    lines.forEach { line ->
                        if (line.trim().isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            val isHighlight = line.contains("官网") || line.contains("http") ||
                                line.contains("https") || line.contains("重要") ||
                                line.contains("⚠️") || line.contains("下载")
                            if (isHighlight) {
                                Surface(
                                    color = if (isDark) Color(0xFF3F2D0B) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = line,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = line,
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!updateInfo.forceUpdate) {
                        TextButton(onClick = onSilence) { Text("不再提醒", color = Color(0xFFDC2626)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismiss) { Text("以后再说", color = textColor.copy(alpha = 0.5f)) }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = bgColor),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("立即更新")
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadProgressDialog(
    progress: Float,
    sizeLabel: String,
    status: String?,
    isDark: Boolean,
    onCancel: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val progressColor = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)

    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "正在下载更新...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = progressColor,
                    trackColor = textColor.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (status == "pending") "等待中..." else "${(progress * 100).roundToInt()}%",
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    Text(text = sizeLabel, fontSize = 13.sp, color = textColor.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) {
                        Text("取消下载", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementDialog(
    title: String,
    content: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) BorderDark else BorderLight
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            modifier = Modifier.fillMaxWidth().padding(16.dp).border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = "Notification",
                        tint = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val lines = content.split("\n")
                    lines.forEach { line ->
                        if (line.trim().isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            val isHighlight = line.contains("官网") || line.contains("http") ||
                                line.contains("https") || line.contains("重要") ||
                                line.contains("⚠️") || line.contains("下载")
                            if (isHighlight) {
                                Surface(
                                    color = if (isDark) Color(0xFF3F2D0B) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = line,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = line,
                                    fontSize = 14.sp,
                                    color = textColor.copy(alpha = 0.8f),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5),
                            contentColor = textColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp).weight(1f)
                    ) {
                        Text("我知道了", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { runCatching { uriHandler.openUri("https://www.lingflame.cn") } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF90CDF4) else Color(0xFF3182CE),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp).weight(1f)
                    ) {
                        Text("前往官网", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
