package com.example.timemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.timemanager.viewmodel.TimeEntryWithCategory
import com.example.timemanager.viewmodel.TodayLedgerViewModel
import kotlin.math.sign

@Composable
fun DayTimeline(
    entries: List<TimeEntryWithCategory>,
    viewModel: TodayLedgerViewModel,
    onTapEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        itemsIndexed(entries, key = { _, item -> item.entry.id }) { index, item ->
            val isDragging = draggingIndex == index
            TimelineBlock(
                item = item,
                isDragging = isDragging,
                dragOffsetY = if (isDragging) dragOffsetY else 0f,
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .pointerInput(item.entry.id) {
                        detectTapGestures(onTap = { onTapEntry(item.entry.id) })
                    }
                    .pointerInput(item.entry.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, dragAmount ->
                                if (draggingIndex == null) return@detectDragGesturesAfterLongPress
                                dragOffsetY += dragAmount.y
                                val activeIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                val draggedItem = listState.layoutInfo.visibleItemsInfo
                                    .find { it.index == activeIndex }
                                if (draggedItem != null) {
                                    val draggedCenter =
                                        draggedItem.offset + draggedItem.size / 2 + dragOffsetY
                                    listState.layoutInfo.visibleItemsInfo.forEach { other ->
                                        if (other.index == activeIndex) return@forEach
                                        val otherCenter = other.offset + other.size / 2
                                        val direction = (other.index - activeIndex).sign
                                        val shouldSwap =
                                            (direction < 0 && draggedCenter < otherCenter) ||
                                                (direction > 0 && draggedCenter > otherCenter)
                                        if (shouldSwap) {
                                            viewModel.reorder(activeIndex, other.index)
                                            dragOffsetY -= (other.size * direction).toFloat()
                                            draggingIndex = other.index
                                        }
                                    }
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffsetY = 0f
                            }
                        )
                    }
            )
        }
        item(key = "footer_add_interval") {
            AddIntervalBlock(onClick = { viewModel.appendInterval() })
        }
    }
}

@Composable
private fun AddIntervalBlock(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RectangleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "新增 10min 间隔",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
