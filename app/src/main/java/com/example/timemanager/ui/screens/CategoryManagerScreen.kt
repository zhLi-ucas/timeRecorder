package com.example.timemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timemanager.data.entity.CategoryEntity
import com.example.timemanager.viewmodel.SettingsViewModel

private val COLOR_KEYS = listOf("blue", "cyan", "green", "amber", "orange", "purple", "grey", "neutral")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val firstLevel = categories.filter { it.parentId == null }.sortedBy { it.sortOrder }
    val archived = firstLevel.filter { it.isArchived }

    var showAddFirst by remember { mutableStateOf(false) }
    var showAddSecondFor by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddFirst = true }) {
                        Icon(Icons.Filled.Add, "新增一级")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            items(firstLevel.filterNot { it.isArchived }, key = { it.id }) { parent ->
                FirstLevelSection(
                    parent = parent,
                    children = categories
                        .filter { it.parentId == parent.id }
                        .sortedBy { it.sortOrder },
                    onEdit = { editing = it },
                    onArchive = { viewModel.archiveCategory(it.id, true) },
                    onUnarchive = { viewModel.archiveCategory(it.id, false) },
                    onDelete = { viewModel.deleteCategory(it.id) },
                    onAddChild = { showAddSecondFor = parent.id }
                )
                HorizontalDivider()
            }
            if (archived.isNotEmpty()) {
                item {
                    Text(
                        text = "已归档",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(archived, key = { it.id }) { parent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(parent.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.archiveCategory(parent.id, false) }) {
                            Icon(Icons.Filled.Unarchive, "取消归档")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddFirst) {
        AddFirstLevelDialog(
            onDismiss = { showAddFirst = false },
            onConfirm = { name, color ->
                viewModel.addFirstLevel(name, color)
                showAddFirst = false
            }
        )
    }
    showAddSecondFor?.let { parentId ->
        AddSecondLevelDialog(
            onDismiss = { showAddSecondFor = null },
            onConfirm = { name ->
                viewModel.addSecondLevel(parentId, name)
                showAddSecondFor = null
            }
        )
    }
    editing?.let { cat ->
        EditCategoryDialog(
            category = cat,
            onDismiss = { editing = null },
            onConfirm = { newName, newColor ->
                viewModel.renameCategory(cat.id, newName)
                if (cat.parentId == null) {
                    viewModel.setCategoryColor(cat.id, newColor)
                }
                editing = null
            }
        )
    }
}

@Composable
private fun FirstLevelSection(
    parent: CategoryEntity,
    children: List<CategoryEntity>,
    onEdit: (CategoryEntity) -> Unit,
    onArchive: (CategoryEntity) -> Unit,
    onUnarchive: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onAddChild: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = parent.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onEdit(parent) }) { Icon(Icons.Filled.Edit, "编辑") }
            IconButton(onClick = { onArchive(parent) }) { Icon(Icons.Filled.Archive, "归档") }
            if (!parent.isSystem) {
                IconButton(onClick = { onDelete(parent) }) { Icon(Icons.Filled.Delete, "删除") }
            }
        }
        children.forEach { child ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "· ${child.name}", modifier = Modifier.weight(1f))
                IconButton(onClick = { onEdit(child) }) { Icon(Icons.Filled.Edit, "编辑") }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onAddChild) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.padding(end = 4.dp))
                Text("二级")
            }
        }
    }
}

@Composable
private fun AddFirstLevelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(COLOR_KEYS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增一级分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true
                )
                Text("配色：$color", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    COLOR_KEYS.forEach { k ->
                        TextButton(onClick = { color = k }) {
                            Text(k, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AddSecondLevelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增二级分类") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var color by remember { mutableStateOf(category.colorKey ?: COLOR_KEYS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true
                )
                if (category.parentId == null) {
                    Text("配色：$color", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        COLOR_KEYS.forEach { k ->
                            TextButton(onClick = { color = k }) {
                                Text(k, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
