package com.example.timemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val QUICK_OPTIONS = listOf(15, 30, 60, 90, 120)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationInput(
    currentMin: Int,
    onMinChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var customText by remember(currentMin) {
        mutableStateOf(if (currentMin in QUICK_OPTIONS) "" else currentMin.toString())
    }

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
                    onClick = {
                        customText = ""
                        onMinChange(min)
                    },
                    label = { Text("${min}m") }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customText,
                onValueChange = { text ->
                    customText = text.filter { it.isDigit() }
                    val parsed = customText.toIntOrNull()
                    if (parsed != null) onMinChange(parsed)
                },
                label = { Text("自定义（分钟）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "当前：${currentMin}m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
