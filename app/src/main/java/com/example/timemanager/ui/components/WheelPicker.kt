package com.example.timemanager.ui.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleCount: Int = 5
) {
    val listState = rememberLazyListState()
    val halfVisible = visibleCount / 2
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(items.size, selectedIndex) {
        if (selectedIndex in items.indices) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index in items.indices) onSelectedChange(index)
            }
    }

    Box(modifier = modifier.height(itemHeight * visibleCount)) {
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items.size) { i ->
                val distance = (i - selectedIndex).let { d -> if (d < 0) -d else d }
                Text(
                    text = items[i],
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (distance == 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.CenterVertically)
                        .alpha(when (distance) { 0 -> 1f; 1 -> 0.7f; else -> 0.4f })
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -itemHeight / 2),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = itemHeight / 2),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
