package com.example.timemanager.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import com.example.timemanager.viewmodel.TimeEntryWithCategory
import com.example.timemanager.viewmodel.TodayLedgerViewModel
import kotlin.math.sign

@Composable
fun DayTimeline(
    entries: List<TimeEntryWithCategory>,
    viewModel: TodayLedgerViewModel,
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
    }
}
