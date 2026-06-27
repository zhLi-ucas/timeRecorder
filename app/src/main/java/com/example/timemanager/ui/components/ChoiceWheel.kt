package com.example.timemanager.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt

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
    headerLabel: String? = null,
) {
    val hasHeader = headerLabel != null
    val headerOffset = if (hasHeader) 1 else 0

    val stateKey = items.firstOrNull() to items.size
    val scrollState = remember(stateKey, headerLabel) { ScrollState(initial = 0) }
    val density = LocalDensity.current
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentCallback by rememberUpdatedState(onSelectedChange)
    val currentItemCount by rememberUpdatedState(items.size)
    var userScrollPending by remember(stateKey, headerLabel) { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.height(56.dp).fillMaxWidth()) {
        val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
        val itemWidthPx = with(density) { itemWidth.toPx() }

        LaunchedEffect(items.size, selectedIndex, itemWidthPx, headerLabel) {
            if (selectedIndex !in items.indices) return@LaunchedEffect
            if (itemWidthPx <= 0f) return@LaunchedEffect
            val target = ((selectedIndex + headerOffset) * itemWidthPx).roundToInt()
            if (scrollState.value != target) {
                scrollState.scrollTo(target)
            }
        }

        LaunchedEffect(scrollState, itemWidthPx, headerOffset) {
            snapshotFlow {
                scrollState.isScrollInProgress
            }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    if (!isScrolling && userScrollPending && itemWidthPx > 0f) {
                        userScrollPending = false
                        if (currentItemCount == 0) return@collect
                        val lazyIndex = (scrollState.value / itemWidthPx)
                            .roundToInt()
                            .coerceIn(headerOffset, headerOffset + currentItemCount - 1)
                        scrollState.animateScrollTo((lazyIndex * itemWidthPx).roundToInt())
                        val centered = lazyIndex - headerOffset
                        if (centered != currentSelected && centered in 0 until currentItemCount) {
                            currentCallback(centered)
                        }
                    }
                }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        userScrollPending = true
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        } while (event.changes.any { it.pressed })
                    }
                }
                .horizontalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.size(width = sidePadding, height = 56.dp))
            if (hasHeader) {
                Text(
                    text = headerLabel!!,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(width = itemWidth, height = 56.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .alpha(0.4f)
                )
            }
            items.forEachIndexed { i, item ->
                val distance = abs(i - selectedIndex)
                Text(
                    text = label(item),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (distance == 0) selectedColor else unselectedColor,
                    modifier = Modifier
                        .size(width = itemWidth, height = 56.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .alpha(when (distance) { 0 -> 1f; 1 -> 0.5f; else -> 0.3f })
                )
            }
            Spacer(modifier = Modifier.size(width = sidePadding, height = 56.dp))
        }
    }
}
