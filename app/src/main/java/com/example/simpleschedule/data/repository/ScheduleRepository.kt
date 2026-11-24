package com.example.simpleschedule.data.repository

import androidx.room.withTransaction
import com.example.simpleschedule.data.local.room.AppDao
import com.example.simpleschedule.data.local.room.AppDatabase
import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.local.room.CourseOverride
import com.example.simpleschedule.data.local.room.CourseStatistic
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.data.local.room.ScheduleGroup
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.data.local.room.TimetableGroup
import com.example.simpleschedule.domain.FullBackupPayload
import com.example.simpleschedule.domain.ScheduleResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import java.util.UUID

enum class RestoreMode {
    FULL,
    MERGE
}

class ScheduleRepository(private val database: AppDatabase) {
    val dao: AppDao = database.appDao()

    companion object {
        const val DEFAULT_TIMETABLE_ID = "tt_cjlu"
        val BUILT_IN_TIMETABLE_IDS = setOf(
            "tt_cjlu",
            "tt_lzjtu",
            "tt_zjhu_summer",
            "tt_zjhu_winter"
        )
    }

    fun observeAllScheduleGroups(): Flow<List<ScheduleGroup>> = dao.getAllScheduleGroups()
    fun observeAllTimetableGroups(): Flow<List<TimetableGroup>> = dao.getAllTimetableGroups()
    fun observeScheduleGroup(id: String): Flow<ScheduleGroup?> = dao.getScheduleGroupFlow(id)
    fun observeTimeNodes(timetableId: String): Flow<List<TimeNode>> = dao.getTimeNodesFlow(timetableId)
    fun observeCoursesBySchedule(scheduleId: String): Flow<List<Course>> = dao.getCoursesBySchedule(scheduleId)
    fun observeAllOverrides(): Flow<List<CourseOverride>> = dao.getAllOverrides()
    fun observeMakeUpRulesBySchedule(scheduleId: String): Flow<List<MakeUpRule>> = dao.getMakeUpRulesBySchedule(scheduleId)
    fun observeCourseStatistic(courseName: String): Flow<CourseStatistic?> = dao.getStatisticFlow(courseName)

    suspend fun getTimeNodes(timetableId: String): List<TimeNode> = dao.getTimeNodes(timetableId)
    suspend fun getCourseStatisticDirect(courseName: String): CourseStatistic? = dao.getStatisticDirect(courseName)

