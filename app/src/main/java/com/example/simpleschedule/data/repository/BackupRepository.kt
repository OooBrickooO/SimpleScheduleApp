package com.example.simpleschedule.data.repository

import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.datastore.SmbConfig
import com.example.simpleschedule.data.local.datastore.WebDavConfig
import com.example.simpleschedule.domain.BackupJsonSerializer
import com.example.simpleschedule.domain.ParsedImportJson
import com.example.simpleschedule.utils.SmbHelper
import com.example.simpleschedule.utils.WebDavHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val payload = scheduleRepository.exportFullBackupPayload()
        BackupJsonSerializer.serializeFullBackup(payload)
    }

    suspend fun exportCurrentScheduleJson(scheduleId: String): String = withContext(Dispatchers.IO) {
        val courses = scheduleRepository.exportScheduleCourses(scheduleId)
        BackupJsonSerializer.serializeCourses(courses)
    }

    /**
     * Validates the JSON completely in memory before opening a DB transaction.
     * Returns Pair(Message, NewActiveScheduleId?).
     */
    suspend fun restoreFromJson(
        jsonString: String,
        currentScheduleId: String,
        mode: RestoreMode
    ): Result<Pair<String, String?>> = withContext(Dispatchers.IO) {
        val parsedResult = BackupJsonSerializer.parseImportJson(jsonString, currentScheduleId)
        val parsed = parsedResult.getOrElse { return@withContext Result.failure(it) }

        when (parsed) {
            is ParsedImportJson.FullBackup -> {
                val nextScheduleId = scheduleRepository.restoreFullBackupAtomic(parsed.payload, mode)
                Result.success(Pair("备份数据恢复成功！", nextScheduleId))
            }
            is ParsedImportJson.CourseList -> {
                val totalWeeks = settingsRepository.settingsFlow.first().totalWeeks
                val maxWeek = scheduleRepository.importCoursesFromJson(
                    scheduleId = currentScheduleId,
                    jsonString = parsed.jsonArrayString,
                    replaceCurrent = (mode == RestoreMode.FULL),
                    currentTotalWeeks = totalWeeks
                ).getOrElse { return@withContext Result.failure(it) }

                if (maxWeek > totalWeeks) {
                    settingsRepository.setPreference(SettingsKeys.TOTAL_WEEKS, maxWeek)
                }
                Result.success(Pair("课程列表导入成功！", null))
            }
        }
    }

    suspend fun testWebDav(config: WebDavConfig): Result<String> {
        settingsRepository.saveWebDavConfig(config)
        return WebDavHelper.testConnection(
            serverUrl = config.url,
            username = config.username,
            password = config.password,
            remotePath = config.remotePath
        )
    }

    suspend fun backupToWebDav(config: WebDavConfig): Result<String> {
        settingsRepository.saveWebDavConfig(config)
        if (config.url.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写 WebDAV 服务器地址"))
        }
        val content = exportFullBackupJson()
        val uploadResult = WebDavHelper.uploadFile(
            serverUrl = config.url,
            username = config.username,
            password = config.password,
            remotePath = config.remotePath,
            content = content
        )
        return uploadResult.map {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            settingsRepository.setPreference(SettingsKeys.LAST_BACKUP_TIME, now)
            "WebDAV 云备份成功！"
        }
    }

    suspend fun restoreFromWebDav(
        config: WebDavConfig,
        currentScheduleId: String,
        mode: RestoreMode
    ): Result<Pair<String, String?>> {
        settingsRepository.saveWebDavConfig(config)
        if (config.url.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写 WebDAV 服务器地址"))
        }
        val content = WebDavHelper.downloadFile(
            serverUrl = config.url,
            username = config.username,
            password = config.password,
            remotePath = config.remotePath
        ).getOrElse { return Result.failure(it) }

        return restoreFromJson(content, currentScheduleId, mode)
    }

    suspend fun testSmb(config: SmbConfig): Result<String> {
        settingsRepository.saveSmbConfig(config)
        return SmbHelper.testConnection(
            host = config.host,
            port = config.port,
            shareName = config.share,
            username = config.username,
            password = config.password,
            domain = config.domain
        )
    }

    suspend fun backupToSmb(config: SmbConfig): Result<String> {
        settingsRepository.saveSmbConfig(config)
        if (config.host.isBlank() || config.share.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写 SMB 服务器 IP 和共享名称"))
        }
        val content = exportFullBackupJson()
        val uploadResult = SmbHelper.uploadFile(
            host = config.host,
            port = config.port,
            shareName = config.share,
            username = config.username,
            password = config.password,
            domain = config.domain,
            remotePath = config.remotePath,
            content = content
        )
        return uploadResult.map {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            settingsRepository.setPreference(SettingsKeys.LAST_BACKUP_TIME, now)
            "SMB 云备份成功！"
        }
    }

    suspend fun restoreFromSmb(
        config: SmbConfig,
        currentScheduleId: String,
        mode: RestoreMode
    ): Result<Pair<String, String?>> {
        settingsRepository.saveSmbConfig(config)
        if (config.host.isBlank() || config.share.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写 SMB 服务器 IP 和共享名称"))
        }
        val content = SmbHelper.downloadFile(
            host = config.host,
            port = config.port,
            shareName = config.share,
            username = config.username,
            password = config.password,
            domain = config.domain,
            remotePath = config.remotePath
        ).getOrElse { return Result.failure(it) }

        return restoreFromJson(content, currentScheduleId, mode)
    }
}
