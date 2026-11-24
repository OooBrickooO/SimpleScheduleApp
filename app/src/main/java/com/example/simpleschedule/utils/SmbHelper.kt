package com.example.simpleschedule.utils

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumSet

object SmbHelper {

    private fun normalizePath(remotePath: String): String {
        val cleaned = remotePath.trim().replace("/", "\\").trimStart('\\')
        require(!cleaned.contains("..")) { "SMB 路径无效" }
        return cleaned
    }

    private fun buildAuthContext(username: String, password: String, domain: String): AuthenticationContext {
        return if (username.isBlank()) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
        }
    }

    private fun ensureParentDirs(diskShare: DiskShare, normalizedFilePath: String) {
        if (!normalizedFilePath.contains("\\")) return
        val parentDir = normalizedFilePath.substringBeforeLast("\\")
        val segments = parentDir.split("\\").filter { it.isNotBlank() }
        var current = ""
        for (segment in segments) {
            current = if (current.isEmpty()) segment else "$current\\$segment"
            if (!diskShare.folderExists(current)) {
                diskShare.mkdir(current)
            }
        }
    }

    suspend fun testConnection(
        host: String,
        port: Int = 445,
        shareName: String,
        username: String,
        password: String,
        domain: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (host.isBlank() || shareName.isBlank() || port !in 1..65535) {
                return@withContext Result.failure(Exception("请填写有效的 SMB 服务器、共享名称和端口"))
            }
            SMBClient().use { client ->
                client.connect(host.trim(), port).use { connection ->
                    connection.authenticate(buildAuthContext(username, password, domain)).use { session ->
                        (session.connectShare(shareName.trim()) as DiskShare).use {
                            Result.success("SMB 共享连接成功！")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("SMB 连接失败: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun uploadFile(
        host: String,
        port: Int = 445,
        shareName: String,
        username: String,
        password: String,
        domain: String = "",
        remotePath: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (host.isBlank() || shareName.isBlank() || remotePath.isBlank() || port !in 1..65535) {
                return@withContext Result.failure(Exception("请填写完整的 SMB 服务器配置"))
            }
            SMBClient().use { client ->
                client.connect(host.trim(), port).use { connection ->
                    connection.authenticate(buildAuthContext(username, password, domain)).use { session ->
                        (session.connectShare(shareName.trim()) as DiskShare).use { diskShare ->
                            val path = normalizePath(remotePath)
                            ensureParentDirs(diskShare, path)

                            diskShare.openFile(
                                path,
                                EnumSet.of(AccessMask.GENERIC_WRITE),
                                null,
                                SMB2ShareAccess.ALL,
                                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                                null
                            ).use { file ->
                                file.outputStream.use { os ->
                                    os.write(content.toByteArray(Charsets.UTF_8))
                                    os.flush()
                                }
                            }
                            Result.success(true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("SMB 上传失败: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun downloadFile(
        host: String,
        port: Int = 445,
        shareName: String,
        username: String,
        password: String,
        domain: String = "",
        remotePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (host.isBlank() || shareName.isBlank() || remotePath.isBlank() || port !in 1..65535) {
                return@withContext Result.failure(Exception("请填写完整的 SMB 服务器配置"))
            }
            SMBClient().use { client ->
                client.connect(host.trim(), port).use { connection ->
                    connection.authenticate(buildAuthContext(username, password, domain)).use { session ->
                        (session.connectShare(shareName.trim()) as DiskShare).use { diskShare ->
                            val path = normalizePath(remotePath)
                            if (!diskShare.fileExists(path)) {
                                return@use Result.failure(Exception("SMB 共享目录中未找到文件: $remotePath"))
                            }
                            val text = diskShare.openFile(
                                path,
                                EnumSet.of(AccessMask.GENERIC_READ),
                                null,
                                SMB2ShareAccess.ALL,
                                SMB2CreateDisposition.FILE_OPEN,
                                null
                            ).use { file ->
                                file.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                            }
                            Result.success(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("SMB 下载失败: ${e.localizedMessage ?: e.message}"))
        }
    }
}
