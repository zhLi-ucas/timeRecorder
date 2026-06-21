package com.example.timemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val QUICK_OPTIONS = listOf(30, 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationInput(
    currentMin: Int,
    onMinChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val isQuick = currentMin in QUICK_OPTIONS
    val isCustom = !isQuick

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "时长",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QUICK_OPTIONS.forEach { min ->
                FilterChip(
                    selected = currentMin == min,
                    onClick = { onMinChange(min) },
                    label = { Text("${min}m") }
                )
            }
            FilterChip(
                selected = isCustom,
                onClick = { showSheet = true },
                label = { Text("自定义") }
            )
        }
        Text(
            text = "当前：${formatDuration(currentMin)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    if (showSheet) {
        DurationPickerSheet(
            initialMinutes = if (isCustom) currentMin else 0,
            onConfirm = onMinChange,
            onDismiss = { showSheet = false }
        )
    }
}

private fun formatDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
