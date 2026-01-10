package com.example.timemanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun ReminderButton(
    type: ReminderType,
    progress: Float, // 0.0 to 1.0 (1.0 = Full, 0.0 = Empty)
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        ReminderType.WATER -> Icons.Filled.Info // Fallback to Info as LocalCafe might be missing
        ReminderType.STAND -> Icons.Filled.Face // Fallback to Face as AccessibilityNew might be missing
    }
    
    val fillColor = when (type) {
        ReminderType.WATER -> MaterialTheme.colorScheme.primary
        ReminderType.STAND -> MaterialTheme.colorScheme.tertiary
    }

    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val iconColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            
            // Draw Background (Empty Container)
            drawCircle(
                color = emptyColor,
                radius = width / 2
            )
            
            // Draw Progress (Fill)
            // We want the fill to go from bottom to top based on progress
            val fillHeight = height * progress.coerceIn(0f, 1f)
            val path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(0f, 0f, width, height))
            }

            clipPath(path) {
                 drawRect(
                    color = fillColor,
                    topLeft = Offset(0f, height - fillHeight),
                    size = Size(width, fillHeight)
                )
            }
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (progress > 0.5f) iconColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}

enum class ReminderType {
    WATER,
    STAND
}
