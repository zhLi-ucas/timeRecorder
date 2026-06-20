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
            Triple("cat_core_work", "核心工作", "blue"),
            Triple("cat_aux_work", "辅助工作", "cyan"),
            Triple("cat_study", "学习研究", "green"),
            Triple("cat_daily", "日常事务", "amber"),
            Triple("cat_rest", "休息恢复", "orange"),
            Triple("cat_social", "社交沟通", "purple"),
            Triple("cat_invalid", "无效消耗", "grey"),
            Triple("cat_uncategorized", "未分类", "neutral")
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
                    isSystem = id == "cat_uncategorized",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val secondLevel = listOf(
            "cat_core_work" to listOf("写作" to "writing", "编程" to "coding", "实验" to "experiment", "设计" to "design", "深度思考" to "thinking"),
            "cat_aux_work" to listOf("资料整理" to "organizing", "文件管理" to "files", "环境配置" to "env", "沟通协调" to "comm", "行政事务" to "admin"),
            "cat_study" to listOf("阅读" to "reading", "查资料" to "lookup", "做笔记" to "notes", "听课" to "class", "复习" to "review"),
            "cat_daily" to listOf("家务" to "chores", "通勤" to "commute", "购物" to "shopping", "用餐" to "meal", "生活处理" to "life"),
            "cat_rest" to listOf("睡眠" to "sleep", "午休" to "noon", "散步" to "walk", "娱乐" to "fun", "运动" to "exercise"),
            "cat_social" to listOf("聊天" to "chat", "会议" to "meeting", "电话" to "call", "线上交流" to "online"),
            "cat_invalid" to listOf("无目标浏览" to "browse", "被动刷视频" to "video", "无效等待" to "wait", "反复切换" to "switch"),
            "cat_uncategorized" to listOf("待整理" to "pending")
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
