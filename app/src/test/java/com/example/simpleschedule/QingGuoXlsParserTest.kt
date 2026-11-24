package com.example.simpleschedule

import com.example.simpleschedule.domain.parsers.parseQingGuoCellLines
import org.junit.Assert.assertEquals
import org.junit.Test

class QingGuoXlsParserTest {

    @Test
    fun parsesStandardAndTeacherAfterLocationAndMultiCourseCells() {
        val standard = parseQingGuoCellLines(
            listOf("高等数学", "张三", "1-16(周)[01-02节]", "教A101"),
            col = 1
        )
        assertEquals(1, standard.size)
        assertEquals("高等数学", standard[0].name)
        assertEquals("张三", standard[0].teacher)
        assertEquals("教A101", standard[0].location)
        assertEquals(1, standard[0].startNode)
        assertEquals(2, standard[0].endNode)
        assertEquals((1..16).toList(), standard[0].weeks)

        val teacherAfterLoc = parseQingGuoCellLines(
            listOf("数据结构", "1-12(周)[03-04节]", "实验楼202", "李导师"),
            col = 3
        )
        assertEquals(1, teacherAfterLoc.size)
        assertEquals("李导师", teacherAfterLoc[0].teacher)
        assertEquals("实验楼202", teacherAfterLoc[0].location)
        assertEquals(3, teacherAfterLoc[0].startNode)
        assertEquals(4, teacherAfterLoc[0].endNode)

        val explicitAndOdd = parseQingGuoCellLines(
            listOf("机器学习", "导师：王伟(教授),陈明", "1-8(单)(周)[05-06节]", "环宇A302"),
            col = 5
        )
        assertEquals(1, explicitAndOdd.size)
        assertEquals("王伟(教授),陈明", explicitAndOdd[0].teacher)
        assertEquals("环宇A302", explicitAndOdd[0].location)
        assertEquals(listOf(1, 3, 5, 7), explicitAndOdd[0].weeks)

        val stacked = parseQingGuoCellLines(
            listOf(
                "高等数学", "张三", "1-16(周)[01-02节]", "教A101",
                "线性代数", "李四", "1-16(周)[03-04节]", "教B202"
            ),
            col = 2
        )
        assertEquals(2, stacked.size)
        assertEquals("高等数学", stacked[0].name)
        assertEquals("张三", stacked[0].teacher)
        assertEquals("教A101", stacked[0].location)
        assertEquals("线性代数", stacked[1].name)
        assertEquals("李四", stacked[1].teacher)
        assertEquals("教B202", stacked[1].location)
    }
}
