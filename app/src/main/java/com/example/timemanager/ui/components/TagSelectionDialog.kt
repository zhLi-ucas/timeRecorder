package com.example.timemanager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.timemanager.data.Tag

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagSelectionDialog(
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (Tag, String) -> Unit,
    onAddTag: (String) -> Unit,
    onDeleteTag: (Tag) -> Unit
) {
    var selectedTagName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "开始专注",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Tag Selection
                Text(
                    text = "选择标签",
                    style = MaterialTheme.typography.titleMedium
                )

                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    items(tags) { t ->
                        val isSelected = selectedTagName == t.name
                        val containerColor = Color(t.colorArgb)

                        Surface(
                            modifier = Modifier.combinedClickable(
                                onClick = { selectedTagName = if (isSelected) "" else t.name },
                                onLongClick = { tagToDelete = t }
                            ),
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) containerColor else containerColor.copy(alpha = 0.3f),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, containerColor)
                        ) {
                            Text(
                                text = t.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else Color.Black
                            )
                        }
                    }

                    item {
                        IconButton(onClick = { showAddTagDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "添加标签")
                        }
                    }
                }

                if (selectedTagName.isBlank()) {
                    Text(
                        text = "请选择一个标签",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 50) description = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    supportingText = { Text("${description.length}/50") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val tag = tags.find { it.name == selectedTagName }
                            if (tag != null) {
                                onConfirm(tag, description)
                            }
                        },
                        enabled = selectedTagName.isNotBlank()
                    ) {
                        Text("开始")
                    }
                }
            }
        }
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("添加新标签") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("标签名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onAddTag(newTagName)
                            showAddTagDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete Tag Dialog
    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("删除标签") },
            text = { Text("确定要删除标签 \"${tagToDelete!!.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTag(tagToDelete!!)
                        if (selectedTagName == tagToDelete!!.name) {
                            selectedTagName = ""
                        }
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}
