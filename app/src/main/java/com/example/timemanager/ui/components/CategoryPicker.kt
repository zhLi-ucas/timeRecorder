package com.example.timemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timemanager.data.entity.CategoryEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(
    parents: List<CategoryEntity>,
    children: List<CategoryEntity>,
    selectedParentId: String?,
    selectedChildId: String?,
    onParentSelect: (String) -> Unit,
    onChildSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "一级分类",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            parents.forEach { parent ->
                FilterChip(
                    selected = parent.id == selectedParentId,
                    onClick = { onParentSelect(parent.id) },
                    label = { Text(parent.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = if (parent.id == selectedParentId)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = CategoryColors.colorFor(parent.colorKey)
                    )
                )
            }
        }

        if (selectedParentId != null) {
            val visibleChildren = children.filter { it.parentId == selectedParentId }
            if (visibleChildren.isNotEmpty()) {
                Text(
                    text = "二级分类",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                visibleChildren.forEach { child ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = child.id == selectedChildId,
                            onClick = { onChildSelect(child.id) }
                        )
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
