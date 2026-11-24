package com.example.simpleschedule.ui.backup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight

@Composable
fun DataManagementFeatureGuideDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onGoNow: () -> Unit
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val primaryColor = LocalAccentColor.current ?: if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF18181B) else Color.White,
            border = BorderStroke(0.5.dp, borderColor),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = primaryColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primaryColor, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新功能上线", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("云端同步与数据管理 · 焕新升级", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "支持本地 JSON 全量备份与 WebDAV / SMB 局域网同步，换机与多设备数据迁移一步到位。",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.6f),
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DataGuideCard(
                        isDark = isDark,
                        icon = Icons.Rounded.FolderSpecial,
                        iconTint = Color(0xFF3B82F6),
                        title = "1. 右上角「⋮」进入数据管理",
                        desc = "可一键导出全部课表、时间表、调休及课堂统计为 JSON，或仅导出当前课表分享。"
                    )
                    DataGuideCard(
                        isDark = isDark,
                        icon = Icons.Rounded.CloudSync,
                        iconTint = Color(0xFF10B981),
                        title = "2. WebDAV 与 SMB 局域网备份",
                        desc = "支持坚果云/Nextcloud 等 WebDAV 网盘（自动建目录）以及家里 NAS 的 SMB 共享目录。"
                    )
                    DataGuideCard(
                        isDark = isDark,
                        icon = Icons.Rounded.Security,
                        iconTint = Color(0xFFF59E0B),
                        title = "3. 加密凭据与安全事务恢复",
                        desc = "密码采用系统 Keystore AES-GCM 加密存储，恢复数据支持「全量覆盖 / 增量合并」与事务回滚保护。"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(44.dp)) {
                        Text("我知道了", color = textColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onGoNow,
                        modifier = Modifier.weight(1.3f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.White)
                    ) {
                        Text("前往数据管理", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataGuideCard(
    isDark: Boolean,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF222227) else Color(0xFFF8FAFC)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Surface(color = iconTint.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(3.dp))
                Text(desc, fontSize = 11.sp, color = textColor.copy(alpha = 0.65f), lineHeight = 16.sp)
            }
        }
    }
}
