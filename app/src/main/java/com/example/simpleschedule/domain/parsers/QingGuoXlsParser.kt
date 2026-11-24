package com.example.simpleschedule.domain.parsers

import jxl.Workbook
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

private val qgCourseColors = listOf("blue", "pink", "purple", "slate", "indigo", "rose")

internal data class ParsedQingGuoCourse(
    val name: String,
    val location: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startNode: Int,
    val endNode: Int,
    val weeks: List<Int>,
    val colorTheme: String
)

private val qgTeacherPrefixRegex = Regex("^(?:教师|老师|导师|任课教师|授课教师|指导教师|主讲教师)\\s*[:：]\\s*(.+)$")
private val qgLocationPrefixRegex = Regex("^(?:上课地点|教学场地|授课地点|地点|教室|场地)\\s*[:：]\\s*(.+)$")
private val qgMetadataNoiseRegex = Regex("^(?:教学班|班级|组成|考核|考核方式|课程性质|性质|总学时|学时|周学时|理论|实践|人数|备注|选课|课程号|课号|编号)\\s*[:：]?.*")
private val qgLocationKeywordRegex = Regex("(?:校区|教学楼|实验楼|综合楼|楼|教室|实验室|实训室|机房|操场|体育馆|运动场|报告厅|中心|馆|厅|室|场|[A-Za-z]-?\\d{2,4}|\\d{3,4})")
private val qgTitleBracketRegex = Regex("[（(\\[][^）)\\]]{1,12}[）)\\]]")
private val qgCnTeacherRegex = Regex("^[\\u4e00-\\u9fa5]{2,4}(?:[·•][\\u4e00-\\u9fa5]{1,4})?(?:\\s*[,，、/;；\\s]\\s*[\\u4e00-\\u9fa5]{2,4}(?:[·•][\\u4e00-\\u9fa5]{1,4})?)*$")
private val qgEnTeacherRegex = Regex("^[A-Za-z][A-Za-z.\\s'-]{2,39}$")
private val qgTimeLineRegex = Regex("周.*\\d|\\d.*周")

private fun isLikelyQgLocation(line: String): Boolean {
    return qgLocationPrefixRegex.matches(line) || qgLocationKeywordRegex.containsMatchIn(line)
}

private fun isLikelyQgTeacher(line: String): Boolean {
    if (qgTeacherPrefixRegex.matches(line)) return true
    if (qgMetadataNoiseRegex.matches(line)) return false
    val stripped = line
        .replace(qgTitleBracketRegex, "")
        .trim()
    if (stripped.length < 2 || stripped.length > 40) return false
    if (Regex("\\d").containsMatchIn(stripped)) return false
    if (Regex("(周|节|楼|校区|教室|实验室|机房|操场|体育馆|学分|学时|必修|选修|考查|考试)").containsMatchIn(stripped)) {
        return false
    }
    return qgCnTeacherRegex.matches(stripped) ||
        (qgEnTeacherRegex.matches(stripped) && stripped.count { it.isLetter() } >= 2)
}

private fun parseQingGuoWeeks(timeStr: String): List<Int> {
    val weekStr = timeStr.substringBefore("(周)").substringBefore("([")
    val globalOdd = timeStr.contains("单周") || Regex("\\(\\s*单\\s*\\)").containsMatchIn(timeStr)
    val globalEven = timeStr.contains("双周") || Regex("\\(\\s*双\\s*\\)").containsMatchIn(timeStr)
    val result = mutableListOf<Int>()
    for (part in weekStr.split(Regex("[,，、]+"))) {
        val p = part.trim()
        if (p.isEmpty()) continue
        val isOdd = p.contains("单") || (globalOdd && !p.contains("双"))
        val isEven = p.contains("双") || (globalEven && !p.contains("单"))
        val rangeMatch = Regex("(\\d+)\\s*[-~～]\\s*(\\d+)").find(p)
        if (rangeMatch != null) {
            val (s, e) = rangeMatch.destructured
            val startW = s.toIntOrNull() ?: continue
            val endW = e.toIntOrNull() ?: continue
            if (startW <= endW) {
                for (w in startW..endW) {
                    if (isOdd && w % 2 == 0) continue
                    if (isEven && w % 2 != 0) continue
                    result.add(w)
                }
            }
        } else {
            val singleMatch = Regex("(\\d+)").find(p)
            val wNum = singleMatch?.groupValues?.get(1)?.toIntOrNull() ?: continue
            if (isOdd && wNum % 2 == 0) continue
            if (isEven && wNum % 2 != 0) continue
            result.add(wNum)
        }
    }
    return result
}

