package com.example.simpleschedule

import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.local.room.CourseOverride
import com.example.simpleschedule.data.local.room.CourseStatistic
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.data.local.room.ScheduleGroup
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.data.local.room.TimetableGroup
import com.example.simpleschedule.domain.BackupJsonSerializer
import com.example.simpleschedule.domain.FullBackupPayload
import com.example.simpleschedule.domain.ParsedImportJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonSerializerTest {

    @Test
    fun roundTripFullBackupPreservesAllEntities() {
        val payload = FullBackupPayload(
            schedules = listOf(ScheduleGroup("s1", "大二上", "tt1", "2026-03-02")),
            timetables = listOf(TimetableGroup("tt1", "自定义时间表")),
            timeNodes = listOf(TimeNode("tn1", "tt1", 1, "08:00", "08:45")),
            courses = listOf(
                Course("c1", "s1", "编译原理", "计301", "王教授", 2, 1, 2, "[1,2,3]", "indigo", "3.0")
            ),
            courseOverrides = listOf(CourseOverride("c1", 4, 3, 4)),
            courseStatistics = listOf(CourseStatistic("编译原理", 1, 2)),
            makeUpRules = listOf(MakeUpRule("m1", "s1", "2026-05-01", "2026-04-26"))
        )

        val json = BackupJsonSerializer.serializeFullBackup(payload)
        val parsed = BackupJsonSerializer.parseImportJson(json, fallbackScheduleId = "s1")

        assertTrue(parsed.isSuccess)
        val full = parsed.getOrThrow() as ParsedImportJson.FullBackup
        assertEquals(payload.schedules, full.payload.schedules)
        assertEquals(payload.timetables, full.payload.timetables)
        assertEquals(payload.timeNodes, full.payload.timeNodes)
        assertEquals(payload.courses, full.payload.courses)
        assertEquals(payload.courseOverrides, full.payload.courseOverrides)
        assertEquals(payload.courseStatistics, full.payload.courseStatistics)
        assertEquals(payload.makeUpRules, full.payload.makeUpRules)
    }

    @Test
    fun rejectsEmptyOrInvalidObjectBeforeDatabaseWipe() {
        val emptyObj = BackupJsonSerializer.parseImportJson("{}", "s1")
        assertTrue(emptyObj.isFailure)

        val emptyArraysObj = BackupJsonSerializer.parseImportJson("""{"schedules":[],"courses":[]}""", "s1")
        assertTrue(emptyArraysObj.isFailure)

        val malformed = BackupJsonSerializer.parseImportJson("not json", "s1")
        assertTrue(malformed.isFailure)
    }

    @Test
    fun parsesCourseListJsonAndSerializesSingleSchedule() {
        val course = Course("c1", "s1", "高数", "A101", "李老师", 1, 1, 2, "[1,2]", "blue", null)
        val json = BackupJsonSerializer.serializeCourses(listOf(course))
        val parsed = BackupJsonSerializer.parseImportJson(json, "s1")
        assertTrue(parsed.isSuccess)
        assertTrue(parsed.getOrThrow() is ParsedImportJson.CourseList)
    }
}
