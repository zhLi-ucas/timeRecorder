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
            "cat_work" to listOf("实验" to "experiment", "阅读" to "reading", "写论文" to "paper", "思考" to "thinking"),
            "cat_rest" to listOf("放空" to "space", "游戏" to "game", "阅读" to "reading"),
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

    companion object {
        const val KEY_SEEDED = "seeded"
        const val KEY_DAY_START_MIN = "day_start_min"
        const val KEY_WEEK_START = "week_start"
        const val KEY_TIME_FORMAT = "time_format"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DURATION_UNIT = "duration_unit"
    }
}