    suspend fun ensureBuiltInTimetablesExist() {
        val existingIds = dao.getAllTimetableGroupsDirect().map { it.id }.toSet()

        if ("tt_lzjtu" !in existingIds) {
            dao.insertTimetableGroup(TimetableGroup("tt_lzjtu", "兰州交通大学"))
            dao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 1, startTime = "08:00", endTime = "08:50"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 2, startTime = "09:00", endTime = "09:50"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 3, startTime = "10:10", endTime = "11:00"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 4, startTime = "11:10", endTime = "12:00"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 5, startTime = "14:30", endTime = "15:20"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 6, startTime = "15:30", endTime = "16:20"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 7, startTime = "16:40", endTime = "17:30"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 8, startTime = "17:40", endTime = "18:30"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 9, startTime = "19:30", endTime = "20:20"),
                TimeNode(timetableId = "tt_lzjtu", nodeIndex = 10, startTime = "20:30", endTime = "21:20")
            ))
        }

        if ("tt_cjlu" !in existingIds) {
            dao.insertTimetableGroup(TimetableGroup("tt_cjlu", "中国计量大学"))
            dao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 1, startTime = "08:00", endTime = "08:45"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 2, startTime = "08:50", endTime = "09:35"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 3, startTime = "09:55", endTime = "10:40"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 4, startTime = "10:45", endTime = "11:30"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 5, startTime = "11:35", endTime = "12:20"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 6, startTime = "13:30", endTime = "14:15"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 7, startTime = "14:20", endTime = "15:05"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 8, startTime = "15:15", endTime = "16:00"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 9, startTime = "16:05", endTime = "16:50"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 10, startTime = "18:00", endTime = "18:45"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 11, startTime = "18:50", endTime = "19:35"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 12, startTime = "19:40", endTime = "20:25"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 13, startTime = "20:35", endTime = "21:05"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 14, startTime = "21:10", endTime = "21:40"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 15, startTime = "21:45", endTime = "22:15"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 16, startTime = "22:20", endTime = "22:50"),
                TimeNode(timetableId = "tt_cjlu", nodeIndex = 17, startTime = "22:55", endTime = "23:25")
            ))
        }

        if ("tt_zjhu_summer" !in existingIds) {
            dao.insertTimetableGroup(TimetableGroup("tt_zjhu_summer", "湖州师范大学(夏)"))
            dao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 1, startTime = "08:00", endTime = "08:40"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 2, startTime = "08:50", endTime = "09:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 3, startTime = "09:50", endTime = "10:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 4, startTime = "10:40", endTime = "11:20"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 5, startTime = "11:30", endTime = "12:10"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 6, startTime = "14:00", endTime = "14:40"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 7, startTime = "14:50", endTime = "15:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 8, startTime = "15:50", endTime = "16:30"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 9, startTime = "16:40", endTime = "17:20"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 10, startTime = "18:30", endTime = "19:10"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 11, startTime = "19:20", endTime = "20:00"),
                TimeNode(timetableId = "tt_zjhu_summer", nodeIndex = 12, startTime = "20:10", endTime = "20:50")
            ))
        }

        if ("tt_zjhu_winter" !in existingIds) {
            dao.insertTimetableGroup(TimetableGroup("tt_zjhu_winter", "湖州师范大学(冬)"))
            dao.insertTimeNodes(listOf(
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 1, startTime = "08:00", endTime = "08:40"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 2, startTime = "08:50", endTime = "09:30"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 3, startTime = "09:50", endTime = "10:30"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 4, startTime = "10:40", endTime = "11:20"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 5, startTime = "11:30", endTime = "12:10"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 6, startTime = "13:30", endTime = "14:10"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 7, startTime = "14:20", endTime = "15:00"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 8, startTime = "15:20", endTime = "16:00"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 9, startTime = "16:10", endTime = "16:50"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 10, startTime = "18:00", endTime = "18:40"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 11, startTime = "18:50", endTime = "19:30"),
                TimeNode(timetableId = "tt_zjhu_winter", nodeIndex = 12, startTime = "19:40", endTime = "20:20")
            ))
        }
    }

    suspend fun initDefaultSchedule(): ScheduleGroup {
        val weekOneMonday = ScheduleResolver.calculateStartDateForWeek(1)
        val defaultGroup = ScheduleGroup("default_id", "空白课表", DEFAULT_TIMETABLE_ID, weekOneMonday)
        dao.insertScheduleGroup(defaultGroup)
        return defaultGroup
    }

    suspend fun getAllScheduleGroupsDirect(): List<ScheduleGroup> = dao.getAllScheduleGroupsDirect()
    suspend fun getScheduleGroupDirect(id: String): ScheduleGroup? = dao.getScheduleGroupFlow(id).firstOrNull()

    suspend fun createSchedule(name: String): ScheduleGroup {
        val group = ScheduleGroup(
            id = UUID.randomUUID().toString(),
            name = name,
            timetableId = DEFAULT_TIMETABLE_ID,
            startDate = ScheduleResolver.calculateStartDateForWeek(1)
        )
        dao.insertScheduleGroup(group)
        return group
    }

    suspend fun updateSchedule(group: ScheduleGroup) {
        dao.updateScheduleGroup(group)
    }

    suspend fun deleteScheduleAtomic(id: String): Boolean {
        val groups = dao.getAllScheduleGroupsDirect()
        if (groups.size <= 1) return false
        database.withTransaction {
            dao.deleteOverridesBySchedule(id)
            dao.deleteCoursesBySchedule(id)
            dao.deleteMakeUpRulesBySchedule(id)
            dao.deleteScheduleGroup(id)
        }
        return true
    }

    suspend fun saveTimetableAtomic(id: String?, name: String, nodes: List<TimeNode>) {
        val tid = id ?: UUID.randomUUID().toString()
        database.withTransaction {
            dao.insertTimetableGroup(TimetableGroup(tid, name))
            dao.deleteTimeNodes(tid)
            dao.insertTimeNodes(nodes.map { it.copy(timetableId = tid, id = UUID.randomUUID().toString()) })
        }
    }

    suspend fun deleteTimetableAtomic(id: String): Boolean {
        if (id in BUILT_IN_TIMETABLE_IDS) return false
        database.withTransaction {
            dao.reassignSchedulesTimetable(deletedTimetableId = id, fallbackTimetableId = DEFAULT_TIMETABLE_ID)
            dao.deleteTimeNodes(id)
            dao.deleteTimetableGroup(id)
        }
        return true
    }

    suspend fun upsertCourse(course: Course, updateExistingOverride: Boolean = false) {
        database.withTransaction {
            dao.insertCourse(course)
            if (updateExistingOverride) {
                val existingOverride = dao.getAllOverridesDirect().find { it.courseId == course.id }
                if (existingOverride != null) {
                    dao.insertOverride(
                        existingOverride.copy(
                            newDayOfWeek = course.dayOfWeek,
                            newStartNode = course.startNode,
                            newEndNode = course.endNode
                        )
                    )
                }
            }
        }
    }

    suspend fun deleteCourseAtomic(courseId: String) {
        database.withTransaction {
            dao.deleteCourse(courseId)
            dao.deleteOverrideByCourse(courseId)
        }
    }

    suspend fun upsertOverride(override: CourseOverride) {
        dao.insertOverride(override)
    }

    suspend fun getAllOverridesDirect(): List<CourseOverride> = dao.getAllOverridesDirect()

    suspend fun addMakeUpRule(scheduleId: String, sourceDate: String, targetDate: String) {
        dao.insertMakeUpRule(MakeUpRule(scheduleId = scheduleId, sourceDate = sourceDate, targetDate = targetDate))
    }

    suspend fun deleteMakeUpRule(id: String) {
        dao.deleteMakeUpRule(id)
    }

    suspend fun incrementAbsenceCount(courseName: String) {
        database.withTransaction {
            val stats = dao.getStatisticDirect(courseName) ?: CourseStatistic(courseName, 0, 0)
            dao.insertStatistic(stats.copy(absenceCount = stats.absenceCount + 1))
        }
    }

    suspend fun incrementCalledCount(courseName: String) {
        database.withTransaction {
            val stats = dao.getStatisticDirect(courseName) ?: CourseStatistic(courseName, 0, 0)
            dao.insertStatistic(stats.copy(calledCount = stats.calledCount + 1))
        }
    }

    suspend fun updateCourseStatistic(courseName: String, absence: Int, called: Int) {
        dao.insertStatistic(CourseStatistic(courseName, absence, called))
    }

    suspend fun importCoursesFromJson(
        scheduleId: String,
        jsonString: String,
        replaceCurrent: Boolean,
        currentTotalWeeks: Int
    ): Result<Int> = runCatching {
        val array = JSONArray(jsonString)
        val parsedCourses = mutableListOf<Course>()
        var maxWeekInImport = currentTotalWeeks

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val weeksStr = obj.optString("weeks", "[]")
            ScheduleResolver.parseWeeks(weeksStr).forEach { w ->
                if (w > maxWeekInImport) maxWeekInImport = w
            }
            parsedCourses.add(
                Course(
                    id = UUID.randomUUID().toString(),
                    scheduleId = scheduleId,
                    name = obj.optString("name", "未知课程"),
                    location = obj.optString("location", "未排地点"),
                    teacher = obj.optString("teacher", ""),
                    dayOfWeek = obj.optInt("dayOfWeek", 1),
                    startNode = obj.optInt("startNode", 1),
                    endNode = obj.optInt("endNode", 2),
                    weeks = weeksStr,
                    colorTheme = obj.optString("colorTheme", "blue"),
                    credits = obj.optString("credits").takeIf { it.isNotBlank() }
                )
            )
        }

        database.withTransaction {
            if (replaceCurrent) {
                dao.deleteOverridesBySchedule(scheduleId)
                dao.deleteCoursesBySchedule(scheduleId)
            }
            if (parsedCourses.isNotEmpty()) {
                dao.insertAllCourses(parsedCourses)
            }
        }
        maxWeekInImport
    }

    suspend fun exportFullBackupPayload(): FullBackupPayload {
        return FullBackupPayload(
            schedules = dao.getAllScheduleGroupsDirect(),
            timetables = dao.getAllTimetableGroupsDirect(),
            timeNodes = dao.getAllTimeNodesDirect(),
            courses = dao.getAllCoursesDirect(),
            courseOverrides = dao.getAllOverridesDirect(),
            courseStatistics = dao.getAllStatisticsDirect(),
            makeUpRules = dao.getAllMakeUpRulesDirect()
        )
    }

    suspend fun exportScheduleCourses(scheduleId: String): List<Course> {
        return dao.getAllCoursesDirect().filter { it.scheduleId == scheduleId }
    }

    suspend fun restoreFullBackupAtomic(payload: FullBackupPayload, mode: RestoreMode): String? {
        return database.withTransaction {
            if (mode == RestoreMode.FULL) {
                dao.deleteAllCourses()
                dao.deleteAllScheduleGroups()
                dao.deleteAllTimetableGroups()
                dao.deleteAllTimeNodes()
                dao.deleteAllOverrides()
                dao.deleteAllStatistics()
                dao.deleteAllMakeUpRules()
            }

            if (payload.timetables.isNotEmpty()) {
                dao.insertAllTimetableGroups(payload.timetables)
            }
            if (payload.timeNodes.isNotEmpty()) {
                dao.insertTimeNodes(payload.timeNodes)
            }
            if (payload.schedules.isNotEmpty()) {
                dao.insertAllScheduleGroups(payload.schedules)
            }
            if (payload.courses.isNotEmpty()) {
                dao.insertAllCourses(payload.courses)
            }
            if (payload.courseOverrides.isNotEmpty()) {
                dao.insertAllOverrides(payload.courseOverrides)
            }
            if (payload.courseStatistics.isNotEmpty()) {
                dao.insertAllStatistics(payload.courseStatistics)
            }
            if (payload.makeUpRules.isNotEmpty()) {
                dao.insertAllMakeUpRules(payload.makeUpRules)
            }

            ensureBuiltInTimetablesExist()
            val groups = dao.getAllScheduleGroupsDirect()
            if (groups.isEmpty()) {
                initDefaultSchedule().id
            } else {
                groups.first().id
            }
        }
    }
}
