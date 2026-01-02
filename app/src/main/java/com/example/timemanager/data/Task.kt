package com.example.timemanager.data

import java.util.Date

data class Task(
    val tag: String,
    val description: String,
    val startTime: Long = System.currentTimeMillis(),
    val durationMinutes: Int
) {
    val endTime: Long
        get() = startTime + durationMinutes * 60 * 1000L
}

