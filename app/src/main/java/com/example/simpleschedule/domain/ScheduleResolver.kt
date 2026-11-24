package com.example.simpleschedule.domain

import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.local.room.CourseOverride
import com.example.simpleschedule.data.local.room.DisplayCourse
import com.example.simpleschedule.data.local.room.MakeUpRule
import com.example.simpleschedule.data.local.room.TimeNode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

data class ResolvedDaySchedule(
    val dateStr: String,
    val courses: List<DisplayCourse>,
    val makeUpRule: MakeUpRule?,
    val holidayRule: MakeUpRule?
)

data class UpcomingCourseEvent(
    val course: Course,
    val timeRange: String,
    val triggerMillis: Long,
    val classStartMillis: Long,
    val classEndMillis: Long
)

/**
 * Pure Kotlin single source of truth for week math, date mapping, make-up rules,
 * course overrides, upcoming reminder resolution, and widget refresh scheduling.
 */
object ScheduleResolver {
    private const val MILLIS_PER_DAY = 86_400_000.0
    private val dateFormatPattern = "yyyy-MM-dd"

    private fun newDateFormat(): SimpleDateFormat =
        SimpleDateFormat(dateFormatPattern, Locale.US)

    private fun normalizeToNoon(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun alignToMonday(cal: Calendar) {
        normalizeToNoon(cal)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    fun parseDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        return try {
            newDateFormat().apply { isLenient = false }.parse(dateStr.trim())
        } catch (_: Exception) {
            null
        }
    }

    fun formatDate(cal: Calendar): String = newDateFormat().format(cal.time)

    fun parseWeeks(weeksRaw: String): List<Int> {
        if (weeksRaw.isBlank()) return emptyList()
        return weeksRaw.removeSurrounding("[", "]")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }

    fun calendarDayToIsoDay(cal: Calendar): Int {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 7 else dow - 1
    }

    fun calculateWeekFromStartDate(
        startDateStr: String,
        targetDate: Calendar = Calendar.getInstance()
    ): Int {
        val parsedStart = parseDate(startDateStr) ?: return 1
        val startCal = Calendar.getInstance().apply {
            time = parsedStart
            alignToMonday(this)
        }
        val currentCal = (targetDate.clone() as Calendar).apply {
            alignToMonday(this)
        }
        val diffDays = ((currentCal.timeInMillis - startCal.timeInMillis) / MILLIS_PER_DAY).roundToLong()
        val week = (diffDays / 7L).toInt() + 1
        return week.coerceAtLeast(1)
    }

    fun calculateStartDateForWeek(week: Int, reference: Calendar = Calendar.getInstance()): String {
        val safeWeek = week.coerceAtLeast(1)
        val cal = (reference.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -(safeWeek - 1) * 7)
            alignToMonday(this)
        }
        return formatDate(cal)
    }

    fun getWeekAndDay(dateStr: String, startDateStr: String): Pair<Int, Int>? {
        val parsedStart = parseDate(startDateStr) ?: return null
        val parsedTarget = parseDate(dateStr) ?: return null
        val startCal = Calendar.getInstance().apply {
            time = parsedStart
            alignToMonday(this)
        }
        val targetCal = Calendar.getInstance().apply {
            time = parsedTarget
            normalizeToNoon(this)
        }
        val dayOfWeek = calendarDayToIsoDay(targetCal)
        val diffDays = ((targetCal.timeInMillis - startCal.timeInMillis) / MILLIS_PER_DAY).roundToLong()
        val week = Math.floorDiv(diffDays, 7L).toInt() + 1
        return Pair(week, dayOfWeek)
    }

    fun getDateByWeekAndDay(week: Int, dayOfWeek: Int, startDateStr: String): String? {
        val parsedStart = parseDate(startDateStr) ?: return null
        val startCal = Calendar.getInstance().apply {
            time = parsedStart
            alignToMonday(this)
            add(Calendar.DAY_OF_YEAR, (week - 1) * 7 + (dayOfWeek - 1))
        }
        return formatDate(startCal)
    }

