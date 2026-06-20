package com.example.timemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timemanager.data.entity.ReviewEntity
import com.example.timemanager.viewmodel.ReviewPeriod
import com.example.timemanager.viewmodel.ReviewViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val period by viewModel.periodType.collectAsState()
    val form by viewModel.form.collectAsState()
    val history by viewModel.history.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ReviewHeader(
            periodLabel = period.label,
            rangeText = "${form.periodStart} ~ ${form.periodEnd}"
        )
        HorizontalDivider()
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReviewPeriod.entries.forEach { p ->
                FilterChip(
                    selected = p == period,
                    onClick = { viewModel.selectPeriod(p) },
                    label = { Text(p.label) }
                )
            }
        }
        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.summaryText,
                onValueChange = viewModel::setSummary,
                label = { Text(period.field1) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = form.mainFindings,
                onValueChange = viewModel::setFindings,
                label = { Text(period.field2) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = form.adjustmentPlan,
                onValueChange = viewModel::setAdjust,
                label = { Text(period.field3) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (form.isEditing) "更新本${period.label}复盘" else "保存本${period.label}复盘")
            }
        }

        HorizontalDivider()
        Text(
            text = "历史复盘（${period.label}）",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (history.isEmpty()) {
            Text(
                text = "还没有历史复盘",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { review ->
                    HistoryRow(
                        review = review,
                        onClick = {
                            viewModel.selectPeriod(
                                ReviewPeriod.valueOf(review.periodType)
                            )
                            viewModel.loadPeriod(review.periodStart, review.periodEnd)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ReviewHeader(periodLabel: String, rangeText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = "复盘", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "本$periodLabel  $rangeText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryRow(review: ReviewEntity, onClick: () -> Unit) {
    val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp)) {
            Text(
                text = "${review.periodStart.format(dateFmt)} ~ ${review.periodEnd.format(dateFmt)}",
                style = MaterialTheme.typography.bodyLarge
            )
            val preview = review.summaryText
                ?.takeIf { it.isNotBlank() }
                ?.take(60)
                ?: "(无内容)"
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onClick) { Text("打开") }
    }
}
