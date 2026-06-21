package com.example.timemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class NavItemData(
    val label: String,
    val icon: ImageVector,
    val key: Any
)

@Composable
fun HomeBottomBar(
    leftItems: List<NavItemData>,
    rightItems: List<NavItemData>,
    centerItem: NavItemData,
    selectedKey: Any,
    onItemClick: (NavItemData) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(80.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach { item ->
                    NavItem(
                        item = item,
                        selected = item.key == selectedKey,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.width(64.dp))
                rightItems.forEach { item ->
                    NavItem(
                        item = item,
                        selected = item.key == selectedKey,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            FloatingActionButton(
                onClick = { onItemClick(centerItem) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(56.dp)
            ) {
                Icon(centerItem.icon, contentDescription = centerItem.label)
            }
        }
    }
}

@Composable
private fun NavItem(
    item: NavItemData,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(item.icon, contentDescription = item.label, tint = color)
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
