package com.example.simpleschedule.utils

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object WebDavHelper {

    private fun normalizeUrl(serverUrl: String, remotePath: String): String {
        val parsed = java.net.URI(serverUrl.trim().let { if (it.contains("://")) it else "https://$it" })
        require(parsed.scheme.equals("https", true) || parsed.scheme.equals("http", true)) { "WebDAV 地址无效" }
        require(parsed.host.isNullOrBlank().not()) { "WebDAV 地址无效" }
        var base = parsed.toString()
        if (!base.endsWith("/")) base += "/"
        val path = remotePath.trim().trimStart('/')
        require(path.isNotBlank() && !path.contains("..")) { "WebDAV 路径无效" }
        return base + path
    }

    private fun validateScheme(serverUrl: String, password: String) {
        val scheme = java.net.URI(serverUrl.trim().let { if (it.contains("://")) it else "https://$it" }).scheme
        require(password.isBlank() || scheme.equals("https", ignoreCase = true)) {
            "使用 WebDAV 密码时必须使用 HTTPS"
        }
    }

    suspend fun testConnection(serverUrl: String, username: String, password: String, remotePath: String = ""): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            validateScheme(serverUrl, password)
            val fullUrl = if (remotePath.isNotBlank()) normalizeUrl(serverUrl, remotePath) else normalizeUrl(serverUrl, "")
            val url = URL(fullUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (username.isNotBlank()) {
                val auth = "$username:$password"
                val encodedAuth = Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            }

            val code = connection.responseCode
            if (code in 200..399 || code == 404) {
                Result.success("连接成功 (状态码 $code)")
            } else if (code == 401) {
                Result.failure(Exception("认证失败 (HTTP 401)，请检查用户名和密码"))
            } else {
                Result.failure(Exception("服务器响应异常 (HTTP $code)"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("连接失败: ${e.localizedMessage ?: e.message}"))
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun uploadFile(serverUrl: String, username: String, password: String, remotePath: String, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            validateScheme(serverUrl, password)
            val fullUrl = normalizeUrl(serverUrl, remotePath)
            val url = URL(fullUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            if (username.isNotBlank()) {
                val auth = "$username:$password"
                val encodedAuth = Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            }

            val bytes = content.toByteArray(Charsets.UTF_8)
            connection.setRequestProperty("Content-Length", bytes.size.toString())

            connection.outputStream.use { os ->
                os.write(bytes)
                os.flush()
            }

            val code = connection.responseCode
            if (code in 200..299) {
                Result.success(true)
            } else if (code == 401) {
                Result.failure(Exception("认证失败 (HTTP 401)"))
            } else {
                Result.failure(Exception("上传失败，服务器状态码: $code"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("WebDAV 上传异常: ${e.localizedMessage ?: e.message}"))
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun downloadFile(serverUrl: String, username: String, password: String, remotePath: String): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            validateScheme(serverUrl, password)
            val fullUrl = normalizeUrl(serverUrl, remotePath)
            val url = URL(fullUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (username.isNotBlank()) {
                val auth = "$username:$password"
                val encodedAuth = Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                connection.setRequestProperty("Authorization", "Basic $encodedAuth")
            }

            val code = connection.responseCode
            if (code == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                Result.success(sb.toString())
            } else if (code == 404) {
                Result.failure(Exception("远程云端未找到备份文件 ($remotePath)"))
            } else if (code == 401) {
                Result.failure(Exception("认证失败 (HTTP 401)"))
            } else {
                Result.failure(Exception("下载失败，服务器状态码: $code"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("WebDAV 下载异常: ${e.localizedMessage ?: e.message}"))
        } finally {
            connection?.disconnect()
        }
    }
}
