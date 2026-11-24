package com.example.simpleschedule.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.util.concurrent.TimeUnit

object WebDavHelper {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val emptyBody = ByteArray(0).toRequestBody(null)

    private fun normalizeBaseUrl(serverUrl: String): String {
        val trimmed = serverUrl.trim()
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val parsed = URI(withScheme)
        require(
            parsed.scheme.equals("https", ignoreCase = true) ||
                parsed.scheme.equals("http", ignoreCase = true)
        ) { "WebDAV 地址无效" }
        require(!parsed.host.isNullOrBlank()) { "WebDAV 地址无效" }
        val base = parsed.toString()
        return if (base.endsWith("/")) base else "$base/"
    }

    private fun normalizeRemotePath(remotePath: String): String {
        val path = remotePath.trim().replace("\\", "/").trimStart('/')
        require(!path.contains("..")) { "WebDAV 路径无效" }
        return path
    }

    private fun normalizeFullUrl(serverUrl: String, remotePath: String): String {
        val base = normalizeBaseUrl(serverUrl)
        val path = normalizeRemotePath(remotePath)
        return if (path.isBlank()) base else base + path
    }

    private fun authHeader(username: String, password: String): String? {
        if (username.isBlank()) return null
        return Credentials.basic(username.trim(), password, Charsets.UTF_8)
    }

    private fun Request.Builder.withAuth(auth: String?): Request.Builder {
        if (auth != null) header("Authorization", auth)
        return this
    }

    suspend fun testConnection(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = normalizeBaseUrl(serverUrl)
            val targetUrl = if (remotePath.isBlank()) baseUrl else normalizeFullUrl(serverUrl, remotePath)
            val auth = authHeader(username, password)

            val propfindReq = Request.Builder()
                .url(baseUrl)
                .method("PROPFIND", emptyBody)
                .header("Depth", "0")
                .withAuth(auth)
                .build()

            client.newCall(propfindReq).execute().use { resp ->
                val code = resp.code
                if (code in 200..299 || code == 207) {
                    return@withContext Result.success("连接成功 (HTTP $code)")
                }
                if (code == 401 || code == 403) {
                    return@withContext Result.failure(Exception("认证失败 (HTTP $code)，请检查用户名和密码"))
                }
            }

            // Fallback for servers that disallow PROPFIND on root
            val headReq = Request.Builder()
                .url(targetUrl)
                .head()
                .withAuth(auth)
                .build()
            client.newCall(headReq).execute().use { resp ->
                when (val code = resp.code) {
                    in 200..399, 404 -> Result.success("连接成功 (状态码 $code)")
                    401, 403 -> Result.failure(Exception("认证失败 (HTTP $code)，请检查用户名和密码"))
                    else -> Result.failure(Exception("服务器响应异常 (HTTP $code)"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("连接失败: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun ensureParentDirectories(baseUrl: String, cleanPath: String, auth: String?): Result<Unit> {
        val segments = cleanPath.split("/").filter { it.isNotBlank() }
        if (segments.size <= 1) return Result.success(Unit)

        var currentDirUrl = baseUrl
        for (dir in segments.dropLast(1)) {
            currentDirUrl += "$dir/"
            val mkcolReq = Request.Builder()
                .url(currentDirUrl)
                .method("MKCOL", emptyBody)
                .withAuth(auth)
                .build()
            client.newCall(mkcolReq).execute().use { resp ->
                when (val code = resp.code) {
                    200, 201, 204, 301, 302, 405 -> Unit // Created or already exists
                    401, 403 -> return Result.failure(Exception("创建目录认证失败 (HTTP $code)"))
                    else -> {
                        // Some WebDAV providers return 409 if existing or strict, probe via PROPFIND
                        if (code != 409) {
                            return Result.failure(Exception("创建远程目录失败 (HTTP $code)"))
                        }
                    }
                }
            }
        }
        return Result.success(Unit)
    }

    suspend fun uploadFile(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String,
        content: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = normalizeBaseUrl(serverUrl)
            val cleanPath = normalizeRemotePath(remotePath)
            require(cleanPath.isNotBlank()) { "请填写远程文件路径" }
            val fullUrl = baseUrl + cleanPath
            val auth = authHeader(username, password)

            val dirResult = ensureParentDirectories(baseUrl, cleanPath, auth)
            if (dirResult.isFailure) {
                return@withContext Result.failure(dirResult.exceptionOrNull()!!)
            }

            val putReq = Request.Builder()
                .url(fullUrl)
                .put(content.toByteArray(Charsets.UTF_8).toRequestBody(jsonMediaType))
                .withAuth(auth)
                .build()

            client.newCall(putReq).execute().use { resp ->
                when (val code = resp.code) {
                    in 200..299 -> Result.success(true)
                    401, 403 -> Result.failure(Exception("认证失败 (HTTP $code)"))
                    else -> Result.failure(Exception("上传失败，服务器状态码: $code"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("WebDAV 上传异常: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun downloadFile(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = normalizeFullUrl(serverUrl, remotePath)
            val auth = authHeader(username, password)
            val getReq = Request.Builder()
                .url(fullUrl)
                .get()
                .withAuth(auth)
                .build()

            client.newCall(getReq).execute().use { resp ->
                when (val code = resp.code) {
                    200 -> {
                        val bodyStr = resp.body?.string() ?: ""
                        Result.success(bodyStr)
                    }
                    404 -> Result.failure(Exception("远程云端未找到备份文件 ($remotePath)"))
                    401, 403 -> Result.failure(Exception("认证失败 (HTTP $code)"))
                    else -> Result.failure(Exception("下载失败，服务器状态码: $code"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("WebDAV 下载异常: ${e.localizedMessage ?: e.message}"))
        }
    }
}
