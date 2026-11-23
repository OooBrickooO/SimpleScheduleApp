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
        return remotePath.trim().replace("/", "\\").trimStart('\\')
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
            val client = SMBClient()
            client.connect(host.trim(), port).use { connection ->
                val authContext = if (username.isBlank()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
                }
                connection.authenticate(authContext).use { session ->
                    (session.connectShare(shareName.trim()) as DiskShare).use { _ ->
                        Result.success("SMB 共享连接成功！")
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
            if (host.isBlank() || shareName.isBlank() || remotePath.isBlank()) {
                return@withContext Result.failure(Exception("请填写完整的 SMB 服务器配置"))
            }
            val client = SMBClient()
            client.connect(host.trim(), port).use { connection ->
                val authContext = if (username.isBlank()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
                }
                connection.authenticate(authContext).use { session ->
                    (session.connectShare(shareName.trim()) as DiskShare).use { diskShare ->
                        val path = normalizePath(remotePath)
                        if (path.contains("\\")) {
                            val parentDir = path.substringBeforeLast("\\")
                            if (!diskShare.folderExists(parentDir)) {
                                diskShare.mkdir(parentDir)
                            }
                        }

                        val file = diskShare.openFile(
                            path,
                            EnumSet.of(AccessMask.GENERIC_WRITE),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OVERWRITE_IF,
                            null
                        )
                        file.use { f ->
                            val bytes = content.toByteArray(Charsets.UTF_8)
                            f.write(bytes, 0)
                        }
                        Result.success(true)
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
            if (host.isBlank() || shareName.isBlank() || remotePath.isBlank()) {
                return@withContext Result.failure(Exception("请填写完整的 SMB 服务器配置"))
            }
            val client = SMBClient()
            client.connect(host.trim(), port).use { connection ->
                val authContext = if (username.isBlank()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
                }
                connection.authenticate(authContext).use { session ->
                    (session.connectShare(shareName.trim()) as DiskShare).use { diskShare ->
                        val path = normalizePath(remotePath)
                        if (!diskShare.fileExists(path)) {
                            return@use Result.failure(Exception("SMB 共享目录中未找到文件: $remotePath"))
                        }
                        val file = diskShare.openFile(
                            path,
                            EnumSet.of(AccessMask.GENERIC_READ),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OPEN,
                            null
                        )
                        val text = file.use { f ->
                            f.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        }
                        Result.success(text)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("SMB 下载失败: ${e.localizedMessage ?: e.message}"))
        }
    }
}
