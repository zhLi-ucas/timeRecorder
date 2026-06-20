package com.example.timemanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.timemanager.ui.components.DayTimeline
import com.example.timemanager.util.formatDurationLong
import com.example.timemanager.viewmodel.TodayLedgerViewModel
import com.example.timemanager.viewmodel.TodayUiEvent
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayLedgerScreen(viewModel: TodayLedgerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is TodayUiEvent.ShowToast ->
                    Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.insertDebugEntry() }) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TodayHeader(
                dateText = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd E", Locale.CHINA)),
                totalText = "已记录 ${formatDurationLong(uiState.totalMin)}"
            )
            HorizontalDivider()
            if (uiState.entries.isEmpty()) {
                EmptyTimeline()
            } else {
                DayTimeline(entries = uiState.entries, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun TodayHeader(dateText: String, totalText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = "今日账本", style = MaterialTheme.typography.headlineSmall)
        Text(text = dateText, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = totalText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
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
                text = "今天还没有记录",
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
