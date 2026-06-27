package com.example.timemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timemanager.viewmodel.MonthWeekOption
import java.time.format.DateTimeFormatter

@Composable
fun MonthReviewPickerDialog(
    options: List<MonthWeekOption>,
    onToggle: (monday: java.time.LocalDate, selected: Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("MM-dd")
    val anySelected = options.any { it.selected }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("月复盘 — 选择上下文周报") },
        text = {
            Column {
                Text(
                    text = "本月涉及以下周，勾选要上传的周复盘：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(options, key = { it.monday }) { opt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = opt.selected,
                                onCheckedChange = if (opt.review != null) {
                                    { sel -> onToggle(opt.monday, sel) }
                                } else null,
                                enabled = opt.review != null
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = "${opt.monday.format(dateFmt)} ~ ${opt.sunday.format(dateFmt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (opt.review != null) "已存" else "未存",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (opt.review != null)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = anySelected) { Text("生成") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll) { Text("全选已存") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
