package com.example.timemanager.data.remote

import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.data.entity.ReviewEntity
import com.example.timemanager.data.entity.TimeEntryEntity
import com.example.timemanager.viewmodel.ReviewPeriod
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class ParsedReview(
    val summary: String,
    val findings: String,
    val adjust: String
)

object AiReviewRequestBuilder {

    private const val SYSTEM_PROMPT =
        "后面的 json 是周/日/月的基本报告，请根据这个写一个 200 字左右的小结，" +
            "包括最有价值时段，浪费多少，对明天的调整。" +
            "必须以 JSON 返回，字段为 {\"summary\": str, \"findings\": str, \"adjust\": str}，" +
            "分别对应「主要时间投入」「时间结构问题」「下一步调整」，每段 60-80 字。"

    fun buildMessages(
        period: ReviewPeriod,
        currentRange: Pair<LocalDate, LocalDate>,
        previousRange: Pair<LocalDate, LocalDate>,
        entries: List<TimeEntryEntity>,
        previousEntries: List<TimeEntryEntity>,
        categories: List<CategoryEntity>,
        previousReview: ReviewEntity?,
        dayStartMin: Int
    ): JSONArray {
        val byId = categories.associateBy { it.id }
        val snapshot = buildSnapshot(
            period, currentRange, entries, byId, dayStartMin
        )
        val previousSnapshot = buildSnapshot(
            period, previousRange, previousEntries, byId, dayStartMin, tag = "previous"
        )
        val userObj = JSONObject().apply {
            put("period", JSONObject().apply {
                put("type", period.name)
                put("start", currentRange.first.toString())
                put("end", currentRange.second.toString())
            })
            put("dayStartMin", dayStartMin)
            put("current", snapshot)
            put("previous", previousSnapshot)
            if (previousReview != null) {
                put("previousReview", JSONObject().apply {
                    put("summary", previousReview.summaryText.orEmpty())
                    put("findings", previousReview.mainFindings.orEmpty())
                    put("adjust", previousReview.adjustmentPlan.orEmpty())
                })
            }
        }
        return JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            put(JSONObject().put("role", "user").put("content", userObj.toString()))
        }
    }

    private fun buildSnapshot(
        period: ReviewPeriod,
        range: Pair<LocalDate, LocalDate>,
        entries: List<TimeEntryEntity>,
        categoriesById: Map<String, CategoryEntity>,
        dayStartMin: Int,
        tag: String = "current"
    ): JSONObject {
        val INVALID_PARENT_ID = "cat_invalid"
        val OTHER_PARENT_ID = "cat_other"
        val firstTotals = mutableMapOf<String, Long>()
        val secondTotals = mutableMapOf<String, Long>()
        var effectiveMin = 0L
        var invalidMin = 0L

        entries.forEach { e ->
            val cat = categoriesById[e.categoryId] ?: return@forEach
            val parent = cat.parentId ?: return@forEach
            val eff = e.effectiveness.coerceIn(0, 100) / 100f
            val dur = e.durationMin.toLong()
            val invalid = (dur * (1f - eff)).toLong()
            val valid = dur - invalid
            effectiveMin += valid
            invalidMin += invalid
            secondTotals[e.categoryId] = (secondTotals[e.categoryId] ?: 0L) + dur
            if (parent == INVALID_PARENT_ID) {
                firstTotals[INVALID_PARENT_ID] = (firstTotals[INVALID_PARENT_ID] ?: 0L) + invalid
                firstTotals[OTHER_PARENT_ID] = (firstTotals[OTHER_PARENT_ID] ?: 0L) + valid
            } else {
                firstTotals[parent] = (firstTotals[parent] ?: 0L) + valid
                firstTotals[INVALID_PARENT_ID] = (firstTotals[INVALID_PARENT_ID] ?: 0L) + invalid
            }
        }

        val totalMin = firstTotals.values.sum()

        val l1Breakdown = JSONArray()
        categoriesById.values
            .filter { it.parentId == null }
            .sortedBy { it.sortOrder }
            .forEach { parent ->
                val mins = firstTotals[parent.id] ?: 0L
                if (mins > 0L) {
                    l1Breakdown.put(JSONObject().apply {
                        put("category", parent.name)
                        put("minutes", mins)
                        put("ratio", if (totalMin > 0) mins.toDouble() / totalMin else 0.0)
                    })
                }
            }

        val l2Breakdown = JSONArray()
        secondTotals.entries
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .forEach { (catId, mins) ->
                val cat = categoriesById[catId] ?: return@forEach
                val parentName = cat.parentId?.let { categoriesById[it]?.name } ?: ""
                l2Breakdown.put(JSONObject().apply {
                    put("parent", parentName)
                    put("category", cat.name)
                    put("minutes", mins)
                })
            }

        val entriesArr = JSONArray()
        entries.sortedWith(compareBy({ it.date }, { it.startMinOfDay })).forEach { e ->
            val cat = categoriesById[e.categoryId]
            val parentName = cat?.parentId?.let { categoriesById[it]?.name } ?: ""
            val catName = cat?.name ?: ""
            entriesArr.put(JSONObject().apply {
                put("date", e.date.toString())
                put("category", if (parentName.isBlank()) catName else "$parentName/$catName")
                put("durationMin", e.durationMin)
                if (!e.note.isNullOrBlank()) put("note", e.note)
                put("effectiveness", e.effectiveness)
            })
        }

        return JSONObject().apply {
            put("totalMinutes", totalMin)
            put("effectiveMinutes", effectiveMin)
            put("invalidMinutes", invalidMin)
            put("l1Breakdown", l1Breakdown)
            put("l2Breakdown", l2Breakdown)
            put("entries", entriesArr)
        }
    }

    fun parseResponse(content: String): ParsedReview? {
        return try {
            val cleaned = content.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
            val json = JSONObject(cleaned)
            ParsedReview(
                summary = json.optString("summary", "").trim(),
                findings = json.optString("findings", "").trim(),
                adjust = json.optString("adjust", "").trim()
            )
        } catch (e: Exception) {
            null
        }
    }
}
