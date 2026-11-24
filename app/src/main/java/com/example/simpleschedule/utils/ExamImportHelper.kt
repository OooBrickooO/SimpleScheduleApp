package com.example.simpleschedule.utils

import com.example.simpleschedule.data.local.room.TimeNode
import com.example.simpleschedule.domain.ScheduleResolver
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.InputStream
import kotlin.math.abs

private val examCourseColors = listOf("blue", "pink", "purple", "slate", "indigo", "rose")

private fun toMinuteOfDay(timeStr: String): Int? {
    val hm = ScheduleResolver.parseTimeMinutes(timeStr) ?: return null
    return hm.first * 60 + hm.second
}

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

        val rows = doc.select("tr.jqgrow")
        if (rows.isEmpty()) {
            return Pair(null, "在 HTML 文件中未找到任何考试数据行。请确保文件是正确的正方教务考试安排页面（包含 tabGrid 表格）！")
        }

        val coursesJsonArray = JSONArray()
        val timePattern = Regex("""(\d{4}-\d{2}-\d{2})\((\d{1,2}:\d{2})-(\d{1,2}:\d{2})\)""")

        for (row in rows) {
            val nameCell = row.selectFirst("td[aria-describedby$=tabGrid_kcmc]")
            val timeCell = row.selectFirst("td[aria-describedby$=tabGrid_kssj]")
            val locCell = row.selectFirst("td[aria-describedby$=tabGrid_cdmc]")
            val seatCell = row.selectFirst("td[aria-describedby$=tabGrid_zwh]")
            val creditsCell = row.selectFirst("td[aria-describedby$=tabGrid_xf]")
                ?: row.selectFirst("td[aria-describedby$=tabGrid_credits]")

            val kcmc = nameCell?.text()?.trim() ?: ""
            val kssj = timeCell?.text()?.trim() ?: ""
            val cdmc = locCell?.text()?.trim() ?: ""
            val zwh = seatCell?.text()?.trim() ?: ""
            val credits = creditsCell?.text()?.trim() ?: ""

            if (kcmc.isEmpty() || kssj.isEmpty()) continue

            val match = timePattern.find(kssj) ?: continue
            val dateStr = match.groupValues[1]
            val examStartMins = toMinuteOfDay(match.groupValues[2]) ?: continue
            val examEndMins = toMinuteOfDay(match.groupValues[3]) ?: continue

            val weekAndDay = getWeekAndDay(dateStr, startDateStr) ?: continue
            val week = weekAndDay.first
            val dayOfWeek = weekAndDay.second

            val overlappingNodes = timeNodes.filter { node ->
                val nodeStartMins = toMinuteOfDay(node.startTime) ?: return@filter false
                val nodeEndMins = toMinuteOfDay(node.endTime) ?: return@filter false
                nodeStartMins < examEndMins && nodeEndMins > examStartMins
            }

            val (startNode, endNode) = if (overlappingNodes.isNotEmpty()) {
                Pair(
                    overlappingNodes.minOf { it.nodeIndex },
                    overlappingNodes.maxOf { it.nodeIndex }
                )
            } else {
                val nearestNode = timeNodes.minByOrNull { node ->
                    val s = toMinuteOfDay(node.startTime) ?: Int.MAX_VALUE
                    abs(s - examStartMins)
                }?.nodeIndex ?: 1
                Pair(nearestNode, nearestNode)
            }

            val finalLocation = if (zwh.isNotEmpty() && zwh != "\u00A0" && zwh != "&nbsp;") {
                "$cdmc (座号: $zwh)"
            } else {
                cdmc
            }

            val nameHash = kcmc.sumOf { it.code }
            val colorTheme = examCourseColors[(nameHash and 0x7fffffff) % examCourseColors.size]
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
                if (credits.isNotEmpty()) {
                    put("credits", credits)
                }
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
