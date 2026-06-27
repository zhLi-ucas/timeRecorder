package com.example.timemanager.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

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

    val listState = remember(items.firstOrNull(), headerLabel) {
        val safe = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        LazyListState(
            firstVisibleItemIndex = safe + headerOffset,
            firstVisibleItemScrollOffset = 0
        )
    }
    val density = LocalDensity.current
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentCallback by rememberUpdatedState(onSelectedChange)

    BoxWithConstraints(modifier = modifier.height(56.dp).fillMaxWidth()) {
        val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
        val sidePaddingPx = with(density) { sidePadding.toPx() }
        val fling = rememberCenterSnapFling(listState, sidePaddingPx, headerOffset)

        LaunchedEffect(items.size, selectedIndex, sidePaddingPx, headerLabel) {
            if (selectedIndex !in items.indices) return@LaunchedEffect
            val centeredReal = listState.layoutInfo.visibleItemsInfo
                .filter { it.index >= headerOffset }
                .minByOrNull { info -> abs(info.offset - sidePaddingPx) }
                ?.let { it.index - headerOffset }
            if (centeredReal != selectedIndex) {
                listState.scrollToItem(selectedIndex + headerOffset)
            }
        }

        LaunchedEffect(listState, sidePaddingPx, headerOffset) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo
                    .filter { it.index >= headerOffset }
                    .minByOrNull { info -> abs(info.offset - sidePaddingPx) }
                    ?.let { it.index - headerOffset }
            }
                .distinctUntilChanged()
                .collect { idx ->
                    if (idx != null && idx != currentSelected && idx in items.indices) {
                        currentCallback(idx)
                    }
                }
        }

        LazyRow(
            state = listState,
            flingBehavior = fling,
            contentPadding = PaddingValues(horizontal = sidePadding),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasHeader) {
                item {
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
            }
            itemsIndexed(items) { i, item ->
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
        }
    }
}

@Composable
private fun rememberCenterSnapFling(
    state: LazyListState,
    sidePaddingPx: Float,
    headerOffset: Int
): FlingBehavior {
    val decaySpec = rememberSplineBasedDecay<Float>()
    return remember(state, sidePaddingPx, decaySpec, headerOffset) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                if (initialVelocity != 0f) {
                    var lastValue = 0f
                    AnimationState(
                        initialValue = 0f,
                        initialVelocity = initialVelocity
                    ).animateDecay(decaySpec) {
                        val delta = value - lastValue
                        lastValue = value
                        scrollBy(delta)
                    }
                }
                val targetLazy = state.layoutInfo.visibleItemsInfo.minByOrNull { info ->
                    abs(info.offset - sidePaddingPx)
                }?.index
                if (targetLazy != null) {
                    val effective = if (targetLazy < headerOffset) headerOffset else targetLazy
                    state.animateScrollToItem(effective)
                }
                return 0f
            }
        }
    }
}
