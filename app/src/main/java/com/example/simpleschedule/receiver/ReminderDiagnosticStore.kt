package com.example.simpleschedule.receiver

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.abs

data class ReminderDiagnosticEntry(
    val timestampMillis: Long,
    val eventType: String,
    val courseName: String,
    val scheduledMillis: Long = 0L,
    val actualMillis: Long = 0L,
    val driftMillis: Long = 0L,
    val detail: String = ""
)

/**
 * Persists a rolling ring-buffer of the most recent reminder lifecycle events in app-private filesDir
 * so real-device alarm drift and TTS latency can be inspected without polluting DataStore preferences.
 */
object ReminderDiagnosticStore {
    const val MAX_ENTRIES = 30
    private const val FILE_NAME = "reminder_diagnostics.json"
    private val lock = Any()

    fun appendEntry(
        existing: List<ReminderDiagnosticEntry>,
        entry: ReminderDiagnosticEntry,
        maxEntries: Int = MAX_ENTRIES
    ): List<ReminderDiagnosticEntry> {
        if (maxEntries <= 0) return emptyList()
        return (listOf(entry) + existing).take(maxEntries)
    }

    fun serialize(entries: List<ReminderDiagnosticEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("timestampMillis", entry.timestampMillis)
                    put("eventType", entry.eventType)
                    put("courseName", entry.courseName)
                    put("scheduledMillis", entry.scheduledMillis)
                    put("actualMillis", entry.actualMillis)
                    put("driftMillis", entry.driftMillis)
                    put("detail", entry.detail)
                }
            )
        }
        return array.toString()
    }

    fun parse(raw: String): List<ReminderDiagnosticEntry> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        ReminderDiagnosticEntry(
                            timestampMillis = obj.optLong("timestampMillis", 0L),
                            eventType = obj.optString("eventType", ""),
                            courseName = obj.optString("courseName", ""),
                            scheduledMillis = obj.optLong("scheduledMillis", 0L),
                            actualMillis = obj.optLong("actualMillis", 0L),
                            driftMillis = obj.optLong("driftMillis", 0L),
                            detail = obj.optString("detail", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun formatDrift(driftMillis: Long): String {
        val sign = if (driftMillis >= 0L) "+" else "-"
        val absMs = abs(driftMillis)
        return if (absMs >= 60_000L) {
            val mins = absMs / 60_000L
            val secs = (absMs % 60_000L) / 1000L
            "${sign}${mins}m${secs}s"
        } else {
            val secs = absMs / 1000.0
            String.format(Locale.US, "%s%.1fs", sign, secs)
        }
    }

    fun record(
        context: Context,
        eventType: String,
        courseName: String,
        scheduledMillis: Long = 0L,
        actualMillis: Long = 0L,
        detail: String = ""
    ) {
        val now = if (actualMillis > 0L) actualMillis else System.currentTimeMillis()
        val drift = if (scheduledMillis > 0L && actualMillis > 0L) {
            actualMillis - scheduledMillis
        } else {
            0L
        }
        val entry = ReminderDiagnosticEntry(
            timestampMillis = now,
            eventType = eventType,
            courseName = courseName,
            scheduledMillis = scheduledMillis,
            actualMillis = actualMillis,
            driftMillis = drift,
            detail = detail
        )
        runCatching {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            synchronized(lock) {
                val existing = if (file.exists()) parse(file.readText()) else emptyList()
                val updated = appendEntry(existing, entry, MAX_ENTRIES)
                writeAtomic(file, serialize(updated))
            }
        }
    }

    fun loadEntries(context: Context): List<ReminderDiagnosticEntry> {
        return runCatching {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            synchronized(lock) {
                if (!file.exists()) emptyList() else parse(file.readText())
            }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        runCatching {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            synchronized(lock) {
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun writeAtomic(target: File, content: String) {
        val parent = target.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val tmp = File(parent, "${target.name}.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            target.writeText(content)
            tmp.delete()
        }
    }
}
