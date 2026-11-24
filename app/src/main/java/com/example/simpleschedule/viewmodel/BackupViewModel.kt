package com.example.simpleschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpleschedule.SimpleScheduleApplication
import com.example.simpleschedule.data.local.datastore.SmbConfig
import com.example.simpleschedule.data.local.datastore.WebDavConfig
import com.example.simpleschedule.data.repository.RestoreMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SimpleScheduleApplication).container
    private val backupRepo = container.backupRepository
    private val settingsRepo = container.settingsRepository

    val webDavConfig: StateFlow<WebDavConfig> = settingsRepo.settingsFlow
        .map { it.webDav }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WebDavConfig())

    val smbConfig: StateFlow<SmbConfig> = settingsRepo.settingsFlow
        .map { it.smb }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SmbConfig())

    val lastBackupTime: StateFlow<String> = settingsRepo.settingsFlow
        .map { it.lastBackupTime }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    suspend fun exportFullBackupJson(): String = backupRepo.exportFullBackupJson()

    suspend fun exportCurrentScheduleJson(scheduleId: String): String =
        backupRepo.exportCurrentScheduleJson(scheduleId)

    fun restoreFromJson(
        jsonString: String,
        currentScheduleId: String,
        mode: RestoreMode,
        onScheduleSwitched: (String) -> Unit,
        onScheduleModified: () -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _loadingMessage.value = "正在校验并恢复数据..."
            val res = backupRepo.restoreFromJson(jsonString, currentScheduleId, mode)
            _loadingMessage.value = null
            res.fold(
                onSuccess = { (msg, nextScheduleId) ->
                    if (nextScheduleId != null) {
                        onScheduleSwitched(nextScheduleId)
                    } else {
                        onScheduleModified()
                    }
                    onResult(true, msg)
                },
                onFailure = { onResult(false, it.message ?: "导入失败") }
            )
        }
    }

    fun testWebDavConnection(config: WebDavConfig, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _loadingMessage.value = "正在测试 WebDAV 连接..."
            val res = backupRepo.testWebDav(config)
            _loadingMessage.value = null
            res.fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message ?: "连接失败") }
            )
        }
    }

    fun backupToWebDav(config: WebDavConfig, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _loadingMessage.value = "正在导出备份到 WebDAV 云端..."
            val res = backupRepo.backupToWebDav(config)
            _loadingMessage.value = null
            res.fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message ?: "WebDAV 备份失败") }
            )
        }
    }

    fun restoreFromWebDav(
        config: WebDavConfig,
        currentScheduleId: String,
        mode: RestoreMode,
        onScheduleSwitched: (String) -> Unit,
        onScheduleModified: () -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _loadingMessage.value = "正在从 WebDAV 云端恢复..."
            val res = backupRepo.restoreFromWebDav(config, currentScheduleId, mode)
            _loadingMessage.value = null
            res.fold(
                onSuccess = { (msg, nextScheduleId) ->
                    if (nextScheduleId != null) onScheduleSwitched(nextScheduleId) else onScheduleModified()
                    onResult(true, msg)
                },
                onFailure = { onResult(false, it.message ?: "WebDAV 恢复失败") }
            )
        }
    }

    fun testSmbConnection(config: SmbConfig, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _loadingMessage.value = "正在测试 SMB 共享连接..."
            val res = backupRepo.testSmb(config)
            _loadingMessage.value = null
            res.fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message ?: "连接失败") }
            )
        }
    }

    fun backupToSmb(config: SmbConfig, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _loadingMessage.value = "正在导出备份到 SMB 共享..."
            val res = backupRepo.backupToSmb(config)
            _loadingMessage.value = null
            res.fold(
                onSuccess = { onResult(true, it) },
                onFailure = { onResult(false, it.message ?: "SMB 备份失败") }
            )
        }
    }

    fun restoreFromSmb(
        config: SmbConfig,
        currentScheduleId: String,
        mode: RestoreMode,
        onScheduleSwitched: (String) -> Unit,
        onScheduleModified: () -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _loadingMessage.value = "正在从 SMB 共享恢复..."
            val res = backupRepo.restoreFromSmb(config, currentScheduleId, mode)
            _loadingMessage.value = null
            res.fold(
                onSuccess = { (msg, nextScheduleId) ->
                    if (nextScheduleId != null) onScheduleSwitched(nextScheduleId) else onScheduleModified()
                    onResult(true, msg)
                },
                onFailure = { onResult(false, it.message ?: "SMB 恢复失败") }
            )
        }
    }
}
