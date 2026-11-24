package com.example.simpleschedule

import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.local.room.CourseOverride
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.domain.ScheduleResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleResolverTest {

    private val timeNodes = mapOf(
        1 to TimeNode(timetableId = "tt", nodeIndex = 1, startTime = "08:00", endTime = "08:45"),
        2 to TimeNode(timetableId = "tt", nodeIndex = 2, startTime = "08:50", endTime = "09:35"),
        3 to TimeNode(timetableId = "tt", nodeIndex = 3, startTime = "09:55", endTime = "10:40"),
        4 to TimeNode(timetableId = "tt", nodeIndex = 4, startTime = "10:45", endTime = "11:30")
    )

    private fun calAt(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun calculateWeekAlignsMidweekStartDateToMonday() {
        // 2026-03-04 is Wednesday; its Monday is 2026-03-02
        assertEquals(1, ScheduleResolver.calculateWeekFromStartDate("2026-03-04", calAt(2026, 3, 2)))
        assertEquals(1, ScheduleResolver.calculateWeekFromStartDate("2026-03-04", calAt(2026, 3, 8)))
        assertEquals(2, ScheduleResolver.calculateWeekFromStartDate("2026-03-04", calAt(2026, 3, 9)))
        assertEquals(1, ScheduleResolver.calculateWeekFromStartDate("2026-03-04", calAt(2026, 2, 20)))
    }

    @Test
    fun resolveNextUpcomingCourseHandlesCrossWeekOddEvenCorrectly() {
        // Start date: 2026-03-02 (Monday of Week 1)
        // Current time: Friday 2026-03-06 20:00 (Week 1)
        // Next Monday 2026-03-09 is Week 2!
        // Course A is only Week 1 Monday (must NOT trigger next Monday)
        // Course B is Week 2 Monday (MUST trigger next Monday)
        val courses = listOf(
            Course("c1", "s1", "仅第1周课", "A101", "T1", dayOfWeek = 1, startNode = 1, endNode = 2, weeks = "[1]", colorTheme = "blue"),
            Course("c2", "s1", "第2周高数", "B202", "T2", dayOfWeek = 1, startNode = 3, endNode = 4, weeks = "[2]", colorTheme = "pink")
        )
        val nowMillis = calAt(2026, 3, 6, 20, 0).timeInMillis

        val next = ScheduleResolver.resolveNextUpcomingCourse(
            nowMillis = nowMillis,
            advanceMins = 20,
            startDateStr = "2026-03-02",
            rawCourses = courses,
            overrideMap = emptyMap(),
            timeNodeMap = timeNodes,
            makeUpRules = emptyList()
        )

        assertNotNull(next)
        assertEquals("第2周高数", next!!.course.name)
        assertEquals("09:55-11:30", next.timeRange)
        val expectedStart = calAt(2026, 3, 9, 9, 55).timeInMillis
        assertEquals(expectedStart, next.classStartMillis)
        assertEquals(expectedStart - 20 * 60_000L, next.triggerMillis)
    }

    @Test
    fun resolveNextUpcomingCourseDoesNotSkipShortBreakBackToBackClass() {
        // Monday 2026-03-02 09:40 (between node 2 end 09:35 and node 3 start 09:55, only 15 mins left)
        // advanceMins = 20, so rawTrigger (09:35) <= now (09:40), should clamp to now + 2s instead of skipping!
        val courses = listOf(
            Course("c1", "s1", "第一节", "A101", "T1", dayOfWeek = 1, startNode = 1, endNode = 2, weeks = "[1]", colorTheme = "blue"),
            Course("c2", "s1", "第二节连堂", "A102", "T2", dayOfWeek = 1, startNode = 3, endNode = 4, weeks = "[1]", colorTheme = "indigo")
        )
        val nowMillis = calAt(2026, 3, 2, 9, 40).timeInMillis

        val next = ScheduleResolver.resolveNextUpcomingCourse(
            nowMillis = nowMillis,
            advanceMins = 20,
            startDateStr = "2026-03-02",
            rawCourses = courses,
            overrideMap = emptyMap(),
            timeNodeMap = timeNodes,
            makeUpRules = emptyList()
        )

        assertNotNull(next)
        assertEquals("第二节连堂", next!!.course.name)
        assertEquals(nowMillis + 2_000L, next.triggerMillis)
    }

    @Test
    fun resolveDayCoursesAppliesMakeUpRulesAndOverrides() {
        val courses = listOf(
            Course("c1", "s1", "周五实验", "Lab", "T", dayOfWeek = 5, startNode = 1, endNode = 2, weeks = "[1]", colorTheme = "blue"),
            Course("c2", "s1", "被拖拽课", "R1", "T", dayOfWeek = 1, startNode = 1, endNode = 2, weeks = "[1]", colorTheme = "pink")
        )
        val overrides = mapOf("c2" to CourseOverride("c2", newDayOfWeek = 3, newStartNode = 3, newEndNode = 4))
        val display = ScheduleResolver.applyOverrides(courses, overrides)

        // Move Friday 2026-03-06 -> Sunday 2026-03-08
        val rules = listOf(MakeUpRule("r1", "s1", sourceDate = "2026-03-06", targetDate = "2026-03-08"))

        // Friday becomes holiday (empty)
        val friday = ScheduleResolver.resolveDayCourses("2026-03-06", 1, 5, "2026-03-02", display, rules)
        assertTrue(friday.courses.isEmpty())
        assertNotNull(friday.holidayRule)

        // Sunday shows Friday's course mapped to day 7
        val sunday = ScheduleResolver.resolveDayCourses("2026-03-08", 1, 7, "2026-03-02", display, rules)
        assertEquals(1, sunday.courses.size)
        assertEquals("周五实验", sunday.courses.first().course.name)
        assertEquals(7, sunday.courses.first().displayDay)

        // Wednesday shows overridden c2
        val wednesday = ScheduleResolver.resolveDayCourses("2026-03-04", 1, 3, "2026-03-02", display, rules)
        assertEquals(1, wednesday.courses.size)
        assertEquals(3, wednesday.courses.first().displayStartNode)
    }

    @Test
    fun resolveNextWidgetUpdateMillisReturnsMidnightEvenOnHoliday() {
        val courses = listOf(
            Course("c1", "s1", "周一课", "R", "T", dayOfWeek = 1, startNode = 1, endNode = 2, weeks = "[1]", colorTheme = "blue")
        )
        val holidayRules = listOf(MakeUpRule("r1", "s1", sourceDate = "2026-03-02", targetDate = "2026-03-08"))
        val nowMillis = calAt(2026, 3, 2, 7, 30).timeInMillis

        val nextUpdate = ScheduleResolver.resolveNextWidgetUpdateMillis(
            nowMillis = nowMillis,
            startDateStr = "2026-03-02",
            rawCourses = courses,
            overrideMap = emptyMap(),
            timeNodeMap = timeNodes,
            makeUpRules = holidayRules
        )

        val expectedMidnight = calAt(2026, 3, 3, 0, 0).apply { set(Calendar.SECOND, 1) }.timeInMillis
        assertEquals(expectedMidnight, nextUpdate)
    }

    @Test
    fun resolveNextUpcomingCourseSkipsDismissedCourseBeforeClassStart() {
        val courses = listOf(
            Course("c1", "s1", "早八高数", "A101", "T1", dayOfWeek = 1, startNode = 1, endNode = 2, weeks = "[1]", colorTheme = "blue"),
            Course("c2", "s1", "第二节线代", "B202", "T2", dayOfWeek = 1, startNode = 3, endNode = 4, weeks = "[1]", colorTheme = "pink")
        )
        val nowMillis = calAt(2026, 3, 2, 7, 42).timeInMillis
        val dismissedCourseStart = calAt(2026, 3, 2, 8, 0).timeInMillis

        val next = ScheduleResolver.resolveNextUpcomingCourse(
            nowMillis = nowMillis,
            advanceMins = 20,
            startDateStr = "2026-03-02",
            rawCourses = courses,
            overrideMap = emptyMap(),
            timeNodeMap = timeNodes,
            makeUpRules = emptyList(),
            minClassStartExclusiveMillis = dismissedCourseStart
        )

        assertNotNull(next)
        assertEquals("第二节线代", next!!.course.name)
        assertEquals(calAt(2026, 3, 2, 9, 55).timeInMillis, next.classStartMillis)
    }

    @Test
    fun resolveNextUpcomingCourseReturnsNullWhenEmpty() {
        assertNull(
            ScheduleResolver.resolveNextUpcomingCourse(
                nowMillis = calAt(2026, 3, 2, 8, 0).timeInMillis,
                advanceMins = 20,
                startDateStr = "2026-03-02",
                rawCourses = emptyList(),
                overrideMap = emptyMap(),
                timeNodeMap = timeNodes,
                makeUpRules = emptyList()
            )
        )
    }
}
