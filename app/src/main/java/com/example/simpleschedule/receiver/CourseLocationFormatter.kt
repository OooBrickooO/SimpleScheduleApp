package com.example.simpleschedule.receiver

/**
 * Normalizes classroom location strings for notification bodies and Android 16 / ColorOS 16
 * promoted ongoing capsule short text.
 */
object CourseLocationFormatter {
    const val FALLBACK_LOCATION = "未排地点"
    const val MAX_CAPSULE_LENGTH = 7

    private val leadingLabelRegex = Regex("^(上课地点|教学场地|授课地点|地点|教室|场地)\\s*[:：]\\s*")
    private val parenNoteRegex = Regex("[（(\\[][^）)\\]]*[）)\\]]")
    private val campusMidRegex = Regex("(^|\\s)[\\u4e00-\\u9fa5A-Za-z0-9]{1,6}校区(\\s+|$)")
    private val campusPrefixRegex = Regex("^[\\u4e00-\\u9fa5]{1,5}校区\\s*")
    private val whitespaceRegex = Regex("\\s+")
    private val tokenSplitRegex = Regex("[\\s/·,，]+")
    private val digitRegex = Regex("\\d")

    fun formatDisplayLocation(rawLocation: String?): String {
        val cleaned = rawLocation
            .orEmpty()
            .trim()
            .replace(leadingLabelRegex, "")
            .replace(whitespaceRegex, " ")
            .trim()
        return if (cleaned.isBlank() || cleaned == "未知地点") FALLBACK_LOCATION else cleaned
    }

    fun formatShortCapsuleLocation(rawLocation: String?): String {
        val base = formatDisplayLocation(rawLocation)
        if (base == FALLBACK_LOCATION || base == "待定" || base == "无") {
            return FALLBACK_LOCATION
        }

        val withoutNotes = base.replace(parenNoteRegex, " ").replace(whitespaceRegex, " ").trim()
        var current = if (withoutNotes.isNotBlank()) withoutNotes else base

        val withoutCampusMid = current.replace(campusMidRegex, " ").replace(whitespaceRegex, " ").trim()
        if (withoutCampusMid.isNotBlank()) {
            current = withoutCampusMid
        }
        val withoutCampusPrefix = current.replace(campusPrefixRegex, "").replace(whitespaceRegex, " ").trim()
        if (withoutCampusPrefix.isNotBlank()) {
            current = withoutCampusPrefix
        }

        val shortenedBuilding = current.replace("教学楼", "教").trim()
        if (shortenedBuilding.isNotBlank()) {
            current = shortenedBuilding
        }
        val withoutFloorChar = current.replace("楼", "").trim()
        if (withoutFloorChar.isNotBlank()) {
            current = withoutFloorChar
        }

        current = current.replace(whitespaceRegex, " ").trim()
        if (current.isBlank()) return FALLBACK_LOCATION
        if (current.length <= MAX_CAPSULE_LENGTH) return current

        val compact = current.replace(" ", "")
        if (compact.length <= MAX_CAPSULE_LENGTH) return compact

        val tokens = current.split(tokenSplitRegex).filter { it.isNotBlank() }
        if (tokens.size >= 2) {
            val lastTwo = "${tokens[tokens.size - 2]}${tokens[tokens.size - 1]}"
            if (digitRegex.containsMatchIn(lastTwo) && lastTwo.length <= MAX_CAPSULE_LENGTH) {
                return lastTwo
            }
            val lastDigitToken = tokens.lastOrNull { digitRegex.containsMatchIn(it) }
            if (lastDigitToken != null) {
                return if (lastDigitToken.length <= MAX_CAPSULE_LENGTH) {
                    lastDigitToken
                } else {
                    lastDigitToken.takeLast(MAX_CAPSULE_LENGTH)
                }
            }
            return tokens.last().take(MAX_CAPSULE_LENGTH)
        }

        return if (digitRegex.containsMatchIn(compact)) {
            compact.takeLast(MAX_CAPSULE_LENGTH)
        } else {
            compact.take(MAX_CAPSULE_LENGTH)
        }
    }
}
