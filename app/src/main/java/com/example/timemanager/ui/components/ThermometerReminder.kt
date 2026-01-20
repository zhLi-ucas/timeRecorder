package com.example.timemanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ThermometerReminder(
    type: ReminderType,
    progress: Float, // 1.0 = Full (Fresh), 0.0 = Empty (Expired)
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        ReminderType.WATER -> Icons.Filled.Info
        ReminderType.STAND -> Icons.Filled.Face
    }

    // Color logic: Full = Primary, Empty = Error/Gray
    // "Effect: Color fades over time"
    val fullColor = when (type) {
        ReminderType.WATER -> MaterialTheme.colorScheme.primary
        ReminderType.STAND -> MaterialTheme.colorScheme.tertiary
    }
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    // Interpolate color based on progress
    val currentColor = Color(
        red = fullColor.red * progress + emptyColor.red * (1 - progress),
        green = fullColor.green * progress + emptyColor.green * (1 - progress),
        blue = fullColor.blue * progress + emptyColor.blue * (1 - progress),
        alpha = 1f
    )

    val animatedColor by animateColorAsState(targetValue = currentColor, label = "color")
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Box(
        modifier = modifier
            .width(40.dp)
            .height(120.dp) // Long bar
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val cornerRadius = width / 2

            // Draw Background (Track)
            drawRoundRect(
                color = emptyColor.copy(alpha = 0.3f),
                size = size,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // Draw Fill (Thermometer liquid)
            val fillHeight = height * animatedProgress.coerceIn(0.1f, 1f) // Keep a little bit at bottom
            drawRoundRect(
                color = animatedColor,
                topLeft = Offset(0f, height - fillHeight),
                size = Size(width, fillHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }

        // Icon at the bottom
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(24.dp)
        )
    }
}
