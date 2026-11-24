package com.example.simpleschedule.domain

import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.local.room.CourseOverride
import com.example.simpleschedule.data.local.room.CourseStatistic
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.data.local.room.ScheduleGroup
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.data.local.room.TimetableGroup
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FullBackupPayload(
    val schedules: List<ScheduleGroup>,
    val timetables: List<TimetableGroup>,
    val timeNodes: List<TimeNode>,
    val courses: List<Course>,
    val courseOverrides: List<CourseOverride>,
    val courseStatistics: List<CourseStatistic>,
    val makeUpRules: List<MakeUpRule>
)

sealed class ParsedImportJson {
    data class FullBackup(val payload: FullBackupPayload) : ParsedImportJson()
    data class CourseList(val jsonArrayString: String) : ParsedImportJson()
}

/**
 * Pure JSON serializer and validating parser for full backups and schedule exports.
 * Validates the entire payload in memory before touching the database.
 */
object BackupJsonSerializer {

    fun serializeFullBackup(payload: FullBackupPayload): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val root = JSONObject().apply {
            put("version", 1)
            put("exportTime", sdf.format(Date()))
            put("appName", "SimpleSchedule")

            put("schedules", JSONArray().apply {
                payload.schedules.forEach { sg ->
                    put(JSONObject().apply {
                        put("id", sg.id)
                        put("name", sg.name)
                        put("timetableId", sg.timetableId)
                        put("startDate", sg.startDate)
                    })
                }
            })

            put("timetables", JSONArray().apply {
                payload.timetables.forEach { tg ->
                    put(JSONObject().apply {
                        put("id", tg.id)
                        put("name", tg.name)
                    })
                }
            })

            put("timeNodes", JSONArray().apply {
                payload.timeNodes.forEach { tn ->
                    put(JSONObject().apply {
                        put("id", tn.id)
                        put("timetableId", tn.timetableId)
                        put("nodeIndex", tn.nodeIndex)
                        put("startTime", tn.startTime)
                        put("endTime", tn.endTime)
                    })
                }
            })

            put("courses", serializeCoursesToArray(payload.courses, includeScheduleId = true))

            put("courseOverrides", JSONArray().apply {
                payload.courseOverrides.forEach { co ->
                    put(JSONObject().apply {
                        put("courseId", co.courseId)
                        put("newDayOfWeek", co.newDayOfWeek)
                        put("newStartNode", co.newStartNode)
                        put("newEndNode", co.newEndNode)
                    })
                }
            })

            put("courseStatistics", JSONArray().apply {
                payload.courseStatistics.forEach { cs ->
                    put(JSONObject().apply {
                        put("courseName", cs.courseName)
                        put("absenceCount", cs.absenceCount)
                        put("calledCount", cs.calledCount)
                    })
                }
            })

            put("makeUpRules", JSONArray().apply {
                payload.makeUpRules.forEach { mr ->
                    put(JSONObject().apply {
                        put("id", mr.id)
                        put("scheduleId", mr.scheduleId)
                        put("sourceDate", mr.sourceDate)
                        put("targetDate", mr.targetDate)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun serializeCourses(courses: List<Course>): String {
        return serializeCoursesToArray(courses, includeScheduleId = false).toString(2)
    }

    private fun serializeCoursesToArray(courses: List<Course>, includeScheduleId: Boolean): JSONArray {
        return JSONArray().apply {
            courses.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    if (includeScheduleId) {
                        put("scheduleId", c.scheduleId)
                    }
                    put("name", c.name)
                    put("location", c.location)
                    put("teacher", c.teacher)
                    put("dayOfWeek", c.dayOfWeek)
                    put("startNode", c.startNode)
                    put("endNode", c.endNode)
                    put("weeks", c.weeks)
                    put("colorTheme", c.colorTheme)
                    put("credits", c.credits ?: "")
                })
            }
        }
    }

    fun parseImportJson(jsonString: String, fallbackScheduleId: String): Result<ParsedImportJson> {
        return runCatching {
            val trimmed = jsonString.trim()
            when {
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    val schedules = parseSchedules(root.optJSONArray("schedules"))
                    val timetables = parseTimetables(root.optJSONArray("timetables"))
                    val timeNodes = parseTimeNodes(root.optJSONArray("timeNodes"))
                    val courses = parseCourses(root.optJSONArray("courses"), fallbackScheduleId)
                    val overrides = parseOverrides(root.optJSONArray("courseOverrides"))
                    val statistics = parseStatistics(root.optJSONArray("courseStatistics"))
                    val makeUpRules = parseMakeUpRules(root.optJSONArray("makeUpRules"))

                    val hasRecognizedSection =
                        root.has("schedules") || root.has("courses") || root.has("timetables")
                    require(hasRecognizedSection) { "备份文件缺少有效的课表结构字段" }
                    require(schedules.isNotEmpty() || courses.isNotEmpty()) {
                        "备份文件不包含任何课表或课程数据，已阻止覆盖"
                    }

                    ParsedImportJson.FullBackup(
                        FullBackupPayload(
                            schedules = schedules,
                            timetables = timetables,
                            timeNodes = timeNodes,
                            courses = courses,
                            courseOverrides = overrides,
                            courseStatistics = statistics,
                            makeUpRules = makeUpRules
                        )
                    )
                }
                trimmed.startsWith("[") -> {
                    val arr = JSONArray(trimmed)
                    require(arr.length() > 0) { "课程列表为空" }
                    ParsedImportJson.CourseList(trimmed)
                }
                else -> throw IllegalArgumentException("无效的 JSON 数据格式")
            }
        }
    }

    private fun parseSchedules(arr: JSONArray?): List<ScheduleGroup> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                require(id.isNotBlank() && name.isNotBlank()) { "课表记录缺少 id 或 name" }
                add(
                    ScheduleGroup(
                        id = id,
                        name = name,
                        timetableId = obj.optString("timetableId", "tt_cjlu").ifBlank { "tt_cjlu" },
                        startDate = obj.optString("startDate", "")
                    )
                )
            }
        }
    }

    private fun parseTimetables(arr: JSONArray?): List<TimetableGroup> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                require(id.isNotBlank() && name.isNotBlank()) { "时间表记录缺少 id 或 name" }
                add(TimetableGroup(id = id, name = name))
            }
        }
    }

    private fun parseTimeNodes(arr: JSONArray?): List<TimeNode> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    TimeNode(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        timetableId = obj.getString("timetableId"),
                        nodeIndex = obj.getInt("nodeIndex"),
                        startTime = obj.getString("startTime"),
                        endTime = obj.getString("endTime")
                    )
                )
            }
        }
    }

    private fun parseCourses(arr: JSONArray?, fallbackScheduleId: String): List<Course> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    Course(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        scheduleId = obj.optString("scheduleId", fallbackScheduleId).ifBlank { fallbackScheduleId },
                        name = obj.optString("name", "未知课程"),
                        location = obj.optString("location", ""),
                        teacher = obj.optString("teacher", ""),
                        dayOfWeek = obj.optInt("dayOfWeek", 1).coerceIn(1, 7),
                        startNode = obj.optInt("startNode", 1).coerceAtLeast(1),
                        endNode = obj.optInt("endNode", 2).coerceAtLeast(1),
                        weeks = obj.optString("weeks", "[]"),
                        colorTheme = obj.optString("colorTheme", "blue").ifBlank { "blue" },
                        credits = obj.optString("credits").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    private fun parseOverrides(arr: JSONArray?): List<CourseOverride> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    CourseOverride(
                        courseId = obj.getString("courseId"),
                        newDayOfWeek = obj.getInt("newDayOfWeek"),
                        newStartNode = obj.getInt("newStartNode"),
                        newEndNode = obj.getInt("newEndNode")
                    )
                )
            }
        }
    }

    private fun parseStatistics(arr: JSONArray?): List<CourseStatistic> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    CourseStatistic(
                        courseName = obj.getString("courseName"),
                        absenceCount = obj.optInt("absenceCount", 0),
                        calledCount = obj.optInt("calledCount", 0)
                    )
                )
            }
        }
    }

    private fun parseMakeUpRules(arr: JSONArray?): List<MakeUpRule> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    MakeUpRule(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        scheduleId = obj.getString("scheduleId"),
                        sourceDate = obj.getString("sourceDate"),
                        targetDate = obj.getString("targetDate")
                    )
                )
            }
        }
    }
}
