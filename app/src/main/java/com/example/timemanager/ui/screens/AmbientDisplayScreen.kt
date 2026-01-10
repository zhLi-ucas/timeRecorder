package com.example.timemanager.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanager.ui.components.ReminderButton
import com.example.timemanager.ui.components.ReminderType
import com.example.timemanager.viewmodel.TimerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AmbientDisplayScreen(
    viewModel: TimerViewModel? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val actualViewModel: TimerViewModel = viewModel ?: viewModel(
        viewModelStoreOwner = context.applicationContext as androidx.lifecycle.ViewModelStoreOwner,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as Application)
    )

    val currentTask by actualViewModel.currentTask.collectAsState()
    val waterProgress by actualViewModel.waterProgress.collectAsState()
    val standProgress by actualViewModel.standProgress.collectAsState()

    // Current Time
    val currentTime = remember {
        mutableStateOf(getCurrentTime())
    }
    val currentDate = remember {
        mutableStateOf(getCurrentDate())
    }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime.value = getCurrentTime()
            currentDate.value = getCurrentDate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(64.dp)
        ) {
            // Left: Clock & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentTime.value,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = currentDate.value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Right: Task Info & Reminders (No Timer Text)
            if (currentTask != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    Text(
                        text = currentTask!!.tag, // Access name property
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (currentTask!!.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentTask!!.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Reminders
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        ReminderButton(
                            type = ReminderType.WATER,
                            progress = waterProgress,
                            onClick = { actualViewModel.resetReminder(ReminderType.WATER) },
                            modifier = Modifier.size(80.dp) // Larger in focus mode
                        )
                        
                        ReminderButton(
                            type = ReminderType.STAND,
                            progress = standProgress,
                            onClick = { actualViewModel.resetReminder(ReminderType.STAND) },
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "暂无任务",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

private fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault())
    return sdf.format(Date())
}
