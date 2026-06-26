package com.example.timemanager.util

import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.ProjectEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CsvExporter {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun toCsv(
        entries: List<TimeEntryEntity>,
        catById: Map<String, CategoryEntity>,
        projById: Map<String, ProjectEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("date,start_time,end_time,duration_minutes,title,category,subcategory,project,description,effectiveness\n")
        entries.sortedWith(compareBy({ it.date }, { it.startMinOfDay })).forEach { e ->
            val cat = catById[e.categoryId]
            val parent = cat?.parentId?.let { catById[it] }
            val proj = e.projectId?.let { projById[it] }
            sb.append(dateFmt.format(e.date)).append(',')
            sb.append(formatMinOfDay(e.startMinOfDay)).append(',')
            sb.append(formatMinOfDay(e.startMinOfDay + e.durationMin)).append(',')
            sb.append(e.durationMin).append(',')
            sb.append(escape(e.title)).append(',')
            sb.append(escape(parent?.name ?: "")).append(',')
            sb.append(escape(cat?.name ?: "")).append(',')
            sb.append(escape(proj?.name ?: "")).append(',')
            sb.append(escape(e.note ?: "")).append(',')
            sb.append(e.effectiveness)
            sb.append('\n')
        }
        return sb.toString()
    }

    fun toMarkdown(
        entries: List<TimeEntryEntity>,
        catById: Map<String, CategoryEntity>,
        projById: Map<String, ProjectEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("# TimeManager 导出 ${LocalDate.now().format(dateFmt)}\n\n")
        sb.append("## 时间记录\n\n")
        sb.append("| 日期 | 时间 | 时长 | 一级 | 二级 | 标题 | 项目 | 备注 | 有效度 |\n")
        sb.append("|---|---|---:|---|---|---|---|---|---:|\n")
        entries.sortedWith(compareBy({ it.date }, { it.startMinOfDay })).forEach { e ->
            val cat = catById[e.categoryId]
            val parent = cat?.parentId?.let { catById[it] }
            val proj = e.projectId?.let { projById[it] }
            sb.append("| ").append(e.date.format(dateFmt))
                .append(" | ").append("${formatMinOfDay(e.startMinOfDay)}-${formatMinOfDay(e.startMinOfDay + e.durationMin)}")
                .append(" | ").append(formatDurationShort(e.durationMin))
                .append(" | ").append(parent?.name ?: "")
                .append(" | ").append(cat?.name ?: "")
                .append(" | ").append(escapeMd(e.title))
                .append(" | ").append(escapeMd(proj?.name ?: ""))
                .append(" | ").append(escapeMd(e.note ?: ""))
                .append(" | ").append(e.effectiveness).append("%")
                .append(" |\n")
        }
        return sb.toString()
    }

    private fun formatMinOfDay(minOfDay: Int): String {
        val clamped = minOfDay.coerceIn(0, 1440)
        return "%02d:%02d".format(clamped / 60, clamped % 60)
    }

    private fun escape(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s

    private fun escapeMd(s: String): String = s.replace("|", "\\|").replace("\n", " ")
}
