package com.example.simpleschedule.data.local.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "schedule_groups")
data class ScheduleGroup(
    @PrimaryKey val id: String,
    val name: String,
    val timetableId: String = "default_timetable",
    val startDate: String = ""
)

@Entity(tableName = "timetable_groups")
data class TimetableGroup(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "time_nodes")
data class TimeNode(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timetableId: String,
    val nodeIndex: Int,
    val startTime: String,
    val endTime: String
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val name: String,
    val location: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startNode: Int,
    val endNode: Int,
    val weeks: String,
    val colorTheme: String,
    val credits: String? = null
)

@Entity(tableName = "course_overrides")
data class CourseOverride(
    @PrimaryKey val courseId: String,
    val newDayOfWeek: Int,
    val newStartNode: Int,
    val newEndNode: Int
)

@Entity(tableName = "course_statistics")
data class CourseStatistic(
    @PrimaryKey val courseName: String,
    val absenceCount: Int = 0,
    val calledCount: Int = 0
)

@Entity(tableName = "makeup_rules")
data class MakeUpRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scheduleId: String,
    val sourceDate: String, // 被调日期 (原本有课改放假)
    val targetDate: String  // 目标日期 (原本放假改上课)
)

data class DisplayCourse(
    val course: Course,
    val displayDay: Int,
    val displayStartNode: Int,
    val displayEndNode: Int
)

@Dao
interface AppDao {
    @Query("SELECT * FROM schedule_groups")
    fun getAllScheduleGroups(): Flow<List<ScheduleGroup>>

    @Query("SELECT * FROM schedule_groups WHERE id = :id")
    fun getScheduleGroupFlow(id: String): Flow<ScheduleGroup?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleGroup(scheduleGroup: ScheduleGroup)

    @Update
    suspend fun updateScheduleGroup(scheduleGroup: ScheduleGroup)

    @Query("DELETE FROM schedule_groups WHERE id = :id")
    suspend fun deleteScheduleGroup(id: String)

    @Query("UPDATE schedule_groups SET timetableId = :fallbackTimetableId WHERE timetableId = :deletedTimetableId")
    suspend fun reassignSchedulesTimetable(deletedTimetableId: String, fallbackTimetableId: String)

    @Query("SELECT * FROM courses WHERE scheduleId = :scheduleId")
    fun getCoursesBySchedule(scheduleId: String): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCourses(courses: List<Course>)

    @Query("DELETE FROM courses WHERE scheduleId = :scheduleId")
    suspend fun deleteCoursesBySchedule(scheduleId: String)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: String)

    @Query("SELECT * FROM course_overrides")
    fun getAllOverrides(): Flow<List<CourseOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: CourseOverride)

    @Query("DELETE FROM course_overrides WHERE courseId = :courseId")
    suspend fun deleteOverrideByCourse(courseId: String)

    @Query("DELETE FROM course_overrides WHERE courseId IN (SELECT id FROM courses WHERE scheduleId = :scheduleId)")
    suspend fun deleteOverridesBySchedule(scheduleId: String)

    @Query("SELECT * FROM timetable_groups")
    fun getAllTimetableGroups(): Flow<List<TimetableGroup>>

    @Query("SELECT * FROM time_nodes WHERE timetableId = :tid ORDER BY nodeIndex ASC")
    fun getTimeNodesFlow(tid: String): Flow<List<TimeNode>>

    @Query("SELECT * FROM time_nodes WHERE timetableId = :tid ORDER BY nodeIndex ASC")
    suspend fun getTimeNodes(tid: String): List<TimeNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableGroup(group: TimetableGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeNodes(nodes: List<TimeNode>)

    @Query("DELETE FROM time_nodes WHERE timetableId = :tid")
    suspend fun deleteTimeNodes(tid: String)

    @Query("DELETE FROM timetable_groups WHERE id = :tid")
    suspend fun deleteTimetableGroup(tid: String)

    @Query("SELECT * FROM course_statistics WHERE courseName = :name")
    fun getStatisticFlow(name: String): Flow<CourseStatistic?>

    @Query("SELECT * FROM course_statistics WHERE courseName = :name")
    suspend fun getStatisticDirect(name: String): CourseStatistic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistic(statistic: CourseStatistic)

    @Query("SELECT * FROM makeup_rules WHERE scheduleId = :scheduleId")
    fun getMakeUpRulesBySchedule(scheduleId: String): Flow<List<MakeUpRule>>

    @Query("SELECT * FROM makeup_rules")
    fun getAllMakeUpRules(): Flow<List<MakeUpRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMakeUpRule(rule: MakeUpRule)

    @Query("DELETE FROM makeup_rules WHERE id = :id")
    suspend fun deleteMakeUpRule(id: String)

    @Query("DELETE FROM makeup_rules WHERE scheduleId = :scheduleId")
    suspend fun deleteMakeUpRulesBySchedule(scheduleId: String)

    // --- 导出与全量备份相关 ---
    @Query("SELECT * FROM schedule_groups")
    suspend fun getAllScheduleGroupsDirect(): List<ScheduleGroup>

    @Query("SELECT * FROM timetable_groups")
    suspend fun getAllTimetableGroupsDirect(): List<TimetableGroup>

    @Query("SELECT * FROM time_nodes")
    suspend fun getAllTimeNodesDirect(): List<TimeNode>

    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesDirect(): List<Course>

    @Query("SELECT * FROM course_overrides")
    suspend fun getAllOverridesDirect(): List<CourseOverride>

    @Query("SELECT * FROM course_statistics")
    suspend fun getAllStatisticsDirect(): List<CourseStatistic>

    @Query("SELECT * FROM makeup_rules")
    suspend fun getAllMakeUpRulesDirect(): List<MakeUpRule>

    @Query("DELETE FROM schedule_groups")
    suspend fun deleteAllScheduleGroups()

    @Query("DELETE FROM timetable_groups")
    suspend fun deleteAllTimetableGroups()

    @Query("DELETE FROM time_nodes")
    suspend fun deleteAllTimeNodes()

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Query("DELETE FROM course_overrides")
    suspend fun deleteAllOverrides()

    @Query("DELETE FROM course_statistics")
    suspend fun deleteAllStatistics()

    @Query("DELETE FROM makeup_rules")
    suspend fun deleteAllMakeUpRules()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllScheduleGroups(groups: List<ScheduleGroup>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTimetableGroups(groups: List<TimetableGroup>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOverrides(overrides: List<CourseOverride>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStatistics(statistics: List<CourseStatistic>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMakeUpRules(rules: List<MakeUpRule>)
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN credits TEXT DEFAULT NULL")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `course_statistics` (`courseName` TEXT NOT NULL, `absenceCount` INTEGER NOT NULL, `calledCount` INTEGER NOT NULL, PRIMARY KEY(`courseName`))")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `makeup_rules` (`id` TEXT NOT NULL, `scheduleId` TEXT NOT NULL, `sourceDate` TEXT NOT NULL, `targetDate` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}

@Database(
    entities = [
        ScheduleGroup::class,
        TimetableGroup::class,
        TimeNode::class,
        Course::class,
        CourseOverride::class,
        CourseStatistic::class,
        MakeUpRule::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "simpleschedule_db")
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}
