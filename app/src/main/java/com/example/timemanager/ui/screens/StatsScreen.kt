package com.example.timemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timemanager.ui.components.CategoryBar
import com.example.timemanager.ui.components.CategoryColors
import com.example.timemanager.ui.components.ReviewSection
import com.example.timemanager.util.DateRange
import com.example.timemanager.util.formatDurationLong
import com.example.timemanager.viewmodel.StatsRange
import com.example.timemanager.viewmodel.StatsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val range by viewModel.range.collectAsState()
    val anchors by viewModel.validAnchors.collectAsState()
    val anchorDate by viewModel.anchorDate.collectAsState()
    val currentReview by viewModel.currentReview.collectAsState()
    val selectedIdx by viewModel.selectedIdx.collectAsState()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    val periodText = if (anchors.isEmpty()) "" else formatPeriod(range, anchorDate)

    Column(modifier = Modifier.fillMaxSize()) {
        StatsHeader(totalMin = uiState.totalMin, periodText = periodText)
        HorizontalDivider()
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsRange.entries.forEach { r ->
                FilterChip(
                    selected = r == range,
                    onClick = { viewModel.setRange(r) },
                    label = { Text(r.label) }
                )
            }
        }
        HorizontalDivider()

        if (anchors.isEmpty()) {
            EmptyStats()
        } else {
            val pagerState = rememberPagerState(
                initialPage = selectedIdx.coerceAtMost(anchors.size - 1),
                pageCount = { anchors.size }
            )

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        if (page != viewModel.selectedIdx.value) viewModel.selectPage(page)
                    }
            }
            LaunchedEffect(viewModel) {
                snapshotFlow { viewModel.selectedIdx.value }
                    .distinctUntilChanged()
                    .collect { idx ->
                        val size = viewModel.validAnchors.value.size
                        if (idx in 0 until size && pagerState.currentPage != idx) {
                            pagerState.animateScrollToPage(idx)
                        }
                    }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageAnchor = anchors[page]
                if (pageAnchor != anchorDate) {
                    Spacer(Modifier.fillMaxSize())
                } else if (uiState.groups.isEmpty()) {
                    EmptyStats()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.groups, key = { it.category.id }) { group ->
                            val color = CategoryColors.colorFor(group.category.colorKey)
                            val isExpanded = expanded[group.category.id] ?: false
                            CategoryBar(
                                name = group.category.name,
                                totalMin = group.totalMin,
                                percentage = group.percentage,
                                barFraction = group.barFraction,
                                color = color,
                                isExpanded = isExpanded,
                                secondLevel = group.secondLevel,
                                onToggleExpand = {
                                    expanded[group.category.id] = !isExpanded
                                }
                            )
                        }
                        if (range != StatsRange.YEAR) {
                            item(key = "review_section") {
                                HorizontalDivider()
                                ReviewSection(
                                    review = currentReview,
                                    periodLabel = periodLabelFor(range)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(totalMin: Long, periodText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(text = "统计", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "总记录 ${formatDurationLong(totalMin)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (periodText.isNotEmpty()) {
            Text(
                text = periodText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyStats() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "该范围暂无记录",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun periodLabelFor(range: StatsRange): String = when (range) {
    StatsRange.TODAY -> "今日复盘"
    StatsRange.WEEK -> "本周复盘"
    StatsRange.MONTH -> "本月复盘"
    StatsRange.YEAR -> "年度复盘"
}

private fun formatPeriod(range: StatsRange, anchor: LocalDate): String {
    val (from, to) = DateRange.statsRange(range, anchor)
    val sameYear = from.year == to.year
    val thisYear = LocalDate.now().year
    val showYear = !sameYear || from.year != thisYear

    val yearFmt = DateTimeFormatter.ofPattern("yy-MM-dd")
    val mdFmt = DateTimeFormatter.ofPattern("MM-dd")

    return when (range) {
        StatsRange.TODAY ->
            if (showYear) from.format(yearFmt) else from.format(mdFmt)
        StatsRange.WEEK -> {
            val f = if (showYear) from.format(yearFmt) else from.format(mdFmt)
            val t = if (showYear || from.year != to.year) to.format(yearFmt) else to.format(mdFmt)
            "$f ~ $t"
        }
        StatsRange.MONTH ->
            if (showYear) from.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            else from.format(DateTimeFormatter.ofPattern("MM"))
        StatsRange.YEAR -> from.year.toString()
    }
}
