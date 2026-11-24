package com.example.simpleschedule.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgLight = Color(0xFFFAFAFA)
val BorderLight = Color(0xFFE4E4E7)
val TextLight = Color(0xFF09090B)
val BgDark = Color(0xFF09090B)
val BorderDark = Color(0xFF27272A)
val TextDark = Color(0xFFFAFAFA)

data class CoursePalette(val bg: Color, val border: Color, val text: Color, val accent: Color)

val CourseColorThemeOptions = listOf(
    "blue", "indigo", "purple", "pink", "rose", "green", "orange", "red", "slate"
)

fun getPalette(theme: String, isDark: Boolean): CoursePalette {
    return when (theme) {
        "blue" -> if (isDark) CoursePalette(Color(0xFF151B23), Color(0xFF2A4365), Color(0xFF90CDF4), Color(0xFF3182CE)) else CoursePalette(Color(0xFFEEF2F6), Color(0xFFD0DBE7), Color(0xFF2A4365), Color(0xFF3182CE))
        "pink" -> if (isDark) CoursePalette(Color(0xFF381A2A), Color(0xFF831843), Color(0xFFFBCFE8), Color(0xFFBE185D)) else CoursePalette(Color(0xFFFDF2F8), Color(0xFFFBCFE8), Color(0xFF9D174D), Color(0xFFEC4899))
        "indigo" -> if (isDark) CoursePalette(Color(0xFF1E1E2D), Color(0xFF3730A3), Color(0xFFC7D2FE), Color(0xFF4F46E5)) else CoursePalette(Color(0xFFEEF2FF), Color(0xFFC7D2FE), Color(0xFF3730A3), Color(0xFF4F46E5))
        "rose" -> if (isDark) CoursePalette(Color(0xFF331B22), Color(0xFF881337), Color(0xFFFECDD3), Color(0xFFE11D48)) else CoursePalette(Color(0xFFFFF1F2), Color(0xFFFECDD3), Color(0xFF9F1239), Color(0xFFE11D48))
        "purple" -> if (isDark) CoursePalette(Color(0xFF1C1228), Color(0xFF581C87), Color(0xFFD8B4FE), Color(0xFF9333EA)) else CoursePalette(Color(0xFFFAF5FF), Color(0xFFE9D5FF), Color(0xFF581C87), Color(0xFF9333EA))
        "slate" -> if (isDark) CoursePalette(Color(0xFF0F1218), Color(0xFF334155), Color(0xFFCBD5E1), Color(0xFF64748B)) else CoursePalette(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFF334155), Color(0xFF64748B))
        "green" -> if (isDark) CoursePalette(Color(0xFF121C17), Color(0xFF166534), Color(0xFF86EFAC), Color(0xFF16A34A)) else CoursePalette(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF166534), Color(0xFF16A34A))
        "orange" -> if (isDark) CoursePalette(Color(0xFF241711), Color(0xFF9A3412), Color(0xFFFDBA74), Color(0xFFEA580C)) else CoursePalette(Color(0xFFFFF7ED), Color(0xFFFED7AA), Color(0xFF9A3412), Color(0xFFEA580C))
        "red" -> if (isDark) CoursePalette(Color(0xFF261414), Color(0xFF991B1B), Color(0xFFFCA5A5), Color(0xFFDC2626)) else CoursePalette(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFF991B1B), Color(0xFFDC2626))
        else -> if (isDark) CoursePalette(Color(0xFF151B23), Color(0xFF2A4365), Color(0xFF90CDF4), Color(0xFF3182CE)) else CoursePalette(Color(0xFFEEF2F6), Color(0xFFD0DBE7), Color(0xFF2A4365), Color(0xFF3182CE))
    }
}

@Composable
fun getCoursePalette(
    theme: String,
    isDark: Boolean,
    useMaterialYou: Boolean,
    courseName: String = "",
    customPoolString: String = ""
): CoursePalette {
    if (customPoolString.isNotBlank() && courseName.isNotBlank()) {
        val pool = customPoolString.split(",").mapNotNull { it.toULongOrNull()?.let { v -> Color(v) } }
        if (pool.isNotEmpty()) {
            val safeHash = courseName.hashCode() and 0x7fffffff
            val baseColor = pool[safeHash % pool.size]
            return if (isDark) {
                CoursePalette(
                    bg = baseColor.copy(alpha = 0.2f),
                    border = baseColor.copy(alpha = 0.5f),
                    text = baseColor.copy(alpha = 0.9f),
                    accent = baseColor
                )
            } else {
                CoursePalette(
                    bg = baseColor.copy(alpha = 0.2f),
                    border = baseColor.copy(alpha = 0.4f),
                    text = Color(baseColor.red * 0.6f, baseColor.green * 0.6f, baseColor.blue * 0.6f, 1f),
                    accent = baseColor
                )
            }
        }
    }

    if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = MaterialTheme.colorScheme
        return when (theme) {
            "blue" -> CoursePalette(scheme.primaryContainer, scheme.primary.copy(alpha = 0.2f), scheme.onPrimaryContainer, scheme.primary)
            "pink" -> CoursePalette(scheme.errorContainer, scheme.error.copy(alpha = 0.2f), scheme.onErrorContainer, scheme.error)
            "indigo" -> CoursePalette(scheme.secondaryContainer, scheme.secondary.copy(alpha = 0.2f), scheme.onSecondaryContainer, scheme.secondary)
            "rose" -> CoursePalette(scheme.tertiaryContainer, scheme.tertiary.copy(alpha = 0.2f), scheme.onTertiaryContainer, scheme.tertiary)
            "purple" -> CoursePalette(scheme.primaryContainer, scheme.primary.copy(alpha = 0.2f), scheme.onPrimaryContainer, scheme.primary)
            "slate" -> CoursePalette(scheme.surfaceVariant, scheme.onSurfaceVariant.copy(alpha = 0.2f), scheme.onSurfaceVariant, scheme.outline)
            "green" -> CoursePalette(scheme.secondaryContainer, scheme.secondary.copy(alpha = 0.2f), scheme.onSecondaryContainer, scheme.secondary)
            "orange" -> CoursePalette(scheme.tertiaryContainer, scheme.tertiary.copy(alpha = 0.2f), scheme.onTertiaryContainer, scheme.tertiary)
            "red" -> CoursePalette(scheme.errorContainer, scheme.error.copy(alpha = 0.2f), scheme.onErrorContainer, scheme.error)
            else -> CoursePalette(scheme.primaryContainer, scheme.primary.copy(alpha = 0.2f), scheme.onPrimaryContainer, scheme.primary)
        }
    } else {
        return getPalette(theme, isDark)
    }
}
