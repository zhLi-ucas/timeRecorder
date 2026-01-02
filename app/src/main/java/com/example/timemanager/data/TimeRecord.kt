package com.example.timemanager.data

import java.util.UUID

data class TimeRecord(
    val id: String = UUID.randomUUID().toString(),
    val tag: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val description: String
)
