package com.example.timemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.data.TimerState
import com.example.timemanager.ui.components.HourglassDisplay
import com.example.timemanager.ui.components.ReminderButton
import com.example.timemanager.ui.components.ReminderType
import com.example.timemanager.ui.components.TagSelectionDialog
import com.example.timemanager.viewmodel.TimerViewModel

@Composable
fun HomeScreen(
    onNavigateToTimer: () -> Unit, // Navigate to Focus Screen
    onNavigateToOptions: () -> Unit,
    onNavigateToRecords: () -> Unit,
    viewModel: TimerViewModel? = null
) {
    val context = LocalContext.current
    val actualViewModel: TimerViewModel = viewModel ?: viewModel(
        viewModelStoreOwner = context.applicationContext as androidx.lifecycle.ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )

    val timerState by actualViewModel.timerState.collectAsState()
    val displaySeconds by actualViewModel.displaySeconds.collectAsState()
    val tags by actualViewModel.tags.collectAsState()
    val waterProgress by actualViewModel.waterProgress.collectAsState()
    val standProgress by actualViewModel.standProgress.collectAsState()
    
    var showTagSelection by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "TimeManager",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)
            )

            // Hourglass Control + Reminders
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Hourglass (Center)
                HourglassDisplay(
                    isRunning = timerState == TimerState.RUNNING,
                    elapsedSeconds = displaySeconds,
                    onStartClick = { showTagSelection = true },
                    onFocusClick = onNavigateToTimer,
                    onEndClick = { actualViewModel.endTask() },
                    modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.7f) // Slightly constrained width
                )
                
                // Reminders (Right Side)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ReminderButton(
                        type = ReminderType.WATER,
                        progress = waterProgress,
                        onClick = { actualViewModel.resetReminder(ReminderType.WATER) }
                    )
                    
                    ReminderButton(
                        type = ReminderType.STAND,
                        progress = standProgress,
                        onClick = { actualViewModel.resetReminder(ReminderType.STAND) }
                    )
                }
            }
        }

        // Bottom Right Action Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledIconButton(onClick = onNavigateToRecords) {
                Icon(Icons.Default.DateRange, contentDescription = "查看记录")
            }
            FilledIconButton(onClick = onNavigateToOptions) {
                Icon(Icons.Default.Settings, contentDescription = "选项")
            }
        }

        // Tag Selection Dialog
        if (showTagSelection) {
            TagSelectionDialog(
                tags = tags,
                onDismiss = { showTagSelection = false },
                onConfirm = { tag, description ->
                    actualViewModel.startTask(tag, description)
                    showTagSelection = false
                },
                onAddTag = { name -> actualViewModel.addTag(name) },
                onDeleteTag = { tag -> actualViewModel.deleteTag(tag) }
            )
        }
    }
}
