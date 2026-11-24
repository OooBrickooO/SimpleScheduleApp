package com.example.simpleschedule.domain.parsers

import jxl.Workbook
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

private val qgCourseColors = listOf("blue", "pink", "purple", "slate", "indigo", "rose")

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

                    val lines = cellContent.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    var i = 0
                    while (i < lines.size) {
                        val name = lines[i]
                        var teacher = ""
                        var location = "未排地点"

                        var timeIndex = -1
                        for (j in i + 1 until lines.size) {
                            if (lines[j].contains("周") && Regex("\\d").containsMatchIn(lines[j])) {
                                timeIndex = j
                                break
                            }
                        }

                        if (timeIndex != -1) {
                            val timeStr = lines[timeIndex]
                            if (timeIndex > i + 1) teacher = lines[timeIndex - 1]
                            if (timeIndex + 1 < lines.size && !lines[timeIndex + 1].contains("周")) {
                                location = lines[timeIndex + 1]
                                i = timeIndex + 2
                            } else {
                                i = timeIndex + 1
                            }

                            var startNode = 1
                            var endNode = 2
                            val weeksArr = mutableListOf<Int>()

                            val weekStr = timeStr.substringBefore("(周)").substringBefore("([")
                            val parts = weekStr.split(",")
                            for (p in parts) {
                                val rangeMatch = Regex("(\\d+)-(\\d+)").find(p)
                                if (rangeMatch != null) {
                                    val (s, e) = rangeMatch.destructured
                                    val startW = s.toIntOrNull() ?: continue
                                    val endW = e.toIntOrNull() ?: continue
                                    if (startW <= endW) {
                                        for (w in startW..endW) weeksArr.add(w)
                                    }
                                } else {
                                    val singleMatch = Regex("(\\d+)").find(p)
                                    singleMatch?.groupValues?.get(1)?.toIntOrNull()?.let { weeksArr.add(it) }
                                }
                            }

                            val sectionMatch = Regex("\\[(\\d+)-(\\d+)").find(timeStr)
                            if (sectionMatch != null) {
                                val (s, e) = sectionMatch.destructured
                                startNode = s.toIntOrNull() ?: 1
                                endNode = e.toIntOrNull() ?: 2
                            }

                            if (weeksArr.isNotEmpty() && name.isNotBlank()) {
                                val nameHash = name.sumOf { it.code }
                                val colorTheme = qgCourseColors[(nameHash and 0x7fffffff) % qgCourseColors.size]

                                courses.put(JSONObject().apply {
                                    put("name", name)
                                    put("location", location)
                                    put("teacher", teacher)
                                    put("dayOfWeek", col)
                                    put("startNode", startNode)
                                    put("endNode", endNode)
                                    put("weeks", JSONArray(weeksArr).toString())
                                    put("colorTheme", colorTheme)
                                })
                            }
                        } else {
                            i++
                        }
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
