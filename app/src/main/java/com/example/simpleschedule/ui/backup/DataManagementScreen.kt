package com.example.simpleschedule.ui.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.data.local.datastore.SmbConfig
import com.example.simpleschedule.data.local.datastore.WebDavConfig
import com.example.simpleschedule.data.repository.RestoreMode
import com.example.simpleschedule.ui.common.AppBackHandler
import com.example.simpleschedule.ui.theme.BgDark
import com.example.simpleschedule.ui.theme.BgLight
import com.example.simpleschedule.ui.theme.BorderDark
import com.example.simpleschedule.ui.theme.BorderLight
import com.example.simpleschedule.ui.theme.LocalAccentColor
import com.example.simpleschedule.ui.theme.TextDark
import com.example.simpleschedule.ui.theme.TextLight
import com.example.simpleschedule.viewmodel.BackupViewModel
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PendingCloudRestoreSource {
    WEBDAV,
    SMB
}

@Composable
fun DataManagementScreen(
    scheduleViewModel: ScheduleViewModel,
    backupViewModel: BackupViewModel,
    isDark: Boolean,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val accent = LocalAccentColor.current ?: if (isDark) Color.White else Color.Black

    val predictiveBackEnabled by scheduleViewModel.predictiveBackEnabled.collectAsState()
    val currentScheduleId by scheduleViewModel.currentScheduleId.collectAsState()
    val backModifier = AppBackHandler(predictiveBackEnabled) { onBack() }

    val savedWebDav by backupViewModel.webDavConfig.collectAsState()
    val savedSmb by backupViewModel.smbConfig.collectAsState()
    val lastBackupTime by backupViewModel.lastBackupTime.collectAsState()
    val loadingMessage by backupViewModel.loadingMessage.collectAsState()

    var webdavUrl by remember(savedWebDav.url) { mutableStateOf(savedWebDav.url) }
    var webdavUsername by remember(savedWebDav.username) { mutableStateOf(savedWebDav.username) }
    var webdavPassword by remember(savedWebDav.password) { mutableStateOf(savedWebDav.password) }
    var webdavRemotePath by remember(savedWebDav.remotePath) { mutableStateOf(savedWebDav.remotePath) }
    var showWebdavPassword by remember { mutableStateOf(false) }

    var smbHost by remember(savedSmb.host) { mutableStateOf(savedSmb.host) }
    var smbPortStr by remember(savedSmb.port) { mutableStateOf(savedSmb.port.toString()) }
    var smbShare by remember(savedSmb.share) { mutableStateOf(savedSmb.share) }
    var smbUsername by remember(savedSmb.username) { mutableStateOf(savedSmb.username) }
    var smbPassword by remember(savedSmb.password) { mutableStateOf(savedSmb.password) }
    var smbDomain by remember(savedSmb.domain) { mutableStateOf(savedSmb.domain) }
    var smbRemotePath by remember(savedSmb.remotePath) { mutableStateOf(savedSmb.remotePath) }
    var showSmbPassword by remember { mutableStateOf(false) }

    var selectedCloudTab by remember { mutableIntStateOf(0) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingCloudRestore by remember { mutableStateOf<PendingCloudRestoreSource?>(null) }

    val currentWebDavConfig = {
        WebDavConfig(
            url = webdavUrl.trim(),
            username = webdavUsername.trim(),
            password = webdavPassword,
            remotePath = webdavRemotePath.trim().ifBlank { "SimpleSchedule/backup.json" }
        )
    }

    val currentSmbConfig = {
        SmbConfig(
            host = smbHost.trim(),
            port = smbPortStr.toIntOrNull()?.coerceIn(1, 65535) ?: 445,
            share = smbShare.trim(),
            username = smbUsername.trim(),
            password = smbPassword,
            domain = smbDomain.trim(),
            remotePath = smbRemotePath.trim().ifBlank { "SimpleSchedule/backup.json" }
        )
    }

    val onScheduleSwitched: (String) -> Unit = { scheduleId ->
        scheduleViewModel.switchSchedule(scheduleId)
    }
    val onScheduleModified: () -> Unit = {
        scheduleViewModel.notifyWidgetUpdate()
        scheduleViewModel.scheduleNextAlarm()
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            val content = pendingExportJson ?: return@let
            pendingExportJson = null
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(targetUri)?.use { os ->
                            os.write(content.toByteArray(Charsets.UTF_8))
                            os.flush()
                        } ?: error("无法打开输出流")
                    }
                }
                result.fold(
                    onSuccess = { Toast.makeText(context, "文件导出成功！", Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, "导出失败: ${it.localizedMessage}", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(sourceUri)?.use { isStream ->
                            isStream.bufferedReader(Charsets.UTF_8).readText()
                        }.orEmpty()
                    }
                }
                result.fold(
                    onSuccess = { json ->
                        if (json.isNotBlank()) {
                            pendingImportJson = json
                        } else {
                            Toast.makeText(context, "所选文件内容为空", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = {
                        Toast.makeText(context, "读取文件失败: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().then(backModifier)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) BgDark else BgLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.clickable { onBack() }.padding(8.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.FolderSpecial,
                    contentDescription = "Data Management",
                    tint = accent,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("数据管理", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                    Text("本地导入导出与 WebDAV / SMB 云备份", fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("本地数据备份与恢复", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))

                    Surface(
                        color = surfaceColor,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("本地导出 JSON", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                            pendingExportJson = backupViewModel.exportFullBackupJson()
                                            exportFileLauncher.launch("simpleschedule_full_backup_$dateStr.json")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("导出全量备份", fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                            pendingExportJson = backupViewModel.exportCurrentScheduleJson(currentScheduleId)
                                            exportFileLauncher.launch("simpleschedule_courses_$dateStr.json")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("仅导出当前课表", fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("本地导入 JSON", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { importFileLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f), contentColor = accent)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("选择本地 JSON 文件恢复", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("云端备份与同步", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))

                    Surface(
                        color = surfaceColor,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("云存储设置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                if (lastBackupTime.isNotEmpty()) {
                                    Text("上次云备份: $lastBackupTime", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selectedCloudTab == 0) accent.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedCloudTab = 0 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedCloudTab == 0) accent else textColor)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("WebDAV", fontSize = 14.sp, fontWeight = if (selectedCloudTab == 0) FontWeight.Bold else FontWeight.Normal, color = if (selectedCloudTab == 0) accent else textColor)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selectedCloudTab == 1) accent.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedCloudTab = 1 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Lan, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedCloudTab == 1) accent else textColor)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SMB 局域网", fontSize = 14.sp, fontWeight = if (selectedCloudTab == 1) FontWeight.Bold else FontWeight.Normal, color = if (selectedCloudTab == 1) accent else textColor)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (selectedCloudTab == 0) {
                                OutlinedTextField(
                                    value = webdavUrl,
                                    onValueChange = { webdavUrl = it },
                                    label = { Text("WebDAV 服务器地址") },
                                    placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = webdavRemotePath,
                                    onValueChange = { webdavRemotePath = it },
                                    label = { Text("云端文件保存路径") },
                                    placeholder = { Text("SimpleSchedule/backup.json") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = webdavUsername,
                                        onValueChange = { webdavUsername = it },
                                        label = { Text("用户名") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = webdavPassword,
                                        onValueChange = { webdavPassword = it },
                                        label = { Text("密码 / 授权码") },
                                        singleLine = true,
                                        visualTransformation = if (showWebdavPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showWebdavPassword = !showWebdavPassword }) {
                                                Icon(if (showWebdavPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            backupViewModel.testWebDavConnection(currentWebDavConfig()) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("测试连接", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            backupViewModel.backupToWebDav(currentWebDavConfig()) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("云端备份", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { pendingCloudRestore = PendingCloudRestoreSource.WEBDAV },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("云端恢复", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = smbHost,
                                        onValueChange = { smbHost = it },
                                        label = { Text("服务器 IP / 主机名") },
                                        placeholder = { Text("192.168.1.100") },
                                        singleLine = true,
                                        modifier = Modifier.weight(2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = smbPortStr,
                                        onValueChange = { smbPortStr = it },
                                        label = { Text("端口") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = smbShare,
                                        onValueChange = { smbShare = it },
                                        label = { Text("共享文件夹名称") },
                                        placeholder = { Text("shared") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = smbRemotePath,
                                        onValueChange = { smbRemotePath = it },
                                        label = { Text("文件路径/文件名") },
                                        placeholder = { Text("SimpleSchedule/backup.json") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = smbUsername,
                                        onValueChange = { smbUsername = it },
                                        label = { Text("用户名 (匿名留空)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = smbPassword,
                                        onValueChange = { smbPassword = it },
                                        label = { Text("密码") },
                                        singleLine = true,
                                        visualTransformation = if (showSmbPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showSmbPassword = !showSmbPassword }) {
                                                Icon(if (showSmbPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = smbDomain,
                                    onValueChange = { smbDomain = it },
                                    label = { Text("域名/工作组 (选填)") },
                                    placeholder = { Text("WORKGROUP") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            backupViewModel.testSmbConnection(currentSmbConfig()) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("测试连接", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            backupViewModel.backupToSmb(currentSmbConfig()) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SMB 备份", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { pendingCloudRestore = PendingCloudRestoreSource.SMB },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SMB 恢复", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (loadingMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF18181B) else Color.White,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accent)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(loadingMessage ?: "正在处理...", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                    }
                }
            }
        }

        if (pendingImportJson != null) {
            val json = pendingImportJson!!
            AlertDialog(
                onDismissRequest = { pendingImportJson = null },
                icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = accent) },
                title = { Text("选择本地数据恢复方式", fontWeight = FontWeight.Bold) },
                text = { Text("全量覆盖恢复会清空本地现有课表并用备份替换；增量合并导入则保留本地数据并按 ID 合并。") },
                confirmButton = {
                    Button(onClick = {
                        pendingImportJson = null
                        backupViewModel.restoreFromJson(
                            jsonString = json,
                            currentScheduleId = currentScheduleId,
                            mode = RestoreMode.FULL,
                            onScheduleSwitched = onScheduleSwitched,
                            onScheduleModified = onScheduleModified
                        ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                    }) {
                        Text("全量覆盖恢复")
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(onClick = {
                            pendingImportJson = null
                            backupViewModel.restoreFromJson(
                                jsonString = json,
                                currentScheduleId = currentScheduleId,
                                mode = RestoreMode.MERGE,
                                onScheduleSwitched = onScheduleSwitched,
                                onScheduleModified = onScheduleModified
                            ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        }) {
                            Text("增量合并")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = { pendingImportJson = null }) { Text("取消") }
                    }
                }
            )
        }

        if (pendingCloudRestore != null) {
            val source = pendingCloudRestore!!
            val sourceName = if (source == PendingCloudRestoreSource.WEBDAV) "WebDAV" else "SMB 局域网"
            AlertDialog(
                onDismissRequest = { pendingCloudRestore = null },
                icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFDC2626)) },
                title = { Text("确认从 $sourceName 恢复？", fontWeight = FontWeight.Bold) },
                text = { Text("「全量覆盖恢复」将清空本机当前所有课表、时间表和调休设置，并替换为云端备份；「增量合并」则保留现有课表并合并新内容。") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                        onClick = {
                            pendingCloudRestore = null
                            if (source == PendingCloudRestoreSource.WEBDAV) {
                                backupViewModel.restoreFromWebDav(
                                    config = currentWebDavConfig(),
                                    currentScheduleId = currentScheduleId,
                                    mode = RestoreMode.FULL,
                                    onScheduleSwitched = onScheduleSwitched,
                                    onScheduleModified = onScheduleModified
                                ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            } else {
                                backupViewModel.restoreFromSmb(
                                    config = currentSmbConfig(),
                                    currentScheduleId = currentScheduleId,
                                    mode = RestoreMode.FULL,
                                    onScheduleSwitched = onScheduleSwitched,
                                    onScheduleModified = onScheduleModified
                                ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            }
                        }
                    ) {
                        Text("全量覆盖恢复")
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(onClick = {
                            pendingCloudRestore = null
                            if (source == PendingCloudRestoreSource.WEBDAV) {
                                backupViewModel.restoreFromWebDav(
                                    config = currentWebDavConfig(),
                                    currentScheduleId = currentScheduleId,
                                    mode = RestoreMode.MERGE,
                                    onScheduleSwitched = onScheduleSwitched,
                                    onScheduleModified = onScheduleModified
                                ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            } else {
                                backupViewModel.restoreFromSmb(
                                    config = currentSmbConfig(),
                                    currentScheduleId = currentScheduleId,
                                    mode = RestoreMode.MERGE,
                                    onScheduleSwitched = onScheduleSwitched,
                                    onScheduleModified = onScheduleModified
                                ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                            }
                        }) {
                            Text("增量合并")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = { pendingCloudRestore = null }) { Text("取消") }
                    }
                }
            )
        }
    }
}