internal fun parseQingGuoCellLines(rawLines: List<String>, col: Int): List<ParsedQingGuoCourse> {
    val lines = rawLines.map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyList()

    val timeIndices = lines.indices.filter { idx ->
        lines[idx].contains("周") && qgTimeLineRegex.containsMatchIn(lines[idx])
    }
    if (timeIndices.isEmpty()) return emptyList()

    val results = mutableListOf<ParsedQingGuoCourse>()
    var blockStart = 0

    for (k in timeIndices.indices) {
        val timeIdx = timeIndices[k]
        if (blockStart >= timeIdx) {
            blockStart = timeIdx + 1
            continue
        }

        val name = lines[blockStart]
        val preLines = if (timeIdx > blockStart + 1) lines.subList(blockStart + 1, timeIdx) else emptyList()

        var teacher = ""
        var location = ""

        preLines.forEach { line ->
            val tp = qgTeacherPrefixRegex.find(line)
            val lp = qgLocationPrefixRegex.find(line)
            when {
                tp != null && teacher.isBlank() -> teacher = tp.groupValues[1].trim()
                lp != null && location.isBlank() -> location = lp.groupValues[1].trim()
                teacher.isBlank() && isLikelyQgTeacher(line) -> teacher = line
                location.isBlank() && isLikelyQgLocation(line) -> location = line
                teacher.isBlank() && !isLikelyQgLocation(line) && !qgMetadataNoiseRegex.matches(line) -> teacher = line
            }
        }

        val nextTimeIdx = timeIndices.getOrNull(k + 1)
        // Reserve at least the next course's title line (and its pre-time teacher if obvious) before nextTimeIdx
        val postLimitExclusive = if (nextTimeIdx == null) {
            lines.size
        } else {
            maxOf(timeIdx + 1, nextTimeIdx - 1)
        }

        var consumedPostEnd = timeIdx + 1
        for (j in (timeIdx + 1) until postLimitExclusive) {
            if (location.isNotBlank() && teacher.isNotBlank()) break
            val line = lines[j]
            val tp = qgTeacherPrefixRegex.find(line)
            val lp = qgLocationPrefixRegex.find(line)
            when {
                lp != null && location.isBlank() -> {
                    location = lp.groupValues[1].trim()
                    consumedPostEnd = j + 1
                }
                tp != null && teacher.isBlank() -> {
                    teacher = tp.groupValues[1].trim()
                    consumedPostEnd = j + 1
                }
                location.isBlank() && isLikelyQgLocation(line) -> {
                    location = line
                    consumedPostEnd = j + 1
                }
                teacher.isBlank() && (nextTimeIdx == null || j + 1 < postLimitExclusive) && isLikelyQgTeacher(line) -> {
                    teacher = line
                    consumedPostEnd = j + 1
                }
                location.isBlank() && !qgMetadataNoiseRegex.matches(line) && !isLikelyQgTeacher(line) -> {
                    location = line
                    consumedPostEnd = j + 1
                }
                else -> break
            }
        }
        blockStart = consumedPostEnd

        val timeStr = lines[timeIdx]
        val weeksArr = parseQingGuoWeeks(timeStr)
        var startNode = 1
        var endNode = 2
        val sectionMatch = Regex("\\[(\\d+)-(\\d+)").find(timeStr)
        if (sectionMatch != null) {
            val (s, e) = sectionMatch.destructured
            startNode = s.toIntOrNull() ?: 1
            endNode = e.toIntOrNull() ?: 2
        }

        if (weeksArr.isNotEmpty() && name.isNotBlank()) {
            val cleanTeacher = teacher.replace(Regex("^(?:教师|老师|导师|任课教师|授课教师)\\s*[:：]\\s*"), "").trim()
            val cleanLocation = location.replace(Regex("^(?:上课地点|教学场地|授课地点|地点|教室|场地)\\s*[:：]\\s*"), "")
                .trim()
                .ifBlank { "未排地点" }
            val nameHash = name.sumOf { it.code }
            val colorTheme = qgCourseColors[(nameHash and 0x7fffffff) % qgCourseColors.size]

            results.add(
                ParsedQingGuoCourse(
                    name = name,
                    location = cleanLocation,
                    teacher = cleanTeacher,
                    dayOfWeek = col,
                    startNode = startNode,
                    endNode = endNode,
                    weeks = weeksArr,
                    colorTheme = colorTheme
                )
            )
        }
    }

    return results
}

/**
 * Parses QingGuo academic system exported .xls files into SimpleSchedule JSON array string.
 */
fun parseQingGuoXls(inputStream: InputStream): String? {
    var workbook: Workbook? = null
    return try {
        inputStream.use { stream ->
            workbook = Workbook.getWorkbook(stream)
            val sheet = workbook!!.getSheet(0)
            val courses = JSONArray()

            for (row in 0 until sheet.rows) {
                val timeHeader = sheet.getCell(0, row).contents ?: ""
                if (timeHeader.isBlank() && row > 0) continue

                for (col in 1..7) {
                    if (col >= sheet.columns) break
                    val cellContent = sheet.getCell(col, row).contents ?: continue
                    if (cellContent.isBlank() || cellContent.length < 5) continue

                    val parsed = parseQingGuoCellLines(cellContent.split("\n"), col)
                    parsed.forEach { course ->
                        courses.put(JSONObject().apply {
                            put("name", course.name)
                            put("location", course.location)
                            put("teacher", course.teacher)
                            put("dayOfWeek", course.dayOfWeek)
                            put("startNode", course.startNode)
                            put("endNode", course.endNode)
                            put("weeks", JSONArray(course.weeks).toString())
                            put("colorTheme", course.colorTheme)
                        })
                    }
                }
            }
            courses.toString()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        try {
            workbook?.close()
        } catch (_: Exception) {
        }
    }
}
