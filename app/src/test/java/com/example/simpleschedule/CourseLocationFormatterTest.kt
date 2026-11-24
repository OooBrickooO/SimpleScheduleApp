package com.example.simpleschedule

import com.example.simpleschedule.receiver.CourseLocationFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseLocationFormatterTest {

    @Test
    fun formatShortCapsuleLocationHandlesPrefixesNotesAndFallbacks() {
        assertEquals("翔宇102", CourseLocationFormatter.formatShortCapsuleLocation("下沙校区 翔宇楼102（智慧教室）"))
        assertEquals("第一教305", CourseLocationFormatter.formatShortCapsuleLocation("东校区 第一教学楼305"))
        assertEquals("格致北 204", CourseLocationFormatter.formatShortCapsuleLocation("上课地点：格致北楼 204"))
        assertEquals("教A-201", CourseLocationFormatter.formatShortCapsuleLocation("教A-201"))
        assertEquals("环宇A302", CourseLocationFormatter.formatShortCapsuleLocation("环宇A302"))
        assertEquals("下沙校区", CourseLocationFormatter.formatShortCapsuleLocation("下沙校区"))

        val longRoom = CourseLocationFormatter.formatShortCapsuleLocation("下沙校区 机电工程实验中心405")
        assertEquals("实验中心405", longRoom)
        assertTrue(longRoom.length <= CourseLocationFormatter.MAX_CAPSULE_LENGTH)
    }

    @Test
    fun formatShortCapsuleLocationUsesFallbackForEmptyOrUnknown() {
        assertEquals(CourseLocationFormatter.FALLBACK_LOCATION, CourseLocationFormatter.formatShortCapsuleLocation(""))
        assertEquals(CourseLocationFormatter.FALLBACK_LOCATION, CourseLocationFormatter.formatShortCapsuleLocation("   "))
        assertEquals(CourseLocationFormatter.FALLBACK_LOCATION, CourseLocationFormatter.formatShortCapsuleLocation(null))
        assertEquals(CourseLocationFormatter.FALLBACK_LOCATION, CourseLocationFormatter.formatShortCapsuleLocation("未排地点"))
        assertEquals(CourseLocationFormatter.FALLBACK_LOCATION, CourseLocationFormatter.formatShortCapsuleLocation("未知地点"))
    }
}