    fun applyOverrides(
        rawCourses: List<Course>,
        overrideMap: Map<String, CourseOverride>
    ): List<DisplayCourse> {
        return rawCourses.map { course ->
            val over = overrideMap[course.id]
            DisplayCourse(
                course = course,
                displayDay = over?.newDayOfWeek ?: course.dayOfWeek,
                displayStartNode = over?.newStartNode ?: course.startNode,
                displayEndNode = over?.newEndNode ?: course.endNode
            )
        }
    }

    fun resolveDayCourses(
        dateStr: String,
        week: Int,
        dayOfWeek: Int,
        startDateStr: String,
        displayCourses: List<DisplayCourse>,
        makeUpRules: List<MakeUpRule>
    ): ResolvedDaySchedule {
        val targetRule = if (dateStr.isNotEmpty()) {
            makeUpRules.find { it.targetDate.isNotEmpty() && it.targetDate == dateStr }
        } else null
        val holidayRule = if (dateStr.isNotEmpty()) {
            makeUpRules.find { it.sourceDate.isNotEmpty() && it.sourceDate == dateStr }
        } else null

        val resolvedCourses = when {
            targetRule != null -> {
                val srcInfo = getWeekAndDay(targetRule.sourceDate, startDateStr)
                if (srcInfo != null) {
                    val (srcWeek, srcDay) = srcInfo
                    displayCourses.filter { dc ->
                        dc.displayDay == srcDay && parseWeeks(dc.course.weeks).contains(srcWeek)
                    }.map { it.copy(displayDay = dayOfWeek) }
                } else {
                    emptyList()
                }
            }
            holidayRule != null -> emptyList()
            else -> {
                displayCourses.filter { dc ->
                    dc.displayDay == dayOfWeek && parseWeeks(dc.course.weeks).contains(week)
                }
            }
        }.sortedBy { it.displayStartNode }

        return ResolvedDaySchedule(
            dateStr = dateStr,
            courses = resolvedCourses,
            makeUpRule = targetRule,
            holidayRule = holidayRule
        )
    }

    fun resolveWeekGridCourses(
        week: Int,
        startDateStr: String,
        displayCourses: List<DisplayCourse>,
        makeUpRules: List<MakeUpRule>,
        showNotThisWeek: Boolean
    ): List<Pair<DisplayCourse, Boolean>> {
        val occupiedSlots = mutableSetOf<String>()
        val currentCourses = mutableListOf<Pair<DisplayCourse, Boolean>>()

        for (day in 1..7) {
            val dayDate = getDateByWeekAndDay(week, day, startDateStr) ?: ""
            val daySchedule = resolveDayCourses(
                dateStr = dayDate,
                week = week,
                dayOfWeek = day,
                startDateStr = startDateStr,
                displayCourses = displayCourses,
                makeUpRules = makeUpRules
            )
            daySchedule.courses.forEach { dc ->
                for (node in dc.displayStartNode..dc.displayEndNode) {
                    occupiedSlots.add("${dc.displayDay}_$node")
                }
                currentCourses.add(Pair(dc, false))
            }
        }

        val futureCourses = mutableListOf<Pair<DisplayCourse, Boolean>>()
        if (showNotThisWeek) {
            val potentialFutures = displayCourses.mapNotNull { dc ->
                val weeksList = parseWeeks(dc.course.weeks)
                if (!weeksList.contains(week)) {
                    val futureWeeks = weeksList.filter { it > week }.sorted()
                    if (futureWeeks.isNotEmpty() && (futureWeeks.first() - week <= 3)) {
                        Pair(dc, futureWeeks.first())
                    } else null
                } else null
            }.sortedBy { it.second }

            for ((dc, _) in potentialFutures) {
                var hasConflict = false
                for (node in dc.displayStartNode..dc.displayEndNode) {
                    if (occupiedSlots.contains("${dc.displayDay}_$node")) {
                        hasConflict = true
                        break
                    }
                }
                if (!hasConflict) {
                    for (node in dc.displayStartNode..dc.displayEndNode) {
                        occupiedSlots.add("${dc.displayDay}_$node")
                    }
                    futureCourses.add(Pair(dc, true))
                }
            }
        }

        return currentCourses + futureCourses
    }

