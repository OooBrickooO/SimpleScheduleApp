package com.example.simpleschedule.utils

import com.example.simpleschedule.data.local.room.TimeNode
import org.jsoup.Jsoup
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * 解析正方教务系统的考试 HTML 文件
 * @return Pair(JSON字符串, 错误消息)
 */
fun parseZhengfangExamHtml(
    inputStream: InputStream,
    timeNodes: List<TimeNode>,
    startDateStr: String
): Pair<String?, String?> {
    if (startDateStr.isEmpty()) {
        return Pair(null, "当前课表未设置起始日期（第一周的周一），无法自动计算考试周次！请先在课表设置中设置第一周的第一天喵~")
    }

    try {
        val htmlContent = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val doc = Jsoup.parse(htmlContent)

        // 查找所有 jqgrow 行
        val rows = doc.select("tr.jqgrow")
        if (rows.isEmpty()) {
            return Pair(null, "在 HTML 文件中未找到任何考试数据行。请确保文件是正确的正方教务考试安排页面（包含 tabGrid 表格）！")
        }

        val coursesJsonArray = JSONArray()
        val colorList = listOf("blue", "pink", "purple", "slate", "indigo", "rose")
        val timePattern = Regex("""(\d{4}-\d{2}-\d{2})\((\d{2}:\d{2})-(\d{2}:\d{2})\)""")

        for (row in rows) {
            val nameCell = row.selectFirst("td[aria-describedby$=tabGrid_kcmc]")
            val timeCell = row.selectFirst("td[aria-describedby$=tabGrid_kssj]")
            val locCell = row.selectFirst("td[aria-describedby$=tabGrid_cdmc]")
            val seatCell = row.selectFirst("td[aria-describedby$=tabGrid_zwh]")

            val kcmc = nameCell?.text()?.trim() ?: ""
            val kssj = timeCell?.text()?.trim() ?: ""
            val cdmc = locCell?.text()?.trim() ?: ""
            val zwh = seatCell?.text()?.trim() ?: ""

            if (kcmc.isEmpty() || kssj.isEmpty()) {
                continue
            }

            // 匹配日期与时间
            val match = timePattern.find(kssj) ?: continue
            val dateStr = match.groupValues[1]
            val examStart = match.groupValues[2]
            val examEnd = match.groupValues[3]

            // 计算考试周次和星期几
            val weekAndDay = getWeekAndDay(dateStr, startDateStr) ?: continue
            val week = weekAndDay.first
            val dayOfWeek = weekAndDay.second

            // 计算占用的节数区间 (向上取整逻辑：重叠的节均被占用)
            val overlappingNodes = timeNodes.filter { node ->
                val nodeStart = node.startTime.trim()
                val nodeEnd = node.endTime.trim()
                nodeStart < examEnd && nodeEnd > examStart
            }

            val startNode = if (overlappingNodes.isNotEmpty()) {
                overlappingNodes.minOf { it.nodeIndex }
            } else {
                1
            }
            val endNode = if (overlappingNodes.isNotEmpty()) {
                overlappingNodes.maxOf { it.nodeIndex }
            } else {
                startNode
            }

            // 如果有座位号，追加到考试地点
            val finalLocation = if (zwh.isNotEmpty() && zwh != " " && zwh != "&nbsp;") {
                "$cdmc (座号: $zwh)"
            } else {
                cdmc
            }

            // 基于课程名哈希选择颜色主题
            var nameHash = 0
            for (char in kcmc) {
                nameHash += char.code
            }
            val colorTheme = colorList[abs(nameHash) % colorList.size]
            val finalName = "[考试]$kcmc"

            val courseObj = JSONObject().apply {
                put("name", finalName)
                put("location", finalLocation)
                put("teacher", "考试")
                put("dayOfWeek", dayOfWeek)
                put("startNode", startNode)
                put("endNode", endNode)
                put("weeks", "[$week]")
                put("colorTheme", colorTheme)
            }
            coursesJsonArray.put(courseObj)
        }

        if (coursesJsonArray.length() == 0) {
            return Pair(null, "解析完成，但未成功提取到任何有效的考试时间信息。请检查 HTML 文件格式是否正确！")
        }

        return Pair(coursesJsonArray.toString(), null)
    } catch (e: Exception) {
        return Pair(null, "解析 HTML 文件失败: ${e.message}")
    }
}

private fun getWeekAndDay(examDateStr: String, startDateStr: String): Pair<Int, Int>? {
    if (startDateStr.isEmpty()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startCal = Calendar.getInstance().apply {
            time = sdf.parse(startDateStr) ?: Date()
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val examCal = Calendar.getInstance().apply {
            time = sdf.parse(examDateStr) ?: Date()
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 对齐 startCal 到周一
        while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            startCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val calDay = examCal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeek = if (calDay == Calendar.SUNDAY) 7 else calDay - 1

        val diffMillis = examCal.timeInMillis - startCal.timeInMillis
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)
        val week = (diffDays / 7).toInt() + 1

        Pair(week, dayOfWeek)
    } catch (e: Exception) {
        null
    }
}
