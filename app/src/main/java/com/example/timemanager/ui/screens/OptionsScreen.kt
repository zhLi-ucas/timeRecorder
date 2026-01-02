package com.example.timemanager.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.data.Tag
import com.example.timemanager.viewmodel.TimerViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    viewModel: TimerViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val actualViewModel: TimerViewModel = viewModel ?: viewModel(
        viewModelStoreOwner = context.applicationContext as androidx.lifecycle.ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val tags by actualViewModel.tags.collectAsState()
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选项") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTagDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加标签")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "标签管理",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(tags) { tag ->
                TagItem(
                    tag = tag,
                    onDelete = { actualViewModel.deleteTag(tag) },
                    onColorChange = { showColorPicker = tag }
                )
            }
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
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    showColorPicker?.let { tag ->
        ColorPickerDialog(
            initialColor = tag.colorArgb,
            onColorSelected = { newColor ->
                actualViewModel.updateTag(tag.copy(colorArgb = newColor))
                showColorPicker = null
            },
            onDismiss = { showColorPicker = null }
        )
    }
}

@Composable
fun TagItem(
    tag: Tag,
    onDelete: () -> Unit,
    onColorChange: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color Indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(tag.colorArgb), shape = MaterialTheme.shapes.small)
                        .clickable(onClick = onColorChange)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除标签",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Simple color picker with random generation for now, as implementing a full color picker is complex
    // Or we can provide a set of preset colors.
    val presetColors = listOf(
        0xFFFF5252, 0xFFFF4081, 0xFFE040FB, 0xFF7C4DFF, 0xFF536DFE,
        0xFF448AFF, 0xFF40C4FF, 0xFF18FFFF, 0xFF64FFDA, 0xFF69F0AE,
        0xFFB2FF59, 0xFFEEFF41, 0xFFFFFF00, 0xFFFFD740, 0xFFFFAB40,
        0xFFFF6E40
    ).map { it.toInt() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Random Color Button
                    Button(onClick = {
                         val red = Random.nextInt(256)
                         val green = Random.nextInt(256)
                         val blue = Random.nextInt(256)
                         val randomColor = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                         onColorSelected(randomColor)
                    }) {
                        Text("随机颜色")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Preset Colors Grid (Simplified)
                Column {
                   presetColors.chunked(4).forEach { rowColors ->
                       Row(
                           modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                           horizontalArrangement = Arrangement.SpaceEvenly
                       ) {
                           rowColors.forEach { colorInt ->
                               Box(
                                   modifier = Modifier
                                       .size(40.dp)
                                       .background(Color(colorInt), shape = MaterialTheme.shapes.medium)
                                       .clickable { onColorSelected(colorInt) }
                               )
                           }
                       }
                   }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
