package com.example.simpleschedule.ui.profile

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.ui.common.SettingItemWithSubtext
import com.example.simpleschedule.ui.common.SettingValueItem
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight

@Composable
fun ProfileScreen(
    isDark: Boolean,
    themeSetting: Boolean?,
    onThemeSelect: (Boolean?) -> Unit,
    onCheckUpdateClick: () -> Unit = {},
    onShowAnnouncementClick: () -> Unit = {},
    onHelpSearchClick: () -> Unit = {},
    onAppearanceSettingsClick: () -> Unit = {}
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color.White
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            "v${packageInfo.versionName}"
        } catch (_: Exception) {
            "v2.9.0"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp)
    ) {
        Text("管理", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor, RoundedCornerShape(12.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(textColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("S", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isDark) BgDark else BgLight)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text("Made By 视界Seekai", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("QQ交流群: 817954315", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = textColor.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text("亮暗模式切换", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.4f), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val modes = listOf(
                Triple(null, "跟随系统", themeSetting == null),
                Triple(false, "浅色", themeSetting == false),
                Triple(true, "深色", themeSetting == true)
            )
            modes.forEach { (value, label, selected) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onThemeSelect(value) }
                        .background(if (selected) textColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .border(0.5.dp, if (selected) Color.Transparent else borderColor, RoundedCornerShape(8.dp))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) (if (isDark) BgDark else BgLight) else textColor
                    )
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAppearanceSettingsClick() }
                    .background(Color.Transparent, RoundedCornerShape(8.dp))
                    .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自定义", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text("关于", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor, RoundedCornerShape(12.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
        ) {
            Column {
                SettingValueItem(title = "版本", value = versionName, textColor = textColor, borderColor = borderColor, onClick = onCheckUpdateClick)
                SettingItemWithSubtext(
                    title = "更新说明",
                    subtext = "点击查看最近版本的更新公告说明",
                    showBottomBorder = true,
                    textColor = textColor,
                    borderColor = borderColor,
                    onClick = onShowAnnouncementClick
                )
                SettingItemWithSubtext(
                    title = "功能查找与使用帮助",
                    subtext = "探索应用内的所有功能、快速跳转与完整使用指南",
                    showBottomBorder = true,
                    textColor = textColor,
                    borderColor = borderColor,
                    onClick = onHelpSearchClick
                )
                SettingItemWithSubtext(
                    title = "开源与反馈",
                    subtext = "点击访问 GitHub 仓库获取源码或提交建议",
                    showBottomBorder = false,
                    textColor = textColor,
                    borderColor = borderColor,
                    onClick = { uriHandler.openUri("https://github.com/OooBrickooO/SimpleScheduleApp") }
                )
            }
        }
    }
}
