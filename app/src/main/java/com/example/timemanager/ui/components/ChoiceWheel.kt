package com.example.timemanager.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun <T> ChoiceWheel(
    items: List<T>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 96.dp,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(items.size, selectedIndex) {
        if (selectedIndex in items.indices) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx -> if (idx in items.indices) onSelectedChange(idx) }
    }

    Box(modifier = modifier.height(56.dp).fillMaxWidth()) {
        LazyRow(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(horizontal = itemWidth),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { i, item ->
                val distance = (i - selectedIndex).let { d -> if (d < 0) -d else d }
                Text(
                    text = label(item),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (distance == 0) selectedColor else unselectedColor,
                    modifier = Modifier
                        .size(width = itemWidth, height = 56.dp)
                        .alpha(when (distance) { 0 -> 1f; 1 -> 0.5f; else -> 0.3f })
                )
            }
        }
    }
}
