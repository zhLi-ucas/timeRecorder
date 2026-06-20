package com.example.timemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timemanager.ui.components.CategoryBar
import com.example.timemanager.ui.components.CategoryColors
import com.example.timemanager.util.formatDurationLong
import com.example.timemanager.viewmodel.StatsRange
import com.example.timemanager.viewmodel.StatsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val range by viewModel.range.collectAsState()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = Modifier.fillMaxSize()) {
        StatsHeader(totalMin = uiState.totalMin)
        HorizontalDivider()
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsRange.entries.forEach { r ->
                FilterChip(
                    selected = r == range,
                    onClick = { viewModel.setRange(r) },
                    label = { Text(r.label) }
                )
            }
        }
        HorizontalDivider()

        if (uiState.groups.isEmpty()) {
            EmptyStats()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.groups, key = { it.category.id }) { group ->
                    val color = CategoryColors.colorFor(group.category.colorKey)
                    val isExpanded = expanded[group.category.id] ?: false
                    CategoryBar(
                        name = group.category.name,
                        totalMin = group.totalMin,
                        percentage = group.percentage,
                        barFraction = group.barFraction,
                        color = color,
                        isExpanded = isExpanded,
                        secondLevel = group.secondLevel,
                        onToggleExpand = {
                            expanded[group.category.id] = !isExpanded
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(totalMin: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = "统计", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "总记录 ${formatDurationLong(totalMin)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "该范围暂无记录",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
