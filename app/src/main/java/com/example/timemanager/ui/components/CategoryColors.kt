package com.example.timemanager.ui.components

import androidx.compose.ui.graphics.Color

object CategoryColors {
    private val palette = mapOf(
        "blue"    to Color(0xFF1976D2),
        "cyan"    to Color(0xFF0097A7),
        "green"   to Color(0xFF388E3C),
        "amber"   to Color(0xFFFFA000),
        "orange"  to Color(0xFFE64A19),
        "purple"  to Color(0xFF7B1FA2),
        "grey"    to Color(0xFF616161),
        "neutral" to Color(0xFF757575)
    )

    fun colorFor(key: String?): Color = palette[key] ?: Color(0xFF9E9E9E)
}
