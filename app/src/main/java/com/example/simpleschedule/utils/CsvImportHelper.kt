package com.example.simpleschedule.utils

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private val courseColors = listOf("blue", "pink", "purple", "slate", "indigo", "rose")

private val monthAbbreviations = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
    "jul" to 7, "aug" to 8, "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)

suspend fun downloadCsvTemplate(context: Context, urlString: String): File? = withContext(Dispatchers.IO) {
    val urlsToTry = listOf(
        urlString,
        "https://www.lingflame.cn/schedule_template.csv"
    )
    for (currentUrl in urlsToTry) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(currentUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) SimpleScheduleApp")
            if (connection.responseCode == 200) {
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty() && bytes.size < 1_000_000) {
                    val decoded = decodeCsvBytes(bytes)
                    if (decoded.contains("课程") || decoded.contains(",")) {
                        return@withContext saveCsvToDownloads(context, bytes, "课表模板.csv")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
    }
    null
}

fun saveCsvToDownloads(context: Context, bytes: ByteArray, fileName: String): File {
    val appPrivateDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    if (appPrivateDir != null && !appPrivateDir.exists()) {
        appPrivateDir.mkdirs()
    }
    val fallbackFile = File(appPrivateDir ?: context.filesDir, fileName)
    try {
        fallbackFile.writeBytes(bytes)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(bytes)
                    os.flush()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDownloads.exists()) {
                publicDownloads.mkdirs()
            }
            val publicFile = File(publicDownloads, fileName)
            publicFile.writeBytes(bytes)
            MediaScannerConnection.scanFile(context, arrayOf(publicFile.absolutePath), arrayOf("text/csv"), null)
            if (publicFile.exists() && publicFile.canRead()) {
                return publicFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val publicCandidate = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
    if (publicCandidate.exists() && publicCandidate.canRead()) {
        return publicCandidate
    }

    return fallbackFile
}

fun openFileManagerForFile(context: Context, file: File?) {
    try {
        val downloadIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (downloadIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(downloadIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val uri = Uri.parse(downloadFolder.absolutePath)
        val folderIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (folderIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(folderIntent)
            return
        }
    } catch (_: Exception) {}

    try {
        if (file != null && file.exists()) {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "选择文件管理器查看"))
            return
        }
    } catch (_: Exception) {}

    Toast.makeText(context, "课表模板已保存至「下载」目录，请打开手机自带「文件管理」查看喵！", Toast.LENGTH_LONG).show()
}

fun parseCourseCsv(inputStream: InputStream): String? {
    return parseCourseCsv(decodeCsvBytes(inputStream.readBytes()))
}

fun parseCourseCsv(csvText: String): String? {
    val rows = parseCsvRows(csvText)
    if (rows.isEmpty()) return null

    val header = rows.first().map { it.trim().removePrefix("\uFEFF") }
    val columnIndexes = mapOf(
        "name" to findColumn(header, "课程名称", "课程名", "课程"),
        "day" to findColumn(header, "星期", "周几"),
        "start" to findColumn(header, "开始节数", "开始节次", "开始节", "起始节次"),
        "end" to findColumn(header, "结束节数", "结束节次", "结束节", "终止节次"),
        "teacher" to findColumn(header, "老师", "教师", "授课教师", "导师", "任课教师", "指导教师", "主讲教师", "Teacher", "teacher"),
        "location" to findColumn(header, "地点", "上课地点", "教室", "教学场地", "Location", "location"),
        "weeks" to findColumn(header, "周数", "周次", "上课周次")
    )

    if (columnIndexes.values.any { it < 0 }) return null

    val courses = JSONArray()
    rows.drop(1).forEach { row ->
        val name = row.valueAt(columnIndexes.getValue("name")).trim()
        val day = row.valueAt(columnIndexes.getValue("day")).toIntOrNull()
        val start = row.valueAt(columnIndexes.getValue("start")).toIntOrNull()
        val end = row.valueAt(columnIndexes.getValue("end")).toIntOrNull()
        val weeks = parseWeeks(row.valueAt(columnIndexes.getValue("weeks")))

        if (name.isBlank() || day == null || start == null || end == null || day !in 1..7 || start !in 1..17 || end !in start..17 || weeks.isEmpty()) return@forEach

        val nameHash = name.sumOf { it.code }
        courses.put(JSONObject().apply {
            put("name", name)
            put("location", row.valueAt(columnIndexes.getValue("location")).trim().ifBlank { "未排地点" })
            put("teacher", row.valueAt(columnIndexes.getValue("teacher")).trim())
            put("dayOfWeek", day)
            put("startNode", start)
            put("endNode", end)
            put("weeks", JSONArray(weeks).toString())
            put("colorTheme", courseColors[(nameHash and 0x7fffffff) % courseColors.size])
        })
    }

    return courses.takeIf { it.length() > 0 }?.toString()
}

private fun findColumn(header: List<String>, vararg names: String): Int {
    return header.indexOfFirst { column -> names.any { it == column } }
}

private fun List<String>.valueAt(index: Int): String = getOrNull(index).orEmpty()

private fun addWeekRange(result: MutableSet<Int>, first: Int, last: Int, type: String) {
    if (first > last) return
    (first..last).filter { week ->
        when (type) {
            "单周", "单" -> week % 2 == 1
            "双周", "双" -> week % 2 == 0
            else -> true
        }
    }.filter { it in 1..50 }.forEach(result::add)
}

/**
 * Parses week ranges including values Excel auto-converts to dates (e.g. "1月16日", "2026/1/16", "16-Jan").
 */
private fun parseWeeks(value: String): List<Int> {
    val result = mutableSetOf<Int>()
    val normalized = normalizeExcelDateWeeks(value)

    val pattern = Regex("(\\d+)\\s*(?:-\\s*(\\d+))?\\s*(单周|双周|单|双|周)?")
    pattern.findAll(normalized).forEach { match ->
        val first = match.groupValues[1].toIntOrNull() ?: return@forEach
        val last = match.groupValues[2].toIntOrNull() ?: first
        val type = match.groupValues[3]
        addWeekRange(result, first, last, type)
    }
    return result.sorted()
}

private fun normalizeExcelDateWeeks(value: String): String {
    var text = value.trim()

    // 1. Chinese date format e.g. "1月16日" or "2026年1月16日单" -> "1-16单"
    val cnDateRegex = Regex("(?:\\d{4}\\s*年\\s*)?(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日?")
    text = cnDateRegex.replace(text) { m ->
        val first = m.groupValues[1].toIntOrNull()
        val second = m.groupValues[2].toIntOrNull()
        if (first != null && second != null && first in 1..30 && second in 1..30 && first <= second) {
            "$first-$second"
        } else {
            m.value
        }
    }

    // 2. Slash or ISO year-month-day format e.g. "2026/1/16", "2026-01-16", "1/16" -> "1-16"
    val slashDateRegex = Regex("(?:\\b\\d{4}[/-])?(\\d{1,2})[/-](\\d{1,2})\\b")
    text = slashDateRegex.replace(text) { m ->
        val first = m.groupValues[1].toIntOrNull()
        val second = m.groupValues[2].toIntOrNull()
        if (first != null && second != null && first in 1..30 && second in 1..30 && first <= second) {
            "$first-$second"
        } else {
            m.value
        }
    }

    // 3. English month-day formats Excel emits: "16-Jan" or "Jan-16"
    val dayMonthRegex = Regex("\\b(\\d{1,2})-([A-Za-z]{3,4})\\b")
    text = dayMonthRegex.replace(text) { m ->
        val day = m.groupValues[1].toIntOrNull()
        val month = monthAbbreviations[m.groupValues[2].lowercase()]
        if (month != null && day != null && month in 1..30 && day in 1..30 && month <= day) {
            "$month-$day"
        } else {
            m.value
        }
    }
    val monthDayRegex = Regex("\\b([A-Za-z]{3,4})-(\\d{1,2})\\b")
    text = monthDayRegex.replace(text) { m ->
        val month = monthAbbreviations[m.groupValues[1].lowercase()]
        val day = m.groupValues[2].toIntOrNull()
        if (month != null && day != null && month in 1..30 && day in 1..30 && month <= day) {
            "$month-$day"
        } else {
            m.value
        }
    }

    return text
}

private fun parseCsvRows(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var index = 0

    fun finishField() {
        row += field.toString()
        field.setLength(0)
    }

    while (index < text.length) {
        val character = text[index]
        when {
            character == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                field.append('"')
                index++
            }
            character == '"' -> quoted = !quoted
            character == ',' && !quoted -> finishField()
            (character == '\n' || character == '\r') && !quoted -> {
                finishField()
                if (row.any { it.isNotBlank() }) rows += row.toList()
                row.clear()
                if (character == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
            }
            else -> field.append(character)
        }
        index++
    }

    if (field.isNotEmpty() || row.isNotEmpty()) {
        finishField()
        if (row.any { it.isNotBlank() }) rows += row.toList()
    }
    return rows
}

private fun decodeCsvBytes(bytes: ByteArray): String {
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
        return bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
    }
    return try {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        Charset.forName("GB18030").decode(ByteBuffer.wrap(bytes)).toString()
    }
}
