package com.example.simpleschedule.viewmodel

import android.app.Application
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpleschedule.SimpleScheduleApplication
import com.example.simpleschedule.data.local.datastore.AppSettings
import com.example.simpleschedule.data.local.datastore.SettingsKeys
import com.example.simpleschedule.data.local.room.Course
import com.example.simpleschedule.data.local.room.CourseOverride
import com.example.simpleschedule.data.local.room.CourseStatistic
import com.example.simpleschedule.data.local.room.DisplayCourse
import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.data.repository.ScheduleRepository
import com.example.simpleschedule.domain.ScheduleResolver
import com.example.simpleschedule.receiver.ReminderEngine
import com.example.simpleschedule.widget.CourseWeekWidget
import com.example.simpleschedule.widget.CourseWidget
import com.example.simpleschedule.widget.CourseWidget2x2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SimpleScheduleApplication).container
    private val scheduleRepo = container.scheduleRepository
    private val settingsRepo = container.settingsRepository

    val appSettings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val scheduleGroups = scheduleRepo.observeAllScheduleGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val timetableGroups = scheduleRepo.observeAllTimetableGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentScheduleId = MutableStateFlow("default_id")
    val currentScheduleId = _currentScheduleId.asStateFlow()

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek = _currentWeek.asStateFlow()

    // Convenience slices backed by unified settingsFlow
    val isDarkTheme = appSettings.map { it.isDarkTheme }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val showSat = appSettings.map { it.showSat }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val showSun = appSettings.map { it.showSun }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val showNotThisWeek = appSettings.map { it.showNotThisWeek }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val cellHeight = appSettings.map { it.cellHeight }.stateIn(viewModelScope, SharingStarted.Eagerly, 64f)
    val cornerRadius = appSettings.map { it.cornerRadius }.stateIn(viewModelScope, SharingStarted.Eagerly, 4f)
    val hideTime = appSettings.map { it.hideTime }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val bottomBlank = appSettings.map { it.bottomBlank }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val vibration = appSettings.map { it.vibration }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val totalWeeks = appSettings.map { it.totalWeeks }.stateIn(viewModelScope, SharingStarted.Eagerly, 20)
    val materialYou = appSettings.map { it.materialYou }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val autoUpdate = appSettings.map { it.autoUpdate }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val reminderEnabled = appSettings.map { it.reminderEnabled }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val reminderVoiceEnabled = appSettings.map { it.reminderVoiceEnabled }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val reminderNotifyEnabled = appSettings.map { it.reminderNotifyEnabled }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val reminderAdvanceMins = appSettings.map { it.reminderAdvanceMins }.stateIn(viewModelScope, SharingStarted.Eagerly, 20)
    val widgetTranslucent = appSettings.map { it.widgetTranslucent }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val dynamicIslandEnabled = appSettings.map { it.dynamicIslandEnabled }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val predictiveBackEnabled = appSettings.map { it.predictiveBackEnabled }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val floatingBottomBar = appSettings.map { it.floatingBottomBar }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tabAnimationType = appSettings.map { it.tabAnimationType }.stateIn(viewModelScope, SharingStarted.Eagerly, "Slide")
    val hasSeenMakeUpGuide = appSettings.map { it.hasSeenMakeUpGuide }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val hasSeenDataManagementGuide = appSettings.map { it.hasSeenDataManagementGuide }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val accentColor = appSettings.map { it.accentColor }.stateIn(viewModelScope, SharingStarted.Eagerly, -1L)
    val courseColorPool = appSettings.map { it.courseColorPool }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val activeTimeNodes = _currentScheduleId.flatMapLatest { sId ->
        scheduleRepo.observeScheduleGroup(sId).flatMapLatest { sg ->
            scheduleRepo.observeTimeNodes(sg?.timetableId ?: ScheduleRepository.DEFAULT_TIMETABLE_ID)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val displayCourses: StateFlow<List<DisplayCourse>> = _currentScheduleId.flatMapLatest { scheduleId ->
        combine(
            scheduleRepo.observeCoursesBySchedule(scheduleId),
            scheduleRepo.observeAllOverrides()
        ) { courses, overrides ->
            ScheduleResolver.applyOverrides(courses, overrides.associateBy { it.courseId })
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val makeUpRules = _currentScheduleId.flatMapLatest { sId ->
        scheduleRepo.observeMakeUpRulesBySchedule(sId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val reminderSensitiveKeys = setOf(
        SettingsKeys.REMINDER_ENABLED.name,
        SettingsKeys.REMINDER_VOICE_ENABLED.name,
        SettingsKeys.REMINDER_NOTIFY_ENABLED.name,
        SettingsKeys.REMINDER_ADVANCE_MINS.name,
        SettingsKeys.DYNAMIC_ISLAND_ENABLED.name
    )

    init {
        viewModelScope.launch {
            scheduleRepo.ensureBuiltInTimetablesExist()
            val savedId = settingsRepo.getPreference(SettingsKeys.CURRENT_SCHEDULE_ID)
            val groups = scheduleRepo.getAllScheduleGroupsDirect()
            if (groups.isEmpty()) {
                val defaultGroup = scheduleRepo.initDefaultSchedule()
                _currentScheduleId.value = defaultGroup.id
                settingsRepo.setPreference(SettingsKeys.CURRENT_SCHEDULE_ID, defaultGroup.id)
                _currentWeek.value = 1
            } else {
                val targetId = if (savedId != null && groups.any { it.id == savedId }) savedId else groups.first().id
                switchSchedule(targetId, resetReminderState = false)
            }
            ReminderEngine.scheduleNextWidgetUpdate(getApplication(), scheduleRepo.dao)
        }
    }

    fun markMakeUpGuideSeen() {
        viewModelScope.launch { settingsRepo.setPreference(SettingsKeys.HAS_SEEN_MAKEUP_GUIDE, true) }
    }

    fun markDataManagementGuideSeen() {
        viewModelScope.launch { settingsRepo.setPreference(SettingsKeys.HAS_SEEN_DATA_MANAGEMENT_GUIDE, true) }
    }

    fun updateSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            settingsRepo.setPreference(key, value)
            if (key.name in reminderSensitiveKeys) {
                scheduleNextAlarm(resetHandledState = true)
            }
        }
    }

    fun updateSetting(key: Preferences.Key<Float>, value: Float) = viewModelScope.launch {
        settingsRepo.setPreference(key, value)
    }

    fun updateSetting(key: Preferences.Key<Int>, value: Int) {
        viewModelScope.launch {
            settingsRepo.setPreference(key, value)
            if (key.name in reminderSensitiveKeys) {
                scheduleNextAlarm(resetHandledState = true)
            }
        }
    }

    fun updateSetting(key: Preferences.Key<String>, value: String) = viewModelScope.launch {
        settingsRepo.setPreference(key, value)
    }

    fun updateSetting(key: Preferences.Key<Long>, value: Long) = viewModelScope.launch {
        settingsRepo.setPreference(key, value)
    }

    fun toggleTheme(isDark: Boolean?) {
        viewModelScope.launch {
            if (isDark == null) {
                settingsRepo.clearDarkTheme()
            } else {
                settingsRepo.setPreference(SettingsKeys.IS_DARK_THEME, isDark)
            }
        }
    }

    fun notifyWidgetUpdate() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            CourseWidget().updateAll(app)
            CourseWidget2x2().updateAll(app)
            CourseWeekWidget().updateAll(app)
            ReminderEngine.scheduleNextWidgetUpdate(app, scheduleRepo.dao)
        }
    }

    fun scheduleNextAlarm(resetHandledState: Boolean = false) {
        viewModelScope.launch {
            ReminderEngine.calculateAndScheduleNext(getApplication(), scheduleRepo.dao, resetHandledState)
        }
    }

    fun changeWeek(week: Int) {
        updateWeekAndReverseCalculateStartDate(_currentScheduleId.value, week)
    }

    fun refreshCurrentWeek() {
        viewModelScope.launch {
            val group = scheduleRepo.getScheduleGroupDirect(_currentScheduleId.value)
            group?.let {
                val calculatedWeek = ScheduleResolver.calculateWeekFromStartDate(it.startDate)
                if (_currentWeek.value != calculatedWeek) {
                    _currentWeek.value = calculatedWeek
                }
            }
        }
    }

    fun updateScheduleStartDate(id: String, dateStr: String) {
        viewModelScope.launch {
            scheduleRepo.getScheduleGroupDirect(id)?.let {
                scheduleRepo.updateSchedule(it.copy(startDate = dateStr))
                _currentWeek.value = ScheduleResolver.calculateWeekFromStartDate(dateStr)
                notifyWidgetUpdate()
                scheduleNextAlarm(resetHandledState = true)
            }
        }
    }

    fun updateWeekAndReverseCalculateStartDate(id: String, week: Int) {
        _currentWeek.value = week
        viewModelScope.launch {
            scheduleRepo.getScheduleGroupDirect(id)?.let {
                val newStartDate = ScheduleResolver.calculateStartDateForWeek(week)
                scheduleRepo.updateSchedule(it.copy(startDate = newStartDate))
                notifyWidgetUpdate()
                scheduleNextAlarm(resetHandledState = true)
            }
        }
    }

    fun switchSchedule(id: String, resetReminderState: Boolean = true) {
        _currentScheduleId.value = id
        viewModelScope.launch {
            settingsRepo.setPreference(SettingsKeys.CURRENT_SCHEDULE_ID, id)
            val group = scheduleRepo.getScheduleGroupDirect(id)
            _currentWeek.value = if (group != null && group.startDate.isNotEmpty()) {
                ScheduleResolver.calculateWeekFromStartDate(group.startDate)
            } else 1
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = resetReminderState)
        }
    }

    fun createNewSchedule(name: String, onCreated: (() -> Unit)? = null) {
        viewModelScope.launch {
            val newGroup = scheduleRepo.createSchedule(name)
            switchSchedule(newGroup.id)
            onCreated?.invoke()
        }
    }

    fun renameSchedule(id: String, newName: String) {
        viewModelScope.launch {
            scheduleRepo.getScheduleGroupDirect(id)?.let {
                if (it.name != newName) {
                    scheduleRepo.updateSchedule(it.copy(name = newName))
                    notifyWidgetUpdate()
                }
            }
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            val groups = scheduleRepo.getAllScheduleGroupsDirect()
            if (groups.size > 1 && scheduleRepo.deleteScheduleAtomic(id)) {
                if (_currentScheduleId.value == id) {
                    switchSchedule(groups.first { it.id != id }.id)
                } else {
                    notifyWidgetUpdate()
                    scheduleNextAlarm(resetHandledState = true)
                }
            }
        }
    }

    fun linkTimetableToCurrentSchedule(timetableId: String) {
        viewModelScope.launch {
            scheduleRepo.getScheduleGroupDirect(_currentScheduleId.value)?.let {
                scheduleRepo.updateSchedule(it.copy(timetableId = timetableId))
                notifyWidgetUpdate()
                scheduleNextAlarm(resetHandledState = true)
            }
        }
    }

    fun saveTimetable(id: String?, name: String, nodes: List<TimeNode>) {
        viewModelScope.launch {
            scheduleRepo.saveTimetableAtomic(id, name, nodes)
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = true)
        }
    }

    fun deleteTimetable(id: String) {
        viewModelScope.launch {
            if (scheduleRepo.deleteTimetableAtomic(id)) {
                notifyWidgetUpdate()
                scheduleNextAlarm(resetHandledState = true)
            }
        }
    }

    suspend fun getTimeNodesForEdit(id: String): List<TimeNode> = scheduleRepo.getTimeNodes(id)

    fun updateCoursePosition(courseId: String, deltaCols: Int, deltaRows: Int, currentCourse: Course) {
        viewModelScope.launch {
            val maxNode = maxOf(12, activeTimeNodes.value.size)
            val oldOverride = scheduleRepo.getAllOverridesDirect().find { it.courseId == courseId }
            val currentDay = oldOverride?.newDayOfWeek ?: currentCourse.dayOfWeek
            val currentStart = oldOverride?.newStartNode ?: currentCourse.startNode
            val currentEnd = oldOverride?.newEndNode ?: currentCourse.endNode
            val span = (currentEnd - currentStart).coerceAtLeast(0)
            val newDay = (currentDay + deltaCols).coerceIn(1, 7)
            val maxStart = (maxNode - span).coerceAtLeast(1)
            val newStart = (currentStart + deltaRows).coerceIn(1, maxStart)
            val newEnd = (newStart + span).coerceIn(newStart, maxNode)
            scheduleRepo.upsertOverride(CourseOverride(courseId, newDay, newStart, newEnd))
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = true)
        }
    }

    fun addCustomCourse(
        name: String,
        location: String,
        teacher: String,
        day: Int,
        start: Int,
        end: Int,
        color: String,
        weeks: String,
        credits: String? = null
    ) {
        viewModelScope.launch {
            scheduleRepo.upsertCourse(
                Course(
                    id = UUID.randomUUID().toString(),
                    scheduleId = _currentScheduleId.value,
                    name = name,
                    location = location,
                    teacher = teacher,
                    dayOfWeek = day,
                    startNode = start,
                    endNode = end,
                    weeks = weeks.ifBlank { "[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]" },
                    colorTheme = color,
                    credits = credits
                )
            )
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = true)
        }
    }

    fun updateCustomCourse(
        id: String,
        name: String,
        location: String,
        teacher: String,
        day: Int,
        start: Int,
        end: Int,
        color: String,
        weeks: String,
        credits: String? = null
    ) {
        viewModelScope.launch {
            val existingCourse = scheduleRepo.observeCoursesBySchedule(_currentScheduleId.value)
                .firstOrNull()
                ?.firstOrNull { it.id == id }
            val updated = (existingCourse ?: Course(
                id = id,
                scheduleId = _currentScheduleId.value,
                name = name,
                location = location,
                teacher = teacher,
                dayOfWeek = day,
                startNode = start,
                endNode = end,
                weeks = weeks,
                colorTheme = color,
                credits = credits
            )).copy(
                name = name,
                location = location,
                teacher = teacher,
                dayOfWeek = day,
                startNode = start,
                endNode = end,
                weeks = weeks,
                colorTheme = color,
                credits = credits
            )
            scheduleRepo.upsertCourse(updated, updateExistingOverride = true)
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = true)
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            scheduleRepo.deleteCourseAtomic(courseId)
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = true)
        }
    }

    fun addMakeUpRule(sourceDate: String, targetDate: String) {
        viewModelScope.launch {
            scheduleRepo.addMakeUpRule(_currentScheduleId.value, sourceDate, targetDate)
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = true)
        }
    }

    fun deleteMakeUpRule(id: String) {
        viewModelScope.launch {
            scheduleRepo.deleteMakeUpRule(id)
            notifyWidgetUpdate()
            scheduleNextAlarm(resetHandledState = true)
        }
    }

    fun getCourseStatistic(courseName: String): Flow<CourseStatistic?> =
        scheduleRepo.observeCourseStatistic(courseName)

    suspend fun getCourseStatisticDirect(courseName: String): CourseStatistic? =
        scheduleRepo.getCourseStatisticDirect(courseName)

    fun incrementAbsenceCount(courseName: String) {
        viewModelScope.launch { scheduleRepo.incrementAbsenceCount(courseName) }
    }

    fun incrementCalledCount(courseName: String) {
        viewModelScope.launch { scheduleRepo.incrementCalledCount(courseName) }
    }

    fun updateCourseStatistic(courseName: String, absence: Int, called: Int) {
        viewModelScope.launch { scheduleRepo.updateCourseStatistic(courseName, absence, called) }
    }

    fun importFromJson(jsonString: String, replaceCurrent: Boolean = false, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = scheduleRepo.importCoursesFromJson(
                scheduleId = _currentScheduleId.value,
                jsonString = jsonString,
                replaceCurrent = replaceCurrent,
                currentTotalWeeks = totalWeeks.value
            )
            result.fold(
                onSuccess = { maxWeek ->
                    if (maxWeek > totalWeeks.value) {
                        updateSetting(SettingsKeys.TOTAL_WEEKS, maxWeek)
                    }
                    notifyWidgetUpdate()
                    scheduleNextAlarm(resetHandledState = true)
                    onResult(true)
                },
                onFailure = { onResult(false) }
            )
        }
    }

    fun importFromShareCode(code: String, onResult: (Boolean) -> Unit) {
        try {
            val jsonString = String(Base64.decode(code, Base64.DEFAULT), Charsets.UTF_8)
            importFromJson(jsonString, onResult = onResult)
        } catch (_: Exception) {
            onResult(false)
        }
    }
}
