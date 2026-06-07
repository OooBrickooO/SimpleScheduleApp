package com.example.simpleschedule

import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.utils.parseZhengfangExamHtml
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class ExamImportHelperTest {
    @Test
    fun testParseZhengfangExamHtml() {
        val htmlFile = File("d:\\AndroiedDevProject\\SimpleSchedule\\Target\\CJLU-2.html")
        assertTrue("测试文件必须存在", htmlFile.exists())

        // 模拟计量大学时间节点
        val timeNodes = listOf(
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 1, startTime = "08:00", endTime = "08:45"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 2, startTime = "08:50", endTime = "09:35"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 3, startTime = "09:55", endTime = "10:40"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 4, startTime = "10:45", endTime = "11:30"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 5, startTime = "11:35", endTime = "12:20"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 6, startTime = "13:30", endTime = "14:15"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 7, startTime = "14:20", endTime = "15:05"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 8, startTime = "15:15", endTime = "16:00"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 9, startTime = "16:05", endTime = "16:50"),
            TimeNode(timetableId = "tt_cjlu", nodeIndex = 10, startTime = "18:00", endTime = "18:45")
        )

        // 模拟开学第一周周一
        val startDateStr = "2026-03-02"

        val inputStream = FileInputStream(htmlFile)
        val (jsonData, errorMsg) = parseZhengfangExamHtml(inputStream, timeNodes, startDateStr)

        assertNull("Error message: $errorMsg", errorMsg)
        assertNotNull(jsonData)

        val array = JSONArray(jsonData)
        assertEquals(8, array.length()) // 该网页包含 8 场考试

        // 检查第一场考试:
        // 习近平新时代中国特色社会主义思想概论 | 2026-07-11(14:00-16:00) | 翔宇楼102（智慧教室） | 座号: 13
        // 2026-07-11 为周六 (星期 6)
        // 从 2026-03-02 开始算： (131 天) / 7 + 1 = 第 19 周
        // 时间 14:00 - 16:00
        // 节数 6 (13:30-14:15), 7 (14:20-15:05), 8 (15:15-16:00) 发生重叠，因此占用节数 6 到 8 节
        val firstExam = array.getJSONObject(0)
        assertEquals("[考试]习近平新时代中国特色社会主义思想概论", firstExam.getString("name"))
        assertEquals("翔宇楼102（智慧教室） (座号: 13)", firstExam.getString("location"))
        assertEquals("考试", firstExam.getString("teacher"))
        assertEquals(6, firstExam.getInt("dayOfWeek"))
        assertEquals(6, firstExam.getInt("startNode"))
        assertEquals(8, firstExam.getInt("endNode"))
        assertEquals("[19]", firstExam.getString("weeks"))

        // 检查第三场考试 (9点开始11点结束占用8:50-11:30，即占用2-4节):
        // 概率论与数理统计A | 2026-07-10(09:00-11:00) | 翔宇楼103（智慧教室） | 座号: 18
        // 2026-07-10 为周五 (星期 5)
        // 从 2026-03-02 开始算：第 19 周
        // 09:00 - 11:00
        // 节数 2 (08:50-09:35), 3 (09:55-10:40), 4 (10:45-11:30) 重叠
        val thirdExam = array.getJSONObject(2)
        assertEquals("[考试]概率论与数理统计A", thirdExam.getString("name"))
        assertEquals("翔宇楼103（智慧教室） (座号: 18)", thirdExam.getString("location"))
        assertEquals(5, thirdExam.getInt("dayOfWeek"))
        assertEquals(2, thirdExam.getInt("startNode"))
        assertEquals(4, thirdExam.getInt("endNode"))
        assertEquals("[19]", thirdExam.getString("weeks"))
    }
}
