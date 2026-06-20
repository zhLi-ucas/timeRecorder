package com.example.timemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String? = null,
    val colorKey: String? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val isSystem: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
