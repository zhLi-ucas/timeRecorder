package com.example.timemanager.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.timemanager.util.formatDurationShort
import com.example.timemanager.util.formatMinOfDay
import com.example.timemanager.viewmodel.TimeEntryWithCategory

@Composable
fun TimelineBlock(
    item: TimeEntryWithCategory,
    isDragging: Boolean,
    dragOffsetY: Float,
    modifier: Modifier = Modifier
) {
    val baseColor = CategoryColors.colorFor(item.parent?.colorKey)
    val effAlpha = 0.5f + (item.entry.effectiveness.coerceIn(0, 100) / 100f) * 0.5f
    val color = if (isDragging) baseColor.copy(alpha = 0.98f) else baseColor.copy(alpha = effAlpha)
    val onBlockColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    val dragLayer = if (isDragging) {
        Modifier.graphicsLayer {
            translationY = dragOffsetY
            shadowElevation = 12f
        }
    } else Modifier

    Surface(
        color = color,
        shape = RectangleShape,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .then(dragLayer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${formatMinOfDay(item.entry.startMinOfDay)} – ${formatMinOfDay(item.entry.startMinOfDay + item.entry.durationMin)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBlockColor.copy(alpha = 0.85f)
                )
                Text(
                    text = item.entry.title.ifBlank { item.category?.name ?: "未命名" },
                    style = MaterialTheme.typography.titleMedium,
                    color = onBlockColor
                )
                Text(
                    text = listOfNotNull(item.parent?.name, item.category?.name)
                        .joinToString(" / "),
                    style = MaterialTheme.typography.labelSmall,
                    color = onBlockColor.copy(alpha = 0.75f)
                )
            }
            Text(
                text = formatDurationShort(item.entry.durationMin),
                style = MaterialTheme.typography.titleLarge,
                color = onBlockColor
            )
        }
    }
}
