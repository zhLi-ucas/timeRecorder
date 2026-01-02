package com.example.timemanager.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.ui.activities.AmbientDisplayActivity
import com.example.timemanager.TimeManagerApplication
import com.example.timemanager.data.Tag
import com.example.timemanager.data.Task
import com.example.timemanager.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimerSetupScreen(
    viewModel: TimerViewModel? = null
) {
    val context = LocalContext.current
    val actualViewModel: TimerViewModel = viewModel(
        viewModelStoreOwner = context.applicationContext as androidx.lifecycle.ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )
    
    val tags by actualViewModel.tags.collectAsState()
    val durations by actualViewModel.durations.collectAsState()
    
    var tag by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf<Int?>(null) }
    var isStopwatchMode by remember { mutableStateOf(false) }
    
    var showAddTagDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "时间管理",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Mode Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = !isStopwatchMode,
                onClick = { isStopwatchMode = false },
                label = { Text("倒计时") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = isStopwatchMode,
                onClick = { isStopwatchMode = true },
                label = { Text("正计时") }
            )
        }

        // 标签选择区
        Text(
            text = "选择标签",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tags) { t ->
                val isSelected = tag == t.name
                val containerColor = Color(t.colorArgb)
                
                Surface(
                    onClick = { tag = if (isSelected) "" else t.name },
                    modifier = Modifier.combinedClickable(
                        onClick = { tag = if (isSelected) "" else t.name },
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
        
        if (tag.isBlank()) {
            Text(
                text = "请选择一个标签",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
             Text(
                text = "当前标签: $tag",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("描述") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        if (!isStopwatchMode) {
            Text(
                text = "选择时长",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(durations) { duration ->
                    val isSelected = selectedDuration == duration
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDuration = if (isSelected) null else duration },
                        label = { Text(formatDuration(duration)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (tag.isNotBlank() && (isStopwatchMode || selectedDuration != null)) {
                    val task = Task(
                        tag = tag,
                        description = description,
                        durationMinutes = if (isStopwatchMode) 0 else selectedDuration!!,
                        isStopwatch = isStopwatchMode
                    )
                    actualViewModel.startTimer(task)
                    
                    val intent = Intent(context, AmbientDisplayActivity::class.java)
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = tag.isNotBlank() && (isStopwatchMode || selectedDuration != null)
        ) {
            Text("开始", style = MaterialTheme.typography.titleLarge)
        }
    }

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
                            actualViewModel.addTag(newTagName)
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

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("删除标签") },
            text = { Text("确定要删除标签 \"${tagToDelete!!.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        actualViewModel.deleteTag(tagToDelete!!)
                        if (tag == tagToDelete!!.name) {
                            tag = ""
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

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}分钟"
        minutes == 60 -> "1小时"
        else -> "${minutes / 60}.${(minutes % 60) / 10}小时"
    }
}

