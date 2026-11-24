package com.example.simpleschedule.utils

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import com.example.simpleschedule.domain.ScheduleResolver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun addMinutes(timeStr: String, mins: Int): String {
    val parts = timeStr.split(":")
    if (parts.size != 2) return timeStr
    val total = (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) + mins
    val normalized = ((total % 1440) + 1440) % 1440
    return String.format(Locale.US, "%02d:%02d", normalized / 60, normalized % 60)
}

fun showTimePicker(context: Context, initialTime: String, onTimeSelected: (String) -> Unit) {
    val parts = initialTime.split(":")
    TimePickerDialog(
        context,
        { _, h, m -> onTimeSelected(String.format(Locale.US, "%02d:%02d", h, m)) },
        parts.getOrNull(0)?.toIntOrNull() ?: 8,
        parts.getOrNull(1)?.toIntOrNull() ?: 0,
        true
    ).show()
}

fun showDatePicker(context: Context, initialDateStr: String, onDateSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
    ScheduleResolver.parseDate(initialDateStr)?.let { cal.time = it }

    DatePickerDialog(
        context,
        { _, y, m, d -> onDateSelected(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)) },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun formatDateForDisplay(dateStr: String): String {
    if (dateStr.isEmpty()) return "未设置"
    return try {
        val parsedDate = ScheduleResolver.parseDate(dateStr) ?: return dateStr
        SimpleDateFormat("yyyy-M-d EEEE", Locale.CHINESE).format(parsedDate)
    } catch (_: Exception) {
        dateStr
    }
}

fun formatShortDate(dateStr: String): String {
    if (dateStr.isEmpty()) return ""
    return try {
        val parsedDate = ScheduleResolver.parseDate(dateStr) ?: return dateStr
        SimpleDateFormat("M/d (E)", Locale.CHINESE).format(parsedDate)
    } catch (_: Exception) {
        dateStr
    }
}

fun getWeekAndDay(dateStr: String, startDateStr: String): Pair<Int, Int>? =
    ScheduleResolver.getWeekAndDay(dateStr, startDateStr)

fun getDateByWeekAndDay(week: Int, dayOfWeek: Int, startDateStr: String): String? =
    ScheduleResolver.getDateByWeekAndDay(week, dayOfWeek, startDateStr)