    fun parseTimeMinutes(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return Pair(hour, minute)
    }

    fun resolveNextUpcomingCourse(
        nowMillis: Long,
        advanceMins: Int,
        startDateStr: String,
        rawCourses: List<Course>,
        overrideMap: Map<String, CourseOverride>,
        timeNodeMap: Map<Int, TimeNode>,
        makeUpRules: List<MakeUpRule>,
        lookaheadDays: Int = 14
    ): UpcomingCourseEvent? {
        if (rawCourses.isEmpty() || timeNodeMap.isEmpty()) return null
        val displayCourses = applyOverrides(rawCourses, overrideMap)

        for (dayOffset in 0..lookaheadDays) {
            val evalCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val evalDateStr = formatDate(evalCal)
            val evalDayOfWeek = calendarDayToIsoDay(evalCal)
            val evalWeek = calculateWeekFromStartDate(startDateStr, evalCal)

            val daySchedule = resolveDayCourses(
                dateStr = evalDateStr,
                week = evalWeek,
                dayOfWeek = evalDayOfWeek,
                startDateStr = startDateStr,
                displayCourses = displayCourses,
                makeUpRules = makeUpRules
            )

            val candidates = daySchedule.courses.mapNotNull { dc ->
                val startNodeInfo = timeNodeMap[dc.displayStartNode] ?: return@mapNotNull null
                val endNodeInfo = timeNodeMap[dc.displayEndNode] ?: return@mapNotNull null
                val startHm = parseTimeMinutes(startNodeInfo.startTime) ?: return@mapNotNull null
                val endHm = parseTimeMinutes(endNodeInfo.endTime) ?: return@mapNotNull null

                val startCal = (evalCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, startHm.first)
                    set(Calendar.MINUTE, startHm.second)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (evalCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, endHm.first)
                    set(Calendar.MINUTE, endHm.second)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val classStartMillis = startCal.timeInMillis
                val classEndMillis = endCal.timeInMillis
                if (classStartMillis <= nowMillis) return@mapNotNull null

                val rawTriggerMillis = classStartMillis - advanceMins * 60_000L
                val triggerMillis = maxOf(rawTriggerMillis, nowMillis + 2_000L)

                UpcomingCourseEvent(
                    course = dc.course,
                    timeRange = "${startNodeInfo.startTime}-${endNodeInfo.endTime}",
                    triggerMillis = triggerMillis,
                    classStartMillis = classStartMillis,
                    classEndMillis = classEndMillis
                )
            }

            if (candidates.isNotEmpty()) {
                return candidates.minByOrNull { it.classStartMillis }
            }
        }
        return null
    }

    fun resolveNextWidgetUpdateMillis(
        nowMillis: Long,
        startDateStr: String,
        rawCourses: List<Course>,
        overrideMap: Map<String, CourseOverride>,
        timeNodeMap: Map<Int, TimeNode>,
        makeUpRules: List<MakeUpRule>
    ): Long {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayDateStr = formatDate(nowCal)
        val todayDayOfWeek = calendarDayToIsoDay(nowCal)
        val currentWeek = calculateWeekFromStartDate(startDateStr, nowCal)
        val displayCourses = applyOverrides(rawCourses, overrideMap)

        val todaySchedule = resolveDayCourses(
            dateStr = todayDateStr,
            week = currentWeek,
            dayOfWeek = todayDayOfWeek,
            startDateStr = startDateStr,
            displayCourses = displayCourses,
            makeUpRules = makeUpRules
        )

        val todayCoursesEndMillis = todaySchedule.courses.mapNotNull { dc ->
            val endNodeInfo = timeNodeMap[dc.displayEndNode] ?: return@mapNotNull null
            val endHm = parseTimeMinutes(endNodeInfo.endTime) ?: return@mapNotNull null
            val endCal = (nowCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, endHm.first)
                set(Calendar.MINUTE, endHm.second)
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }
            val millis = endCal.timeInMillis
            if (millis > nowMillis) millis else null
        }

        val nextCourseEnd = todayCoursesEndMillis.minOrNull()
        if (nextCourseEnd != null) return nextCourseEnd

        return (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
