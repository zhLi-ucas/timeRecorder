package com.example.timemanager.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.data.Tag
import com.example.timemanager.ui.components.ReminderType
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
    val waterInterval by actualViewModel.waterInterval.collectAsState()
    val standInterval by actualViewModel.standInterval.collectAsState()
    
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showEditReminderDialog by remember { mutableStateOf<ReminderType?>(null) }
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Tag Management ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "标签管理",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { showAddTagDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加标签")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(tags) { tag ->
                TagItem(
                    tag = tag,
                    onDelete = { actualViewModel.deleteTag(tag) },
                    onColorChange = { showColorPicker = tag },
                    onShowOnHomeChange = { isChecked ->
                        actualViewModel.updateTag(tag.copy(showOnHome = isChecked))
                    }
                )
            }
            
            // --- Reminder Settings ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "提醒设置",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Water Reminder
                ReminderSettingItem(
                    title = "喝水提醒间隔",
                    value = "${waterInterval} 分钟",
                    onClick = { showEditReminderDialog = ReminderType.WATER }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stand Reminder
                ReminderSettingItem(
                    title = "久坐起身提醒间隔",
                    value = "${standInterval} 分钟",
                    onClick = { showEditReminderDialog = ReminderType.STAND }
                )
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
    
    // Edit Reminder Dialog
    showEditReminderDialog?.let { type ->
        var intervalText by remember { mutableStateOf(
            when (type) {
                ReminderType.WATER -> waterInterval.toString()
                ReminderType.STAND -> standInterval.toString()
            }
        )}
        
        AlertDialog(
            onDismissRequest = { showEditReminderDialog = null },
            title = { Text(if (type == ReminderType.WATER) "设置喝水间隔" else "设置久坐间隔") },
            text = {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) intervalText = it },
                    label = { Text("分钟") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = intervalText.toIntOrNull()
                        if (minutes != null && minutes > 0) {
                            actualViewModel.setReminderInterval(type, minutes)
                            showEditReminderDialog = null
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditReminderDialog = null }) {
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
    onColorChange: () -> Unit,
    onShowOnHomeChange: (Boolean) -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "首页展示",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = tag.showOnHome,
                    onCheckedChange = onShowOnHomeChange,
                    modifier = Modifier.scale(0.7f)
                )
                
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
}

@Composable
fun ReminderSettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
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
