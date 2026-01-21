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
    ) {
        // --- Layer 1: Full Screen Tag Pager (Only active in IDLE state) ---
        androidx.compose.animation.AnimatedVisibility(
            visible = timerState == TimerState.IDLE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 0.dp) // Full width swipe
            ) { page ->
                val isOther = page == displayTags.size
                val tag = if (isOther) null else displayTags[page]

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Tag Display at the bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f) // Wider display
                            // 控制Tag位置：修改 bottom 的 padding 值 (例如 100.dp)
                            .padding(bottom = 200.dp)
                            .height(80.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
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
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = tag?.name ?: "其他",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                ),
                                color = if (isOther) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else Color(tag?.colorArgb ?: 0)
                            )
                            if (isOther) {
                                Icon(
                                    Icons.Default.ArrowDropDown, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Layer 2: Fixed Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header (Lowered to where the old tag selector was)
            Text(
                text = "TimeManager",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                // 控制标题位置：修改 top 的 padding 值 (例如 100.dp)
                modifier = Modifier.padding(top = 100.dp)
            )

            // Central Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .offset(y = (-100).dp) // Lowered by 100dp
                    .fillMaxWidth(),
                // 控制开始按钮位置：这里使用了 Center 居中对齐
                contentAlignment = Alignment.Center
            ) {
                // IDLE State Button
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
                        size = 220.dp // Slightly larger
                    )
                }

                // RUNNING State Controls
                androidx.compose.animation.AnimatedVisibility(
                    visible = timerState == TimerState.RUNNING,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Focus Button (Top)
                        StartButton(
                            onLongPressComplete = onNavigateToTimer,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-150).dp)
                                .animateEnterExit(
                                    enter = slideInVertically(initialOffsetY = { 150 * 3 }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { 150 * 3 }) + fadeOut()
                                ),
                             text = "专注",
                             size = 120.dp
                        )

                        // End Button (Bottom)
                        StartButton(
                            onLongPressComplete = { actualViewModel.endTask() },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 150.dp)
                                .animateEnterExit(
                                    enter = slideInVertically(initialOffsetY = { -150 * 3 }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { -150 * 3 }) + fadeOut()
                                ),
                            text = "结束",
                            color = MaterialTheme.colorScheme.error,
                            size = 120.dp
                        )
                    }
                }
                
                // Reminders
                androidx.compose.animation.AnimatedVisibility(
                    visible = timerState == TimerState.RUNNING,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                     Box(modifier = Modifier.fillMaxSize()) {
                        ThermometerReminder(
                            type = ReminderType.WATER,
                            progress = waterProgress,
                            onClick = { actualViewModel.resetReminder(ReminderType.WATER) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 24.dp)
                                .animateEnterExit(
                                    enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                                    exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                )
                        )
                        
                        ThermometerReminder(
                            type = ReminderType.STAND,
                            progress = standProgress,
                            onClick = { actualViewModel.resetReminder(ReminderType.STAND) },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 24.dp)
                                .animateEnterExit(
                                    enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                    exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                )
                        )
                     }
                }
            }
        }

        // --- Layer 3: Dropdown & Overlays ---
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
