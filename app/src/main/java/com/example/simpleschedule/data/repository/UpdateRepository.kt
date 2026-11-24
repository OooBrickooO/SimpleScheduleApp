package com.example.simpleschedule.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val apkUrl: String,
    val forceUpdate: Boolean
)

data class AnnouncementInfo(
    val id: String,
    val title: String,
    val content: String
)

data class DownloadState(
    val status: String? = null,
    val progress: Float = 0f,
    val sizeLabel: String = ""
)

class UpdateRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        const val UPDATE_URL = "https://www.lingflame.cn/update.json"
        const val ANNOUNCEMENT_URL = "https://www.lingflame.cn/announcement.json"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val installTriggered = AtomicBoolean(false)
    private var trackingJob: Job? = null
    private var downloadId: Long = -1L

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == downloadId && id != -1L) {
                val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id)
                manager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = if (statusCol >= 0) cursor.getInt(statusCol) else DownloadManager.STATUS_FAILED
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            installDownloadedApk()
                        } else {
                            Toast.makeText(ctx, "下载失败，请检查网络后重试喵", Toast.LENGTH_SHORT).show()
                            _downloadState.value = DownloadState()
                        }
                    }
                }
            }
        }
    }

    private var receiverRegistered = false

    fun registerReceiver() {
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                context,
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )
            receiverRegistered = true
        }
    }

    fun unregisterReceiver() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(downloadReceiver) }
            receiverRegistered = false
        }
    }

    fun cleanupOldApk() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val oldApkFile = getApkTargetFile()
                if (oldApkFile.exists()) {
                    oldApkFile.delete()
                }
            }
        }
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    suspend fun shouldAutoCheckToday(): Boolean {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.autoUpdate) return false
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastCheckedDate = settingsRepository.getPreference(SettingsKeys.LAST_CHECKED_UPDATE_DATE) ?: ""
        if (lastCheckedDate == todayStr) return false
        settingsRepository.setPreference(SettingsKeys.LAST_CHECKED_UPDATE_DATE, todayStr)
        return true
    }

    suspend fun checkForAutoUpdate(): UpdateInfo? {
        if (!shouldAutoCheckToday()) return null
        val update = checkUpdate() ?: return null
        val currentVersionCode = getCurrentVersionCode()
        if (update.versionCode <= currentVersionCode) return null
        val lastSilencedCode = settingsRepository.getPreference(SettingsKeys.LAST_SILENCED_VERSION_CODE) ?: -1
        return if (update.versionCode > lastSilencedCode) update else null
    }

    suspend fun silenceVersion(versionCode: Int) {
        settingsRepository.setPreference(SettingsKeys.LAST_SILENCED_VERSION_CODE, versionCode)
    }

    suspend fun checkUpdate(updateUrl: String = UPDATE_URL): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val cacheBusterUrl = if (updateUrl.contains("?")) {
                "$updateUrl&_t=${System.currentTimeMillis()}"
            } else {
                "$updateUrl?_t=${System.currentTimeMillis()}"
            }
            connection = (URL(cacheBusterUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val jsonObject = JSONObject(body)
                UpdateInfo(
                    versionCode = jsonObject.getInt("versionCode"),
                    versionName = jsonObject.getString("versionName"),
                    changelog = jsonObject.getString("changelog"),
                    apkUrl = jsonObject.getString("apkUrl"),
                    forceUpdate = jsonObject.optBoolean("forceUpdate", false)
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchAnnouncement(): AnnouncementInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(ANNOUNCEMENT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val jsonObject = JSONObject(body)
                AnnouncementInfo(
                    id = jsonObject.getString("id"),
                    title = jsonObject.getString("title"),
                    content = jsonObject.getString("content")
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun checkUnreadAnnouncement(): AnnouncementInfo? {
        val result = fetchAnnouncement() ?: return null
        val lastSeenId = settingsRepository.getPreference(SettingsKeys.LAST_SEEN_ANNOUNCEMENT_ID) ?: ""
        return if (lastSeenId != result.id) {
            settingsRepository.setPreference(SettingsKeys.LAST_SEEN_ANNOUNCEMENT_ID, result.id)
            result
        } else null
    }

    fun startDownloadApk(url: String) {
        val apkFile = getApkTargetFile()
        runCatching {
            if (apkFile.exists()) apkFile.delete()
        }

        try {
            installTriggered.set(false)
            val cacheBusterUrl = if (url.contains("?")) {
                "$url&_t=${System.currentTimeMillis()}"
            } else {
                "$url?_t=${System.currentTimeMillis()}"
            }
            val request = DownloadManager.Request(Uri.parse(cacheBusterUrl)).apply {
                setTitle("正在下载课表更新...")
                setDescription("SimpleSchedule 最新版本")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(apkFile))
                setMimeType("application/vnd.android.package-archive")
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)
            trackDownloadProgress(downloadId)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "启动下载失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun trackDownloadProgress(id: Long) {
        trackingJob?.cancel()
        _downloadState.value = DownloadState(status = "pending", progress = 0f, sizeLabel = "准备中...")

        trackingJob = scope.launch(Dispatchers.IO) {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = manager.query(query)
                if (cursor != null) {
                    cursor.use { c ->
                        if (c.moveToFirst()) {
                            val dlCol = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalCol = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val statusCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val bytesDownloaded = if (dlCol >= 0) c.getLong(dlCol) else 0L
                            val bytesTotal = if (totalCol >= 0) c.getLong(totalCol) else -1L
                            val status = if (statusCol >= 0) c.getInt(statusCol) else DownloadManager.STATUS_FAILED

                            val newProgress = if (bytesTotal > 0L) {
                                (bytesDownloaded.toFloat() / bytesTotal.toFloat()).coerceIn(0f, 1f)
                            } else _downloadState.value.progress

                            val newSizeLabel = if (bytesTotal > 0L) {
                                val downloadedMb = String.format(Locale.US, "%.2f", bytesDownloaded.toFloat() / (1024 * 1024))
                                val totalMb = String.format(Locale.US, "%.2f", bytesTotal.toFloat() / (1024 * 1024))
                                "$downloadedMb MB / $totalMb MB"
                            } else _downloadState.value.sizeLabel

                            val newStatus = when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    downloading = false
                                    "success"
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    downloading = false
                                    "failed"
                                }
                                DownloadManager.STATUS_PENDING -> "pending"
                                DownloadManager.STATUS_RUNNING -> "downloading"
                                DownloadManager.STATUS_PAUSED -> "paused"
                                else -> _downloadState.value.status
                            }

                            withContext(Dispatchers.Main) {
                                _downloadState.value = DownloadState(
                                    status = newStatus,
                                    progress = newProgress,
                                    sizeLabel = newSizeLabel
                                )
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    installDownloadedApk()
                                }
                            }
                        } else {
                            downloading = false
                        }
                    }
                } else {
                    downloading = false
                }
                if (downloading) delay(300)
            }
        }
    }

    fun cancelDownload() {
        trackingJob?.cancel()
        if (downloadId != -1L) {
            runCatching {
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.remove(downloadId)
            }
            downloadId = -1L
        }
        _downloadState.value = DownloadState()
    }

    private fun installDownloadedApk() {
        if (!installTriggered.compareAndSet(false, true)) return

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var fileUri: Uri? = null
        if (downloadId != -1L) {
            manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val uriCol = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val status = if (statusCol >= 0) cursor.getInt(statusCol) else DownloadManager.STATUS_FAILED
                    if (status == DownloadManager.STATUS_SUCCESSFUL && uriCol >= 0) {
                        val uriString = cursor.getString(uriCol)
                        if (!uriString.isNullOrEmpty()) {
                            fileUri = Uri.parse(uriString)
                        }
                    }
                }
            }
        }

        val apkFile = if (fileUri != null && fileUri?.path != null && !fileUri.toString().startsWith("content://")) {
            File(fileUri!!.path!!)
        } else {
            getApkTargetFile()
        }

        if (!apkFile.exists()) {
            installTriggered.set(false)
            Toast.makeText(context, "未找到下载的更新包喵", Toast.LENGTH_SHORT).show()
            _downloadState.value = DownloadState()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            installTriggered.set(false)
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(context, "请先允许应用「安装未知应用」权限喵！", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "无法自动跳转设置，请在系统设置中手动授权未知应用安装权限", Toast.LENGTH_LONG).show()
            }
            return
        }

        val authority = "${context.packageName}.fileprovider"
        try {
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            _downloadState.value = DownloadState()
        } catch (e: Exception) {
            installTriggered.set(false)
            e.printStackTrace()
            Toast.makeText(context, "拉起安装程序失败，请尝试在文件管理器中手动安装: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getApkTargetFile(): File {
        val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!privateDir.exists()) {
            privateDir.mkdirs()
        }
        return File(privateDir, "app-release.apk")
    }
}
