package com.example.timemanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.timemanager.ui.components.DayTimeline
import com.example.timemanager.util.formatDurationLong
import com.example.timemanager.viewmodel.TodayLedgerViewModel
import com.example.timemanager.viewmodel.TodayUiEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PIVOT = 100_000

private fun pageToDate(page: Int, today: LocalDate): LocalDate =
    today.plusDays((page - PIVOT).toLong())

private fun dateToPage(date: LocalDate, today: LocalDate): Int =
    (PIVOT + date.toEpochDay() - today.toEpochDay()).toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayLedgerScreen(
    viewModel: TodayLedgerViewModel,
    onRecordClick: () -> Unit,
    onEditEntry: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is TodayUiEvent.ShowToast ->
                    Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = dateToPage(selectedDate, today),
        pageCount = { Int.MAX_VALUE }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val date = pageToDate(page, today)
                when {
                    date.isAfter(today) -> {
                        pagerState.scrollToPage(dateToPage(today, today))
                    }
                    date != viewModel.selectedDate.value -> {
                        viewModel.setDate(date)
                    }
                }
            }
    }

    LaunchedEffect(viewModel) {
        snapshotFlow { viewModel.selectedDate.value }
            .distinctUntilChanged()
            .collect { date ->
                val target = dateToPage(date, today)
                if (pagerState.currentPage != target) {
                    pagerState.animateScrollToPage(target)
                }
            }
    }

    val dateText = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd E", Locale.CHINA))
    val titleText = if (selectedDate == today) "今日账本" else dateText

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onRecordClick) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TodayHeader(
                titleText = titleText,
                dateText = dateText,
                totalText = "已记录 ${formatDurationLong(uiState.totalMin)}",
                onDateClick = { showDatePicker = true }
            )
            HorizontalDivider()
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageDate = pageToDate(page, today)
                if (pageDate.isAfter(today)) {
                    Spacer(Modifier.fillMaxSize())
                } else if (pageDate == selectedDate) {
                    if (uiState.entries.isEmpty()) {
                        EmptyTimeline()
                    } else {
                        DayTimeline(
                            entries = uiState.entries,
                            viewModel = viewModel,
                            onTapEntry = onEditEntry
                        )
                    }
                } else {
                    Spacer(Modifier.fillMaxSize())
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
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
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { ms ->
                            viewModel.setDate(
                                Instant.ofEpochMilli(ms)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun TodayHeader(
    titleText: String,
    dateText: String,
    totalText: String,
    onDateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titleText, style = MaterialTheme.typography.headlineSmall)
            TextButton(
                onClick = onDateClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(text = dateText, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = totalText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDateClick) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = "选择日期")
        }
    }
}

@Composable
private fun EmptyTimeline() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "此日还没有记录",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "点右下角 + 记第一笔",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
