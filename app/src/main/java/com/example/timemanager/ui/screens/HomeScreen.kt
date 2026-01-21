package com.example.timemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.data.TimeRecord
import com.example.timemanager.data.TimerState
import com.example.timemanager.ui.components.*
import com.example.timemanager.viewmodel.TimerViewModel
import com.example.timemanager.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

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
    val displayTags by actualViewModel.displayTags.collectAsState()
    val otherTags by actualViewModel.otherTags.collectAsState()
    val currentSelectedTag by actualViewModel.currentSelectedTag.collectAsState()
    val waterProgress by actualViewModel.waterProgress.collectAsState()
    val standProgress by actualViewModel.standProgress.collectAsState()
    
    val pagerState = rememberPagerState(pageCount = { displayTags.size + 1 })
    var showOtherTagsMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf<TimeRecord?>(null) }
    var showTagSelection by remember { mutableStateOf(false) }

    // Sync pager with currentSelectedTag
    LaunchedEffect(displayTags, pagerState.currentPage) {
        if (pagerState.currentPage < displayTags.size) {
            actualViewModel.selectTag(displayTags[pagerState.currentPage])
        }
    }

    // Handle UI Events
    LaunchedEffect(Unit) {
        actualViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSaveRecordDialog -> {
                    showSaveDialog = event.record
                }
            }
        }
    }

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
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            // Tag Selector
            androidx.compose.animation.AnimatedVisibility(
                visible = timerState == TimerState.IDLE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentPadding = PaddingValues(horizontal = 120.dp)
                    ) { page ->
                        val isOther = page == displayTags.size
                        val tag = if (isOther) null else displayTags[page]
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (isOther) MaterialTheme.colorScheme.surfaceVariant 
                                    else Color(tag?.colorArgb ?: 0).copy(alpha = 0.15f)
                                )
                                .clickable {
                                    if (isOther) showOtherTagsMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = tag?.name ?: "其他",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOther) MaterialTheme.colorScheme.onSurfaceVariant 
                                            else Color(tag?.colorArgb ?: 0)
                                )
                                if (isOther) {
                                    Icon(
                                        Icons.Default.ArrowDropDown, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // Other Tags Dropdown/Menu
                    DropdownMenu(
                        expanded = showOtherTagsMenu,
                        onDismissRequest = { showOtherTagsMenu = false }
                    ) {
                        otherTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = {
                                    actualViewModel.selectTag(tag)
                                    showOtherTagsMenu = false
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(tag.colorArgb))
                                    )
                                }
                            )
                        }
                        if (otherTags.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("没有更多标签") },
                                onClick = { showOtherTagsMenu = false },
                                enabled = false
                            )
                        }
                    }
                    
                    // Selected Tag Indicator (if from "Other")
                    if (pagerState.currentPage == displayTags.size && currentSelectedTag != null && displayTags.none { it.name == currentSelectedTag?.name }) {
                        Text(
                            text = "已选: ${currentSelectedTag?.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

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
                        onLongPressComplete = { 
                            actualViewModel.startTask(creationType = "PRESET") 
                        },
                        text = "开始",
                        size = 200.dp
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
                tags = displayTags + otherTags, // Combine all tags
                onDismiss = { showTagSelection = false },
                onConfirm = { tag, description ->
                    actualViewModel.startTask(tag, description)
                    showTagSelection = false
                },
                onAddTag = { name -> actualViewModel.addTag(name) },
                onDeleteTag = { tag -> actualViewModel.deleteTag(tag) }
            )
        }

        // Finish Task / Save Record Dialog
        showSaveDialog?.let { record ->
            FinishTaskDialog(
                record = record,
                onDismiss = { showSaveDialog = null },
                onSave = { description ->
                    actualViewModel.addRecord(record.copy(description = description))
                    showSaveDialog = null
                },
                onDiscard = {
                    showSaveDialog = null
                }
            )
        }
    }
}
