package com.example.timemanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.timemanager.ui.components.CategoryColors
import com.example.timemanager.ui.components.ChoiceWheel
import com.example.timemanager.viewmodel.RecordUiEvent
import com.example.timemanager.viewmodel.RecordViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DURATION_ITEMS = (0..240 step 5).toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: RecordViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val form by viewModel.form.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RecordUiEvent.Saved -> onDone()
                is RecordUiEvent.Toast ->
                    Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val parents = categories.filter { it.parentId == null && !it.isArchived }
        .sortedBy { it.sortOrder }
    val children = categories.filter { it.parentId != null && !it.isArchived }
    val visibleChildren = form.parentCategoryId?.let { pid ->
        children.filter { it.parentId == pid }
    } ?: emptyList()

    LaunchedEffect(parents) {
        if (parents.isNotEmpty() && form.parentCategoryId == null) {
            viewModel.selectParent(parents[0].id)
        }
    }

    LaunchedEffect(visibleChildren, form.categoryId) {
        if (visibleChildren.isNotEmpty() && visibleChildren.none { it.id == form.categoryId }) {
            viewModel.selectCategory(visibleChildren[0].id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditing) "编辑" else "记一笔") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DatePickerRow(date = form.date, onDateChange = viewModel::setDate)

            ChoiceWheel(
                items = parents,
                selectedIndex = parents.indexOfFirst { it.id == form.parentCategoryId }.coerceAtLeast(0),
                onSelectedChange = { i -> viewModel.selectParent(parents[i].id) },
                label = { it.name },
                headerLabel = "一级分类",
                selectedColor = parents.getOrNull(
                    parents.indexOfFirst { it.id == form.parentCategoryId }.coerceAtLeast(0)
                )?.colorKey?.let {
                    CategoryColors.colorFor(it)
                } ?: MaterialTheme.colorScheme.primary
            )

            if (visibleChildren.isNotEmpty()) {
                val selectedIdx = visibleChildren.indexOfFirst { it.id == form.categoryId }
                val safeIdx = if (selectedIdx < 0) 0 else selectedIdx
                ChoiceWheel(
                    items = visibleChildren,
                    selectedIndex = safeIdx,
                    onSelectedChange = { i -> viewModel.selectCategory(visibleChildren[i].id) },
                    label = { it.name },
                    headerLabel = "二级分类"
                )
            }

            ChoiceWheel(
                items = DURATION_ITEMS,
                selectedIndex = DURATION_ITEMS.indexOf(form.durationMin).coerceAtLeast(0),
                onSelectedChange = { i -> viewModel.setDuration(DURATION_ITEMS[i]) },
                label = { "${it}m" }
            )

            EffectivenessRow(
                effectiveness = form.effectiveness,
                onEffectivenessChange = viewModel::setEffectiveness
            )

            val keyboard = LocalSoftwareKeyboardController.current
            OutlinedTextField(
                value = form.note,
                onValueChange = viewModel::setNote,
                label = { Text("备注（可选）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (form.isEditing) "保存修改" else "保存")
            }
        }
    }
}

@Composable
private fun EffectivenessRow(
    effectiveness: Int,
    onEffectivenessChange: (Int) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "有效度",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$effectiveness%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = effectiveness.toFloat(),
            onValueChange = { onEffectivenessChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 19,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerRow(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd E", Locale.CHINA)

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(date.format(formatter))
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val todayUtcMillis = LocalDate.now()
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                    return utcTimeMillis <= todayUtcMillis
                }
            }
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { ms ->
                            onDateChange(
                                Instant.ofEpochMilli(ms)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                        showPicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
