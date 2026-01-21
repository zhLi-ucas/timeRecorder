package com.example.timemanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timemanager.data.TimeRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FinishTaskDialog(
    record: TimeRecord,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var description by remember { mutableStateOf(record.description) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("任务已结束") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Read-only Tag
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("标签: ", style = MaterialTheme.typography.labelLarge)
                    Text(record.tag, style = MaterialTheme.typography.bodyLarge)
                }

                // Read-only Times
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("时间: ", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${timeFormatter.format(Date(record.startTime))} - ${timeFormatter.format(Date(record.endTime))}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Duration
                val durationMins = record.durationSeconds / 60
                val durationSecs = record.durationSeconds % 60
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("时长: ", style = MaterialTheme.typography.labelLarge)
                    Text("${durationMins}分 ${durationSecs}秒", style = MaterialTheme.typography.bodyLarge)
                }

                // Editable Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(description) }
            ) {
                Text("保存记录")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("丢弃")
            }
        }
    )
}
