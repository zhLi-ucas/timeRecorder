package com.example.timemanager.data

data class Task(
    val tag: String,
    val description: String,
    val startTime: Long = System.currentTimeMillis(),
    val durationMinutes: Int, // For stopwatch, this can be 0 or ignored
    val isStopwatch: Boolean = false
) {
    val endTime: Long
        get() = if (isStopwatch) 0 else startTime + durationMinutes * 60 * 1000L
}

