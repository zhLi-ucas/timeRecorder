package com.example.timemanager.data.remote

import com.example.timemanager.data.DefaultDataSeeder
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

    fun buildMessages(
        period: ReviewPeriod,
        currentRange: Pair<LocalDate, LocalDate>,
        entries: List<TimeEntryEntity>,
        categories: List<CategoryEntity>,
        recentReviews: List<ReviewEntity>,
        dayStartMin: Int,
        effectivePrompt: String
    ): JSONArray {
        val userObj = buildUserObject(
            period, currentRange, entries, categories, recentReviews, dayStartMin
        )
        val systemContent = effectivePrompt + "\n\n" + DefaultDataSeeder.JSON_FORMAT_SUFFIX
        return JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemContent))
            put(JSONObject().put("role", "user").put("content", userObj.toString()))
        }
    }

    /**
     * 预览用：返回人类可读的 system + user 完整内容，不发请求。
     * 与 buildMessages 共享 user object 构造逻辑。
     */
    fun buildPreview(
        period: ReviewPeriod,
        currentRange: Pair<LocalDate, LocalDate>,
        entries: List<TimeEntryEntity>,
        categories: List<CategoryEntity>,
        recentReviews: List<ReviewEntity>,
        dayStartMin: Int,
        effectivePrompt: String,
        model: String
    ): String {
        val userObj = buildUserObject(
            period, currentRange, entries, categories, recentReviews, dayStartMin
        )
        val systemContent = effectivePrompt + "\n\n" + DefaultDataSeeder.JSON_FORMAT_SUFFIX
        val prettyJson = userObj.toString(2)
        return buildString {
            appendLine("═══ 模型 ═══")
            appendLine(model)
            appendLine()
            appendLine("═══ System message ═══")
            appendLine(systemContent)
            appendLine()
            appendLine("═══ User message (JSON) ═══")
            appendLine(prettyJson)
        }
    }

    private fun buildUserObject(
        period: ReviewPeriod,
        currentRange: Pair<LocalDate, LocalDate>,
        entries: List<TimeEntryEntity>,
        categories: List<CategoryEntity>,
        recentReviews: List<ReviewEntity>,
        dayStartMin: Int
    ): JSONObject {
        val byId = categories.associateBy { it.id }
        val snapshot = buildSnapshot(period, currentRange, entries, byId, dayStartMin)
        return JSONObject().apply {
            put("period", JSONObject().apply {
                put("type", period.name)
                put("start", currentRange.first.toString())
                put("end", currentRange.second.toString())
            })
            put("dayStartMin", dayStartMin)
            put("current", snapshot)
            if (recentReviews.isNotEmpty()) {
                put("recentReviews", JSONArray().apply {
                    recentReviews.forEach { r -> put(JSONObject().apply {
                        put("periodType", r.periodType)
                        put("periodStart", r.periodStart.toString())
                        put("periodEnd", r.periodEnd.toString())
                        put("summary", r.summaryText.orEmpty())
                        put("findings", r.mainFindings.orEmpty())
                        put("adjust", r.adjustmentPlan.orEmpty())
                    }) }
                })
            }
        }
    }

    private fun buildSnapshot(
        period: ReviewPeriod,
        range: Pair<LocalDate, LocalDate>,
        entries: List<TimeEntryEntity>,
        categoriesById: Map<String, CategoryEntity>,
        dayStartMin: Int
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
