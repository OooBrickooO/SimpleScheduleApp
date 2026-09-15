package com.example.simpleschedule.utils

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import kotlin.math.abs

private val courseColors = listOf("blue", "pink", "purple", "slate", "indigo", "rose")

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
        "teacher" to findColumn(header, "老师", "教师", "授课教师"),
        "location" to findColumn(header, "地点", "上课地点"),
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
            put("location", row.valueAt(columnIndexes.getValue("location")).ifBlank { "未排地点" })
            put("teacher", row.valueAt(columnIndexes.getValue("teacher")))
            put("dayOfWeek", day)
            put("startNode", start)
            put("endNode", end)
            put("weeks", JSONArray(weeks).toString())
            put("colorTheme", courseColors[abs(nameHash) % courseColors.size])
        })
    }

    return courses.takeIf { it.length() > 0 }?.toString()
}

private fun findColumn(header: List<String>, vararg names: String): Int {
    return header.indexOfFirst { column -> names.any { it == column } }
}

private fun List<String>.valueAt(index: Int): String = getOrNull(index).orEmpty()

private fun parseWeeks(value: String): List<Int> {
    val result = mutableSetOf<Int>()
    val pattern = Regex("(\\d+)\\s*(?:-\\s*(\\d+))?\\s*(单周|双周|单|双|周)?")
    pattern.findAll(value).forEach { match ->
        val first = match.groupValues[1].toIntOrNull() ?: return@forEach
        val last = match.groupValues[2].toIntOrNull() ?: first
        val type = match.groupValues[3]
        if (first > last) return@forEach
        (first..last).filter { week ->
            when (type) {
                "单周", "单" -> week % 2 == 1
                "双周", "双" -> week % 2 == 0
                else -> true
            }
        }.filter { it in 1..50 }.forEach(result::add)
    }
    return result.sorted()
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
