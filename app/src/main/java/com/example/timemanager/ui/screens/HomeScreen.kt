package com.example.timemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.example.timemanager.ui.components.ReminderType
import com.example.timemanager.ui.components.StartButton
import com.example.timemanager.ui.components.TagSelectionDialog
import com.example.timemanager.ui.components.ThermometerReminder
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

            // Central Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Layer 1: Start Button (IDLE State)
                androidx.compose.animation.AnimatedVisibility(
                    visible = timerState == TimerState.IDLE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    StartButton(
                        onLongPressComplete = { showTagSelection = true }
                    )
                }

                // Layer 2: Running Controls (RUNNING State)
                // "Focus" at Top, "End" at Bottom
                androidx.compose.animation.AnimatedVisibility(
                    visible = timerState == TimerState.RUNNING,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Focus Button (Top)
                        // Split animation: Starts from center (0 offset) and moves up
                        StartButton(
                            onLongPressComplete = onNavigateToTimer, // Click to navigate (Short press logic?) - Re-using StartButton for visual consistency, but maybe just Button is better? 
                            // Request says: "Circle button smaller than Start button... Split from Start button location"
                            // "One goes up to become Focus button, one goes down to become End button"
                            modifier = Modifier
                                .align(Alignment.Center) // Start at center
                                .offset(y = (-150).dp) // Move up
                                .animateEnterExit(
                                    enter = slideInVertically(
                                        initialOffsetY = { 150 * 3 } // Start from roughly center (relative to final position)
                                        // Actually slideInVertically is relative to its own placement. 
                                        // If placed at -150dp, we want it to start at 0dp (Center). 
                                        // 0dp is +150dp down from -150dp.
                                    ) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { 150 * 3 }) + fadeOut()
                                ),
                             text = "专注",
                             size = 120.dp
                        )

                        // End Button (Bottom)
                        StartButton(
                            onLongPressComplete = { actualViewModel.endTask() },
                            modifier = Modifier
                                .align(Alignment.Center) // Start at center
                                .offset(y = 150.dp) // Move down
                                .animateEnterExit(
                                    enter = slideInVertically(initialOffsetY = { -150 * 3 }) + fadeIn(), // Start from center (up)
                                    exit = slideOutVertically(targetOffsetY = { -150 * 3 }) + fadeOut()
                                ),
                            text = "结束",
                            color = MaterialTheme.colorScheme.error,
                            size = 120.dp
                        )
                    }
                }
                
                // Reminders (Right Side) - Visible when Running
                // "Health icons: Dynamically generated after start"
                // Water to Right, Stand to Left
                androidx.compose.animation.AnimatedVisibility(
                    visible = timerState == TimerState.RUNNING,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                     Box(modifier = Modifier.fillMaxSize()) {
                        // Water (Right)
                        ThermometerReminder(
                            type = ReminderType.WATER,
                            progress = waterProgress,
                            onClick = { actualViewModel.resetReminder(ReminderType.WATER) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 24.dp)
                                .animateEnterExit(
                                    enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) + fadeIn(), // From Center (Left) to Right
                                    exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                )
                        )
                        
                        // Stand (Left)
                        ThermometerReminder(
                            type = ReminderType.STAND,
                            progress = standProgress,
                            onClick = { actualViewModel.resetReminder(ReminderType.STAND) },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 24.dp)
                                .animateEnterExit(
                                    enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + fadeIn(), // From Center (Right) to Left
                                    exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                )
                        )
                     }
                }
            }
        }

        // Bottom Right Action Buttons
        // "Entries: Bottom right circular entry for History and Settings"
        // Fade out when running
        androidx.compose.animation.AnimatedVisibility(
            visible = timerState == TimerState.IDLE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledIconButton(onClick = onNavigateToRecords) {
                    Icon(Icons.Default.DateRange, contentDescription = "查看记录")
                }
                FilledIconButton(onClick = onNavigateToOptions) {
                    Icon(Icons.Default.Settings, contentDescription = "选项")
                }
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
