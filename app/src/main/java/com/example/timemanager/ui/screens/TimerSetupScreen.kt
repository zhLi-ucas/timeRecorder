package com.example.timemanager.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.ui.activities.AmbientDisplayActivity
import com.example.timemanager.TimeManagerApplication
import com.example.timemanager.data.Task
import com.example.timemanager.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSetupScreen(
    viewModel: TimerViewModel? = null
) {
    val context = LocalContext.current
    val actualViewModel = viewModel ?: remember {
        val app = context.applicationContext as TimeManagerApplication
        ViewModelProvider(app)[TimerViewModel::class.java]
    }
    
    var tag by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf<Int?>(null) }
    
    val presetDurations = listOf(15, 30, 45, 60, 90, 120) // 分钟

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "时间管理",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = tag,
            onValueChange = { tag = it },
            label = { Text("标签") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("描述") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Text(
            text = "选择时长",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presetDurations.forEach { duration ->
                val isSelected = selectedDuration == duration
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDuration = if (isSelected) null else duration },
                    label = { Text(formatDuration(duration)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (tag.isNotBlank() && selectedDuration != null) {
                    val task = Task(
                        tag = tag,
                        description = description,
                        durationMinutes = selectedDuration!!
                    )
                    actualViewModel.startTimer(task)
                    
                    val intent = Intent(context, AmbientDisplayActivity::class.java)
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = tag.isNotBlank() && selectedDuration != null
        ) {
            Text("开始", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun formatDuration(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}分钟"
        minutes == 60 -> "1小时"
        else -> "${minutes / 60}.${(minutes % 60) / 10}小时"
    }
}

