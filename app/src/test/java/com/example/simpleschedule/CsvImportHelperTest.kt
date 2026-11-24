package com.example.simpleschedule

import com.example.simpleschedule.utils.parseCourseCsv
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CsvImportHelperTest {
    @Test
    fun parsesTemplateColumnsAndWeekModes() {
        val csv = """
            课程名称,星期,开始节数,结束节数,老师,地点,周数
            高等数学,1,1,2,李老师,"东阶楼,201",1-5、7-11单、12-16双
            程序设计,2,3,4,王老师,南阶楼110,1-16
            大学英语,2,3,4,赵老师,西楼125,2周5周8周
        """.trimIndent()

        val result = parseCourseCsv(csv)
        assertNotNull(result)

        val courses = JSONArray(result)
        assertEquals(3, courses.length())
        assertEquals("东阶楼,201", courses.getJSONObject(0).getString("location"))
        assertEquals("[1,2,3,4,5,7,9,11,12,14,16]", courses.getJSONObject(0).getString("weeks"))
        assertEquals("[2,5,8]", courses.getJSONObject(2).getString("weeks"))
    }

    @Test
    fun restoresWeeksConvertedToDatesByExcel() {
        val csv = """
            课程名称,星期,开始节数,结束节数,老师,地点,周数
            线性代数,1,1,2,张老师,教A101,1月16日
            物理实验,2,3,4,刘老师,实验楼202,2026/1/8
            数据结构,3,5,6,陈老师,信工303,16-Jan
            概率统计,4,7,8,周老师,明德404,1月7日单
        """.trimIndent()

        val result = parseCourseCsv(csv)
        assertNotNull(result)

        val courses = JSONArray(result)
        assertEquals(4, courses.length())
        assertEquals((1..16).toList().toString().replace(" ", ""), courses.getJSONObject(0).getString("weeks"))
        assertEquals((1..8).toList().toString().replace(" ", ""), courses.getJSONObject(1).getString("weeks"))
        assertEquals((1..16).toList().toString().replace(" ", ""), courses.getJSONObject(2).getString("weeks"))
        assertEquals("[1,3,5,7]", courses.getJSONObject(3).getString("weeks"))
    }
}
