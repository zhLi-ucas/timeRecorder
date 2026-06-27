package com.example.timemanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.timemanager.data.entity.ReviewEntity
import com.example.timemanager.viewmodel.AiState
import com.example.timemanager.viewmodel.ReviewPeriod
import com.example.timemanager.viewmodel.ReviewViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val period by viewModel.periodType.collectAsState()
    val form by viewModel.form.collectAsState()
    val history by viewModel.history.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val aiConfirmed by viewModel.aiConfirmed.collectAsState()

    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(aiState) {
        (aiState as? AiState.Error)?.let {
            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            viewModel.resetAiState()
        }
    }

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

            OutlinedButton(
                onClick = {
                    if (aiConfirmed) viewModel.generateWithAi()
                    else showPrivacyDialog = true
                },
                enabled = aiState !is AiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (aiState is AiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成中…")
                } else {
                    Text("AI 生成草稿")
                }
            }

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

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("隐私确认") },
            text = {
                Text(
                    "将上传本${period.label}（${form.periodStart} ~ ${form.periodEnd}）" +
                        "的时间记录到 DeepSeek 服务器处理，包含分类、时长、备注、效能数据。" +
                        "API key 明文存储于本机数据库。请确认后继续。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPrivacyDialog = false
                    viewModel.setAiConfirmed()
                    viewModel.generateWithAi()
                }) { Text("确认并生成") }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("取消") }
            }
        )
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
