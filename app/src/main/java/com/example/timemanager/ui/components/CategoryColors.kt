package com.example.timemanager.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CategoryColors {
    private val lightPalette = mapOf(
        "blue"    to Color(0xFF90CAF9),
        "cyan"    to Color(0xFF80DEEA),
        "green"   to Color(0xFFA5D6A7),
        "amber"   to Color(0xFFFFD54F),
        "orange"  to Color(0xFFFFAB91),
        "purple"  to Color(0xFFCE93D8),
        "grey"    to Color(0xFFEEEEEE),
        "neutral" to Color(0xFFBDBDBD)
    )

    private val darkPalette = mapOf(
        "blue"    to Color(0xFF1976D2),
        "cyan"    to Color(0xFF0097A7),
        "green"   to Color(0xFF388E3C),
        "amber"   to Color(0xFFFFA000),
        "orange"  to Color(0xFFE64A19),
        "purple"  to Color(0xFF7B1FA2),
        "grey"    to Color(0xFF616161),
        "neutral" to Color(0xFF757575)
    )

    @Composable
    fun colorFor(key: String?): Color {
        val palette = if (isSystemInDarkTheme()) darkPalette else lightPalette
        return palette[key] ?: Color(0xFF9E9E9E)
    }
}
