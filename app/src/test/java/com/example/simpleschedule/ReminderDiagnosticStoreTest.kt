package com.example.simpleschedule

import com.example.simpleschedule.receiver.ReminderDiagnosticEntry
import com.example.simpleschedule.receiver.ReminderDiagnosticStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDiagnosticStoreTest {

    @Test
    fun appendEntryCapsAtMaxEntriesNewestFirst() {
        var entries = emptyList<ReminderDiagnosticEntry>()
        for (i in 1..35) {
            entries = ReminderDiagnosticStore.appendEntry(
                entries,
                ReminderDiagnosticEntry(
                    timestampMillis = i.toLong() * 1000L,
                    eventType = "WAKEUP",
                    courseName = "Course $i",
                    detail = "detail $i"
                )
            )
        }
        assertEquals(ReminderDiagnosticStore.MAX_ENTRIES, entries.size)
        assertEquals("Course 35", entries.first().courseName)
        assertEquals("Course 6", entries.last().courseName)
    }

    @Test
    fun serializeAndParseRoundTripPreservesFields() {
        val original = listOf(
            ReminderDiagnosticEntry(
                timestampMillis = 1_700_000_000_500L,
                eventType = "WAKEUP",
                courseName = "高等数学",
                scheduledMillis = 1_700_000_000_000L,
                actualMillis = 1_700_000_000_500L,
                driftMillis = 500L,
                detail = "通知=true, 播报=true"
            ),
            ReminderDiagnosticEntry(
                timestampMillis = 1_700_000_010_000L,
                eventType = "DISMISS",
                courseName = "高等数学",
                detail = "上课时间到达自动消除"
            )
        )

        val json = ReminderDiagnosticStore.serialize(original)
        val parsed = ReminderDiagnosticStore.parse(json)
        assertEquals(original, parsed)
    }

    @Test
    fun parseHandlesBlankAndMalformedJsonGracefully() {
        assertTrue(ReminderDiagnosticStore.parse("").isEmpty())
        assertTrue(ReminderDiagnosticStore.parse("   ").isEmpty())
        assertTrue(ReminderDiagnosticStore.parse("{not a valid array").isEmpty())
    }

    @Test
    fun formatDriftFormatsSubSecondSecondsAndMinutes() {
        assertEquals("+0.3s", ReminderDiagnosticStore.formatDrift(300L))
        assertEquals("-0.2s", ReminderDiagnosticStore.formatDrift(-200L))
        assertEquals("+1m12s", ReminderDiagnosticStore.formatDrift(72_000L))
    }
}
