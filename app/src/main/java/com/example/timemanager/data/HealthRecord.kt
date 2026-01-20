package com.example.timemanager.data

import java.util.UUID

data class HealthRecord(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "WATER" or "STAND"
    val timestamp: Long
)
