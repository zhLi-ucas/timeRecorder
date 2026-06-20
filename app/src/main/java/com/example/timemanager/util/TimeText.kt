package com.example.timemanager.util

fun formatMinOfDay(minOfDay: Int): String {
    val clamped = minOfDay.coerceIn(0, 1440)
    val h = clamped / 60
    val m = clamped % 60
    return "%02d:%02d".format(h, m)
}

fun formatDurationShort(min: Int): String = when {
    min >= 60 -> "${min / 60}h ${min % 60}m"
    else -> "${min}m"
}

fun formatDurationLong(min: Long): String {
    val h = min / 60
    val m = min % 60
    return "${h}h ${m}m"
}
