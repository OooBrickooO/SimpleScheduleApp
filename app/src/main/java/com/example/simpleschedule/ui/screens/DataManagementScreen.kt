package com.example.simpleschedule.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.simpleschedule.ui.theme.*
import com.example.simpleschedule.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataManagementScreen(
    viewModel: ScheduleViewModel,
    isDark: Boolean,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val textColor = if (isDark) TextDark else TextLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
    val accent = LocalAccentColor.current ?: if (isDark) Color.White else Color.Black

    // WebDAV Settings state
    val savedWebdavUrl by viewModel.webdavUrl.collectAsState()
    val savedWebdavUsername by viewModel.webdavUsername.collectAsState()
    val savedWebdavPassword by viewModel.webdavPassword.collectAsState()
    val savedWebdavRemotePath by viewModel.webdavRemotePath.collectAsState()

    var webdavUrl by remember(savedWebdavUrl) { mutableStateOf(savedWebdavUrl) }
    var webdavUsername by remember(savedWebdavUsername) { mutableStateOf(savedWebdavUsername) }
    var webdavPassword by remember(savedWebdavPassword) { mutableStateOf(savedWebdavPassword) }
    var webdavRemotePath by remember(savedWebdavRemotePath) { mutableStateOf(savedWebdavRemotePath) }
    var showWebdavPassword by remember { mutableStateOf(false) }

    // SMB Settings state
    val savedSmbHost by viewModel.smbHost.collectAsState()
    val savedSmbPort by viewModel.smbPort.collectAsState()
    val savedSmbShare by viewModel.smbShare.collectAsState()
    val savedSmbUsername by viewModel.smbUsername.collectAsState()
    val savedSmbPassword by viewModel.smbPassword.collectAsState()
    val savedSmbDomain by viewModel.smbDomain.collectAsState()
    val savedSmbRemotePath by viewModel.smbRemotePath.collectAsState()

    var smbHost by remember(savedSmbHost) { mutableStateOf(savedSmbHost) }
    var smbPortStr by remember(savedSmbPort) { mutableStateOf(savedSmbPort.toString()) }
    var smbShare by remember(savedSmbShare) { mutableStateOf(savedSmbShare) }
    var smbUsername by remember(savedSmbUsername) { mutableStateOf(savedSmbUsername) }
    var smbPassword by remember(savedSmbPassword) { mutableStateOf(savedSmbPassword) }
    var smbDomain by remember(savedSmbDomain) { mutableStateOf(savedSmbDomain) }
    var smbRemotePath by remember(savedSmbRemotePath) { mutableStateOf(savedSmbRemotePath) }
    var showSmbPassword by remember { mutableStateOf(false) }

    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

    var selectedCloudTab by remember { mutableIntStateOf(0) } // 0: WebDAV, 1: SMB
    var isLoading by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("正在处理...") }

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    // SAF Launchers
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val content = pendingExportJson ?: return@let
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "文件导出成功！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)?.use { isStream ->
                    isStream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!json.isNullOrBlank()) {
                    pendingImportJson = json
                    showImportConfirmDialog = true
                } else {
                    Toast.makeText(context, "所选文件内容为空", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取文件失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) BgDark else BgLight)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.FolderSpecial,
                    contentDescription = "Data Management",
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "数据管理",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    Text(
                        "本地导入导出与 WebDAV / SMB 云备份",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // --- Section 1: 本地数据导入与导出 ---
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "本地数据备份与恢复",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Surface(
                        color = surfaceColor,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Rounded.SaveAlt,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "本地导出 JSON",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                            pendingExportJson = viewModel.exportFullBackupJson()
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
                                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                            pendingExportJson = viewModel.exportCurrentScheduleJson()
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Rounded.FileUpload,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "本地导入 JSON",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    importFileLauncher.launch(arrayOf("application/json", "*/*"))
                                },
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

                // --- Section 2: 云端备份 (WebDAV / SMB) ---
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "云端备份与同步",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Surface(
                        color = surfaceColor,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Status & Last Backup Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("云存储设置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                if (lastBackupTime.isNotEmpty()) {
                                    Text(
                                        "上次云备份: $lastBackupTime",
                                        fontSize = 11.sp,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Segmented Tabs: WebDAV vs SMB
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selectedCloudTab == 0) accent.copy(alpha = 0.2f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
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
                                        .background(
                                            if (selectedCloudTab == 1) accent.copy(alpha = 0.2f) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
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
                                // --- WebDAV Configuration ---
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
                                                Icon(
                                                    if (showWebdavPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // WebDAV Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.saveWebDavSettings(webdavUrl, webdavUsername, webdavPassword, webdavRemotePath)
                                            isLoading = true
                                            loadingText = "正在测试 WebDAV 连接..."
                                            viewModel.testWebDavConnection(webdavUrl, webdavUsername, webdavPassword, webdavRemotePath) { success, msg ->
                                                isLoading = false
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
                                            viewModel.saveWebDavSettings(webdavUrl, webdavUsername, webdavPassword, webdavRemotePath)
                                            isLoading = true
                                            loadingText = "正在导出备份到 WebDAV 云端..."
                                            viewModel.backupToWebDav { success, msg ->
                                                isLoading = false
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
                                        onClick = {
                                            viewModel.saveWebDavSettings(webdavUrl, webdavUsername, webdavPassword, webdavRemotePath)
                                            isLoading = true
                                            loadingText = "正在从 WebDAV 云端恢复..."
                                            viewModel.restoreFromWebDav(mode = "FULL") { success, msg ->
                                                isLoading = false
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("云端恢复", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                // --- SMB Configuration ---
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
                                                Icon(
                                                    if (showSmbPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = null
                                                )
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

                                // SMB Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val portInt = smbPortStr.toIntOrNull() ?: 445

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.saveSmbSettings(smbHost, portInt, smbShare, smbUsername, smbPassword, smbDomain, smbRemotePath)
                                            isLoading = true
                                            loadingText = "正在测试 SMB 共享连接..."
                                            viewModel.testSmbConnection(smbHost, portInt, smbShare, smbUsername, smbPassword, smbDomain) { success, msg ->
                                                isLoading = false
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
                                            viewModel.saveSmbSettings(smbHost, portInt, smbShare, smbUsername, smbPassword, smbDomain, smbRemotePath)
                                            isLoading = true
                                            loadingText = "正在导出备份到 SMB 共享..."
                                            viewModel.backupToSmb { success, msg ->
                                                isLoading = false
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
                                        onClick = {
                                            viewModel.saveSmbSettings(smbHost, portInt, smbShare, smbUsername, smbPassword, smbDomain, smbRemotePath)
                                            isLoading = true
                                            loadingText = "正在从 SMB 共享恢复..."
                                            viewModel.restoreFromSmb(mode = "FULL") { success, msg ->
                                                isLoading = false
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
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

        // --- Loading Overlay ---
        if (isLoading) {
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
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = accent)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(loadingText, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                    }
                }
            }
        }

        // --- Import Option Dialog ---
        if (showImportConfirmDialog && pendingImportJson != null) {
            val json = pendingImportJson!!
            AlertDialog(
                onDismissRequest = {
                    showImportConfirmDialog = false
                    pendingImportJson = null
                },
                icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = accent) },
                title = { Text("选择数据恢复方式", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "检测到有效的数据文件。\n请选择如何恢复此备份数据："
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showImportConfirmDialog = false
                            viewModel.restoreFullBackupJson(json, mode = "FULL") { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                            pendingImportJson = null
                        }
                    ) {
                        Text("全量覆盖恢复 (推荐)")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showImportConfirmDialog = false
                            viewModel.restoreFullBackupJson(json, mode = "MERGE") { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                            pendingImportJson = null
                        }
                    ) {
                        Text("增量合并导入")
                    }
                }
            )
        }
    }
}
