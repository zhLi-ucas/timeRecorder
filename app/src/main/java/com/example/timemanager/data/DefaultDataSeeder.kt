package com.example.timemanager.data

import com.example.timemanager.data.db.AppDatabase
import com.example.timemanager.data.entity.AppSettingEntity
import com.example.timemanager.data.entity.CategoryEntity
import java.time.LocalDateTime

class DefaultDataSeeder(private val db: AppDatabase) {

    suspend fun seedIfNeeded() {
        val already = db.appSettingDao().getByKey(KEY_SEEDED)
        if (already == "true") return
        seed()
    }

    private suspend fun seed() {
        val now = LocalDateTime.now()
        val firstLevel = listOf(
            Triple("cat_work", "工作", "blue"),
            Triple("cat_rest", "休息", "cyan"),
            Triple("cat_self", "自我实现", "orange"),
            Triple("cat_invalid", "无效消耗", "grey"),
            Triple("cat_other", "其他", "neutral")
        )
        firstLevel.forEachIndexed { index, (id, name, colorKey) ->
            db.categoryDao().insert(
                CategoryEntity(
                    id = id,
                    name = name,
                    parentId = null,
                    colorKey = colorKey,
                    sortOrder = index,
                    isArchived = false,
                    isSystem = id == "cat_other",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val secondLevel = listOf(
            "cat_work" to listOf("实验" to "experiment", "阅读" to "reading", "写论文" to "paper", "思考" to "thinking", "讨论" to "discussion"),
            "cat_rest" to listOf("放空" to "space", "游戏" to "game", "阅读" to "reading", "间隔" to "interval"),
            "cat_self" to listOf("规划" to "plan", "户外" to "outdoor", "室内" to "indoor"),
            "cat_invalid" to listOf("无效等待" to "wait", "被动阅读" to "passive"),
            "cat_other" to listOf("待分类" to "pending")
        )
        secondLevel.forEach { (parentId, children) ->
            children.forEachIndexed { index, (name, slug) ->
                db.categoryDao().insert(
                    CategoryEntity(
                        id = "${parentId}_$slug",
                        name = name,
                        parentId = parentId,
                        colorKey = null,
                        sortOrder = index,
                        isArchived = false,
                        isSystem = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }

        db.appSettingDao().upsert(AppSettingEntity(KEY_SEEDED, "true"))
        db.appSettingDao().upsert(AppSettingEntity(KEY_DAY_START_MIN, "480"))
        db.appSettingDao().upsert(AppSettingEntity(KEY_WEEK_START, "monday"))
        db.appSettingDao().upsert(AppSettingEntity(KEY_TIME_FORMAT, "24h"))
        db.appSettingDao().upsert(AppSettingEntity(KEY_THEME_MODE, "system"))
        db.appSettingDao().upsert(AppSettingEntity(KEY_DURATION_UNIT, "minute"))
    }

    suspend fun seedV1_2IfNeeded() {
        if (db.appSettingDao().getByKey(KEY_SEED_V1_2) == "true") return
        val now = LocalDateTime.now()
        maybeInsert("cat_work_discussion", "讨论", "cat_work", 4, now)
        maybeInsert("cat_rest_interval",   "间隔", "cat_rest",  3, now)
        db.appSettingDao().upsert(AppSettingEntity(KEY_SEED_V1_2, "true"))
    }

    private suspend fun maybeInsert(
        id: String,
        name: String,
        parentId: String,
        sortOrder: Int,
        now: LocalDateTime
    ) {
        if (db.categoryDao().getById(id) != null) return
        db.categoryDao().insert(
            CategoryEntity(
                id = id,
                name = name,
                parentId = parentId,
                colorKey = null,
                sortOrder = sortOrder,
                isArchived = false,
                isSystem = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    companion object {
        const val KEY_SEEDED = "seeded"
        const val KEY_SEED_V1_2 = "seed_v1_2_done"
        const val KEY_DAY_START_MIN = "day_start_min"
        const val KEY_WEEK_START = "week_start"
        const val KEY_TIME_FORMAT = "time_format"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DURATION_UNIT = "duration_unit"
        const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        const val KEY_AI_CONFIRMED = "ai_confirmed"
        const val DEFAULT_DEEPSEEK_MODEL = "deepseek-v4-flash"

        // v1.3：AI prompt 可编辑 + 上下文窗口配置
        const val KEY_DEEPSEEK_PROMPT = "deepseek_prompt"
        const val KEY_AI_CONTEXT_DAY_DAYS = "ai_context_day_days"
        const val KEY_AI_CONTEXT_WEEK_DAYS = "ai_context_week_days"

        // 用户可编辑部分；{period} 占位符发送前替换为 "日"/"周"/"月"
        const val DEFAULT_DEEPSEEK_PROMPT =
            "后面的 json 是本{period}的时间统计报告，请根据这个写一个 200 字左右的小结，" +
                "包括最有价值时段，浪费多少，对明天的调整。"

        // 始终追加在用户 prompt 末尾——保底 JSON 输出格式，防解析失败
        const val JSON_FORMAT_SUFFIX =
            "必须以 JSON 返回，字段为 {\"summary\": str, \"findings\": str, \"adjust\": str}，" +
                "分别对应「主要时间投入」「时间结构问题」「下一步调整」，每段 60-80 字。"
    }
}
