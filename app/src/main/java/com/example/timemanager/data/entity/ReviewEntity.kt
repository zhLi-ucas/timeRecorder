package com.example.timemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val periodType: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val summaryText: String? = null,
    val mainFindings: String? = null,
    val adjustmentPlan: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
