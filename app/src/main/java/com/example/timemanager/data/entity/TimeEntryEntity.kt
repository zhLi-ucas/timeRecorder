package com.example.timemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "time_entries")
data class TimeEntryEntity(
    @PrimaryKey val id: String,
    val date: java.time.LocalDate,
    val startMinOfDay: Int,
    val durationMin: Int,
    val title: String,
    val categoryId: String,
    val projectId: String? = null,
    val note: String? = null,
    val isEstimated: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
