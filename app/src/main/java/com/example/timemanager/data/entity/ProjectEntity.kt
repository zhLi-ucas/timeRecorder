package com.example.timemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
